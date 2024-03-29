package pir
package codegen

import pir.node._
import pir.mapper._

import prism.collection.mutable._

class PlastisimDotCodegen(fileName:String)(implicit compiler: PIR) extends PIRIRDotCodegen(fileName) with PlastisimCodegen with RoutingUtil {
  import pirmeta._
  import spademeta._

  override def runPass = {
    super.runPass // traverse dataflow graph and call emitNode on each node
    linkGroupOf.values.toSet.foreach { link => emitLink(link) }
  }

  override def emitNode(n:N) = n match {
    case n:NetworkNode if ctrlOf(n).isInstanceOf[ArgInController] => super.visitNode(n)
    case n:NetworkNode => emitSingleNode(n)
    case n:ArgFringe => super.visitNode(n)
    case n:GlobalContainer => emitSubGraph(n) { super.visitNode(n) }
    case n => super.visitNode(n)
  }

  override def color(attr:DotAttr, n:Any) = n match {
    case n:NetworkNode if activeOf.contains(n) & countOf.contains(n) => 
      checkActive(n).map { case (active, expected) =>
        if (active == 0) attr.fillcolor("red").style(filled)
        else if (active < expected) {
          finalStateOf.get(n).map {
            case "STARVE" => attr.fillcolor("orangered").style(filled)
            case "STALL" => attr.fillcolor("orange").style(filled)
            case "BOTH" => attr.fillcolor("orangered").style(filled)
          }.getOrElse(attr.fillcolor("orange").style(filled))
        } else {
          // Generate Green between 100 to 255
          val G = Math.round((1 - activeRateOf(n) / 100) * (255 - 50) + 50).toInt
          var HG = G.toHexString
          while (HG.size < 2) HG = "0" + HG
          attr.fillcolor(s"#00${HG}00").style(filled)
        }
      }.getOrElse(super.color(attr, n))
    case n => super.color(attr, n)
  }

  override def label(attr:DotAttr, n:Any) = n match {
    case n:ContextEnable =>
      var label = ""
      label += s"${quote(n)}"
      label += s"\nctx=${quote(contextOf(n).get)}"
      label += s"\nsrcCts=${quote(srcCtxOf.get(contextOf(n).get).getOrElse(""))}"
      label += s"\nctrl=${ctrlOf(n)}"
      inMemsOf(n).foreach { 
        case (mem:ArgIn, reads) =>
          label += s"\n$mem"
          getScaleOf(reads).foreach { sin => label += s" sin=$sin" }
          bufferSizeOf(mem).foreach { bs => label += s" bs=$bs" }
        case _ =>
      }
      startAtToken(n).foreach { token => label += s"\nstart_at_tokens=$token" }
      stopAfterToken(n).foreach { token => label += s"\nstop_after_tokens=$token" }
      countOf(n).foreach { count => label += s"\ncount=$count" }
      zipOption(activeOf.get(n), activeRateOf.get(n)).foreach { case (active, rate) => label += s"\nactive=$active ($rate %)" }
      stallRateOf.get(n).foreach { stalled => label += s"\nstalled=$stalled %" }
      starveRateOf.get(n).foreach { starved => label += s"\nstarved=$starved %" }
      finalStateOf.get(n).foreach { state => label += s"\nstate=$state" }
      val cuP = globalOf(n).get
      cuP match {
        case cuP:DramFringe if PIRConfig.enableTrace =>
        case cuP:ArgFringe =>
        case cuP =>
          latencyOf(n).foreach { lat => label += s"\nlat = $lat" }
      }
      if (spade.node.isDynamic(topS)) {
        addrOf(n).foreach { addr => label += s"\naddr=${addr}" }
      }
      attr.label(label)
    case n => super.label(attr, n)
  }

  def emitLink(n:Link) = dbgblk(s"emitLink(${quote(n)})") {
    val srcMap = srcsOf(n)
    val dstMap = dstsOf(n)
    val srcs:List[NetworkNode] = srcMap.values.flatMap { _.keys }.toSet.toList
    val dsts:List[NetworkNode] = dstMap.values.flatMap { _.keys }.toSet.toList
    val count = assertOptionUnify(n, "count") { mem => countOf.getOrElse(mem, None) }

    val from = goutOf(n)
    n.foreach { mem =>
      val bs = bufferSizeOf(mem)
      srcMap(mem).foreach { case (src, ias) =>
        val lat = staticLatencyOf(src, mem)
        val sout = getScaleOf(ias)
        dstMap(mem).foreach { case (dst, oas) =>
          val sin = getScaleOf(oas)
          var label = s"$mem"
          from.foreach{ from => label += s"\nid=${from.id}" } 
          sout.foreach { sout => label += s"\nsout=$sout" }
          count.foreach { count => label += s"\ncount=$count" }
          sin.foreach { sin => label += s"\nsin=$sin" }
          bs.foreach { bs => label += s"\nbs=$bs" }
          lat.foreach { lat => label += s"\nlat=$lat" }
          emitEdgeMatched(src, dst, DotAttr.empty.label(label))
        }
      }
    }
  }
}
