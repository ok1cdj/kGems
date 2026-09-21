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

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShuffleTest {

    @Test
    fun shufflePreservesTheMultisetAndStaysPlayable() {
        val failures = mutableListOf<String>()
        val rng = Rng(1234L)
        repeat(2_000) { i ->
            val original = Generator.newBoard(rng)
            val shuffled = Generator.shuffle(original, rng)

            if (!typeCounts(original).contentEquals(typeCounts(shuffled))) {
                failures += "shuffle #$i changed the gem multiset"
            }
            if (Engine.findMatches(shuffled).isNotEmpty()) failures += "shuffle #$i has a match"
            if (!Engine.hasValidMove(shuffled)) failures += "shuffle #$i has no valid move"
        }
        assertTrue(failures.isEmpty()) { "shuffle invariants broken: ${failures.take(10)}" }
    }

    @Test
    fun shuffleIsAPermutationOfExactlyTheSameCells() {
        val rng = Rng(99L)
        val original = Generator.newBoard(rng)
        val shuffled = Generator.shuffle(original, rng)
        // Sorting both cell arrays must give identical sequences (same multiset).
        assertArrayEquals(original.cells.sortedArray(), shuffled.cells.sortedArray())
    }
}
