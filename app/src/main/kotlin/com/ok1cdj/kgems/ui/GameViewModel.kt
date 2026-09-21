/*
 * kGems — match-3 puzzle for the Mudita Kompakt
 * Copyright (C) 2026 Ondrej Kolonicny (OK1CDJ)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.ok1cdj.kgems.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ok1cdj.kgems.core.Board
import com.ok1cdj.kgems.core.CELLS
import com.ok1cdj.kgems.core.Engine
import com.ok1cdj.kgems.core.GameState
import com.ok1cdj.kgems.core.Generator
import com.ok1cdj.kgems.core.Move
import com.ok1cdj.kgems.core.Rng
import com.ok1cdj.kgems.core.adjacent
import com.ok1cdj.kgems.data.ProgressStore
import com.ok1cdj.kgems.data.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single source of truth for the live game, its score/counters and settings. A
 * thin, deterministic driver over the pure [Engine]: it owns one [Rng], plays
 * back the animation [MoveResult.frames] on a coroutine, and flushes the whole
 * resumable [GameState] to [ProgressStore] after every completed move and on
 * pause. The game never ends — when a move leaves no legal swap, the board is
 * auto-shuffled (bumping the shuffle counter) so the player is never stuck.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ProgressStore(app)

    /** The last snapshot handed to the store, so identical writes are skipped. */
    private var lastSaved: GameState? = null

    private var rng = Rng(System.nanoTime())

    // The authoritative settled board — always full (gem types 0..6) and the only
    // thing persisted. `board` below is the *display* board, which briefly holds
    // transient animation frames (EMPTY holes) during a move; persistence must never
    // capture those. This starts as a throwaway placeholder that is never shown (the
    // UI waits for [loaded]); init installs the saved or freshly-generated board.
    private var settledBoard: Board = Board(ByteArray(CELLS))

    var board by mutableStateOf(settledBoard)
        private set
    var score by mutableStateOf(0)
        private set
    var highScore by mutableStateOf(0)
        private set
    var shuffles by mutableStateOf(0)
        private set
    var hints by mutableStateOf(0)
        private set

    /** The selected cell's linear index, or `null` when nothing is picked. */
    var selected: Int? by mutableStateOf(null)
        private set

    /** The two cells of the current hint to highlight, or `null`. */
    var hint: Move? by mutableStateOf(null)
        private set

    var settings by mutableStateOf(Settings())
        private set

    /** True while a move's frames are animating — taps and buttons are ignored. */
    var busy by mutableStateOf(false)
        private set

    /** Monotonic counter bumped on each completed move — drives one haptic pulse. */
    var moveTick by mutableStateOf(0)
        private set

    /** False until the saved game (if any) has loaded; input is ignored until then. */
    var loaded by mutableStateOf(false)
        private set

    val haptics: Boolean get() = settings.haptics
    val showHint: Boolean get() = settings.showHint
    val keepScreenOn: Boolean get() = settings.keepScreenOn

    init {
        viewModelScope.launch {
            settings = store.loadSettings()
            val saved = store.loadGame()
            if (saved != null) {
                rng = saved.rng
                score = saved.score
                highScore = saved.highScore
                shuffles = saved.shuffles
                hints = saved.hints
                settledBoard = saved.board
                board = saved.board
                lastSaved = saved
                // A stored board is always playable, but guard against corruption.
                ensurePlayable()
            } else {
                // Fresh install: generate the first board now (not eagerly, so a
                // resumed game never pays for a board it immediately discards).
                settledBoard = Generator.newBoard(rng)
                board = settledBoard
            }
            loaded = true
        }
    }

    // --- gameplay -------------------------------------------------------------

    /** Handle a tap on the cell at linear index [i]. */
    fun onCellTap(i: Int) {
        if (busy || !loaded) return
        hint = null
        val sel = selected
        when {
            sel == null -> selected = i
            i == sel -> selected = null                 // tap again to deselect
            !adjacent(sel, i) -> selected = i           // non-adjacent: reselect
            else -> attemptSwap(sel, i)                 // adjacent: try the move
        }
    }

    /** Tap outside the board clears any selection. */
    fun clearSelection() {
        if (busy) return
        selected = null
        hint = null
    }

    private fun attemptSwap(a: Int, b: Int) {
        // apply() advances rng only when the swap is legal; on an illegal swap it
        // returns null untouched, so we keep the first selection (the spec's
        // "invalid swaps do nothing").
        val result = Engine.apply(board, Move(a, b), rng) ?: return
        selected = null

        // Commit the move's logical outcome at once. apply() has already advanced
        // rng to the settled result, so score, the settled board and the auto-
        // shuffle must all advance together and be persisted now — otherwise a
        // pause mid-animation would save a board that disagrees with rng. Only the
        // *display* animates afterwards; the saved state is already final.
        score += result.score
        if (score > highScore) highScore = score
        settledBoard = result.board
        ensurePlayable()
        val finalBoard = settledBoard
        persist()

        busy = true
        viewModelScope.launch {
            val step = settings.frameDelayMs
            if (step > 0) {
                for (frame in result.frames) {
                    board = frame
                    delay(step.toLong())
                }
            }
            // Land on the settled board (also reveals a reshuffle, if one happened).
            board = finalBoard
            moveTick++
            busy = false
        }
    }

    /** Reshuffle the settled board until it has a legal move (auto, never ends). */
    private fun ensurePlayable() {
        while (!Engine.hasValidMove(settledBoard)) {
            settledBoard = Generator.shuffle(settledBoard, rng)
            board = settledBoard
            shuffles++
        }
    }

    /** Show a legal swap and count it; no-op while animating or before load. */
    fun showHint() {
        if (busy || !loaded) return
        val m = Engine.findHint(settledBoard) ?: return
        hint = m
        selected = null
        hints++
        persist()
    }

    /** Start a brand-new game (fresh seed, counters reset; high score kept). */
    fun newGame() {
        if (busy || !loaded) return
        rng = Rng(System.nanoTime())
        settledBoard = Generator.newBoard(rng)
        board = settledBoard
        score = 0
        shuffles = 0
        hints = 0
        selected = null
        hint = null
        persist()
    }

    // --- settings -------------------------------------------------------------

    fun setFrameDelay(ms: Int) = updateSettings(settings.copy(frameDelayMs = ms))
    fun setHaptics(on: Boolean) = updateSettings(settings.copy(haptics = on))
    fun setShowHint(on: Boolean) = updateSettings(settings.copy(showHint = on))
    fun setKeepScreenOn(on: Boolean) = updateSettings(settings.copy(keepScreenOn = on))

    fun resetHighScore() {
        highScore = 0
        persist()
    }

    private fun updateSettings(s: Settings) {
        settings = s
        viewModelScope.launch { store.saveSettings(s) }
    }

    // --- persistence ----------------------------------------------------------

    /**
     * Snapshot and flush the whole game. Called after each move and on pause.
     *
     * Always persists [settledBoard] (never the transient display [board]), so a
     * pause mid-animation can't save a board with EMPTY holes or one that is out
     * of step with [rng]. Identical back-to-back snapshots (e.g. a move followed
     * immediately by the on-pause save) are skipped to avoid a redundant write.
     */
    fun persist() {
        if (!loaded) return
        val snapshot = GameState(
            board = settledBoard,
            rng = rng.copy(),
            score = score,
            highScore = highScore,
            shuffles = shuffles,
            hints = hints,
        )
        if (snapshot.sameStateAs(lastSaved)) return
        lastSaved = snapshot
        viewModelScope.launch { store.saveGame(snapshot) }
    }

    /** Content comparison (Board `==` is reference identity, so compare cells). */
    private fun GameState.sameStateAs(other: GameState?): Boolean =
        other != null &&
            score == other.score && highScore == other.highScore &&
            shuffles == other.shuffles && hints == other.hints &&
            rng.state == other.rng.state &&
            board.cells.contentEquals(other.board.cells)
}
