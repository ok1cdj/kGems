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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cascades and scoring. Refills are random, so a full-board move almost always
 * cascades several waves; with a fixed [Rng] seed the whole outcome is exact and
 * reproducible (SplitMix64 + a fixed engine), which is what these constants pin.
 */
class CascadeTest {

    // Swapping (4,3)<->(5,3) makes three 5s in row 5 (wave 1); clearing them drops
    // a 3 into column 2 to line up three 3s (wave 2); refills carry it further.
    private val cascadeBoard = board(
        "01010101", "10101010", "01010101", "10101010",
        "01350101", "15521010", "01310101", "10301010",
    )
    private val move = Move(indexOf(4, 3), indexOf(5, 3))

    @Test
    fun deterministicOutcomeForAFixedSeed() {
        val r = Engine.apply(cascadeBoard, move, Rng(0L))!!
        assertEquals(5, r.cascades)
        assertEquals(38, r.cleared)
        assertEquals(11300, r.score)
        assertEquals(1 + 2 * r.cascades, r.frames.size)
        assertTrue(Engine.findMatches(r.board).isEmpty())
    }

    @Test
    fun firstWaveIsASingleTripleWorthThirty() {
        val r = Engine.apply(cascadeBoard, move, Rng(0L))!!
        // Wave 1 is refill-independent: it is exactly the run the swap created.
        val wave1 = Engine.findMatches(r.frames[0])
        assertEquals(setOf(indexOf(5, 1), indexOf(5, 2), indexOf(5, 3)), wave1)
        // One group of 3 ⇒ 10*(3-2)*3 = 30 points, at the wave-1 multiplier of 1.
        val groups = connectedComponentSizes(wave1)
        assertEquals(listOf(3), groups)
        assertEquals(30, groups.sumOf { n -> 10 * (n - 2) * n } * 1)
    }

    @Test
    fun totalScoreMatchesThePerWaveFormulaReDerivedFromFrames() {
        val r = Engine.apply(cascadeBoard, move, Rng(0L))!!
        // Re-derive the score independently from the frames: the board *before*
        // clear of wave k is frame[2*(k-1)]; its matches are what wave k cleared.
        var expected = 0
        for (k in 1..r.cascades) {
            val before = r.frames[2 * (k - 1)]
            val waveBase = connectedComponentSizes(Engine.findMatches(before))
                .sumOf { n -> 10 * (n - 2) * n }
            expected += waveBase * k
        }
        assertEquals(expected, r.score)
    }
}
