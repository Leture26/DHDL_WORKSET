package scala.virtualization.lms
package internal

import util.GraphUtil
import java.io.{File, PrintWriter}
import scala.reflect.RefinedManifest

trait QuotingExp {
  val IR: Blocks
  import IR._
  // TODO: Can probably just use toString for all types here, throwing an exception now to catch
  // any unexpected calls to this function
  def quote(x: Any): String = x match {
    case x: Int => x.toString
    case x: Long => x.toString
    case x: Float => x.toString
    case x: Double => x.toString
    case x: String => x
    case x: Boolean => x.toString
    case x: Exp[_] => quote(x)
    case _ => throw new RuntimeException("could not quote " + x)
  }

  def quote(x: Exp[Any]) : String = x match {
    case Const(s: String) => "\""+s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")+"\"" // TODO: more escapes?
    case Const(f: Float) => f+"f"
    case Const(c: Char) => "'"+(""+c).replace("'", "\\'").replace("\n", "\\n")+"'"
    case Const(z) => z.toString
    case Param(x) => quote(Const(x))  // Quote as if it was defined as a constant
    case Sym(n) => "x"+n
    case _ => throw new RuntimeException("could not quote " + x)
  }
}

trait GenericCodegen extends BlockTraversal with QuotingExp {
  val IR: Blocks
  import IR._

  /** these methods support a kernel model of execution and are only used by Delite, should be moved into Delite only? **/
  def deviceTarget: Targets.Value = throw new Exception("deviceTarget is not defined for this codegen.")
  def hostTarget: Targets.Value = Targets.getHostTarget(deviceTarget)
  def isAcceleratorTarget: Boolean = hostTarget != deviceTarget

  def kernelInit(syms: List[Sym[Any]], vals: List[Sym[Any]], vars: List[Sym[Any]], resultIsVar: Boolean): Unit = {}
  def emitKernel(syms: List[Sym[Any]], rhs: Any): Unit = { throw new Exception("emitKernel not implemented!") } 
  def emitKernelHeader(syms: List[Sym[Any]], vals: List[Sym[Any]], vars: List[Sym[Any]], resultType: String, resultIsVar: Boolean, external: Boolean, isMultiLoop: Boolean): Unit = {}
  def emitKernelFooter(syms: List[Sym[Any]], vals: List[Sym[Any]], vars: List[Sym[Any]], resultType: String, resultIsVar: Boolean, external: Boolean, isMultiLoop: Boolean): Unit = {}

  def emitTransferFunctions(): Unit = {}

  def resourceInfoType = ""
  def resourceInfoSym = ""

  /******/

  def fileExtension = ""
  def emitFileHeader(): Unit = {}
  def emitFileFooter(): Unit = {}

  def initializeGenerator(buildDir:String): Unit = { }
  def finalizeGenerator(): Unit = {}

  def emitDataStructures(stream: PrintWriter): Unit = {}
  def emitDataStructures(path: String): Unit = {}
  def getDataStructureHeaders(): String = ""

  // Define if code generation should produce everything
  // in a single file. Defaults to generating multiple files (one per kernel)
  def emitSingleFile(): Boolean = false

  def singleFileName = s"UnnamedSingleFile.$fileExtension"

  private var kernelFile: Option[File] = None
  private var kernelStream: Option[PrintWriter] = None
  def getFileStream(fileName: String = ""): (File, PrintWriter) = {
    if (emitSingleFile) {
      if (!kernelFile.isDefined) {
        kernelFile = Some(new File(fileName))
        kernelStream = Some(new PrintWriter(kernelFile.get))
        Console.println(s"""[getFileStream] Created file $fileName""")
      }
      (kernelFile.get, kernelStream.get)
    } else {
      val outFile = new File(fileName)
      val kStream = new PrintWriter(outFile)
      (outFile, kStream)
    }
  }

  def dataPath = {
    "data" + java.io.File.separator
  }

  def symDataPath(sym: Sym[Any]) = {
    dataPath + sym.id
  }

  def emitData(sym: Sym[Any], data: Seq[Any]) {
    val outDir = new File(dataPath)
    outDir.mkdirs()
    val outFile = new File(symDataPath(sym))
    val stream = new PrintWriter(outFile)

    for(v <- data) {
      stream.println(v)
    }

    stream.close()
  }

  // optional type remapping (default is identity)
  def remap(s: String): String = s
  def remap[A](s: String, method: String, t: Manifest[A]) : String = remap(s, method, t.toString)
  def remap(s: String, method: String, t: String) : String = s + method + "[" + remap(t) + "]"
  def remap[A](m: Manifest[A]): String = m match {
    case rm: RefinedManifest[A] =>  "AnyRef{" + rm.fields.foldLeft(""){(acc, f) => {val (n,mnf) = f; acc + "val " + n + ": " + remap(mnf) + ";"}} + "}"
    case _ if m.erasure == classOf[Variable[Any]] =>
        remap(m.typeArguments.head)
    case _ =>
      // call remap on all type arguments
      val targs = m.typeArguments
      if (targs.length > 0) {
        val ms = m.toString
        ms.take(ms.indexOf("[")+1) + targs.map(tp => remap(tp)).mkString(", ") + "]"
      }
      else m.toString
  }
  def remapImpl[A](m: Manifest[A]): String = remap(m)
  //def remapVar[A](m: Manifest[Variable[A]]) : String = remap(m.typeArguments.head)

  def remapHost[A](m: Manifest[A]): String = remap(m).replaceAll(deviceTarget.toString,hostTarget.toString)

  def hasMetaData: Boolean = false
  def getMetaData: String = null

  // ---------

  protected var _stream: PrintWriter = _
  def stream = _stream
  def stream_=(s: PrintWriter) { _stream = s }

  def withStream[A](out: PrintWriter)(body: => A): A = {
    val save = stream
    stream = out
    try { body } finally { stream.flush; stream = save }
  }

  // ----------

  override def traverseStm(stm: Stm) = stm match {
    case TP(sym, rhs) => emitNode(sym,rhs)
    case _ => throw new GenerationFailedException(this.toString + ": don't know how to generate code for statement: " + stm)
  }

  def emitBlock(y: Block[Any]): Unit = traverseBlock(y)

  def emitNode(sym: Sym[Any], rhs: Def[Any]): Unit = {
    throw new GenerationFailedException(this.toString + ": don't know how to generate code for: " + rhs)
  }

  def emitValDef(sym: Sym[Any], rhs: String): Unit

  def preProcess[A : Manifest](body: Block[A]): Unit = { }
  def postProcess[A : Manifest](body: Block[A]): Unit = { }

  def emitSource[T : Manifest, R : Manifest](f: Exp[T] => Exp[R], className: String, stream: PrintWriter): List[(Sym[Any], Any)] = {
    val s = fresh[T]
    val body = reifyBlock(f(s))
    emitSource(List(s), body, className, stream)
  }

  def emitSource2[T1 : Manifest, T2 : Manifest, R : Manifest](f: (Exp[T1], Exp[T2]) => Exp[R], className: String, stream: PrintWriter): List[(Sym[Any], Any)] = {
    val s1 = fresh[T1]
    val s2 = fresh[T2]
    val body = reifyBlock(f(s1, s2))
    emitSource(List(s1, s2), body, className, stream)
  }

  def emitSource3[T1 : Manifest, T2 : Manifest, T3 : Manifest, R : Manifest](f: (Exp[T1], Exp[T2], Exp[T3]) => Exp[R], className: String, stream: PrintWriter): List[(Sym[Any], Any)] = {
    val s1 = fresh[T1]
    val s2 = fresh[T2]
    val s3 = fresh[T3]
    val body = reifyBlock(f(s1, s2, s3))
    emitSource(List(s1, s2, s3), body, className, stream)
  }

  def emitSource4[T1 : Manifest, T2 : Manifest, T3 : Manifest, T4 : Manifest, R : Manifest](f: (Exp[T1], Exp[T2], Exp[T3], Exp[T4]) => Exp[R], className: String, stream: PrintWriter): List[(Sym[Any], Any)] = {
    val s1 = fresh[T1]
    val s2 = fresh[T2]
    val s3 = fresh[T3]
    val s4 = fresh[T4]
    val body = reifyBlock(f(s1, s2, s3, s4))
    emitSource(List(s1, s2, s3, s4), body, className, stream)
  }

  def emitSource5[T1 : Manifest, T2 : Manifest, T3 : Manifest, T4 : Manifest, T5 : Manifest, R : Manifest](f: (Exp[T1], Exp[T2], Exp[T3], Exp[T4], Exp[T5]) => Exp[R], className: String, stream: PrintWriter): List[(Sym[Any], Any)] = {
    val s1 = fresh[T1]
    val s2 = fresh[T2]
    val s3 = fresh[T3]
    val s4 = fresh[T4]
    val s5 = fresh[T5]
    val body = reifyBlock(f(s1, s2, s3, s4, s5))
    emitSource(List(s1, s2, s3, s4, s5), body, className, stream)
  }

  /**
   * @param args List of symbols bound to `body`
   * @param body Block to emit
   * @param className Name of the generated identifier
   * @param stream Output stream
   */
  def emitSource[A : Manifest](args: List[Sym[_]], body: Block[A], className: String, stream: PrintWriter): List[(Sym[Any], Any)] // return free static data in block

  // ----------

  override def reset {
    stream = null
    super.reset
  }

  def isPrimitiveType[A](m: Manifest[A]) : Boolean = {
    m.toString match {
      case "Boolean" | "Byte" | "Char" | "Short" | "Int" | "Long" | "Float" | "Double" => true
      case _ => false
    }
  }

  def isVoidType[A](m: Manifest[A]) : Boolean = {
    m.toString match {
      case "Unit" => true
      case _ => false
    }
  }

  def isVariableType[A](m: Manifest[A]) : Boolean = {
    if(m.erasure == classOf[Variable[_]]) true
    else false
  }

  // Provides automatic quoting and remapping in the gen string interpolater
  implicit class CodegenHelper(sc: StringContext) {
    def printToStream(arg: Any): Unit = {
      stream.print(quoteOrRemap(arg))
    }

    def quoteOrRemap(arg: Any): String = arg match {
      case xs: Seq[_] => xs.map(quoteOrRemap).mkString(",")
      case e: Exp[_] => quote(e)
      case m: Manifest[_] => remap(m)
      case s: String => s
      case _ => throw new RuntimeException(s"Could not quote or remap $arg")
    }

    // First line of a part of the context may contain
    // a | and therefore should not be stripped
    def stripContextPart(part: String): String = {
      val lines = part.linesWithSeparators
      if (!lines.hasNext) part
      else lines.next + (lines.foldLeft("")(_+_).stripMargin)
    }

    def src(args: Any*): String = {
      sc.raw(args.map(quoteOrRemap): _*).stripMargin
    }

    def gen(args: Any*): Unit = {
      sc.checkLengths(args)
      val start :: contextStrings = sc.parts.iterator.toList
      printToStream(start.stripMargin)
      for ((arg, contextString) <- args zip contextStrings) {
        printToStream(arg)
        printToStream(stripContextPart(contextString))
      }
      stream.println()
    }
  }
}



trait GenericNestedCodegen extends NestedBlockTraversal with GenericCodegen {
  val IR: Expressions with Effects
  import IR._

  override def traverseStm(stm: Stm) = super[GenericCodegen].traverseStm(stm)

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
//    case Read(s) =>
//      emitValDef(sym, quote(s))
    case Reflect(s, u, effects) =>
      emitNode(sym, s)
    case Reify(s, u, effects) =>
      // just ignore -- effects are accounted for in emitBlock
    case _ => super.emitNode(sym, rhs)
  }

  case class NestedBlock(b: Block[Any])
  def nestedBlock(b: Block[Any]) = NestedBlock(b)

  // Allows the gen string interpolator to perform emitBlock when passed a Block
  implicit class NestedCodegenHelper(sc: StringContext) extends CodegenHelper(sc) {

    override def printToStream(arg: Any): Unit = arg match {
      case NestedBlock(b) => emitBlock(b)
      case b: Block[_] => stream.print(quoteOrRemap(getBlockResult(b)))
      case _ => stream.print(quoteOrRemap(arg))
    }
  }

}
