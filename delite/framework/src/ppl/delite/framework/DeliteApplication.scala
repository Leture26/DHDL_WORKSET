package ppl.delite.framework

import java.io.{FileWriter, File, PrintWriter}
import scala.collection.mutable.{Map => MMap}
import scala.tools.nsc.io._
import scala.virtualization.lms.common.{BaseExp, Base}
import scala.virtualization.lms.internal.{GenericFatCodegen, ScalaCompile, GenericCodegen, ScalaCodegen, Transforming, GenerationFailedException, CCodegen, CudaCodegen}

import codegen.cpp.TargetCpp
import codegen.cuda.TargetCuda
import codegen.delite.{DeliteCodeGenPkg, DeliteCodegen, TargetDelite}
import codegen.delite.overrides.DeliteAllOverridesExp
import codegen.opencl.TargetOpenCL
import codegen.dot.TargetDot
import codegen.maxj.TargetMaxJ
import codegen.scala.TargetScala
import codegen.restage.TargetRestage
import codegen.Target
import ops.DeliteOpsExp
import transform.{DeliteTransform, DistributedArrayTransformer}

trait DeliteApplication extends DeliteOpsExp with ScalaCompile with DeliteTransform with DeliteAllOverridesExp {
  type DeliteApplicationTarget = Target{val IR: DeliteApplication.this.type}

  /*
   * code generators
   */
  def getCodeGenPkg(t: DeliteApplicationTarget) : GenericFatCodegen{val IR: DeliteApplication.this.type}

  lazy val scalaTarget = new TargetScala{val IR: DeliteApplication.this.type = DeliteApplication.this}
  lazy val cudaTarget = new TargetCuda{val IR: DeliteApplication.this.type = DeliteApplication.this}
  lazy val cppTarget = new TargetCpp{val IR: DeliteApplication.this.type = DeliteApplication.this}
  lazy val openclTarget = new TargetOpenCL{val IR: DeliteApplication.this.type = DeliteApplication.this}
  lazy val dotTarget = new TargetDot{val IR: DeliteApplication.this.type = DeliteApplication.this}
  lazy val maxjTarget = new TargetMaxJ{val IR: DeliteApplication.this.type = DeliteApplication.this}
  lazy val restageTarget = new TargetRestage{val IR: DeliteApplication.this.type = DeliteApplication.this}

  def targets = {
    var target = List[DeliteApplicationTarget](scalaTarget)
    if(Config.generateCUDA)
      target = cudaTarget :: target
    if(Config.generateCpp)
      target = cppTarget :: target
    if(Config.generateOpenCL)
      target = openclTarget :: target
    if(Config.generateDot)
      target = dotTarget :: target
    if(Config.generateMaxJ)
      target = maxjTarget :: target
    target
  }
  lazy val generators: List[GenericFatCodegen{ val IR: DeliteApplication.this.type }] = targets.reverse.map(getCodeGenPkg(_))


  // TODO: refactor, this is from ScalaCompile trait
  lazy val codegen: ScalaCodegen { val IR: DeliteApplication.this.type } =
    getCodeGenPkg(scalaTarget).asInstanceOf[ScalaCodegen { val IR: DeliteApplication.this.type }]

  // generators created by getCodeGenPkg will use the 'current' scope of the deliteGenerator as global scope
  lazy val deliteGenerator = new DeliteCodeGenPkg { val IR : DeliteApplication.this.type = DeliteApplication.this;
                                               val generators = DeliteApplication.this.generators; }


  /*
   * misc state
   */
  var stagingArgs: Array[String] = _

  var args: Rep[Array[String]] = _

  var staticDataMap: Map[String,_] = _


  final def main(args: Array[String]) {
    println("Delite Application Being Staged:[" + this.getClass.getName + "]")
    stagingArgs = args

    println("******Generating the program******")

    //clean up the code gen directory
    Directory(Path(Config.buildDir)).deleteRecursively()

    val stream =
      if (Config.degFilename == ""){
        new PrintWriter(System.out)
      }
      else {
        new PrintWriter(new FileWriter(Config.degFilename))
      }

    def writeModules(baseDir: String) {
      Directory(Path(baseDir)).createDirectory()
      val writer = new FileWriter(baseDir + "modules.dm")
      writer.write("datastructures:\n")
      writer.write("kernels:datastructures\n")
      writer.close()
    }

    // set traversals to be applied before codegen
    deliteGenerator.traversals = traversals
    //val distributedTransformer = new DistributedArrayTransformer{ val IR: DeliteApplication.this.type = DeliteApplication.this }
    //deliteGenerator.transformers :+= distributedTransformer

    deliteGenerator.emitDataStructures(Config.buildDir + File.separator)

    for (g <- generators) {
      //TODO: Remove c generator specialization
      val baseDir = Config.buildDir + File.separator + g.toString + File.separator
      writeModules(baseDir)
      g.initializeGenerator(baseDir + "kernels" + File.separator)
    }

    // Call all generators that emit a single file here
    // Generate a single source output for each generator when in debug mode
    for (g <- generators) {
      if ((Config.debug && Config.degFilename.endsWith(".deg"))) {
        val baseDir = Config.buildDir + File.separator + g.toString + File.separator
        val buildPath = baseDir + "kernels" + File.separator
        val singleStream = new PrintWriter(new FileWriter(buildPath + Config.degFilename.replace(".deg","." + g.toString)))
        g.initializeGenerator(buildPath)
        g match {
          case gen: CCodegen => singleStream.println("#include \"DeliteStandaloneMain.h\"\n")
          case _ => //
        }
        try {
          emitRegisteredSource(g, singleStream)
        } catch {
          case e: GenerationFailedException => // no generator found
            if (Config.dumpException) {
              e.printStackTrace
            }

            if (Config.strictGeneration(g.toString, e)) throw e
        }
        singleStream.close()

        // TODO: dot output
        // [raghu] Not sure what the comment above means
        //reset
      }
    }

    deliteGenerator.initializeGenerator(Config.buildDir)
    val sd = emitRegisteredSource(deliteGenerator, stream)
    deliteGenerator.finalizeGenerator()

    for (g <- generators) {
      val baseDir = Config.buildDir + File.separator + g.toString + File.separator
      g.emitDataStructures(baseDir + "datastructures" + File.separator)
      g.finalizeGenerator()
    }

    if(Config.printGlobals) {
      println("Global definitions")
      for(globalDef <- globalDefs) {
        println(globalDef)
      }
    }

    // Transfer functions not emitted currently when MaxJ code generation
    // is enabled
    if (Config.generateTransferFunctions) {
      generators foreach { _.emitTransferFunctions()}
    }
    /*
    generators foreach { g =>
      try { g.emitTransferFunctions() }
      catch {
        case e: GenerationFailedException =>
      }
    }
    */

    staticDataMap = Map() ++ sd map { case (s,d) => (deliteGenerator.quote(s), d) }
  }

  final def generateScalaSource(name: String, stream: PrintWriter) = {
    /* NOTE: we're always emitting liftedMain here and disregarding
             any use of registerFunction */
    reset
    stream.println("object "+name+"Main {"/*}*/)
    stream.println("def main(args: Array[String]) {"/*}*/)
    stream.println("val o = new "+name)
    stream.println("o.apply(args)")
    stream.println("ppl.delite.runtime.profiler.PerformanceTimer.print(\"app\")")
    stream.println(/*{*/"}")
    stream.println(/*{*/"}")
    codegen.emitSource(liftedMain, name, stream)
  }


  final def execute(args: Array[String]) {
    /* NOTE: we're always running liftedMain here and disregarding
             any use of registerFunction */
    println("Delite Application Being Executed:[" + this.getClass.getName + "]")

    println("******Executing the program*********")
    globalDefs = scala.collection.immutable.Queue.empty
    val g = compile(liftedMain)
    g(args)
  }

  /**
   * this is the entry method for our applications, user implement this method. Note, that it is missing the
   * args parameter, args are now accessed via the args field. This basically hides the notion of Reps from
   * user code
   */
  def main(): Unit

  /**
   * For multi-scope staging, to extract the return value of a scope
   */
  def mainWithResult(): Unit = main()
  var _mainResult: Unit = () //null // passes along whatever was returned by the block (could be staged or not staged, i.e. Rep[T] or T)

  def liftedMain(x: Rep[Array[String]]): Rep[Unit] = { this.args = x; val y = mainWithResult(); this._mainResult = y; this.args = null; unit(y) }

  /**
   * Used when staging a function (to be called by external code) rather than an entire app
  */

  def registerFunction[A:Manifest,R:Manifest](func: Rep[A] => Rep[R]) = {
    stagedFunc = func
    arity = 1
    fm = List(manifest[A],manifest[R])
  }

  def registerFunction[A:Manifest,B:Manifest,R:Manifest](func: (Rep[A],Rep[B]) => Rep[R]) = {
    stagedFunc = func
    arity = 2
    fm = List(manifest[A],manifest[B],manifest[R])
  }

  def registerFunction[A:Manifest,B:Manifest,C:Manifest,R:Manifest](func: (Rep[A],Rep[B],Rep[C]) => Rep[R]) = {
    stagedFunc = func
    arity = 3
    fm = List(manifest[A],manifest[B],manifest[C],manifest[R])
  }

  def registerFunction[A:Manifest,B:Manifest,C:Manifest,D:Manifest,R:Manifest](func: (Rep[A],Rep[B],Rep[C],Rep[D]) => Rep[R]) = {
    stagedFunc = func
    arity = 4
    fm = List(manifest[A],manifest[B],manifest[C],manifest[D],manifest[R])
  }

  def registerFunction[A:Manifest,B:Manifest,C:Manifest,D:Manifest,E:Manifest,R:Manifest](func: (Rep[A],Rep[B],Rep[C],Rep[D],Rep[E]) => Rep[R]) = {
    stagedFunc = func
    arity = 5
    fm = List(manifest[A],manifest[B],manifest[C],manifest[D],manifest[E],manifest[R])
  }

  def emitRegisteredSource(gen: GenericFatCodegen{val IR: DeliteApplication.this.type}, stream: PrintWriter): List[(Sym[Any], Any)] = arity match {
    case 1 => gen.emitSource(stagedFunc.asInstanceOf[Rep[Any]=>Rep[Any]], functionName, stream)(fm(0),fm(1))
    case 2 => gen.emitSource2(stagedFunc.asInstanceOf[(Rep[Any],Rep[Any])=>Rep[Any]], functionName, stream)(fm(0),fm(1),fm(2))
    case 3 => gen.emitSource3(stagedFunc.asInstanceOf[(Rep[Any],Rep[Any],Rep[Any])=>Rep[Any]], functionName, stream)(fm(0),fm(1),fm(2),fm(3))
    case 4 => gen.emitSource4(stagedFunc.asInstanceOf[(Rep[Any],Rep[Any],Rep[Any],Rep[Any])=>Rep[Any]], functionName, stream)(fm(0),fm(1),fm(2),fm(3),fm(4))
    case 5 => gen.emitSource5(stagedFunc.asInstanceOf[(Rep[Any],Rep[Any],Rep[Any],Rep[Any],Rep[Any])=>Rep[Any]], functionName, stream)(fm(0),fm(1),fm(2),fm(3),fm(4),fm(5))
    case _ => throw new RuntimeException("Unsupported function arity for emitSource: " + arity)
  }

  private var stagedFunc: Any = (liftedMain _)
  private var fm: List[Manifest[Any]] = List(manifest[Array[String]],manifest[Unit]).asInstanceOf[List[Manifest[Any]]]
  private var arity = 1

}
