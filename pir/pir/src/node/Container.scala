package pir
package node

trait Container extends PIRNode with prism.node.ProductSubGraph[PIRNode] { self =>
  override def ins:List[Input] = super.ins.asInstanceOf[List[Input]]
  override def outs:List[Output] = super.outs.asInstanceOf[List[Output]]
}

trait ContainerUtil {
  def innerCtrlOf(container:Container) = {
    import container.pirmeta._
    val primitives = container.collectDown[Primitive]()
    val ctrls = primitives.flatMap { comp => ctrlOf.get(comp) }.toSet[Controller]
    ctrls.minBy { _.descendents.size }
  }

}
