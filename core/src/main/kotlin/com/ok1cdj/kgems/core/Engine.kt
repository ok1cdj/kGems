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
 * The match-3 rules engine: pure functions over a [Board], deterministic given
 * an [Rng]. No Android, no UI, no mutable global state.
 *
 * The move model is the classic swap-match-cascade loop:
 * swap two adjacent gems → if that forms no run of ≥3 the swap is illegal and is
 * never performed → otherwise clear every run, let survivors fall **down**,
 * refill the emptied top from the [Rng], and repeat while new runs keep forming.
 */
object Engine {

    /**
     * Every cell that is part of a horizontal or vertical run of ≥3 equal gems.
     *
     * Overlapping runs (the shared cell of an L or T) yield a single merged set —
     * that cell simply appears once. [EMPTY] cells never match.
     */
    fun findMatches(b: Board): Set<Int> {
        val matched = HashSet<Int>()
        val cells = b.cells
        // Horizontal runs.
        for (row in 0 until SIZE) {
            var col = 0
            while (col < SIZE) {
                val t = cells[indexOf(row, col)]
                var run = 1
                while (col + run < SIZE && cells[indexOf(row, col + run)] == t) run++
                if (t != EMPTY && run >= 3) {
                    for (k in 0 until run) matched.add(indexOf(row, col + k))
                }
                col += run
            }
        }
        // Vertical runs.
        for (col in 0 until SIZE) {
            var row = 0
            while (row < SIZE) {
                val t = cells[indexOf(row, col)]
                var run = 1
                while (row + run < SIZE && cells[indexOf(row + run, col)] == t) run++
                if (t != EMPTY && run >= 3) {
                    for (k in 0 until run) matched.add(indexOf(row + k, col))
                }
                row += run
            }
        }
        return matched
    }

    /**
     * Apply the swap [m] to [b], drawing refills from [rng].
     *
     * @return the full [MoveResult] if the swap is legal (it forms at least one
     *   run); `null` if it is illegal — in which case nothing happens: [b] is
     *   untouched and [rng] is not advanced.
     */
    fun apply(b: Board, m: Move, rng: Rng): MoveResult? {
        if (!adjacent(m.a, m.b)) return null

        val work = b.cells.copyOf()
        swap(work, m.a, m.b)

        // Legality is decided on the cheap: does either moved gem now sit in a
        // run? If not, the swap is illegal — return null without advancing rng.
        if (!hasMatchAt(work, m.a) && !hasMatchAt(work, m.b)) return null

        val frames = ArrayList<Board>()
        frames.add(Board(work.copyOf())) // the swap itself

        var score = 0
        var cleared = 0
        var wave = 0
        while (true) {
            val matches = findMatches(Board(work))
            if (matches.isEmpty()) break
            wave++

            // Score: each connected group of size n is worth 10*(n-2)*n; the
            // whole wave's points are multiplied by the 1-based wave number, so
            // deeper cascades pay progressively more.
            var waveBase = 0
            for (n in groupSizes(matches)) waveBase += 10 * (n - 2) * n
            score += waveBase * wave
            cleared += matches.size

            for (i in matches) work[i] = EMPTY
            frames.add(Board(work.copyOf())) // post-clear (holes)

            gravityAndRefill(work, rng)
            frames.add(Board(work.copyOf())) // settled (fallen + refilled)
        }

        return MoveResult(frames, score, wave, cleared)
    }

    /** True if any legal swap exists on [b]. */
    fun hasValidMove(b: Board): Boolean = firstValidSwap(b) != null

    /** The first legal swap in index scan order, or `null` if the board is dead. */
    fun findHint(b: Board): Move? = firstValidSwap(b)

    // ---- internals ----------------------------------------------------------

    /** Scans cells left-to-right, top-to-bottom, testing the right and down swap. */
    private fun firstValidSwap(b: Board): Move? {
        val cells = b.cells.copyOf()
        for (i in 0 until CELLS) {
            val r = rowOf(i)
            val c = colOf(i)
            if (c + 1 < SIZE && swapMakesMatch(cells, i, i + 1)) return Move(i, i + 1)
            if (r + 1 < SIZE && swapMakesMatch(cells, i, i + SIZE)) return Move(i, i + SIZE)
        }
        return null
    }

    /** Swaps [a],[b] in place, checks both sites for a run, then swaps back. */
    private fun swapMakesMatch(cells: ByteArray, a: Int, b: Int): Boolean {
        swap(cells, a, b)
        val hit = hasMatchAt(cells, a) || hasMatchAt(cells, b)
        swap(cells, a, b)
        return hit
    }

    /** True if the gem now at [index] is part of a horizontal or vertical run ≥3. */
    private fun hasMatchAt(cells: ByteArray, index: Int): Boolean {
        val t = cells[index]
        if (t == EMPTY) return false
        val r = rowOf(index)
        val c = colOf(index)

        var count = 1
        var k = c - 1
        while (k >= 0 && cells[indexOf(r, k)] == t) { count++; k-- }
        k = c + 1
        while (k < SIZE && cells[indexOf(r, k)] == t) { count++; k++ }
        if (count >= 3) return true

        count = 1
        k = r - 1
        while (k >= 0 && cells[indexOf(k, c)] == t) { count++; k-- }
        k = r + 1
        while (k < SIZE && cells[indexOf(k, c)] == t) { count++; k++ }
        return count >= 3
    }

    /** Sizes of the connected (orthogonal) components of the matched cell set. */
    private fun groupSizes(matched: Set<Int>): List<Int> {
        val seen = HashSet<Int>()
        val sizes = ArrayList<Int>()
        val stack = ArrayDeque<Int>()
        for (start in matched) {
            if (!seen.add(start)) continue
            var size = 0
            stack.addLast(start)
            while (stack.isNotEmpty()) {
                val i = stack.removeLast()
                size++
                val r = rowOf(i)
                val c = colOf(i)
                if (c > 0) tryPush(indexOf(r, c - 1), matched, seen, stack)
                if (c < SIZE - 1) tryPush(indexOf(r, c + 1), matched, seen, stack)
                if (r > 0) tryPush(indexOf(r - 1, c), matched, seen, stack)
                if (r < SIZE - 1) tryPush(indexOf(r + 1, c), matched, seen, stack)
            }
            sizes.add(size)
        }
        return sizes
    }

    private fun tryPush(i: Int, matched: Set<Int>, seen: HashSet<Int>, stack: ArrayDeque<Int>) {
        if (i in matched && seen.add(i)) stack.addLast(i)
    }

    /**
     * Compacts each column downward (survivors fall to the bottom, holes rise to
     * the top) and fills the emptied top cells with fresh gems from [rng].
     */
    private fun gravityAndRefill(cells: ByteArray, rng: Rng) {
        for (col in 0 until SIZE) {
            var writeRow = SIZE - 1
            // Pack survivors toward the bottom. Reads always stay at or above
            // the write cursor, so no unread cell is ever overwritten.
            for (row in SIZE - 1 downTo 0) {
                val v = cells[indexOf(row, col)]
                if (v != EMPTY) {
                    cells[indexOf(writeRow, col)] = v
                    writeRow--
                }
            }
            // Refill the gap left at the top.
            for (row in writeRow downTo 0) {
                cells[indexOf(row, col)] = rng.nextInt(TYPES).toByte()
            }
        }
    }

    private fun swap(cells: ByteArray, a: Int, b: Int) {
        val t = cells[a]; cells[a] = cells[b]; cells[b] = t
    }
}
