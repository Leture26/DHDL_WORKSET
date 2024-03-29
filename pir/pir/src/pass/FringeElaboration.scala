package pir
package pass

import pir.node._

class FringeElaboration(implicit compiler:PIR) extends PIRTransformer with SiblingFirstTraversal with UnitTraversal with ConstantPropogator {
  import pirmeta._

  lazy val argFringe = top.argFringe
  lazy val argInCtrl = argFringe.argInController
  lazy val argOutCtrl = argFringe.argOutController
  lazy val topController = compiler.top.topController

  override def visitNode(n:N) = dbgblk(s"visitNode($n)") { 
    n match {
      case n:Top =>
        n.argFringe // HACK: create lazy value
        super.visitNode(n)
      case n:ArgIn => transformInMem(n)
      case n:DramAddress => transformInMem(n)
      case n:LUT => transformInMem(n)
      case n:ArgOut => transformOutMem(n)
      case n:TokenOut => transformOutMem(n)
      case n@FringeDenseLoad(dram,cmdStream,dataStream) =>
        dram.foreach { dram => transformDram(dram) }
        transformDense(n, cmdStream, dataStream)
      case n@FringeSparseLoad(dram,addrStream,dataStream) =>
        dram.foreach { dram => transformDram(dram) }
        transformSparse(n, addrStream, dataStream)
      case n@FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
        dram.foreach { dram => transformDram(dram) }
        transformDense(n, cmdStream ++ dataStream, ackStream)
        insertCountAck(n, dataStream.head, ackStream.head)
      case n@FringeSparseStore(dram,cmdStream,ackStream) =>
        dram.foreach { dram => transformDram(dram) }
        transformSparse(n, cmdStream, ackStream)
        insertCountAck(n, cmdStream.head, ackStream.head)
      case n:StreamIn => transformStreamIn(n)
      case n:StreamOut => transformStreamOut(n)
      case _ => super.visitNode(n)
    }
  }

  def transformDram(dram:DRAM) = {
    val dimsBounds = dram.dims.map { 
      case Const(c) => Some(c)
      case Def(n, LocalLoad(mem::Nil, None)) if boundOf.contains(mem) =>
        Some(boundOf(mem))
      case d => None
    }
    if (dimsBounds.forall(_.nonEmpty)) {
      staticDimsOf(dram) = dimsBounds.map(_.get.asInstanceOf[Int])
    }
  }

  def transformInMem(n:Memory) = {
    n match {
      case n:ArgIn =>
        withParentCtrl(argFringe, argInCtrl) {
          val argInDef = ArgInDef()
          WriteMem(n, argInDef)
          boundOf.get(n).foreach { b => boundOf(argInDef) = b }
        }
      case n:DramAddress => 
        withParentCtrl(argFringe, argInCtrl) {
          val argInDef = ArgInDef()
          WriteMem(n, argInDef)
          boundOf.get(n).foreach { b => boundOf(argInDef) = b }
        }
      case n:LUT =>
        val tokenIn = withParent(top) {
          TokenIn()
        }
        withParentCtrl(argFringe, argInCtrl) {
          WriteMem(tokenIn, argFringe.tokenInDef)
        }
        withParentCtrl(top, topController) {
          withCtrl(UnitController(SeqPipe, InnerControl)) {
            ResetMem(n, ReadMem(tokenIn))
          }
        }
    }
  }

  def transformOutMem(n:Memory) = {
    val readMem = withParentCtrl(argFringe, argOutCtrl) {
      ReadMem(n)
    }
    argFringe.hostRead.connect(readMem.out)
  }

  def transformDense(fringe:DramFringe, streamOuts:List[StreamOut], streamIns:List[StreamIn]) = {
    val size = fringe.collectDown[StreamOut]().filter { _.field == "size" }.head
    val csize = getBoundOf(size).asInstanceOf[Option[Int]]
    val par = fringe match {
      case FringeDenseLoad(dram,cmdStream,dataStream) =>
        getParOf(ctrlOf(readersOf(dataStream.head).head))
      case FringeDenseStore(dram,cmdStream,dataStream,ackStream) =>
        getParOf(ctrlOf(writersOf(dataStream.head).head))
    }
    withParentCtrl(fringe, ctrlOf(fringe)) {
      fringe match {
        case FringeDenseLoad(dram::Nil,cmdStream,dataStream) =>
          val offset = streamOuts.filter { _.field=="offset"}.head
          val size = streamOuts.filter { _.field=="size"}.head
          val data = streamIns.filter { _.field=="data"}.head
          withCtrl(UnitController(SeqPipe, OuterControl)) {
            val offsetRead = ReadMem(offset)
            val sizeRead = ReadMem(size)
            withCtrl(DramController(csize, par)) {
              val pdc = ProcessDramDenseLoad(dram, offsetRead, sizeRead)
              WriteMem(data, pdc)
            }
          }
        case FringeDenseStore(dram::Nil,cmdStream,dataStream,ackStream) =>
          val offset = streamOuts.filter { _.field=="offset"}.head
          val size = streamOuts.filter { _.field=="size"}.head
          val data = streamOuts.filter { _.field=="data"}.head
          val ack = streamIns.filter { _.field=="ack"}.head
          withCtrl(UnitController(SeqPipe, OuterControl)) {
            val offsetRead = ReadMem(offset)
            val sizeRead = ReadMem(size)
            withCtrl(DramController(csize, par)) {
              val dataRead = ReadMem(data)
              val pdc = ProcessDramDenseStore(dram, offsetRead, sizeRead, dataRead)
              WriteMem(ack, pdc)
            }
          }
      }
    }
  }

  def transformSparse(fringe:DramFringe, streamOuts:List[StreamOut], streamIns:List[StreamIn]) = {
    withParentCtrl(fringe, ctrlOf(fringe)) {
      withCtrl(ForeverController(level=InnerControl)) {
        fringe match {
          case FringeSparseLoad(dram::Nil, addrStream, dataStream) =>
            val addr = streamOuts.filter { _.field=="addr"}.head
            val data = streamIns.filter { _.field=="data"}.head
            val addrRead = ReadMem(addr)
            val pdc = ProcessDramSparseLoad(dram, addrRead)
            WriteMem(data, pdc)
          case FringeSparseStore(dram::Nil, cmdStream, ackStream) =>
            val ack = streamIns.filter { _.field=="ack"}.head
            val addr = cmdStream(0)
            val data = cmdStream(1)
            val addrRead = ReadMem(addr)
            val dataRead = ReadMem(data)
            val pdc = ProcessDramSparseStore(dram, addrRead, dataRead)
            WriteMem(ack, pdc)
        }
      }
    }
  }

  def insertCountAck(fringe:DramFringe, dataStream:StreamOut, ackStream:StreamIn) = {
    val dataCtrl = ctrlOf(writersOf(dataStream).head)
    withParentCtrl(top, dataCtrl) {
      val cu = CUContainer().setParent(top)
      withParent(CUContainer()) {
        val reader = ReadMem(ackStream)
        val count = CountAck(reader)
        val mem = withParent(argFringe) { withCtrl(argOutCtrl) { TokenOut() } }
        val writer = WriteMem(mem, count)
      }
    }
  }

  def transformStreamIn(streamIn:StreamIn) = {
    withParent(top) {
      withParentCtrl(FringeStreamIn(streamIn), topController) {
        val innerCtrl = ForeverController(level=InnerControl)
        withCtrl(innerCtrl) {
          val streamInDef = StreamInDef()
          val count = countOf.get(streamIn).getOrElse(None)
          countOf(innerCtrl) = count
          countOf(streamInDef) = count
          WriteMem(streamIn, streamInDef)
        }
      }
    }
  }

  def transformStreamOut(streamOut:StreamOut) = {
    withParent(top) {
      val fringe = FringeStreamOut(streamOut)
      withParent(fringe) {
        withCtrl(topController) {
          withCtrl(ForeverController(level=InnerControl)) {
            val load = ReadMem(streamOut)
            val processStreamOut = ProcessStreamOut(load)
          }
        }
      }
    }
  }

}
