package dhdl.compiler.ops

import scala.reflect.{Manifest,SourceContext}

import scala.virtualization.lms.internal.{Traversal, QuotingExp}
import scala.collection.mutable.HashMap

import dhdl.shared._
import dhdl.shared.ops._
import dhdl.compiler._
import dhdl.compiler.ops._

import java.io.PrintWriter
import ppl.delite.framework.Config

trait PIRGen extends Traversal with PIRCommon {
  val IR: DHDLExp with PIRScheduleAnalysisExp
  import IR._

  override val name = "PIR Generation"
  override val recurse = Always
  debugMode = SpatialConfig.debugging || SpatialConfig.pirdebug
  verboseMode = SpatialConfig.verbose || SpatialConfig.pirdebug

  val dir = sys.env("PIR_HOME") + "/apps/"
  val app = Config.degFilename.take(Config.degFilename.length - 4)
  val filename = app + ".scala"

  lazy val prescheduler = new PIRScheduleAnalyzer{val IR: PIRGen.this.IR.type = PIRGen.this.IR}
  lazy val scheduler = new PIRScheduler{val IR: PIRGen.this.IR.type = PIRGen.this.IR}
  lazy val optimizer = new PIROptimizer{val IR: PIRGen.this.IR.type = PIRGen.this.IR}
  lazy val splitter  = new PIRSplitter{val IR: PIRGen.this.IR.type = PIRGen.this.IR}
  lazy val top       = prescheduler.top
  val cus = HashMap[Exp[Any],List[ComputeUnit]]()

  def allocateCU(pipe: Exp[Any]): ComputeUnit = throw new Exception("Cannot allocate CUs during generation")

  var stream: PrintWriter = null
  var indent = 0
  def emit(x: => Any) { stream.println("  "*indent + x) }
  def open(x: => Any) { emit(x); indent += 1 }
  def close(x: => Any) { indent -= 1; emit(x) }

  override def quote(x: Exp[Any]) = x match {
    case Const(_) => quote(allocateConst(x))
    case Param(_) => quote(allocateConst(x))
    case Fixed(_) => quote(allocateConst(x))
    case Def(ConstBit(_)) => quote(allocateConst(x))
    case _ => super.quote(x)
  }

  override def run[A:Manifest](b: Block[A]) = {
    if (SpatialConfig.genCGRA) {
      stream = new PrintWriter(dir + filename)
      try {
        emitPIR(b)
      }
      catch { case e: Throwable =>
        stageWarn("Exception during PIR generation: " + e)
        if (debugMode) e.printStackTrace;
      }
      stream.flush()
      stream.close()
    }
    (b)
  }

  def emitPIR(b: Block[Any]) {
    // prescheduling
    prescheduler.run(b)
    val cuMapping = prescheduler.cuMapping
    // scheduling
    scheduler.cus ++= cuMapping.toList
    scheduler.run(b)
    // optimization
    optimizer.globals ++= (prescheduler.globals ++ scheduler.globals)
    optimizer.cuMapping ++= cuMapping.toList
    optimizer.run(b)

    splitter.globals ++= optimizer.globals
    splitter.cuMapping ++= optimizer.cuMapping.toList
    splitter.run(b)

    globals ++= splitter.globals
    cus ++= splitter.cus.toList

    debug("Scheduling complete. Generating...")
    generateHeader()
    generateGlobals()
    traverseBlock(b)
    generateFooter()
  }

  def generateHeader() {
    emit("import pir.graph._")
    emit("import pir.graph")
    emit("import pir.codegen._")
    emit("import pir.plasticine.config._")
    emit("import pir.Design")
    emit("import pir.PIRMisc._")
    emit("import pir.PIRApp")
    emit("")
    open(s"""object ${app}Design extends PIRApp {""")
    emit(s"""override val arch = Config0""")
    open(s"""def main(args: String*)(top:Top) = {""")
    //emit(s"""top = Top()""")
  }

  def generateGlobals() {
    val (mems, memCtrls) = globals.partition{case MemCtrl(_,_,_) => false; case _ => true}
    mems.foreach(emitComponent(_))
    memCtrls.foreach(emitComponent(_))
  }

  def generateFooter() {
    val args = globals.flatMap{case InputArg(name)=>Some(name); case OutputArg(name)=>Some(name); case _ => None}.mkString(", ")
    val mcs  = globals.flatMap{case MemCtrl(name,_,_)=>Some(name); case _ => None}.mkString(", ")
    //emit(s"top.updateFields(List(${cus(top.get).name}), List($args), List($mcs))")
    emit(s"")
    close("}")
    close("}")
  }

  override def traverse(lhs: Sym[Any], rhs: Def[Any]) {
    if (isControlNode(lhs) && cus.contains(lhs))
      for (cu <- cus(lhs)) generateCU(lhs, cu)
  }

  def cuDeclaration(cu: ComputeUnit) = {
    val parent = cu.parent.map(_.name).getOrElse("top")
    val deps = cu.deps.map{dep => dep.name }.mkString("List(", ", ", ")")
    cu match {
      case cu: BasicComputeUnit if cu.isUnitCompute =>
        s"""UnitComputeUnit(name ="${cu.name}", parent=$parent, deps=$deps)"""
      case cu: BasicComputeUnit =>
        s"""ComputeUnit(name="${cu.name}", parent=$parent, tpe = ${quote(cu.tpe)}, deps=$deps)"""
      case cu: TileTransferUnit =>
        s"""TileTransfer(name="${cu.name}", parent=$parent, memctrl=${quote(cu.ctrl)}, mctpe=${cu.mode}, deps=$deps, vec=${quote(cu.vec)})"""
    }
  }

  def generateCU(pipe: Exp[Any], cu: ComputeUnit, suffix: String = "") {
    debug(s"Generating CU for $pipe")
    debug(cu.dumpString)

    open(s"val ${cu.name} = ${cuDeclaration(cu)} { implicit CU => ")
    emit(s"val stage0 = CU.emptyStage")
    preallocateRegisters(cu)                // Includes scalar inputs/outputs, temps, accums
    cu.cchains.foreach(emitComponent(_))    // Allocate all counterchains
    cu.srams.foreach(emitComponent(_))      // Allocate all SRAMs
    preallocateWriteRegs(cu)                // Local write addresses

    emitAllStages(cu)

    close("}")
  }

  def quoteInCounter(reg: LocalMem) = reg match {
    case reg:ScalarIn => s"CU.scalarIn(stage0, ${quote(reg)}).out"
    case reg:ConstReg => s"""${quote(reg)}.out"""
    case _ => throw new Exception(s"Disallowed input to counter: $reg")
  }

  def emitComponent(x: Any): Unit = x match {
    case CounterChainCopy(name, owner) =>
      emit(s"""val $name = CounterChain.copy(${owner.name}, "$name")""")

    case cc@CounterChainInstance(name, ctrs) =>
      for (ctr <- ctrs) emitComponent(ctr)
      val ctrList = ctrs.map{_.name}.mkString(", ")
      var decl = s"""val $name = CounterChain(name = "$name", $ctrList)"""
      if (cc.isStreaming) decl += ".isStreaming(true)"
      emit(decl)

    case cc@UnitCounterChain(name) =>
      var decl = s"""val $name = CounterChain(name = "$name", (Const("0i"), Const("1i"), Const("1i")))"""
      if (cc.isStreaming) decl += ".isStreaming(true)"
      emit(decl)

    case CUCounter(name,start,end,stride) =>
      debug(s"Generating counter $x")
      emit(s"""val $name = (${quoteInCounter(start)}, ${quoteInCounter(end)}, ${quoteInCounter(stride)}) // Counter""")

    case sram@CUMemory(sym, size) =>
      debug(s"Generating ${sram.dumpString}")
      var decl = s"""val ${quote(sym)} = SRAM(size = $size"""
      sram.swapRead match {
        case Some(cchain) => decl += s""", swapRead = ${cchain.name}(0)"""
        case None => throw new Exception(s"No swap read controller defined for $sram")
      }
      sram.swapWrite match {
        case Some(cchain) => decl += s""", swapWrite = ${cchain.name}(0)"""
        case None => throw new Exception(s"No swap write controller defined for $sram")
      }
      sram.writeCtrl match {
        case Some(cchain) => decl += s""", writeCtr = ${cchain.name}(0)"""
        case None => throw new Exception(s"No write controller defined for $sram")
      }
      sram.banking match {
        case Some(banking) => decl += s""", banking = $banking"""
        case None => throw new Exception(s"No banking defined for $sram")
      }
      decl += s""", doubleBuffer = ${sram.isDoubleBuffer})"""

      sram.vector match {
        case Some(LocalVector) => // Nothing?
        case Some(vec) => decl += s""".wtPort(${quote(vec)})"""
        case None => throw new Exception(s"Memory $sram has no vector defined")
      }
      sram.readAddr match {
        case Some(_:CounterReg | _:ConstReg) => decl += s""".rdAddr(${quote(sram.readAddr.get)})"""
        case Some(_:ReadAddrWire) =>
        case addr => throw new Exception(s"Disallowed memory read address in $sram: $addr")
      }
      sram.writeAddr match {
        case Some(_:CounterReg | _:ConstReg) => decl += s""".wtAddr(${quote(sram.writeAddr.get)})"""
        case Some(_:WriteAddrWire | _:LocalWriteReg) =>
        case addr => throw new Exception(s"Disallowed memory write address in $sram: $addr")
      }
      emit(decl)

    case mem@MemCtrl(_,region,mode) => emit(s"val ${quote(mem)} = MemoryController($mode, ${quote(region)})")
    case mem: Offchip   => emit(s"val ${quote(mem)} = OffChip()")
    case mem: InputArg  => emit(s"val ${quote(mem)} = ArgIn()")
    case mem: OutputArg => emit(s"val ${quote(mem)} = ArgOut()")
    case mem: ScalarMem => emit(s"val ${quote(mem)} = Scalar()")
    case mem: VectorMem => emit(s"val ${quote(mem)} = Vector()")
    case _ => throw new Exception(s"Don't know how to generate CGRA component: $x")
  }

  def preallocateRegisters(cu: ComputeUnit) = cu.regs.foreach{
    case reg@TempReg(_)         => emit(s"val ${quote(reg)} = CU.temp")
    case reg@AccumReg(_,init)   => emit(s"val ${quote(reg)} = CU.accum(init = ${quote(init)})")
    //case reg@ScalarIn(_,mem)    => emit(s"val ${quote(reg)} = CU.scalarIn(${quote(mem)})")
    //case reg@ScalarOut(_,mem:OutputArg) => emit(s"val ${quote(reg)} = CU.scalarOut(${quote(mem)})")
    //case reg@ScalarOut(_,mem:ScalarMem) => emit(s"val ${quote(reg)} = CU.scalarOut(${quote(mem)})")
    case _ => // No preallocation
  }

  def preallocateWriteRegs(cu: ComputeUnit) = cu.regs.foreach{
    case reg@LocalWriteReg(mem) => emit(s"val ${quote(reg)} = CU.wtAddr(${quote(mem)})")
    case _ => //nothing
  }

  def quote(sram: CUMemory): String = sram.name
  def quote(mem: GlobalMem): String = mem match {
    case Offchip(name)     => s"${name}_oc"
    case MemCtrl(name,_,_) => s"${name}_mc"
    case InputArg(name)    => s"${name}_argin"
    case OutputArg(name)   => s"${name}_argout"
    case ScalarMem(name)   => s"${name}_scalar"
    case VectorMem(name)   => s"${name}_vector"
    case LocalVector       => "local"
  }

  def quote(tpe: ControlType) = tpe match {
    case InnerPipe => "Pipe"
    case CoarsePipe => "MetaPipeline"
    case SequentialPipe => "Sequential"
    case StreamPipe => throw new Exception("Stream pipe not yet supported in PIR")
  }

  def quote(reg: LocalMem): String = reg match {
    case ConstReg(c) => s"""Const("$c")"""                    // Constant
    case CounterReg(cchain, idx) => s"${cchain.name}($idx)"   // Counter

    case WriteAddrWire(mem) => s"${quote(mem)}.writeAddr"     // Write address wire
    case ReadAddrWire(mem)  => s"${quote(mem)}.readAddr"      // Read address wire
    case LocalWriteReg(mem) => s"wr${reg.id}"                 // Local write address register

    case ReduceReg(_)       => s"rr${reg.id}"                 // Reduction register
    case AccumReg(_,_)      => s"ar${reg.id}"                 // After preallocation
    case TempReg(_)         => s"tr${reg.id}"                 // Temporary register

    case ScalarIn(_, mem:InputArg)   => quote(mem)            // Scalar inputs from input arg
    case ScalarIn(_, mem:ScalarMem)  => quote(mem)            // Scalar inputs from CU
    case ScalarOut(_, out:OutputArg) => quote(out)            // Scalar output to output arg
    case ScalarOut(_, mem:ScalarMem) => quote(mem)            // Output to another CU
    case ScalarOut(_, mc:MemCtrl) => s"${quote(mc)}.saddr"    // Output to memory address

    case VectorIn(mem)                => quote(mem)           // Global vector read
    case InputReg(mem)                => quote(mem)           // Local vector read
    case VectorLocal(_, mem)          => quote(mem)           // Local vector write
    case VectorOut(_, vec: VectorMem) => quote(vec)           // Global vector write

    case _ => throw new Exception(s"Invalid local memory $reg")
  }

  var allocatedReduce: Set[ReduceReg] = Set.empty

  def quote(ref: LocalRef): String = ref match {
    case LocalRef(stage, reg: ConstReg)   => quote(reg)
    case LocalRef(stage, reg: CounterReg) => if (stage >= 0) s"CU.ctr(stage($stage), ${quote(reg)})" else quote(reg)

    case LocalRef(stage, wire: WriteAddrWire) => quote(wire)
    case LocalRef(stage, wire: ReadAddrWire)  => quote(wire)
    case LocalRef(stage, reg: LocalWriteReg)  => s"CU.wtAddr(stage($stage), ${quote(reg)})"

    case LocalRef(stage, reg: ReduceReg) if allocatedReduce.contains(reg) => quote(reg)
    case LocalRef(stage, reg: ReduceReg) => s"CU.reduce(stage($stage))"
    case LocalRef(stage, reg: AccumReg)  => s"CU.accum(stage($stage), ${quote(reg)})"
    case LocalRef(stage, reg: TempReg)   => s"CU.temp(stage($stage), ${quote(reg)})"

    case LocalRef(stage, reg: ScalarIn)  => s"CU.scalarIn(stage($stage), ${quote(reg)})"
    case LocalRef(stage, reg: ScalarOut) => s"CU.scalarOut(stage($stage), ${quote(reg)})"

    case LocalRef(stage, reg: VectorIn)  => s"CU.vecIn(stage($stage), ${quote(reg)})"
    case LocalRef(stage, reg: InputReg)    => if (stage >= 0) s"CU.load(stage($stage), ${quote(reg)})" else s"${quote(reg)}.load"
    case LocalRef(stage, reg: VectorLocal) => s"CU.store(stage($stage), ${quote(reg)})"
    case LocalRef(stage, reg: VectorOut)   => s"CU.vecOut(stage($stage), ${quote(reg)})"
  }

  def emitAllStages(cu: ComputeUnit) {
    var i = 1
    var r = 1
    def emitStages(stages: List[Stage]) = stages.foreach{
      case MapStage(op,inputs,outputs) =>
        val ins = inputs.map(quote(_)).mkString(", ")
        val outs = outputs.map(quote(_)).mkString(", ")
        emit(s"""Stage(stage($i), operands=List($ins), op=$op, results=List($outs))""")
        i += 1

      case ReduceStage(op,init,acc) =>
        emit(s"""val (rs$r, ${quote(acc)}) = Stage.reduce(op=$op, init=${quote(init)})""")
        allocatedReduce += acc
        r += 1
    }

    if (cu.stages.nonEmpty || cu.writeStages.exists{case (mem,stages) => stages.nonEmpty}) {
      emit(s"var stage: List[Stage] = Nil")
    }
    for ((srams,stages) <- cu.writeStages if stages.nonEmpty) {
      i = 1
      val nWrites  = stages.filter{_.isInstanceOf[MapStage]}.length
      emit(s"stage = stage0 +: WAStages(${nWrites}, ${srams.map(quote(_))})")
      emitStages(stages)
    }
    if (cu.stages.nonEmpty) {
      i = 1
      val nCompute = cu.stages.filter{_.isInstanceOf[MapStage]}.length
      emit(s"stage = stage0 +: Stages(${nCompute})")
      emitStages(cu.stages)
    }
  }
}
