package spade.compiler.ops

import scala.virtualization.lms.common.BaseExp
import scala.virtualization.lms.common.DotGenBase
import scala.virtualization.lms.common.ScalaGenBase
import scala.reflect.{Manifest,SourceContext}

import spade.shared._
import spade.shared.ops._
import spade.compiler._
import spade.compiler.ops._

trait ModuleIOOpsExp extends ModuleIOOps with BaseExp {
  this: SpadeExp =>

  type Feedback[+T] = FeedbackWire[T]
  case class FeedbackWire[+T](val e: Exp[Feedback[T]])

  // Node representing a new, unassigned feedback wire
  case class FreshFeedback[T](mT: Manifest[T]) extends Def[Feedback[T]]

  // Assignment to a feedback wire
  case class Link[T:Manifest](lhs: Feedback[T], rhs: Exp[T]) extends Def[Unit]

  // Read a feedback wire (needed because value may not have been available yet)
  case class ReadFeedback[T:Manifest](f: Feedback[T]) extends Def[T]

  def feedback_new[T:Manifest](implicit ctx: SourceContext): Feedback[T] = {
    FeedbackWire(reflectMutable(FreshFeedback(manifest[T])))
  }
  def feedback_assign[T:Manifest](lhs: Feedback[T], rhs: Rep[T])(implicit ctx: SourceContext): Rep[Unit] = {
    reflectWrite(lhs.e)(Link(lhs, rhs))
  }
  def feedback_read[T:Manifest](f: Feedback[T])(implicit ctx: SourceContext): Rep[T] = ReadFeedback(f)
}

trait ScalaGenModuleIOOps extends ScalaGenBase {
  val IR: ModuleIOOpsExp
  import IR._

  override def remap[T](mT: Manifest[T]): String = {
    mT.erasure.getSimpleName match {
      case "FeedbackWire" => remap(mT.typeArguments(0))
      case "ALU" => "Array[String]"
      case "Switch" => "Array[String]"
      case "MyLink" => "Array[String]"
      case _ => super.remap(mT)
    }
  }

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case mn@FreshFeedback(_) =>
      stream.println(" // " + quote(sym) + " " + rhs)

    case mn@Link(lhs, rhs) =>
      stream.println(" // " + quote(sym) + " " + rhs)

    case ReadFeedback(f) =>
      stream.println(" // " + quote(sym) + " " + rhs)

    case _ => super.emitNode(sym, rhs)
  }
}

trait DotGenModuleIOOps extends DotGenBase {
  val IR: ModuleIOOpsExp
  import IR._

  /*
   * This remap is not needed, but is a simple example
   * to implement with other backends
   */
  override def remap[T](mT: Manifest[T]): String = {
    mT.erasure.getSimpleName match {
      case "FeedbackWire" => remap(mT.typeArguments(0))
      case _ => super.remap(mT)
    }
  }

  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case mn@FreshFeedback(_) =>
      stream.println(quote(sym) + " [shape=\"box\" style=\"filled\" fillcolor=\"blue\" color=\"white\"]")

    case mn@Link(lhs, rhs) =>
      stream.println(quote(sym) + " [shape=\"box\" style=\"filled\" fillcolor=\"blue\" color=\"white\"]")
      stream.println(s"""${quote(lhs.e)} -> ${quote(rhs)}""")

    case ReadFeedback(f) =>
      stream.println(quote(sym) + " [shape=\"box\" style=\"filled\" fillcolor=\"blue\" color=\"white\"]")
      stream.println(s"""${quote(f.e)} -> ${quote(sym)}""")

    case _ => super.emitNode(sym, rhs)
  }
}
