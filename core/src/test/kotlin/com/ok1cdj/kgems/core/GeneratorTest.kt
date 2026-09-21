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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeneratorTest {

    @Test
    fun everyGeneratedBoardStartsCleanAndPlayable() {
        val failures = mutableListOf<String>()
        val rng = Rng(0xBADC0FFEEL)
        repeat(10_000) { i ->
            val b = Generator.newBoard(rng)
            if (Engine.findMatches(b).isNotEmpty()) failures += "board #$i starts with a match"
            if (!Engine.hasValidMove(b)) failures += "board #$i has no valid move"
            if (b.cells.any { it.toInt() !in 0 until TYPES }) failures += "board #$i has an out-of-range cell"
        }
        assertTrue(failures.isEmpty()) { "generator invariants broken: ${failures.take(10)}" }
    }
}
