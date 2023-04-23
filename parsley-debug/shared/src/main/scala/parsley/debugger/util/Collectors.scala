package parsley.debugger.util

import parsley.Parsley
import parsley.debugger.internal.Rename
import parsley.token.Lexer

import parsley.internal.deepembedding.frontend.LazyParsley

/** Attempt to collect all the fields in a class or object that contain a
  * parser of type [[Parsley]].
  *
  * This information is used later in the debug tree-building process to rename certain parsers
  * so that they do not end up being named things like "packageanon".
  *
  * You only need to run this once per parser-holding object.
  */
object CollectNames {
  def apply(obj: Any)(implicit collector: CollectorImpl): Unit =
    Rename.addNames(collector.collectNames(obj))
}

/** A [[CollectNames]] derivative designed specifically to work on [[Lexer]] instances
  * to collect all the automatically-derived parsers.
  */
object CollectLexer {
  def apply(lexer: Lexer)(implicit collector: CollectorImpl): Unit =
    Rename.addNames(collector.collectLexer(lexer))
}

/** A representation of the current implementation that [[CollectNames]] uses in order to
  * actually collect the names of parsers. This should be implicitly available should
  * you import `parsley.debugger.util.CollectNamesImpl`.
  */
abstract class CollectorImpl private [parsley] () {
  /** Collect names of parsers from an object. */
  def collectNames(obj: Any): Map[LazyParsley[_], String]

  /** Collect names of parsers from a [[Lexer]]. */
  def collectLexer(lexer: Lexer): Map[LazyParsley[_], String]

  // Try grabbing a parser from a LazyParsley or Parsley instance.
  private def tryExtract(p: Any): LazyParsley[_] = {
    try {
      p.asInstanceOf[LazyParsley[_]]
    } catch {
      case _: ClassCastException => p.asInstanceOf[Parsley[_]].internal
    }
  }
}
