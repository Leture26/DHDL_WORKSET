package pir
package codegen

import pir.node._

class SimpleIRDotCodegen(override val fileName:String)(implicit compiler:PIR) extends PIRIRDotCodegen(fileName) {

  import pirmeta._

  override val horizontal:Boolean = false

  override def color(attr:DotAttr, n:Any) = n match {
    case n:DramFringe => attr.fillcolor("lightseagreen").style(filled)
    case n:CUContainer if isPMU(n) => attr.fillcolor(chartreuse).style(filled)
    case n:CUContainer if isDAG(n) => attr.fillcolor("deeppink").style(filled)
    case n:CUContainer if isSCU(n) => attr.fillcolor("gold").style(filled)
    case n:CUContainer if isOCU(n) => attr.fillcolor("darkorange1").style(filled)
    case n:CUContainer if isPCU(n) => attr.fillcolor("deepskyblue").style(filled)
    case n => super.color(attr,n)
  }

  override def emitNode(n:N) = {
    n match {
      //case g:ArgFringe => super.visitNode(n)
      case g:GlobalContainer => emitSingleNode(n)
      case _ => super.visitNode(n)
    }
  }

  override def emitEdge(from:prism.node.Output[N], to:prism.node.Input[N], attr:DotAttr):Unit = {
    dbg(s"edge:${from.src}.$from -> ${to.src}.$to")
    val fromPinType = pinTypeOf(from.src)
    val toPinType = pinTypeOf(to.src)
    dbg(s"from:${from.src}[$fromPinType], to:${to.src}[$toPinType]")
    //(from.src, to.src.asInstanceOf[N]) match {
      //case (fromsrc, Def(tosrc, LocalAccess(_, Some(addrs)))) if (addrs.contains(fromsrc)) =>
      //case (fromsrc:Memory, tosrc:LocalAccess) =>
      //case (fromsrc:LocalAccess, tosrc:Memory) =>
      //case (fromsrc, tosrc) =>
        //assert(fromPinType == toPinType, s"from:${fromsrc}[$fromPinType], to:${tosrc}[$toPinType]")
    //}
    fromPinType match {
      case tp if isBit(tp) => attr.set("style", "dashed").set("color","red").label(s"${from.src.id}")
      case tp if isWord(tp) => attr.set("style", "solid").label(s"${from.src.id}")
      case tp if isVector(tp) => attr.set("style", "bold").set("color","sienna").label(s"${from.src.id}")
    }
    super.emitEdge(from, to, attr)
  }

  override def emitEdgeMatched(from:N, to:N, attr:DotAttr):Unit = {
    (lift(from), lift(to)) match {
      case (Some(from:ArgFringe), _) =>
      case (_, _) => super.emitEdgeMatched(from, to, attr)
    }
  }

  override def label(attr:DotAttr, n:Any) = {
    var label = super.label(attr, n).label.get
    n match {
      case n:GlobalContainer => 
        label += s"\ncuType=${cuType(n).get}"
        val accums = n.children.collect { case mem:Memory if isRemoteMem(mem) && isAccum(mem) => mem }
        if (accums.nonEmpty) {
          label += s"\nisAccum=true"
        }
      case n =>
    }
    attr.label(label)
  }

}

