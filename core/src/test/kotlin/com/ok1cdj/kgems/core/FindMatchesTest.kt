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
 * [Engine.findMatches] over hand-authored boards. Every fixture is a 0/1
 * checkerboard (which has no run) with the run under test injected using types
 * 2..6, so the injected gems can only ever match each other — never the filler.
 */
class FindMatchesTest {

    @Test
    fun noMatchesOnACheckerboard() {
        val b = board(
            "01010101", "10101010", "01010101", "10101010",
            "01010101", "10101010", "01010101", "10101010",
        )
        assertTrue(Engine.findMatches(b).isEmpty())
    }

    @Test
    fun emptyBoardHasNoMatches() {
        // A board of nothing but EMPTY sentinels must not report a run.
        val b = Board(ByteArray(CELLS) { EMPTY })
        assertTrue(Engine.findMatches(b).isEmpty())
    }

    @Test
    fun horizontalRunOfThree() {
        val b = board(
            "01555101", "10101010", "01010101", "10101010",
            "01010101", "10101010", "01010101", "10101010",
        )
        assertEquals(setOf(indexOf(0, 2), indexOf(0, 3), indexOf(0, 4)), Engine.findMatches(b))
    }

    @Test
    fun horizontalRunOfFour() {
        val b = board(
            "01555501", "10101010", "01010101", "10101010",
            "01010101", "10101010", "01010101", "10101010",
        )
        assertEquals(
            setOf(indexOf(0, 2), indexOf(0, 3), indexOf(0, 4), indexOf(0, 5)),
            Engine.findMatches(b),
        )
    }

    @Test
    fun horizontalRunOfFive() {
        val b = board(
            "05555501", "10101010", "01010101", "10101010",
            "01010101", "10101010", "01010101", "10101010",
        )
        assertEquals(
            setOf(indexOf(0, 1), indexOf(0, 2), indexOf(0, 3), indexOf(0, 4), indexOf(0, 5)),
            Engine.findMatches(b),
        )
    }

    @Test
    fun verticalRunOfThree() {
        val b = board(
            "61010101", "63232323", "61010101", "10101010",
            "01010101", "10101010", "01010101", "10101010",
        )
        assertEquals(setOf(indexOf(0, 0), indexOf(1, 0), indexOf(2, 0)), Engine.findMatches(b))
    }

    @Test
    fun lShapeIsOneMergedSet() {
        // Horizontal arm (2,0)(2,1)(2,2) and vertical arm (2,0)(3,0)(4,0) share
        // the corner (2,0), which appears exactly once — five cells, not six.
        val b = board(
            "01010101", "10101010", "44410101", "40101010",
            "41010101", "10101010", "01010101", "10101010",
        )
        val expected = setOf(
            indexOf(2, 0), indexOf(2, 1), indexOf(2, 2), indexOf(3, 0), indexOf(4, 0),
        )
        assertEquals(expected, Engine.findMatches(b))
    }

    @Test
    fun tShapeIsOneMergedSet() {
        // Horizontal (5,3)(5,4)(5,5) and vertical (4,4)(5,4)(6,4) share (5,4).
        val b = board(
            "01010101", "10101010", "01010101", "10101010",
            "01016101", "10166610", "01016101", "10101010",
        )
        val expected = setOf(
            indexOf(5, 3), indexOf(5, 4), indexOf(5, 5), indexOf(4, 4), indexOf(6, 4),
        )
        assertEquals(expected, Engine.findMatches(b))
    }
}
