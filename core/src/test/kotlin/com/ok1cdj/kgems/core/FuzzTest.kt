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

/**
 * Long random self-play. Whatever the seed, the two hard invariants of an
 * endless match-3 game must always hold: the player is never stuck (auto-shuffle
 * always restores a move) and no run is ever left sitting on a settled board
 * between moves.
 */
class FuzzTest {

    @Test
    fun selfPlayNeverStrandsThePlayerOrLeavesAMatch() {
        val failures = mutableListOf<String>()
        val games = 1_000
        val movesPerGame = 200

        outer@ for (g in 0 until games) {
            val rng = Rng(g.toLong() * 0x9E37 + 1)
            var board = Generator.newBoard(rng)

            if (Engine.findMatches(board).isNotEmpty()) {
                failures += "game $g started with a match"
                continue@outer
            }

            repeat(movesPerGame) { m ->
                var hint = Engine.findHint(board)
                if (hint == null) {
                    board = Generator.shuffle(board, rng)
                    hint = Engine.findHint(board)
                }
                if (hint == null) {
                    failures += "game $g move $m: stuck even after a shuffle"
                    return@repeat
                }
                val result = Engine.apply(board, hint, rng)
                if (result == null) {
                    failures += "game $g move $m: a hinted move was rejected"
                    return@repeat
                }
                board = result.board
                if (Engine.findMatches(board).isNotEmpty()) {
                    failures += "game $g move $m: a match persisted after settling"
                    return@repeat
                }
                if (board.cells.any { it == EMPTY }) {
                    failures += "game $g move $m: a hole persisted after settling"
                    return@repeat
                }
            }
        }
        assertTrue(failures.isEmpty()) { "fuzz invariants broken (${failures.size}): ${failures.take(10)}" }
    }
}
