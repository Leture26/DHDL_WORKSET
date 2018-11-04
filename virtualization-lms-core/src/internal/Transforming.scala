package scala.virtualization.lms
package internal

import scala.collection.mutable.HashMap
import scala.reflect.SourceContext

trait AbstractTransformer {
  val IR: Transforming
  import IR._

  protected var globalMode = false
  def inGlobalMode[A:Manifest](block: => A): A = {
    var prevMode = globalMode
    globalMode = true
    val r = block
    globalMode = prevMode
    r
  }

  def hasContext = false
  def reflectBlock[A](xs: Block[A]): Exp[A] = sys.error("reflectBlock not supported by context-free transformers")

  // should be overridden by transformers with context
  def transformBlock[A](xs: Block[A]): Block[A] = {
    assert(!hasContext)
    Block(apply(xs.res))
  }

  def apply[A](x: Exp[A]): Exp[A]
  def apply[A:Manifest](xs: Block[A]): Block[A] = transformBlock(xs)

  def apply[A](xs: List[Exp[A]]): List[Exp[A]] = xs map (e => apply(e))
  def apply[A](xs: Seq[Exp[A]]): Seq[Exp[A]] = xs map (e => apply(e))
  def apply[A](xs: Option[Exp[A]]): Option[Exp[A]] = xs map (e => apply(e))
  def apply[X,A](f: X=>Exp[A]): X=>Exp[A] = (z:X) => apply(f(z))
  def apply[X,Y,A](f: (X,Y)=>Exp[A]): (X,Y)=>Exp[A] = (z1:X,z2:Y) => apply(f(z1,z2))
  def apply[X,Y,Z,A](f: (X,Y,Z)=>Exp[A]): (X,Y,Z)=>Exp[A] = (z1:X,z2:Y,z3:Z) => apply(f(z1,z2,z3))
  def apply[W,X,Y,Z,A](f: (W,X,Y,Z)=>Exp[A]): (W,X,Y,Z)=>Exp[A] = (z1:W,z2:X,z3:Y,z4:Z) => apply(f(z1,z2,z3,z4))
  def apply[V,W,X,Y,Z,A](f: (V,W,X,Y,Z)=>Exp[A]): (V,W,X,Y,Z)=>Exp[A] = (z1:V,z2:W,z3:X,z4:Y,z5:Z) => apply(f(z1,z2,z3,z4,z5))
  //def apply[A](xs: Summary): Summary = xs //TODO
  def onlySyms[A](xs: List[Sym[A]]): List[Sym[A]] = xs map (e => apply(e)) collect { case e: Sym[A] => e }
}

trait AbstractSubstTransformer extends AbstractTransformer {
  import IR._
  private var _subst: Map[Exp[Any],Exp[Any]] = Map.empty
  private var allSubst: Map[Exp[Any], Exp[Any]] = Map.empty
  protected var blockSubst: Map[Block[Any], Block[Any]] = Map.empty
  protected var copyingBlocks: Boolean = true // Default behavior: never use block substitution method

  def copyBlock[A](xs: Block[A]): Block[A] = {
    val oldCopy = copyingBlocks
    copyingBlocks = true
    val block2 = transformBlock(xs)
    copyingBlocks = oldCopy
    (block2)
  }

  // TODO: These should not be necessary, but defining the setter method for subst causes the
  // scala typer to crash...

  def subst: Map[Exp[Any],Exp[Any]] = { _subst }
  def resetSubst() { _subst = Map.empty }

  def remove(x: Exp[Any]) { _subst -= x }

  def register(x: Exp[Any], y: Exp[Any]) {
    _subst += x -> y
    allSubst += x -> y
  }
  def register(extend: (Exp[Any], Exp[Any])*) {
    _subst ++= extend
    allSubst ++= extend
  }

  def withSubstScope[A](extend: (Exp[Any],Exp[Any])*)(block: => A): A =
    withSubstScope {
      register(extend:_*)
      block
    }

  def withSubstScope[A](block: => A): A = {
    val save = subst
    val r = block
    _subst = save
    r
  }
  def withSubstRules[A](rules: Map[Exp[Any],Exp[Any]])(block: => A): A = {
    val save = subst
    _subst = rules
    val r = block
    _subst = save
    r
  }

  // Note: In substituion, perform only one step of lookup, otherwise we confuse things:
  //                     x4 --> x7 (input)
  // val x5 = 2 * x4     x5 --> x8
  // val x6 = x5 + 3     x6 --> x9
  // val x7 = x4 + 1                val x12 = x7 + 1
  // val x8 = 2 * x7                val x13 = 2 * x12
  // val x9 = x8 + 3                val x14 = x13 + 3     // this sets x9 --> x14
  // val x10 = x6 + x9              val x15 = x14 + x14   // here, transitively x6 --> x9 --> x14
  //                                                      // but we'd rather have x15 = x9 + x14

  override def apply[A:Manifest](xs: Block[A]): Block[A] = blockSubst.get(xs) match {
    case Some(ys) if !copyingBlocks => ys.asInstanceOf[Block[A]]
    case None => transformBlock(xs)
  }

  // Attempt to use closest substitution rule. If none exists, use global rule if global is enabled
  def apply[A](x: Exp[A]): Exp[A] = subst.get(x) match {
    case Some(y) => y.asInstanceOf[Exp[A]]
    case None if globalMode => allSubst.get(x) match {
      case Some(y) => y.asInstanceOf[Exp[A]]
      case None => x
    }
    case None => x
  }
}

trait Transforming extends Expressions with Blocks { self =>

  type Transformer = AbstractTransformer { val IR: self.type }

  class SubstTransformer extends AbstractTransformer {
    val IR: self.type = self
    val subst = new HashMap[Exp[Any], Exp[Any]]
    def apply[A](x: Exp[A]): Exp[A] = subst.get(x) match {
      case Some(y) if y != x => apply(y.asInstanceOf[Exp[A]]) case _ => x
    }
  }

  // FIXME: mirroring for effects!
  def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = mirrorDef(e,f)

  def mirrorDef[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Def[A] = sys.error("don't know how to mirror " + e)
  def mirrorFatDef[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Def[A] = sys.error("don't know how to mirror " + e) //hm...
}

trait FatTransforming extends Transforming with FatExpressions {

  override def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = (e match {
    case Forward(x) => toAtom(Forward(f(x)))(mtype(manifest[A]),pos)
    case _ => super.mirror(e,f)
  }).asInstanceOf[Exp[A]]

  //def mirror[A:Manifest](e: FatDef, f: Transformer): Exp[A] = sys.error("don't know how to mirror " + e)

}


trait MetaTransforming extends Transforming with SymbolMetadata {
  // Mirroring metadata
  final def mirror(p: SymbolProperties, f: Transformer): SymbolProperties = f.inGlobalMode{ p match {
    case ScalarProperties(data) => ScalarProperties(mirror(data,f))
    case StructProperties(children,data) => StructProperties(mirror(children,f),mirror(data,f))
    case ArrayProperties(child,data) => ArrayProperties(mirror(child,f),mirror(data,f))
  }}

  final def mirror[K,V](p: PropMap[K,V], f: Transformer): PropMap[K,V] = {
    PropMap(p.toList.map{case (k,v) => metaMirror(k,f) -> metaMirror(v,f) })
  }

  // By default, do nothing (most metadata does not include symbols)
  def mirror[T<:Metadata](m: T, f: Transformer): T = m

  def mirror[T](x: Option[T], f: Transformer): Option[T] = x.map(v=> metaMirror(v,f))

  // HACK: Double dispatch for mirroring unknown types (for prop map keys/values)
  def metaMirror[T](x: T, f: Transformer): T = x match {
    case p: SymbolProperties => mirror(p, f).asInstanceOf[T]
    case m: Metadata => mirror(m, f).asInstanceOf[T]
    case _ => x
  }
}

