/*
 * Copyright 2020 Parsley Contributors <https://github.com/j-mie6/Parsley/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.internal

import scala.collection.mutable

import org.typelevel.scalaccompat.annotation.unused

// For the JVM and Native, its WeakHashMap does the job.
private [parsley] final class XWeakMap[K, V](@unused startSize: Int) extends mutable.Map[K, V] { // scalastyle:ignore magic.number
    private val backing: XWeakMapImpl[K, V] = new XWeakMapImpl()

    // $COVERAGE-OFF$
    override def subtractOne(k: K): XWeakMap.this.type = {
        backing.drop(k)
        this
    }
    // $COVERAGE-ON$

    override def addOne(kv: (K, V)): XWeakMap.this.type = {
        backing.push(kv)
        this
    }

    override def get(key: K): Option[V] =
        backing.lookup(key)

    // $COVERAGE-OFF$
    override def iterator: Iterator[(K, V)] =
        backing.iterator
    // $COVERAGE-ON$
}
