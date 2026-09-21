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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplyTest {

    // Swapping (4,3)<->(5,3) lines up three 5s in row 5.
    private val playable = board(
        "01010101", "10101010", "01010101", "10101010",
        "01350101", "15521010", "01310101", "10301010",
    )
    private val goodMove = Move(indexOf(4, 3), indexOf(5, 3))

    @Test
    fun validMoveProducesFramesAndSettlesWithNoMatch() {
        val result = Engine.apply(playable, goodMove, Rng(0L))
        assertNotNull(result)
        result!!
        assertTrue(result.frames.isNotEmpty())
        assertTrue(result.cascades >= 1)
        // The final board is fully settled: no run remains, no hole remains.
        assertTrue(Engine.findMatches(result.board).isEmpty())
        assertFalse(result.board.cells.any { it == EMPTY })
        // frames = one swap frame + (clear, settle) per wave.
        assertEquals(1 + 2 * result.cascades, result.frames.size)
    }

    @Test
    fun invalidSwapReturnsNullAndTouchesNothing() {
        // On a checkerboard, swapping two adjacent cells never forms a run.
        val cb = board(
            "01010101", "10101010", "01010101", "10101010",
            "01010101", "10101010", "01010101", "10101010",
        )
        val before = cb.cells.copyOf()
        val rng = Rng(42L)
        val stateBefore = rng.state

        val result = Engine.apply(cb, Move(indexOf(0, 0), indexOf(0, 1)), rng)

        assertNull(result)
        assertTrue(cb.cells.contentEquals(before)) { "board must be untouched on an illegal swap" }
        assertEquals(stateBefore, rng.state) { "rng must not advance on an illegal swap" }
    }

    @Test
    fun nonAdjacentSwapReturnsNull() {
        assertNull(Engine.apply(playable, Move(indexOf(0, 0), indexOf(0, 2)), Rng(0L)))
        assertNull(Engine.apply(playable, Move(indexOf(0, 0), indexOf(2, 0)), Rng(0L)))
    }
}
