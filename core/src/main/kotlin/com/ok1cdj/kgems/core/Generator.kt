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

/**
 * Builds and reshuffles boards. Every board it returns is *playable*: it has no
 * pre-existing run (nothing would clear on load) and at least one legal move
 * (the player is never handed a dead board). Both draw from an [Rng] so the
 * results are reproducible.
 */
object Generator {

    /**
     * A fresh random board. Cells are filled top-left to bottom-right, each
     * chosen to avoid *completing* a run of three with the two cells already
     * placed to its left or above — so mid-construction runs never appear. The
     * finished board is then checked for a valid move and regenerated on the
     * (rare) chance it has none.
     */
    fun newBoard(rng: Rng): Board {
        while (true) {
            val cells = ByteArray(CELLS)
            for (i in 0 until CELLS) {
                val r = rowOf(i)
                val c = colOf(i)
                while (true) {
                    val t = rng.nextInt(TYPES).toByte()
                    val runLeft = c >= 2 && cells[i - 1] == t && cells[i - 2] == t
                    val runUp = r >= 2 && cells[i - SIZE] == t && cells[i - 2 * SIZE] == t
                    if (!runLeft && !runUp) {
                        cells[i] = t
                        break
                    }
                }
            }
            val board = Board(cells)
            if (Engine.findMatches(board).isEmpty() && Engine.hasValidMove(board)) return board
        }
    }

    /**
     * Reshuffle [b] into another playable board that keeps the exact same
     * multiset of gems (a Fisher–Yates permutation of the current cells), used
     * when the board runs out of moves. Repeats until the result has no run and
     * at least one legal move.
     */
    fun shuffle(b: Board, rng: Rng): Board {
        while (true) {
            val cells = b.cells.copyOf()
            for (i in CELLS - 1 downTo 1) {
                val j = rng.nextInt(i + 1)
                val t = cells[i]; cells[i] = cells[j]; cells[j] = t
            }
            val board = Board(cells)
            if (Engine.findMatches(board).isEmpty() && Engine.hasValidMove(board)) return board
        }
    }
}
