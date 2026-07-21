package mardek.content.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * This is basically an `Array<Duration>`, but stored using a `LongArray` rather than an `Array<Long>`.
 *
 * This may have performance benefits, and is much more convenient for bitser.
 */
@JvmInline
value class DurationArray(private val raw: LongArray) {

	constructor(array: Array<Duration>) : this(array.map { it.inWholeNanoseconds }.toLongArray())

	val size: Int
		get() = raw.size

	operator fun get(index: Int) = raw[index].nanoseconds
}
