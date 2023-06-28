/* SPDX-FileCopyrightText: © 2023 Parsley Contributors <https://github.com/j-mie6/Parsley/graphs/contributors>
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.internal.deepembedding.frontend.debugger

import scala.annotation.nowarn
import scala.collection.mutable

import parsley.debugger.internal.DebugContext

import parsley.internal.deepembedding.backend.StrictParsley
import parsley.internal.deepembedding.frontend
import parsley.internal.deepembedding.frontend.LazyParsley

object helpers {
  private [parsley] def traverseDown[A]
  (parser: LazyParsley[A])
  (implicit seen: mutable.Map[LazyParsley[_], Debugged[_]], dbgCtx: DebugContext): LazyParsley[A] = {
  // This stops recursive parsers from causing an infinite recursion.
    val (usedParser_, optName) = parser match {
      case Named(par, name) => (par, Some(name))
      case _                => (parser, None)
    }

    val usedParser = usedParser_.asInstanceOf[LazyParsley[A]]

    if (seen.contains(usedParser)) {
      // Return a parser with a debugger attached.
      seen(usedParser).asInstanceOf[Debugged[A]]
    } else {
      val current = new Debugged[A](usedParser, None, None)

      // Without this, we could potentially have infinite recursion from lazy-initialised parsers.
      seen.put(usedParser, current)

      // Function is buried in the frontend package to facilitate access to the GeneralisedEmbedding
      // abstract classes and their getters.
      implicit val attached: List[LazyParsley[Any]] = getChildren(usedParser).map(traverseDown(_))

      // Populate our parser with the new debugged children.
      current.par = Some(reconstruct(usedParser))

      // Return a parser with a debugger attached.
      optName match {
        case None       => seen(usedParser).asInstanceOf[Debugged[A]]
        case Some(name) => seen(usedParser).asInstanceOf[Debugged[A]].withName(name)
      }
    }
  }

  // Attempt to retrieve the child parsers.
  private [this] def getChildren(parser: LazyParsley[_]): List[LazyParsley[_]] =
    parser match {
      case frontend.Unary(par)       => List(par)
      case frontend.Binary(l, r)     => List(l, r)
      case frontend.Ternary(f, s, t) => List(f, s, t)
      case frontend.<|>(p, q)        => List(p, q)
      case frontend.ChainPre(p, op)  => List(p, op)
      case _ =>
        // This catches all atomic parsers (e.g. satisfy parsers).
        Nil
    }

  // XXX: Very unsafe due to asInstanceOf call.
  private [this] def coerce[A](ix: Int)(implicit children: List[LazyParsley[Any]]): LazyParsley[A] =
    children(ix).asInstanceOf[LazyParsley[A]]

  // Reconstruct the original parser with new components.
  // XXX: Very unsafe due to call to coerce, and there is unsafe type matching on runtime-erased
  //      types, but there should be no reason that a ClassCastException will occur as we are not
  //      creating new type information, just using some from before that was erased in order to
  //      make this method more generic.
  @nowarn private [this] def reconstruct[A, X, Y, Z]
  (parser: LazyParsley[A])
  (implicit children: List[LazyParsley[Any]]): LazyParsley[A] =
    parser match {
      case par: frontend.Unary[X, A] =>
        new frontend.Unary[X, A](coerce[X](0)) {
          override def make(p: StrictParsley[X]): StrictParsley[A] = par.make(p)
        }
      case par: frontend.Binary[X, Y, A] =>
        new frontend.Binary[X, Y, A](coerce[X](0), coerce[Y](1)) {
          override def make(p: StrictParsley[X], q: StrictParsley[Y]): StrictParsley[A] = par.make(p, q)
        }
      case par: frontend.Ternary[X, Y, Z, A] =>
        new frontend.Ternary[X, Y, Z, A](coerce[X](0), coerce[Y](1), coerce[Z](2)) {
          override def make(p: StrictParsley[X], q: StrictParsley[Y], r: StrictParsley[Z]): StrictParsley[A] =
            par.make(p, q, r)
        }
      case _: frontend.<|>[A] =>
        new frontend.<|>[A](coerce[A](0), coerce[A](1))
      case _: frontend.ChainPre[A] =>
        new frontend.ChainPre[A](coerce[A](0), coerce[A => A](1))
      case _ => parser
    }
}
