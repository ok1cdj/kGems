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

/** Board edge length — an 8×8 grid. */
const val SIZE = 8

/** Total cells on the board (`SIZE * SIZE`). */
const val CELLS = SIZE * SIZE

/** Number of distinct gem types, 0..[TYPES]-1. Matches [GemGlyphs.TYPE_COUNT]. */
const val TYPES = 7

/**
 * Sentinel cell value for a *transient* empty slot — a gem that has just been
 * cleared but not yet had another fall into it. It appears only inside the
 * intermediate animation frames of a [MoveResult]; a settled board is always
 * full and holds only values `0 until TYPES`.
 */
const val EMPTY: Byte = -1

/**
 * An 8×8 match-3 board: 64 cells stored row-major, each a gem type `0 until
 * TYPES` (or [EMPTY] in a transient frame).
 *
 * It is a `value class` wrapping the raw [ByteArray], so it costs nothing at
 * runtime — but that means `==` and `hashCode()` are **reference** identity on
 * the array, NOT content. Compare two boards with
 * `a.cells.contentEquals(b.cells)` (see [sameAs]); never `a == b`.
 */
@JvmInline
value class Board(val cells: ByteArray) {

    init {
        require(cells.size == CELLS) { "board must be $CELLS cells, got ${cells.size}" }
    }

    /** The cell at linear index [i] (0 until [CELLS]). */
    operator fun get(i: Int): Byte = cells[i]

    /** The cell at [row], [col] (both 0 until [SIZE]); origin top-left. */
    operator fun get(row: Int, col: Int): Byte = cells[row * SIZE + col]

    /** An independent deep copy (the [cells] array is duplicated). */
    fun copy(): Board = Board(cells.copyOf())

    /** Content equality — the correct way to compare boards (see class note). */
    infix fun sameAs(other: Board): Boolean = cells.contentEquals(other.cells)

    companion object {
        /** Build a board from exactly [CELLS] `Int` cell values. */
        fun of(vararg cells: Int): Board {
            require(cells.size == CELLS) { "need $CELLS cells, got ${cells.size}" }
            return Board(ByteArray(CELLS) { cells[it].toByte() })
        }
    }
}

/** A candidate swap of the two adjacent cells at linear indices [a] and [b]. */
data class Move(val a: Int, val b: Int)

/**
 * The full outcome of one [Engine.apply] call.
 *
 * @property frames ordered board snapshots for the UI to play back — the swapped
 *   board, then per cascade wave a post-clear frame (holes = [EMPTY]) and a
 *   settled frame (gravity applied, top refilled). The last frame is the final
 *   settled board and always has no matches.
 * @property score points earned by the whole move (all waves summed).
 * @property cascades number of cascade waves that cleared gems (≥1).
 * @property cleared total gems removed across all waves.
 */
data class MoveResult(
    val frames: List<Board>,
    val score: Int,
    val cascades: Int,
    val cleared: Int,
) {
    /** The final settled board — the last animation frame. */
    val board: Board get() = frames.last()
}

/**
 * A serializable snapshot of a whole game, shared by the engine, the view model
 * and persistence. The game never ends (auto-shuffle on no-move), so there is no
 * win/lose flag — just the live state to restore exactly across process death.
 */
data class GameState(
    val board: Board,
    val rng: Rng,
    val score: Int = 0,
    val highScore: Int = 0,
    val shuffles: Int = 0,
    val hints: Int = 0,
)

/** The row (0 until [SIZE]) of linear index [i]. */
fun rowOf(i: Int): Int = i / SIZE

/** The column (0 until [SIZE]) of linear index [i]. */
fun colOf(i: Int): Int = i % SIZE

/** The linear index of cell ([row], [col]). */
fun indexOf(row: Int, col: Int): Int = row * SIZE + col

/** True if cells [a] and [b] are on the board and orthogonally adjacent. */
fun adjacent(a: Int, b: Int): Boolean {
    if (a !in 0 until CELLS || b !in 0 until CELLS) return false
    val dr = rowOf(a) - rowOf(b)
    val dc = colOf(a) - colOf(b)
    return (dr == 0 && (dc == 1 || dc == -1)) || (dc == 0 && (dr == 1 || dr == -1))
}
