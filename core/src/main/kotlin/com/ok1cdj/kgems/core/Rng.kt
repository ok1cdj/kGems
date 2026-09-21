/*
 * kGems — match-3 puzzle for the Mudita Kompakt
 * Copyright (C) 2026 Ondrej Kolonicny (OK1CDJ)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.ok1cdj.kgems.core

// SplitMix64 constants. Written as unsigned literals converted to Long so the
// full 64-bit pattern is unambiguous (the golden-ratio gamma has bit 63 set).
private val GAMMA: Long = 0x9E3779B97F4A7C15uL.toLong()
private val MIX_1: Long = 0xBF58476D1CE4E5B9uL.toLong()
private val MIX_2: Long = 0x94D049BB133111EBuL.toLong()

/**
 * A seedable, serializable SplitMix64 PRNG.
 *
 * The whole game (board generation, refills, shuffles) draws from one of these,
 * NOT [java.util.Random], because the entire point is exact reproducibility:
 * the same [state] followed by the same sequence of calls yields a bit-identical
 * stream on every platform. That is what lets a game be saved and resumed —
 * including all future refills — by persisting a single [Long] (see
 * [serialize]).
 */
class Rng(var state: Long) {

    /** Next 64-bit value; advances [state] by the golden-ratio gamma, then mixes. */
    fun nextLong(): Long {
        state += GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /**
     * A uniformly distributed `Int` in `0 until [bound]`, with no modulo bias
     * (rejection sampling over the 63-bit non-negative range).
     */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, got $bound" }
        while (true) {
            val bits = nextLong() ushr 1            // 0 .. 2^63-1
            val value = bits % bound
            if (bits - value + (bound - 1) >= 0) {  // no overflow ⇒ unbiased
                return value.toInt()
            }
        }
    }

    /** An independent copy positioned at the same [state]. */
    fun copy(): Rng = Rng(state)

    /** The state as a decimal string — an exact, lossless round-trip. */
    fun serialize(): String = state.toString()

    companion object {
        /** Restore an [Rng] from [serialize]. */
        fun deserialize(text: String): Rng = Rng(text.toLong())
    }
}
