package co.edu.eafit.dis.progfun.catsintro

import cats.Functor // Import the Functor type class.
import cats.instances.function._ // Brings the implicit Functor[X => _] instance to scope.
import cats.instances.int._ // Brings the implicit Eq[Int] instance to scope.
import cats.instances.list._ // Brings the implicit Functor[List[_]] instance to scope.
import cats.instances.string._ // Brings the implicit Eq[String] instance to scope.
import cats.syntax.eq.catsSyntaxEq // Provides the === operator for type safe equality checks.
import cats.syntax.functor.toFunctorOps // Provides the map operator for mapping a Functor.
import scala.language.higherKinds // Enable the use of higher-kinded types, like F[_].

object FunctorNotes extends App {
  println("- Functor -")
  println("-- The map function --")

  // Map allow us to sequence computations inside a context.
  val mapped1 =
    List(1, 2, 3)
      .map(x => x + 1)
      .map(x => x * 2)
      .map(x => s"${x}!")
  println(s"List(1, 2, 3).map(x => x + 1).map(x => x * 2).map(x => s'$${x}!') = ${mapped1}")

  // This common behavior is encapsulated in the Functor[F[_]] type class.
  val assertion1 = List(1, 2, 3).map(x => x + 1) === Functor[List].map(List(1, 2, 3))(x => x + 1)
  println(s"List(1, 2, 3).map(x => x + 1) === Functor[List].map(List(1, 2, 3))(x => x + 1) is ${assertion1}")

  // Note that the 'map' method in Functor is curryfied, thus we could use it to
  // encapsulate the data we would like to map, and return a function,
  // which requires another function as an input and returns the mapped data.
  val mappeable: (Int => Int) => List[Int] = Functor[List].map(List(1, 2, 3)) _
  val mapped2 = mappeable(x => x + 1)
  println(s"Given mappeable = Functor[List].map(List(1, 2, 3))\t->\tmappeable(x => x + 1) = ${mapped2}")
  // This is very powerful. However, since a function can't be polymorphic (only methods),
  // we are forced to specify the output type of the inner function (which must be the same of the output type parameter).
  // If we don't specify this value, we would end with the following type: (Int => Nothing) => List[Nothing], which is absolute useless.
  // We would like it to have the following type signature: [T] -> (Int => T) => List[T], and resolve the T type parameter in each call site.
  // Hope Dotty#4672 gets merged for Scala 3 - which adds this functionality. https://github.com/lampepfl/dotty/pull/4672
  println()

  // We could also use the Functors 'lift' method to transform a function A => B into a function F[A] => F[B]
  println("-- The lift function -- ")
  val fun: Int => Int = x => x + 1
  val lifted: List[Int] => List[Int] = Functor[List].lift(fun)
  val mapped3 = lifted(List(1, 2, 3))
  println(s"Given lifted = Functor[List].lift(x => x + 1)\t->\tlifted(List(1, 2, 3)) = ${mapped3}")
  // Once again, this is very powerful. However, again we are limited in our expressiveness,
  // Since we can't have polymorphic functions, we need to specify the context for which this function is lifted.
  // But we would like to create a function with the following type signature:
  // [A, B, F[_]] -> (A => B) => (implicit Functor[F]) => (F[A] => F[B])
  println()

  // Functions as Functors!
  // It turns out that single argument functions are also functors.
  // But since Function1[-In, +Out] (In => Out) has two type parameters, and Functor expects somthing with only one.
  // We need to coerce them to the correct shape by fixing one of their types - usually we fix the input type, and let the output type  vary.
  println("-- Functions as Functors --")
  val fun1: Int => Int = x => x + 1
  val fun2: Int => String = x => s"${x}!"
  val fun3: Int => String = fun1 map fun2
  val mapped4 = fun3(1)
  println(s"((x => x + 1) map (x => s'$${x}!'))(1) = ${mapped4}")
  // We can see that mapping over functions is the same as function composition using 'and0Then'.
  // Which at the same time is the same as calling the second function after calling the first one.
  val andThenComposed = (fun1 andThen fun2)(1)
  val normalComposed = fun2(fun1(1))
  println(s"((x => x + 1) map (x => s'$${x}!'))(1) === ((x => x + 1) andThen (x => s'$${x}!'))(1) is ${mapped4 === andThenComposed}")
  println(s"((x => x + 1) map (x => s'$${x}!'))(1) === (x => s'$${x + 1}!') is ${mapped4 === normalComposed}")
  println()

  // Functor laws!
  // A correct implementation of a Functor for some type F[_] must satisfy two laws:
  // Identity: Calling map on some fa with the identity function must return the same fa.
  // Composition: Given two functions f and g, and a functor fa then mapping fa with f and then with g, must be the same as mapping fa with f andThen g.
  println("-- Functor laws --")
  val fa = List(1, 2, 3)
  val f: Int => Int = x => x + 1
  val g: Int => Int = x => x * 2
  println(s"List(1, 2, 3).map(identity) === ${fa.map(identity)}")
  val assetion2 = fa.map(f).map(g) === fa.map(f andThen g)
  println(s"Given f = x => x + 1 & g = x => x * 2 & fa = List(1, 2, 3)\t->\tfa.map(f).map(g) === fa.map(f andThen g) is ${assetion2}")
  println()

  // Generic functions!
  // We can use the Functor typeclass to write a generic function
  // that sums one to a value inside any context F which provides a map method.
  println("-- Generic functions using Functor --")
  def sumOne[F[_]: Functor](fa: F[Int]): F[Int] = fa.map(x => x + 1)
  val summedList = sumOne(List(1, 2, 3))
  println(s"Given sumOne[F[_]: Functor](fa: F[Int]): F[Int] = fa.map(x => x + 1)")
  println(s"\tsumOne(List(1, 2, 3)) = ${summedList}")

  // Let's define our own instance of Functor for Options.
  implicit val OptionFunctor: Functor[Option] = new Functor[Option] {
    override def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa match {
      case Some(a) => Some(f(a))
      case None    => None
    }
  }
  val summedOption = sumOne(Option(41))
  println(s"\tsumOne(Option(41)) = ${summedOption}")

  // Let's create a Functor for a binary tree.
  sealed trait Tree[+A]
  final case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]
  final case class Leaf[A](value: A) extends Tree[A]
  implicit val TreeFunctor: Functor[Tree] = new Functor[Tree] {
    override def map[A, B](fa: Tree[A])(f: A => B): Tree[B] = fa match {
      case Branch(left, right) => Branch(map(left)(f), map(right)(f))
      case Leaf(a)             => Leaf(f(a))
    }
  }
  val tree: Tree[Int] = Branch(left  = Leaf(10), right = Branch(left  = Leaf(3), right = Leaf(5)))
  val summedTree = sumOne(tree)
  println(s"\tsumOne(Branch(Leaf(10),Branch(Leaf(3),Leaf(5)))) = ${summedTree}")
  println()

  // Functor composition.
  // Functors are composable, that means given a Functor[F[_]] and a Functor[G[_]],
  // You can create a Functor[F[G[_]]], which will map values inside the two contexts a the same time.
  println("-- Functor composition --")
  val ListOptionFunctor: Functor[Lambda[A => List[Option[A]]]] = Functor[List].compose[Option]
  val mapped5 = ListOptionFunctor.map(List(Some(3), None, Some(5)))(x => x + 1)
  println(s"Functor[List].compose[Option].map(List(Some(3), None, Some(5)))(x => x + 1) = ${mapped5}")
  // Note: For a more general solution to the above problem use Cat's Nested data type.
  // https://typelevel.org/cats/typeclasses/functor.html#functors-compose
  // https://typelevel.org/cats/datatypes/nested.html
  println()

  // Contravariant Functors.
  // If normal (covariant) Functors allow us to append operations inside a context,
  // Contravariant Functors allow us to prepend operations to the chain using the contramap method.
  println("-- Contravariant Functors --")
  trait Printer[A] {
    self =>
    def format(value: A): String
    def contramap[B](f: B => A): Printer[B] = new Printer[B] {
      override def format(value: B): String = self.format(f(value))
    }
  }
  val StringPrinter: Printer[String] = new Printer[String] {
    override def format(value: String): String = value
  }
  val IntPrinter: Printer[Int] = StringPrinter.contramap(x => s"${x}!")
  println(s"Given IntPrinter = Printer[String].contramap(x => s'$${x}')\t->\tIntPrinter.format(1) = ${IntPrinter.format(1)}")
  println()

  // Invariant Functors.
  // Invariant Functors allows us to both append & prepend operations inside a context.
  // The imap method is informally equivalent to a convination of map and contramap.
  println("-- Invariant Functors --")
  trait Codec[A] {
    self =>
    def encode(value: A): String
    def decode(data: String): A
    def imap[B](dec: A => B, enc: B => A): Codec[B] = new Codec[B] {
      override def encode(value: B): String = self.encode(enc(value))
      override def decode(data: String): B = dec(self.decode(data))
    }
  }
  val StringCodec: Codec[String] = new Codec[String] {
    override def encode(value: String): String = value
    override def decode(data: String): String = data
  }
  val IntCodec: Codec[Int] = StringCodec.imap(s => s.toInt, x => x.toString)
  val encoded = IntCodec.encode(10)
  val decoded = IntCodec.decode(encoded)
  println(s"Given IntCodec = Codec[String].imap(s => s.toInt, x => x.toString)\t->\tIntCodec.decode(IntCodec.encode(10)) = ${decoded}")
}
