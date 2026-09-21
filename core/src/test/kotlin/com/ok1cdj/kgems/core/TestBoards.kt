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
 * Builds a board from [SIZE] row strings of [SIZE] digits each (`'0'`..`'6'`),
 * top row first. Compact and readable for hand-authored test fixtures.
 */
fun board(vararg rows: String): Board {
    require(rows.size == SIZE) { "need $SIZE rows, got ${rows.size}" }
    val cells = ByteArray(CELLS)
    for (r in 0 until SIZE) {
        val row = rows[r]
        require(row.length == SIZE) { "row $r must be $SIZE chars, got '${row}'" }
        for (c in 0 until SIZE) {
            val d = row[c]
            require(d in '0'..'6') { "bad cell '$d' at ($r,$c)" }
            cells[indexOf(r, c)] = (d - '0').toByte()
        }
    }
    return Board(cells)
}

/** The multiset of gem types on [b], as a count per type index. */
fun typeCounts(b: Board): IntArray {
    val counts = IntArray(TYPES)
    for (v in b.cells) if (v != EMPTY) counts[v.toInt()]++
    return counts
}

/**
 * Sizes of the orthogonally connected components of [matched] — a test-side
 * re-implementation used to independently re-derive scores from frame data
 * (see CascadeTest), deliberately not sharing the engine's private version.
 */
fun connectedComponentSizes(matched: Set<Int>): List<Int> {
    val seen = HashSet<Int>()
    val sizes = ArrayList<Int>()
    for (start in matched) {
        if (start in seen) continue
        seen.add(start)
        val stack = ArrayDeque<Int>().apply { addLast(start) }
        var size = 0
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            size++
            val r = rowOf(i)
            val c = colOf(i)
            val neighbours = listOf(
                if (c > 0) indexOf(r, c - 1) else -1,
                if (c < SIZE - 1) indexOf(r, c + 1) else -1,
                if (r > 0) indexOf(r - 1, c) else -1,
                if (r < SIZE - 1) indexOf(r + 1, c) else -1,
            )
            for (j in neighbours) if (j >= 0 && j in matched && seen.add(j)) stack.addLast(j)
        }
        sizes.add(size)
    }
    return sizes
}
