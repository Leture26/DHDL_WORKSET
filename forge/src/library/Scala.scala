package ppl.dsl.forge
package library

import core.{ForgeApplication,ForgeApplicationRunner}
import templates.Utilities.nl

/**
 * This file re-implements LMS common ops in Forge.
 */
@dsl
trait ScalaOps extends PrimitiveMathGen {
  this: ForgeApplication =>

  def importScalaOps() = {
    importProxies()
    importPrimitives()
    importMisc()
    importCasts()
    importNumerics()
    importOrdering()
    importStrings()
    importMath()
    importTuples()
    importHashMap()
    importByteBuffer()
    importForgeArrayAPI()
  }

  /**
   * TODO: Use reflection to auto-generate the ops and code generators
   * Question: how do we handle effects?
   *   withWrites("update", "insert", ...)?
   */
  def importProxies() = {
    // proxy(scala.collection.immutable.Array)
  }

  def importPrimitives() = {
    val Prim = grp("Primitive")
    val T = tpePar("T")

    lift (Prim) (MBoolean)
    lift (Prim) (MShort)
    lift (Prim) (MInt)
    lift (Prim) (MLong)
    lift (Prim) (MFloat)
    lift (Prim) (MDouble)

    // ------ Nodes
    // specialized versions for primitives
    // the forge_ prefix is to avoid conflicting with LMS primitive ops

    // -- Numeric
    val toInt = direct (Prim) ("numeric_to_int", T withBound TNumeric, T :: MInt)
    val toLong = direct (Prim) ("numeric_to_long", T withBound TNumeric, T :: MLong)
    val toFloat = direct (Prim) ("numeric_to_float", T withBound TNumeric, T :: MFloat)
    val toDouble = direct (Prim) ("numeric_to_double", T withBound TNumeric, T :: MDouble)

    // -- Boolean
    val boolean_not = direct (Prim) ("forge_boolean_not", Nil, MBoolean :: MBoolean)
    val boolean_and = direct (Prim) ("forge_boolean_and", Nil, (MBoolean,MBoolean) :: MBoolean)
    val boolean_or  = direct (Prim) ("forge_boolean_or",  Nil, (MBoolean,MBoolean) :: MBoolean)

    // -- Int
    val int_add = direct (Prim) ("forge_int_plus", Nil, (MInt,MInt) :: MInt)   // Add
    val int_sub = direct (Prim) ("forge_int_minus", Nil, (MInt,MInt) :: MInt)  // Subtract
    val int_mul = direct (Prim) ("forge_int_times", Nil, (MInt,MInt) :: MInt)  // Multiply
    val int_div = direct (Prim) ("forge_int_divide", Nil, (MInt,MInt) :: MInt) // Divide
    val int_mod = direct (Prim) ("forge_int_mod", Nil, (MInt,MInt) :: MInt)    // Modulus
    val int_not = direct (Prim) ("forge_int_not", Nil, MInt :: MInt)           // Bitwise NOT
    val int_and = direct (Prim) ("forge_int_and", Nil, (MInt,MInt) :: MInt)    // Bitwise AND
    val int_or  = direct (Prim) ("forge_int_or",  Nil, (MInt,MInt) :: MInt)    // Bitwise OR
    val int_left_shift  = direct (Prim) ("forge_int_left_shift", Nil, (MInt,MInt) :: MInt)
    val int_right_shift = direct (Prim) ("forge_int_right_shift", Nil, (MInt,MInt) :: MInt)
    val int_unsigned_right_shift = direct (Prim) ("forge_int_unsigned_right_shift", Nil, (MInt,MInt) :: MInt)

    // -- Long
    val long_add = direct (Prim) ("forge_long_plus", Nil, (MLong,MLong) :: MLong)    // Add
    val long_sub = direct (Prim) ("forge_long_minus", Nil, (MLong,MLong) :: MLong)   // Subtract
    val long_mul = direct (Prim) ("forge_long_times", Nil, (MLong,MLong) :: MLong)   // Multiply
    val long_div = direct (Prim) ("forge_long_divide", Nil, (MLong,MLong) :: MLong)  // Divide
    val long_mod = direct (Prim) ("forge_long_mod", Nil, (MLong,MLong) :: MLong)     // Modulus
    val long_not = direct (Prim) ("forge_long_not", Nil, MLong :: MLong)
    val long_and = direct (Prim) ("forge_long_and", Nil, (MLong,MLong) :: MLong)     // Bitwise AND
    val long_or  = direct (Prim) ("forge_long_or",  Nil, (MLong,MLong) :: MLong)     // Bitwise OR
    val long_xor = direct (Prim) ("forge_long_xor", Nil, (MLong,MLong) :: MLong)     // Bitwise XOR

    val long_left_shift  = direct (Prim) ("forge_long_left_shift", Nil, (MLong,MInt) :: MLong)
    val long_right_shift = direct (Prim) ("forge_long_right_shift", Nil, (MLong,MInt) :: MLong)
    val long_unisgned_right_shift = direct (Prim) ("forge_long_unsigned_right_shift", Nil, (MLong,MInt) :: MLong)
    val long_divide_double  = direct (Prim) ("forge_long_divide_double", Nil, (MLong,MDouble) :: MDouble)

    // -- Float
    val float_add = direct (Prim) ("forge_float_plus", Nil, (MFloat,MFloat) :: MFloat)
    val float_sub = direct (Prim) ("forge_float_minus", Nil, (MFloat,MFloat) :: MFloat)
    val float_mul = direct (Prim) ("forge_float_times", Nil, (MFloat,MFloat) :: MFloat)
    val float_div = direct (Prim) ("forge_float_divide", Nil, (MFloat,MFloat) :: MFloat)

    // -- Double
    val double_add = direct (Prim) ("forge_double_plus", Nil, (MDouble,MDouble) :: MDouble)
    val double_sub = direct (Prim) ("forge_double_minus", Nil, (MDouble,MDouble) :: MDouble)
    val double_mul = direct (Prim) ("forge_double_times", Nil, (MDouble,MDouble) :: MDouble)
    val double_div = direct (Prim) ("forge_double_divide", Nil, (MDouble,MDouble) :: MDouble)


    // ----- API

    // -- Numeric
    /** Converts this numeric value to an Int **/
    infix (Prim) ("toInt", T withBound TNumeric, T :: MInt) implements redirect ${ numeric_to_int($0) }
    /** Converts this numeric value to a Long **/
    infix (Prim) ("toLong", T withBound TNumeric, T :: MLong) implements redirect ${ numeric_to_long($0) }
    /** Converts this numeric value to a Float **/
    infix (Prim) ("toFloat", T withBound TNumeric, T :: MFloat) implements redirect ${ numeric_to_float($0) }
    /** Converts this numeric value to an Double **/
    infix (Prim) ("toDouble", T withBound TNumeric, T :: MDouble) implements redirect ${ numeric_to_double($0) }

    // -- Boolean
    /** Boolean inverse **/
    infix (Prim) ("unary_!", Nil, MBoolean :: MBoolean) implements redirect ${ forge_boolean_not($0) }
    /** Boolean OR **/
    infix (Prim) ("||", Nil, (MBoolean, MBoolean) :: MBoolean) implements redirect ${ forge_boolean_or($0,$1) }
    /** Boolean AND **/
    infix (Prim) ("&&", Nil, (MBoolean, MBoolean) :: MBoolean) implements redirect ${ forge_boolean_and($0, $1) }

    // -- Int
    /** Negation **/
    infix (Prim) ("unary_-", Nil, MInt :: MInt) implements redirect ${ unit(-1)*$0 }
    infix (Prim) ("%", Nil, (MInt,MInt) :: MInt) implements redirect ${ forge_int_mod($0, $1) }
    infix (Prim) ("unary_~", Nil, MInt :: MInt)  implements redirect ${ forge_int_not($0) }
    infix (Prim) ("&", Nil, (MInt,MInt) :: MInt) implements redirect ${ forge_int_and($0,$1) }
    infix (Prim) ("|", Nil, (MInt,MInt) :: MInt) implements redirect ${ forge_int_or($0,$1) }
    infix (Prim) ("<<",Nil, (MInt,MInt) :: MInt) implements redirect ${ forge_int_left_shift($0,$1) }
    infix (Prim) (">>",Nil, (MInt,MInt) :: MInt) implements redirect ${ forge_int_right_shift($0,$1) }
    infix (Prim) (">>>",Nil, (MInt,MInt) :: MInt) implements redirect ${ forge_int_unsigned_right_shift($0,$1) }

    /** Negation **/
    infix (Prim) ("unary_-", Nil, MLong :: MLong) implements redirect ${ unit(-1L)*$0 }
    infix (Prim) ("%", Nil, (MLong,MLong) :: MLong) implements redirect ${ forge_long_mod($0,$1) }
    infix (Prim) ("unary_~", Nil, MLong :: MLong) implements redirect ${ forge_long_not($0) }
    infix (Prim) ("&", Nil, (MLong,MLong) :: MLong) implements redirect ${ forge_long_and($0,$1) }
    infix (Prim) ("|", Nil, (MLong,MLong) :: MLong) implements redirect ${ forge_long_or($0,$1) }
    infix (Prim) ("^", Nil, (MLong,MLong) :: MLong) implements redirect ${ forge_long_xor($0,$1) }
    infix (Prim) ("<<", Nil, (MLong,MInt) :: MLong) implements redirect ${ forge_long_left_shift($0,$1) }
    infix (Prim) (">>", Nil, (MLong,MInt) :: MLong) implements redirect ${ forge_long_right_shift($0,$1) }
    infix (Prim) (">>>", Nil, (MLong,MInt) :: MLong) implements redirect ${ forge_long_unsigned_right_shift($0,$1) }

    /** Negation **/
    infix (Prim) ("unary_-", Nil, MFloat :: MFloat) implements redirect ${ unit(-1f)*$0 }
    /** Negation **/
    infix (Prim) ("unary_-", Nil, MDouble :: MDouble) implements redirect ${ unit(-1)*$0 }


    /** Enables implicit conversion from Int to Double **/
    fimplicit (Prim) ("repInt2ToRepDouble", Nil, MInt :: MDouble) implements composite ${ $0.toDouble }
    /** Enables implicit conversion from Int to Float **/
    fimplicit (Prim) ("repInt2ToRepFloat", Nil, MInt :: MFloat) implements composite ${ $0.toFloat }
    /** Enables implicit conversion from Int to Long **/
    fimplicit (Prim) ("repInt2ToRepLong", Nil, MInt :: MLong) implements composite ${ $0.toLong }
    /** Enables implicit conversion from Float to Double **/
    fimplicit (Prim) ("repFloat2ToRepDouble", Nil, MFloat :: MDouble) implements composite ${ $0.toDouble }


    // --- Codegen
    impl (toInt) (codegen($cala, ${ $0.toInt }))
    impl (toLong) (codegen($cala, ${ $0.toLong }))
    impl (toFloat) (codegen($cala, ${ $0.toFloat }))
    impl (toDouble) (codegen($cala, ${ $0.toDouble }))

    for (g <- List(cuda, cpp)) {
      impl (toInt) (codegen(g, ${ (int32_t) $0 }))
      impl (toLong) (codegen(g, ${ (int64_t) $0 }))
      impl (toFloat) (codegen(g, ${ (float) $0 }))
      impl (toDouble) (codegen(g, ${ (double) $0 }))
    }

    impl (int_unsigned_right_shift)  (codegen($cala, ${ $0 >>> $1 }))
    impl (long_unisgned_right_shift) (codegen($cala, ${ $0 >>> $1 }))


    for (g <- List($cala, cuda, cpp)) {
      impl (boolean_not) (codegen(g, "!" + quotedArg(0)))
      impl (boolean_and) (codegen(g, quotedArg(0) + " && " + quotedArg(1)))
      impl (boolean_or) (codegen(g, quotedArg(0) + " || " + quotedArg(1)))

      impl (int_add) (codegen(g, ${$0 + $1}))
      impl (int_sub) (codegen(g, ${$0 - $1}))
      impl (int_mul) (codegen(g, ${$0 * $1}))
      impl (int_div) (codegen(g, ${$0 / $1}))
      impl (int_mod) (codegen(g, ${$0 % $1}))
      impl (int_not) (codegen(g, ${~$0}))
      impl (int_and) (codegen(g, ${$0 & $1}))
      impl (int_or)  (codegen(g, ${$0 | $1}))
      impl (int_left_shift)  (codegen(g, ${$0 << $1}))
      impl (int_right_shift) (codegen(g, ${$0 >> $1}))

      impl (long_add) (codegen(g, ${$0 + $1}))
      impl (long_sub) (codegen(g, ${$0 - $1}))
      impl (long_mul) (codegen(g, ${$0 * $1}))
      impl (long_div) (codegen(g, ${$0 / $1}))
      impl (long_mod) (codegen(g, ${$0 % $1}))
      impl (long_not) (codegen(g, ${~$0}))
      impl (long_and) (codegen(g, ${$0 & $1}))
      impl (long_or)  (codegen(g, ${$0 | $1}))
      impl (long_xor) (codegen(g, ${$0 ^ $1}))
      impl (long_left_shift) (codegen(g, ${ $0 << $1 }))
      impl (long_right_shift) (codegen(g, ${ $0 >> $1 }))
      impl (long_divide_double) (codegen(g, ${$0 / $1}))

      impl (float_add) (codegen(g, ${$0 + $1}))
      impl (float_sub) (codegen(g, ${$0 - $1}))
      impl (float_mul) (codegen(g, ${$0 * $1}))
      impl (float_div) (codegen(g, ${$0 / $1}))

      impl (double_add) (codegen(g, ${$0 + $1}))
      impl (double_sub) (codegen(g, ${$0 - $1}))
      impl (double_mul) (codegen(g, ${$0 * $1}))
      impl (double_div) (codegen(g, ${$0 / $1}))
    }

    // --- Rewrite Ops (mostly constant propagation)

    // Forward boolean negation to Delite internal implementation
    rewrite (boolean_not) using forwarding ${ delite_boolean_negate($0) }

    // Forward integer operations used for index calc to Delite internal implementations
    rewrite (int_add) using forwarding ${ delite_int_plus($0, $1) }
    rewrite (int_sub) using forwarding ${ delite_int_minus($0, $1) }
    rewrite (int_mul) using forwarding ${ delite_int_times($0, $1) }


    val zero = "Const(0 | 0L | 0.0 | 0.0f | -0.0 | -0.0f)"
    rewrite (float_add,double_add,long_add) using pattern((${Const(x)}, ${Const(y)}) -> ${ unit(x + y)} )
    rewrite (float_add,double_add,long_add) using commutative((zero, ${x}) -> ${x} )
    rewrite (float_sub,double_sub,long_sub) using pattern((${Const(x)}, ${Const(y)}) -> ${ unit(x - y)} )
    rewrite (float_sub,double_sub,long_sub) using pattern((${x}, zero) -> ${x})
    rewrite (float_mul,double_mul,long_mul) using pattern((${Const(x)}, ${Const(y)}) -> ${ unit(x * y)} )
    rewrite (float_div,double_div,long_div) using pattern((${Const(x)}, ${Const(y)}) -> ${ unit(x / y)} )

    // TODO: Should these be DSL specific? App specific? How often are these even important?
    //rewrite (float_times) using commutative((zero, ${x}) -> ${unit(0f)} ) // Not completely correct (Inf * 0 = NaN)
    //rewrite (float_divide) using pattern((zero, ${x}) -> ${unit(0f)} )    // Also not always correct (e.g. 0 / 0 = NaN)

    // For some reason using the infix version of these casts was giving crazy scalac typer crashes
    rewrite (toInt) using pattern(${Const(x)} -> ${ unit(implicitly[Numeric[T]].toInt(x)) })
    rewrite (toLong) using pattern(${Const(x)} -> ${ unit(implicitly[Numeric[T]].toLong(x)) })
    rewrite (toFloat) using pattern(${Const(x)} -> ${ unit(implicitly[Numeric[T]].toFloat(x)) })
    rewrite (toDouble) using pattern(${Const(x)} -> ${ unit(implicitly[Numeric[T]].toDouble(x)) })

    // Uncomment this to generate type classes for primitive math combinations.
    // importPrimitiveMathTypeClasses()

    // Uncomment this to generate infix methods for primitive math combinations.
    importPrimitiveMathInfix(Prim)
  }

  def importMisc() = {
    /**
     * Miscellaneous operations, including assertions, console printing, and exception throwing.
     **/
    val Misc = grp("Misc")

    val T = tpePar("T")

    // ------ API
    /** Exits the program with given error code.
     * @param code
     **/
    val exit = direct (Misc) ("exit", Nil, MInt :: MNothing, effect = simple)

    /** Exits the program with a fatal exception message.
     * @param message
     **/
    val fatal = direct (Misc) ("fatal", Nil, MString :: MNothing, effect = simple)

    /** Prints the specified value to the console *without* a newline.
     * At runtime, this will attempt to use the toString method to cast the value to string if one exists
     * @param value
     **/
    val print = direct (Misc) ("print", Nil, MAny :: MUnit, effect = simple)

    /** Prints the specified value to the console *with* a newline.
     * At runtime, this will attempt to use the toString method to cast the value to string if one exists
     * @param value
     **/
    val println = direct (Misc) ("println", Nil, List(MAny) :: MUnit, effect = simple)

    /** Prints a newline to the console **/
    val printnl = direct (Misc) ("println", Nil, Nil :: MUnit, effect = simple)

    /** If the condition is true, does nothing. If it is false, terminates the program with the specified exception message.
     * Assertions are only evaluated in the library backend and during compiler debugging. Otherwise, they are ignored to enable
     * more fusion and code motion opportunities.
     * @param cond: condition to be checked
     * @param message: exception message to be shown to the user if the assertion fails
     **/
    val fassert = direct (Misc) ("fassert", Nil, (MBoolean, MString) :: MUnit, effect = simple)

    /** Returns the maximum heap size from the JVM **/
    direct (Misc) ("getMaxHeapSize", Nil, Nil :: MLong) implements codegen($cala, ${
      Runtime.getRuntime.maxMemory()
    })

    /** Gives a hint to the compiler to consider this value as immutable for future operations
     * Note that this operation is unsafe as it does not prevent mutations prior to this call. For example, in:
     * {{{
     * val x = Array[Int](32)
     * for (i <- 0 until N) {
     *   x(i) = i
     *   f(x.unsafeImmutable)
     * }
     * }}}
     * This will tell the compiler that x is not mutable in the function x, but x can still be mutated
     * after this call has been made (in the next iteration of the loop, for example).
     **/
    val immutable = infix (Misc) ("unsafeImmutable", List(T), List(T) :: T, aliasHint = copies(0))

    /** Enables unsafe mutation of this value. **/
    val mut = infix (Misc) ("unsafeMutable", List(T), List(T) :: T, effect = mutable, aliasHint = copies(0))


    // ----- Nodes
    impl (exit) (codegen($cala, ${sys.exit($0)}))
    impl (fatal) (codegen($cala, ${throw new Exception($0)}))
    impl (print) (codegen($cala, ${print($0)}))
    impl (println) (codegen($cala, "println(" + quotedArg(0) + ")"))
    impl (printnl) (codegen($cala, "println()"))
    impl (fassert) (codegen($cala, ${ assert($0, $1) }))

    impl (immutable) (codegen($cala, quotedArg(0) + " /* unsafe immutable */"))
    impl (mut) (codegen($cala, quotedArg(0) + " /* unsafe mutable */"))

   for (g <- List(cuda, cpp)) {
      impl (exit) (codegen(g, ${exit($0)}))
      impl (print) (codegen(g, ${std::cout << $0}))
      impl (fatal) (codegen(g, ${assert(0)}))
      impl (println) (codegen(g, ${std::cout << $0 << std::endl}))
      impl (printnl) (codegen(g, ${std::cout << std::endl}))
      impl (immutable) (codegen(g, ${$0}))
    }

    val whileDo = direct (Misc) ("__whileDo", Nil, List(MThunk(MBoolean),MThunk(MUnit)) :: MUnit)

    // function (block) arguments should be referenced using $b[<arg name>]
    impl (whileDo) (codegen($cala, ${
      while ($b[0]) {
        $b[1]
      }
    }))

    // TODO: something is broken with IfThenElse here; bound symbols (effects) are getting hoisted if the frequencies are not set to cold.
    val ifThenElse = direct (Misc) ("__ifThenElse", List(T), List(MBoolean,MThunk(T,cold),MThunk(T,cold)) :: T)
    impl (ifThenElse) (codegen($cala, ${
      if ($0) {
        $b[1]
      }
      else {
        $b[2]
      }
    }))


    // ------ Rewrites
    // Forward Misc methods to Delite internal implementations
    rewrite (whileDo) using forwarding ${ delite_while($0, $1) }
    rewrite (immutable) using forwarding ${ delite_unsafe_immutable($0) }
    rewrite (ifThenElse) using forwarding ${ delite_ifThenElse($0, $1, $2, false, true) }

    // Only assert when debug is true, since assert effects can interfere with optimizations
    rewrite (fassert) using rule ${
      if (Config.debug) super.misc_fassert($0, $1) // TODO: Should have more straightforward call here
      else ()
    }
  }

  def importCasts() = {
    val Cast = grp("Cast")
    val T = tpePar("T")
    val R = tpePar("R")

    // these don't work as infix_ methods
    noInfixList :::= List("AsInstanceOf", "IsInstanceOf")

    /** Casts this to the given type **/
    val asinstance = infix (Cast) ("AsInstanceOf", (T,R), T :: R)
    impl (asinstance) (codegen($cala, ${ $0.asInstanceOf[$t[R]] }))
    impl (asinstance) (codegen(cuda, "(" + unquotes("remapWithRef(manifest[T])") + ")" + quotedArg(0)))
    impl (asinstance) (codegen(cpp, "(" + unquotes("remapWithRef(manifest[T])") + ")" + quotedArg(0)))

    /** Checks if this is the specified type **/
    val isinstance = infix (Cast) ("IsInstanceOf", (T,R), T :: MBoolean)
    impl (isinstance) (codegen($cala, ${ $0.isInstanceOf[$t[R]] }))
    // TODO: how to implement isinstance for clike targets?
  }

  def importNumerics() = {
    val Num = grp("Numeric")
    val T = tpePar("T")

    lift (Num) (T withBound TNumeric)

    val zero = infix (Num) ("zero", List(T withBound TNumeric), Nil :: T)
    val plus = infix (Num) ("+", List(T withBound TNumeric), List(T,T) :: T)
    val minus = infix (Num) ("-", List(T withBound TNumeric), List(T,T) :: T)
    val times = infix (Num) ("*", List(T withBound TNumeric), List(T,T) :: T)
    impl (zero) (codegen($cala, "implicitly[Numeric["+quotedTpe(0,zero)+"]].zero"))
    impl (zero) (codegen(cuda, "0"))
    impl (zero) (codegen(cpp, "0"))
    for (g <- List($cala, cuda, cpp)) {
      impl (plus) (codegen(g, quotedArg(0) + " + " + quotedArg(1)))
      impl (minus) (codegen(g, quotedArg(0) + " - " + quotedArg(1)))
      impl (times) (codegen(g, quotedArg(0) + " * " + quotedArg(1)))
    }

    val Frac = grp("Fractional")
    val R = tpePar("R")
    val div = infix (Frac) ("/", List(T,R withBound TFractional), (T,R) :: R, T ==> R)
    impl (div) (codegen($cala, ${ implicitly[Fractional[$t[R]]].div($0,$1) }))
    impl (div) (codegen(cuda, ${ $0 / $1 }))
    impl (div) (codegen(cpp, ${ $0 / $1 }))
  }

  def importOrdering() = {
    val Ord = grp("Ordering")
    val A = tpePar("A")
    val B = tpePar("B")
    val AC = tpePar("A", stage = now)
    val BC = tpePar("B", stage = now)

    val eq = direct (Ord) ("__equal", (A,B), (A,B) :: MBoolean)
    label (eq, "forge_equals")
    impl (eq) (codegen($cala, quotedArg(0) + " == " + quotedArg(1)))
    impl (eq) (codegen(cuda, quotedArg(0) + " == " + quotedArg(1)))
    impl (eq) (codegen(cpp, quotedArg(0) + " == " + quotedArg(1)))

    direct (Ord) ("__equal", (A,B), (MVar(A),B) :: MBoolean) implements redirect ${ forge_equals(readVar($0), $1) }
    direct (Ord) ("__equal", (A,B), (A,MVar(B)) :: MBoolean) implements redirect ${ forge_equals($0, readVar($1)) }
    direct (Ord) ("__equal", (A,B), (MVar(A),MVar(B)) :: MBoolean) implements redirect ${ forge_equals(readVar($0), readVar($1)) }
    direct (Ord) ("__equal", (A,BC), (A,BC) :: MBoolean) implements redirect ${ forge_equals($0, unit($1)) }
    direct (Ord) ("__equal", (A,BC), (MVar(A),BC) :: MBoolean) implements redirect ${ forge_equals(readVar($0), unit($1)) }
    direct (Ord) ("__equal", (AC,B), (AC,B) :: MBoolean) implements redirect ${ forge_equals(unit($0), $1) }
    direct (Ord) ("__equal", (AC,B), (AC,MVar(B)) :: MBoolean) implements redirect ${ forge_equals(unit($0), readVar($1)) }

    val neq = infix (Ord) ("!=", (A,B), (A,B) :: MBoolean)
    label (neq, "forge_notequals")
    impl (neq) (codegen($cala, quotedArg(0) + " != " + quotedArg(1)))
    impl (neq) (codegen(cuda, quotedArg(0) + " != " + quotedArg(1)))
    impl (neq) (codegen(cpp, quotedArg(0) + " != " + quotedArg(1)))

    infix (Ord) ("!=", (A,B), (MVar(A),B) :: MBoolean) implements redirect ${ forge_notequals(readVar($0), $1) }
    infix (Ord) ("!=", (A,B), (A,MVar(B)) :: MBoolean) implements redirect ${ forge_notequals($0, readVar($1)) }
    infix (Ord) ("!=", (A,B), (MVar(A),MVar(B)) :: MBoolean) implements redirect ${ forge_notequals(readVar($0), readVar($1)) }
    infix (Ord) ("!=", (A,BC), (A,BC) :: MBoolean) implements redirect ${ forge_notequals($0, unit($1)) }
    infix (Ord) ("!=", (A,BC), (MVar(A),BC) :: MBoolean) implements redirect ${ forge_notequals(readVar($0), unit($1)) }
    infix (Ord) ("!=", (AC,B), (AC,B) :: MBoolean) implements redirect ${ forge_notequals(unit($0), $1) }
    infix (Ord) ("!=", (AC,B), (AC,MVar(B)) :: MBoolean) implements redirect ${ forge_notequals(unit($0), readVar($1)) }

    val min = infix (Ord) ("min", List(A withBound TOrdering), List(A,A) :: A)
    val max = infix (Ord) ("max", List(A withBound TOrdering), List(A,A) :: A)
    impl (min) (codegen($cala, quotedArg(0) + " min " + quotedArg(1)))
    impl (max) (codegen($cala, quotedArg(0) + " max " + quotedArg(1)))
    for (g <- List(cuda,cpp)) {
      impl (min) (codegen(g, quotedArg(0) + " < " + quotedArg(1) + "?" + quotedArg(0) + ":" + quotedArg(1)))
      impl (max) (codegen(g, quotedArg(0) + " > " + quotedArg(1) + "?" + quotedArg(0) + ":" + quotedArg(1)))
    }
    //infix (Ord) ("compare", List(A withBound TOrdering), List(A,A) :: MInt) implements (codegen($cala, quotedArg(0) + " compare " + quotedArg(1)))
    val lt = infix (Ord) ("<", List(A withBound TOrdering), List(A,A) :: MBoolean)
    val lte = infix (Ord) ("<=", List(A withBound TOrdering), List(A,A) :: MBoolean)
    val gt = infix (Ord) (">", List(A withBound TOrdering), List(A,A) :: MBoolean)
    val gte = infix (Ord) (">=", List(A withBound TOrdering), List(A,A) :: MBoolean)

    for (g <- List($cala, cuda, cpp)) {
      impl (lt) (codegen(g, quotedArg(0) + " < " + quotedArg(1)))
      impl (lte) (codegen(g, quotedArg(0) + " <= " + quotedArg(1)))
      impl (gt) (codegen(g, quotedArg(0) + " > " + quotedArg(1)))
      impl (gte) (codegen(g, quotedArg(0) + " >= " + quotedArg(1)))
    }

    rewrite (eq) using forwarding ${ delite_equals($0, $1) }
    rewrite (neq) using forwarding ${ delite_notequals($0, $1) }
  }

  def importStrings() = {
    val Str = grp("FString") // have to use either grp("xx") or MString, NOT grp("String") which is ambiguous with MString
    lift (Str) (MString)

    // overloaded variants of string concat
    val T = tpePar("T")

    val toInt = infix (Str) ("toInt", Nil, MString :: MInt)
    val toLong = infix (Str) ("toLong", Nil, MString :: MLong)
    val toFloat = infix (Str) ("toFloat", Nil, MString :: MFloat)
    val toDouble = infix (Str) ("toDouble", Nil, MString :: MDouble)
    val toBoolean = infix (Str) ("toBoolean", Nil, MString :: MBoolean)
    val trim = infix (Str) ("trim", Nil, MString :: MString)
    val fcharAt = infix (Str) ("fcharAt", Nil, (MString,MInt) :: MChar)
    val length = infix (Str) ("length", Nil, MString :: MInt)
    val startsWith = infix (Str) ("startsWith", Nil, (MString,MString) :: MBoolean)
    infix (Str) ("slice", Nil, (MString,MInt,MInt) :: MString) implements redirect ${ fstring_substring($0,$1,$2) }
    val endsWith = infix (Str) ("endsWith", Nil, (MString,MString) :: MBoolean)
    val contains = infix (Str) ("contains", Nil, (MString,MString) :: MBoolean)
    val substring1 = infix (Str) ("substring", Nil, (MString,MInt) :: MString)
    val substring2 = infix (Str) ("substring", Nil, (MString,MInt,MInt) :: MString)
    val toLowerCase = infix (Str) ("toLowerCase", Nil, MString :: MString)
    val toUpperCase = infix (Str) ("toUpperCase", Nil, MString :: MString)
    val getBytes = infix (Str) ("getBytes", Nil, MString :: MArray(MByte))
    val replaceAll = infix (Str) ("replaceAllLiterally", Nil, (MString,MString,MString) :: MString)

    impl (toInt) (codegen($cala, ${ $0.toInt }))
    impl (toLong) (codegen($cala, ${ $0.toLong }))
    impl (toFloat) (codegen($cala, ${ $0.toFloat }))
    impl (toDouble) (codegen($cala, ${ $0.toDouble }))
    impl (toBoolean) (codegen($cala, ${ $0.toBoolean }))
    impl (trim) (codegen($cala, ${ $0.trim }))
    impl (fcharAt) (codegen($cala, ${ $0.charAt($1) }))
    impl (length) (codegen($cala, ${ $0.length }))
    impl (startsWith) (codegen($cala, ${ $0.startsWith($1) }))
    impl (endsWith) (codegen($cala, ${ $0.endsWith($1) }))
    impl (contains) (codegen($cala, ${ $0.contains($1) }))
    impl (substring1) (codegen($cala, ${ $0.substring($1) }))
    impl (substring2) (codegen($cala, ${ $0.substring($1,$2) }))
    impl (toLowerCase) (codegen($cala, ${ $0.toLowerCase }))
    impl (toUpperCase) (codegen($cala, ${ $0.toUpperCase }))
    impl (getBytes) (codegen($cala, ${ $0.getBytes() }))
    impl (replaceAll) (codegen($cala, ${ $0.replaceAllLiterally($1,$2) }))

    impl (toInt) (codegen(cpp, ${ string_toInt($0) }))
    impl (toLong) (codegen(cpp, ${ string_toLong($0) }))
    impl (toFloat) (codegen(cpp, ${ string_toFloat($0) }))
    impl (toDouble) (codegen(cpp, ${ string_toDouble($0) }))
    impl (toBoolean) (codegen(cpp, ${ string_toBoolean($0) }))
    impl (trim) (codegen(cpp, ${ string_trim($0) }))
    impl (fcharAt) (codegen(cpp, ${ string_charAt($0,$1) }))
    impl (startsWith) (codegen(cpp, ${ string_startsWith($0,$1) }))
    impl (length) (codegen(cpp, ${ string_length($0) }))
    impl (endsWith) (codegen(cpp, ${ string_endsWith($0,$1) }))
    impl (contains) (codegen(cpp, ${ string_contains($0,$1) }))
    impl (substring1) (codegen(cpp, ${ string_substr($0,$1) }))
    impl (substring2) (codegen(cpp, ${ string_substr($0,$1,$2) }))

    // We leave fsplit, though deprecated, here for compatibility
    infix (Str) ("fsplit", Nil, MethodSignature(List(MString,MString,("numSplits",MInt,"unit(0)")), MArray(MString))) implements redirect ${ $0.split($1, $2) }

    infix (Str) ("split", Nil, MethodSignature(List(MString,MString,("numSplits",MInt,"unit(0)")), MArray(MString))) implements composite ${
      array_string_split($0,$1,$2)
    }

    val B = tpePar("B")
    val concat = direct (Str) ("forge_string_plus", (T,B), (T,B) :: MString)
    impl (concat) (codegen($cala, ${ $0.toString + $1.toString }))
    impl (concat) (codegen(cpp, "string_plus( convert_to_string< " + unquotes("remapWithRef("+opArgPrefix+"0.tp)") + ">(" + quotedArg(0) + "), convert_to_string< " + unquotes("remapWithRef("+opArgPrefix+"1.tp)") + ">(" + quotedArg(1) + "))"))

    // TODO: check these combinations to see if they could be condensed or if there is anything missing
    infix (Str) ("+", T, (CString, T) :: MString) implements redirect ${ forge_string_plus(unit($0), $1) }
    infix (Str) ("+", T, (MString, T) :: MString) implements redirect ${ forge_string_plus($0, $1) }
    infix (Str) ("+", T, (CString, MVar(T)) :: MString) implements redirect ${ forge_string_plus(unit($0), readVar($1)) }
    infix (Str) ("+", T, (MString, MVar(T)) :: MString) implements redirect ${ forge_string_plus($0, readVar($1)) }
    infix (Str) ("+", T, (MVar(MString), T) :: MString) implements redirect ${ forge_string_plus(readVar($0), $1) }
    infix (Str) ("+", T, (MVar(MString), MVar(T)) :: MString) implements redirect ${ forge_string_plus(readVar($0), readVar($1)) }

    infix (Str) ("+", T, (T, CString) :: MString) implements redirect ${ forge_string_plus($0, unit($1)) }
    infix (Str) ("+", T, (T, MString) :: MString) implements redirect ${ forge_string_plus($0, $1) }
    infix (Str) ("+", T, (MVar(T), CString) :: MString) implements redirect ${ forge_string_plus(readVar($0), unit($1)) }
    infix (Str) ("+", T, (MVar(T), MString) :: MString) implements redirect ${ forge_string_plus(readVar($0), $1) }
    infix (Str) ("+", T, (MVar(T), MVar(MString)) :: MString) implements redirect ${ forge_string_plus(readVar($0), readVar($1)) }

    // infix (Str) ("+", Nil, (MString, CString) :: MString) implements redirect ${ forge_string_plus($0, unit($1)) }
    infix (Str) ("+", Nil, (CString, MString) :: MString) implements redirect ${ forge_string_plus(unit($0), $1) }
    infix (Str) ("+", Nil, (MString, MString) :: MString) implements redirect ${ forge_string_plus($0, $1) }
    infix (Str) ("+", Nil, (MString, MVar(MString)) :: MString) implements redirect ${ forge_string_plus($0, readVar($1)) }
    infix (Str) ("+", Nil, (MVar(MString), MString) :: MString) implements redirect ${ forge_string_plus(readVar($0), $1) }
    infix (Str) ("+", Nil, (MVar(MString), MVar(MString)) :: MString) implements redirect ${ forge_string_plus(readVar($0), readVar($1)) }
  }

  def importMath() = {
    val Math = grp("Math")

    // constants
    val inf = direct (Math) ("INF", Nil, Nil :: MDouble)
    val ninf = direct (Math) ("nINF", Nil, Nil :: MDouble)
    direct (Math) ("Pi", Nil, Nil :: MDouble) implements redirect ${ unit(java.lang.Math.PI) }
    direct (Math) ("E", Nil, Nil :: MDouble) implements redirect ${ unit(java.lang.Math.E) }

    impl (inf) (codegen($cala, "Double.PositiveInfinity"))
    impl (ninf) (codegen($cala, "Double.NegativeInfinity"))
    impl (inf) (codegen(cpp, "std::numeric_limits<double>::infinity()"))
    impl (ninf) (codegen(cpp, "-std::numeric_limits<double>::infinity()"))
    impl (inf) (codegen(cuda, "__longlong_as_double(0x7ff0000000000000ULL)"))
    impl (ninf) (codegen(cuda, "__longlong_as_double(0xfff0000000000000ULL)"))

    // methods
    val bitCount = static (Math) ("bitcount", Nil, MLong :: MInt)
    val abs = static (Math) ("abs", Nil, MDouble :: MDouble)
    val exp = static (Math) ("exp", Nil, MDouble :: MDouble)
    val log = static (Math) ("log", Nil, MDouble :: MDouble)
    val log10 = static (Math) ("log10", Nil, MDouble :: MDouble)
    val sqrt = static (Math) ("sqrt", Nil, MDouble :: MDouble)
    val ceil = static (Math) ("ceil", Nil, MDouble :: MDouble)
    val floor = static (Math) ("floor", Nil, MDouble :: MDouble)
    val round = static (Math) ("round", Nil, MDouble :: MLong)
    val sin = static (Math) ("sin", Nil, MDouble :: MDouble)
    val sinh = static (Math) ("sinh", Nil, MDouble :: MDouble)
    val asin = static (Math) ("asin", Nil, MDouble :: MDouble)
    val cos = static (Math) ("cos", Nil, MDouble :: MDouble)
    val cosh = static (Math) ("cosh", Nil, MDouble :: MDouble)
    val acos = static (Math) ("acos", Nil, MDouble :: MDouble)
    val tan = static (Math) ("tan", Nil, MDouble :: MDouble)
    val tanh = static (Math) ("tanh", Nil, MDouble :: MDouble)
    val atan = static (Math) ("atan", Nil, MDouble :: MDouble)
    val atan2 = static (Math) ("atan2", Nil, (MDouble, MDouble) :: MDouble)
    val pow = static (Math) ("pow", Nil, (MDouble, MDouble) :: MDouble)
    val max = static (Math) ("max", Nil, (MDouble,MDouble) :: MDouble)
    val min = static (Math) ("min", Nil, (MDouble,MDouble) :: MDouble)

    impl (bitCount) (codegen($cala, "java.lang.Long.bitCount(" + quotedArg(0) + ")"))
    impl (abs) (codegen($cala, "java.lang.Math.abs(" + quotedArg(0) + ")"))
    impl (exp) (codegen($cala, "java.lang.Math.exp(" + quotedArg(0) + ")"))
    impl (log) (codegen($cala, "java.lang.Math.log(" + quotedArg(0) + ")"))
    impl (log10) (codegen($cala, "java.lang.Math.log10(" + quotedArg(0) + ")"))
    impl (sqrt) (codegen($cala, "java.lang.Math.sqrt(" + quotedArg(0) + ")"))
    impl (ceil) (codegen($cala, "java.lang.Math.ceil(" + quotedArg(0) + ")"))
    impl (floor) (codegen($cala, "java.lang.Math.floor(" + quotedArg(0) + ")"))
    impl (round) (codegen($cala, "java.lang.Math.round(" + quotedArg(0) + ")"))
    impl (sin) (codegen($cala, "java.lang.Math.sin(" + quotedArg(0) + ")"))
    impl (sinh) (codegen($cala, "java.lang.Math.sinh(" + quotedArg(0) + ")"))
    impl (asin) (codegen($cala, "java.lang.Math.asin(" + quotedArg(0) + ")"))
    impl (cos) (codegen($cala, "java.lang.Math.cos(" + quotedArg(0) + ")"))
    impl (cosh) (codegen($cala, "java.lang.Math.cosh(" + quotedArg(0) + ")"))
    impl (acos) (codegen($cala, "java.lang.Math.acos(" + quotedArg(0) + ")"))
    impl (tan) (codegen($cala, "java.lang.Math.tan(" + quotedArg(0) + ")"))
    impl (tanh) (codegen($cala, "java.lang.Math.tanh(" + quotedArg(0) + ")"))
    impl (atan) (codegen($cala, "java.lang.Math.atan(" + quotedArg(0) + ")"))
    impl (atan2) (codegen($cala, "java.lang.Math.atan2(" + quotedArg(0) + ", " + quotedArg(1) + ")"))
    impl (pow) (codegen($cala, "java.lang.Math.pow(" + quotedArg(0) + ", " + quotedArg(1) + ")"))
    impl (max) (codegen($cala, "java.lang.Math.max(" + quotedArg(0) + ", " + quotedArg(1) + ")"))
    impl (min) (codegen($cala, "java.lang.Math.min(" + quotedArg(0) + ", " + quotedArg(1) + ")"))

    for (g <- List(cuda, cpp)) {
      impl (abs) (codegen(g, "fabs(" + quotedArg(0) + ")"))
      impl (exp) (codegen(g, "exp(" + quotedArg(0) + ")"))
      impl (log) (codegen(g, "log(" + quotedArg(0) + ")"))
      impl (log10) (codegen(g, "log10(" + quotedArg(0) + ")"))
      impl (sqrt) (codegen(g, "sqrt(" + quotedArg(0) + ")"))
      impl (ceil) (codegen(g, "ceil(" + quotedArg(0) + ")"))
      impl (floor) (codegen(g, "floor(" + quotedArg(0) + ")"))
      impl (round) (codegen(g, "(int64_t) round(" + quotedArg(0) + ")"))
      impl (sin) (codegen(g, "sin(" + quotedArg(0) + ")"))
      impl (sinh) (codegen(g, "sinh(" + quotedArg(0) + ")"))
      impl (asin) (codegen(g, "asin(" + quotedArg(0) + ")"))
      impl (cos) (codegen(g, "cos(" + quotedArg(0) + ")"))
      impl (cosh) (codegen(g, "cosh(" + quotedArg(0) + ")"))
      impl (acos) (codegen(g, "acos(" + quotedArg(0) + ")"))
      impl (tan) (codegen(g, "tan(" + quotedArg(0) + ")"))
      impl (tanh) (codegen(g, "tan(" + quotedArg(0) + ")"))
      impl (atan) (codegen(g, "atan(" + quotedArg(0) + ")"))
      impl (atan2) (codegen(g, "atan2(" + quotedArg(0) + ", " + quotedArg(1) + ")"))
      impl (pow) (codegen(g, "pow(" + quotedArg(0) + ", " + quotedArg(1) + ")"))
      impl (max) (codegen(g, "fmax(" + quotedArg(0) + ", " + quotedArg(1) + ")"))
      impl (min) (codegen(g, "fmin(" + quotedArg(0) + ", " + quotedArg(1) + ")"))
    }
  }

  def importTuples() = {
    val e = tpePar("_")

    // only go to 10 for the sake of compile-time overhead (for now)
    for (arity <- (2 until maxTuples)) {
      val pars = (0 until arity).map(i => tpePar(('A'.toInt+i).toChar.toString)).toList
      val elems = (0 until arity).map(i => "_" + (i+1))

      // the abstract name needs to be different than the Scala name, since we don't want to shadow it.
      val TT = tpe("Tup" + arity, pars)
      data(TT, elems.zip(pars): _*)

      for (i <- 0 until arity) {
        val concrete = pars.zipWithIndex.map(t => if (t._2 == i) t._1 else e)
        infix (TT) ("_"+(i+1), pars(i), TT(concrete: _*) :: pars(i)) implements getter(0, elems(i))
      }

      val CT = tpe("Tuple"+arity, pars, stage = compile)
      val unpackTupleStr = "(" + elems.zipWithIndex.map(t => "tup"+arity+"__"+(t._2+1)+"(t)").mkString("(",",",")") + ")"
      val parStr = elems.map(e => "t."+e)

      direct (TT) ("unpack", pars, ("t",TT(pars: _*)) :: CT(pars: _*)) implements composite ${ \$unpackTupleStr }
      direct (TT) ("pack", pars, ("t",CT(pars: _*)) :: TT(pars: _*)) implements composite ("internal_pack" + arity + "(" + parStr.mkString(",") + ")")

      // internal_pack is necessary so that we don't store a stage = now type (CT) in a Delite IR node, which expects Reps.
      val argStr = (0 until pars.length).map(i => unit(quotedArg(i)))
      internal (TT) ("internal_pack" + arity, pars, (pars :: TT(pars: _*))) implements allocates(TT, argStr: _*)

      val makeTupleStrStr = "\"(\"+" + (1 to arity).map(i => "t._"+i).mkString("+\",\"+") + "+\")\""
      infix (TT) ("toString", pars, ("t",TT(pars: _*)) :: MString) implements composite ${ \$makeTupleStrStr }
    }

    // using an implicit conversion requires us to name all of the type parameters, whereas infix does not
    for (arity <- 1 until maxTuples) {
      mustInfixList ::= "_" + arity
    }

    // add pack for Var combinations inside Tuple2s. We don't do this for all of them,
    // since the number of T,Var[T],Rep[T] combinations is exponential in the size of the tuple
    val Tuple2 = lookupTpe("Tup2")
    val A = tpePar("A")
    val B = tpePar("B")
    direct (Tuple2) ("pack", (A,B), CTuple2(MVar(A),B) :: Tuple2(A,B)) implements redirect ${ tup2_pack(($0._1,$0._2)) }
    direct (Tuple2) ("pack", (A,B), CTuple2(A,MVar(B)) :: Tuple2(A,B)) implements redirect ${ tup2_pack(($0._1,$0._2)) }
    direct (Tuple2) ("pack", (A,B), CTuple2(MVar(A),MVar(B)) :: Tuple2(A,B)) implements redirect ${ tup2_pack(($0._1,$0._2)) }
  }

  // Applications may need direct access to ForgeArrays, if, for example, they use string fsplit
  // but don't want to expose all operations on arrays to users
  def importForgeArrayAPI() {
    val T = tpePar("T")
    val R = tpePar("R")

    val ForgeArrayAPI = grp("ForgeArrayAPI")
    infix (ForgeArrayAPI) ("apply", T, (MArray(T), MInt) :: T) implements composite ${ array_apply($0, $1) }
    infix (ForgeArrayAPI) ("length", T, MArray(T) :: MInt) implements composite ${ array_length($0) }

    internal.infix (ForgeArrayAPI) ("update", T, (MArray(T), MInt, T) :: MUnit, effect = write(0)) implements composite ${ array_update($0, $1, $2) }
    internal.infix (ForgeArrayAPI) ("map", (T,R), (MArray(T), T ==> R) :: MArray(R)) implements composite ${ array_map($0, $1) }
    internal.infix (ForgeArrayAPI) ("Clone", T, MArray(T) :: MArray(T)) implements composite ${ array_clone($0) }
  }

  // Forge's HashMap is not mutable, so a Scala HashMap can be used if updates are necessary.
  def importHashMap() = {
    val K = tpePar("K")
    val V = tpePar("V")
    val T = tpePar("T")

    // in order to define lifted operations on an existing Scala type, we must place the lifted ops in a separate group
    // to avoid Forge attempting to use the fully qualified type name in traits
    /** @nodoc **/
    val SArray = tpe("scala.Array", T)
    /** @nodoc **/
    val SHashMap = tpe("scala.collection.mutable.HashMap", (K,V))
    /** @nodoc **/
    val HashMapOps = grp("SHashMap")

    val hashmap = direct (HashMapOps) ("SHashMap", (K,V), Nil :: SHashMap(K,V), effect = mutable)
    impl (hashmap) (codegen($cala, ${ new scala.collection.mutable.HashMap[$t[K],$t[V]]() }))
    // impl (hashmap) (codegen(cpp, ${ new std::map<$t[K],$t[V]>() }))

    internal (HashMapOps) ("shashmap_from_arrays", (K,V), (MArray(K),MArray(V)) :: SHashMap(K,V), effect = mutable) implements codegen($cala, ${ scala.collection.mutable.HashMap($0.zip($1): _*) })
    val keys_array = internal (HashMapOps) ("shashmap_keys_array", (K,V), (SHashMap(K,V)) :: SArray(K))
    val values_array = internal (HashMapOps) ("shashmap_values_array", (K,V), (SHashMap(K,V)) :: SArray(V))
    impl (keys_array) (codegen($cala, ${ $0.keys.toArray }))
    impl (values_array) (codegen($cala, ${ $0.values.toArray }))
    // impl (keys_array) (codegen(cpp, "new " + unquotes("remap(sym.tp)") + ${ ($0->size()); int keys_idx_$0 = 0; for(std::map<$t[K],$t[V]>::iterator it = $0->begin(); it != $0->end(); ++it) } + unquotes("quote(sym)") + ${->update(keys_idx_$0++, it->first); }))
    // impl (values_array) (codegen(cpp, "new " + unquotes("remap(sym.tp)") + ${ ($0->size()); int values_idx_$0 = 0; for(std::map<$t[K],$t[V]>::iterator it = $0->begin(); it != $0->end(); ++it) } + unquotes("quote(sym)") + ${->update(values_idx_$0++, it->second); }))

    val apply = infix (HashMapOps) ("apply", (K,V), (SHashMap(K,V), K) :: V)
    val update = infix (HashMapOps) ("update", (K,V), (SHashMap(K,V), K, V) :: MUnit, effect = write(0))
    val contains = infix (HashMapOps) ("contains", (K,V), (SHashMap(K,V), K) :: MBoolean)
    infix (HashMapOps) ("keys", (K,V), SHashMap(K,V) :: MArray(K)) implements composite ${ farray_from_sarray(shashmap_keys_array($0)) }
    infix (HashMapOps) ("values", (K,V), SHashMap(K,V) :: MArray(V)) implements composite ${ farray_from_sarray(shashmap_values_array($0)) }

    impl (apply) (codegen($cala, ${ $0($1) }))
    // impl (apply) (codegen(cpp, ${ $0->find($1)->second }))
    impl (update) (codegen($cala, ${ $0.put($1,$2); () }))
    // impl (update) (codegen(cpp, ${ $0->insert(std::pair<$t[K],$t[V]>($1,$2)) }))
    impl (contains) (codegen($cala, ${ $0.contains($1) }))
    // impl (contains) (codegen(cpp, ${ $0->find($1) != $0->end() }))
  }

  def importConcurrentHashMap() = {
    val K = tpePar("K")
    val V = tpePar("V")
    val T = tpePar("T")

    /** @nodoc **/
    val SArray = tpe("scala.Array", T)
    /** @nodoc **/
    val CHashMap = tpe("java.util.concurrent.ConcurrentHashMap", (K,V))
    /** @nodoc **/
    val HashMapOps = grp("CHashMap")

    direct (HashMapOps) ("CHashMap", (K,V), Nil :: CHashMap(K,V), effect = mutable) implements codegen($cala, ${ new java.util.concurrent.ConcurrentHashMap[$t[K],$t[V]]() })
    internal (HashMapOps) ("chashmap_from_arrays", (K,V), (MArray(K),MArray(V)) :: CHashMap(K,V), effect = mutable) implements codegen($cala, ${
      val map = new java.util.concurrent.ConcurrentHashMap[$t[K],$t[V]]()
      for (i <- 0 until $0.length) {
        map.put($0(i),$1(i))
      }
      map
    })
    internal (HashMapOps) ("chashmap_keys_array", (K,V), (CHashMap(K,V)) :: SArray(K)) implements codegen($cala, ${ scala.collection.JavaConverters.enumerationAsScalaIteratorConverter($0.keys).asScala.toArray })
    internal (HashMapOps) ("chashmap_values_array", (K,V), (CHashMap(K,V)) :: SArray(V)) implements codegen($cala, ${ scala.collection.JavaConverters.collectionAsScalaIterableConverter($0.values).asScala.toArray })

    infix (HashMapOps) ("apply", (K,V), (CHashMap(K,V), K) :: V) implements codegen($cala, ${ $0.get($1) })
    infix (HashMapOps) ("update", (K,V), (CHashMap(K,V), K, V) :: MUnit, effect = write(0)) implements codegen($cala, ${ $0.put($1,$2); () })
    infix (HashMapOps) ("contains", (K,V), (CHashMap(K,V), K) :: MBoolean) implements codegen($cala, ${ $0.containsKey($1) })
    infix (HashMapOps) ("keys", (K,V), CHashMap(K,V) :: MArray(K)) implements composite ${ farray_from_sarray(chashmap_keys_array($0)) }
    infix (HashMapOps) ("values", (K,V), CHashMap(K,V) :: MArray(V)) implements composite ${ farray_from_sarray(chashmap_values_array($0)) }
  }

  def importConcurrentQueue() = {
    val T = tpePar("T")
    val CQueue = tpe("java.util.concurrent.ArrayBlockingQueue", T)
    val CQueueOps = grp("CQueue")

    direct (CQueueOps) ("CQueue", T, MInt :: CQueue(T), effect = mutable) implements codegen($cala, ${ new java.util.concurrent.ArrayBlockingQueue[$t[T]]($0) })

    infix (CQueueOps) ("add", T, (CQueue(T), T) :: MBoolean) implements codegen($cala, ${ $0.add($1) })
    infix (CQueueOps) ("contains", T, (CQueue(T), T) :: MBoolean) implements codegen($cala, ${ $0.contains($1) })
    infix (CQueueOps) ("peek", T, (CQueue(T) :: T)) implements codegen($cala, ${ $0.peek() })
    infix (CQueueOps) ("poll", T, (CQueue(T) :: T)) implements codegen($cala, ${ $0.poll() })
  }

  def importThreads() = {
    val SThread = tpe("java.lang.Thread")
    val SThreadOps = grp("SThread")

    infix (SThreadOps) ("interrupt", Nil, SThread :: MUnit, effect = simple) implements codegen($cala, ${
      $0.interrupt()
    })

    infix (SThreadOps) ("join", Nil, SThread :: MUnit, effect = simple) implements codegen($cala, ${
      $0.join()
    })
  }

  def importByteBuffer() = {
    /** @nodoc **/
    val ByteBuffer = tpe("java.nio.ByteBuffer")
    /** @nodoc **/
    val IntBuffer = tpe("java.nio.IntBuffer")
    /** @nodoc **/
    val DoubleBuffer = tpe("java.nio.DoubleBuffer")

    /** @nodoc **/
    val ByteBufferOps = grp("SByteBuffer")
    direct (ByteBufferOps) ("ByteBuffer", Nil, MInt :: ByteBuffer, effect = mutable) implements codegen($cala, ${ java.nio.ByteBuffer.allocate($0) })
    direct (ByteBufferOps) ("ByteBufferWrap", Nil, MArray(MByte) :: ByteBuffer, effect = mutable) implements codegen($cala, ${ java.nio.ByteBuffer.wrap($0) })

    infix (ByteBufferOps) ("rewind", Nil, ByteBuffer :: MUnit, effect = write(0)) implements codegen($cala, ${ $0.rewind(); () })
    infix (ByteBufferOps) ("array", Nil, ByteBuffer :: MArray(MByte)) implements codegen($cala, ${ $0.array })

    infix (ByteBufferOps) ("getInt", Nil, ByteBuffer :: MInt) implements codegen($cala, ${ $0.getInt() })
    infix (ByteBufferOps) ("getDouble", Nil, ByteBuffer :: MDouble) implements codegen($cala, ${ $0.getDouble() })
    infix (ByteBufferOps) ("putInt", Nil, (ByteBuffer, MInt) :: ByteBuffer, effect = write(0)) implements codegen($cala, ${ $0.putInt($1) })
    infix (ByteBufferOps) ("putDouble", Nil, (ByteBuffer, MDouble) :: ByteBuffer, effect = write(0)) implements codegen($cala, ${ $0.putDouble($1) })

    // We deviate slightly from the actual ByteBuffer API here to observe our nested mutability rules by chaining the operations together implicitly.
    infix (ByteBufferOps) ("get", Nil, (ByteBuffer, MArray(MInt), MInt, MInt) :: IntBuffer, effect = write(1)) implements codegen($cala, ${ $0.asIntBuffer.get($1, $2, $3) })
    infix (ByteBufferOps) ("get", Nil, (ByteBuffer, MArray(MDouble), MInt, MInt) :: DoubleBuffer, effect = write(1)) implements codegen($cala, ${ $0.asDoubleBuffer.get($1, $2, $3) })
    infix (ByteBufferOps) ("put", Nil, (ByteBuffer, MArray(MInt), MInt, MInt) :: IntBuffer, effect = write(0)) implements codegen($cala, ${ $0.asIntBuffer.put($1, $2, $3) })
    infix (ByteBufferOps) ("put", Nil, (ByteBuffer, MArray(MDouble), MInt, MInt) :: DoubleBuffer, effect = write(0)) implements codegen($cala, ${ $0.asDoubleBuffer.put($1, $2, $3) })
  }
}
