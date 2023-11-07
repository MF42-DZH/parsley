/*
 * Copyright 2020 Parsley Contributors <https://github.com/j-mie6/Parsley/graphs/contributors>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.debugger.util

// Sadly, no reflective capabilities exist in Scala 3 yet, so these
// methods don't do anything yet.
// TODO: Find a substitute for reflection for Scala 3.
private [parsley] object XCollector extends XDummyCollector
