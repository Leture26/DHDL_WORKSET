package pir
package pass

import pir.node._
import scala.collection.mutable

class RouteThroughElimination(implicit compiler:PIR) extends PIRTransformer with BFSBottomUpTopologicalTraversal with UnitTraversal {
  import pirmeta._

  val forward = false

  def matchType(mem:Memory, rmem:Memory) = (isReg(mem) == isReg(rmem)) && (isFIFO(mem) == isFIFO(rmem))
  override def visitNode(n:N, prev:T):T = {
    n match {
      // Pattern if write is inside mem CU
      case Def(rwrite:LocalStore, LocalStore(mem::Nil,None,Def(rread, LocalLoad(WithWriter(Def(write, LocalStore(rmem::Nil, None, data)))::Nil,None)))) if matchType(mem, rmem) =>
        dbgblk(s"Found Route Through ${qdef(write)}") {
          dbgs(s"pattern: data -> wirte -> rmem -> rread -> rwrite -> mem => data -> write -> mem")
          dbg(s"data:${qdef(data)}")
          dbg(s"write:${qdef(write)}")
          dbg(s"rmem:${qdef(rmem)}")
          dbg(s"rread:${qdef(rread)}") //TODO rread might connect to multiple rwrite
          dbg(s"rwrite:${qdef(rwrite)}")
          dbg(s"mem:${qdef(mem)}")
          val memCU = globalOf(mem).get
          //disconnect(write, rmem)
          disconnect(mem, rwrite)
          val mwrite = withParent(memCU) {
            mirror(write, init=Map(rmem -> mem, data -> data))
          }
          //swapConnection(mem, rwrite.out, write.out)
          //swapParent(write, memCU)
        }
      case Def(rread:LocalLoad, LocalLoad((rmem@WithWriter(Def(rwrite, LocalStore(_, None, Def(read, LocalLoad(mem, None))))))::Nil, None)) if isFIFO(rmem) & ctrlOf(rread) == ctrlOf(read) =>
        dbgblk(s"Found Route Through ${qdef(rread)}") {
          dbgs(s"pattern: write -> mem -> read -> rwrite -> rmem -> rread => write -> mem.clone => read.clone")
          dbg(s"mem:${qdef(mem)}")
          dbg(s"read:${qdef(read)}")
          dbg(s"rwrite:${qdef(rwrite)}")
          dbg(s"rmem:${qdef(rmem)}")
          dbg(s"rread:${qdef(rread)}")
          val memCU = globalOf(rmem).get
          val mread = withParent(memCU) {
            mirror(read)
          }
          rread.deps.foreach { dep =>
            swapUsage(from=rread, to=mread)
          }
        }
      // Pattern if write is inside writer CU
      //case Def(rwrite:LocalStore, LocalStore(mems,None,Def(rread, LocalLoad(WithWriter(Def(write, LocalStore(rmem::Nil, None, data)))::Nil,None)))) =>
        //dbgblk(s"Found Route Through ${qdef(write)}") {
          //dbgs(s"pattern: data -> rwirte -> rmem -> rread -> write -> mem => data -> write -> mems")
          //dbg(s"data:${qdef(data)}")
          //dbg(s"rwrite:${qdef(rwrite)}")
          //dbg(s"rmem:${qdef(rmem)}")
          //dbg(s"rread:${qdef(rread)}")
          //dbg(s"write:${qdef(write)}")
          //dbg(s"mems:${qdef(mems)}")
          //mems.foreach { mem =>
            //swapConnection(mem, rwrite.out, write.out)
          //}
        //}
      case _ => dbg(s"unmatched ${qdef(n)}")
    }
    super.visitNode(n, prev)
  }

}

