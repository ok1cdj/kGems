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
 * The whole engine is a pure function of the seed: the same seed played through
 * the same move sequence must yield a bit-identical board and score, every time
 * and on every platform. This is the property that lets a game be saved and
 * resumed by persisting only the [Rng] state.
 */
class DeterminismTest {

    private data class Outcome(val board: Board, val score: Int, val shuffles: Int)

    /** Play 500 auto-piloted moves (always the first hint) from one seed. */
    private fun play(seed: Long): Outcome {
        val rng = Rng(seed)
        var board = Generator.newBoard(rng)
        var score = 0
        var shuffles = 0
        repeat(500) {
            var hint = Engine.findHint(board)
            if (hint == null) {
                board = Generator.shuffle(board, rng)
                shuffles++
                hint = Engine.findHint(board)
            }
            val result = Engine.apply(board, hint!!, rng)!!
            board = result.board
            score += result.score
        }
        return Outcome(board, score, shuffles)
    }

    @Test
    fun sameSeedSameMovesYieldIdenticalBoardAndScore() {
        val a = play(0xC0FFEEL)
        val b = play(0xC0FFEEL)
        assertTrue(a.board sameAs b.board) { "boards diverged for identical seeds" }
        assertEquals(a.score, b.score)
        assertEquals(a.shuffles, b.shuffles)
    }

    @Test
    fun differentSeedsGenerallyDiverge() {
        // Not a hard guarantee, but two unrelated seeds must not coincidentally
        // produce the same 500-move board — a smoke test that the seed matters.
        val a = play(1L)
        val b = play(2L)
        assertTrue(!(a.board sameAs b.board) || a.score != b.score)
    }

    @Test
    fun rngRoundTripsThroughSerialization() {
        val rng = Rng(0x1234_5678_9ABCL)
        repeat(37) { rng.nextLong() }
        val restored = Rng.deserialize(rng.serialize())
        // Both must now produce the same continuation stream.
        repeat(100) { assertEquals(rng.nextLong(), restored.nextLong()) }
    }
}
