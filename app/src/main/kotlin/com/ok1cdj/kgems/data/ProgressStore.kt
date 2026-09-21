/*
 * kGems — match-3 puzzle for the Mudita Kompakt
 * Copyright (C) 2026 Ondrej Kolonicny (OK1CDJ)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.ok1cdj.kgems.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ok1cdj.kgems.core.Board
import com.ok1cdj.kgems.core.CELLS
import com.ok1cdj.kgems.core.GameState
import com.ok1cdj.kgems.core.Json
import com.ok1cdj.kgems.core.Rng
import com.ok1cdj.kgems.core.TYPES
import kotlinx.coroutines.flow.first

/**
 * User-configurable options.
 *
 * @property frameDelayMs delay between cascade animation frames, in ms; `0` means
 *   skip the intermediate frames entirely and snap to the settled board.
 * @property haptics buzz on each completed move.
 * @property showHint whether the Hint button is offered during play.
 * @property keepScreenOn keep the display awake while the game is in front
 *   (uses the window's keep-screen-on flag — needs no permission).
 */
data class Settings(
    val frameDelayMs: Int = 500,
    val haptics: Boolean = true,
    val showHint: Boolean = true,
    val keepScreenOn: Boolean = false,
)

private val Context.dataStore by preferencesDataStore(name = "progress")

/**
 * Reads and writes the resumable [GameState] and [Settings] via Preferences
 * DataStore. Two hand-serialized JSON blobs (via the core [Json]) — the live game
 * under `game`, options under `settings`. No Gson/Room.
 *
 * The whole game is captured — including the [Rng] state — so a killed app
 * resumes to a bit-identical position, all future refills included. Written after
 * each completed move and on pause; read once at startup.
 */
class ProgressStore(private val context: Context) {

    /** The saved game, or `null` if none has been stored yet (fresh install). */
    suspend fun loadGame(): GameState? {
        val prefs = context.dataStore.data.first()
        val m = prefs[GAME]?.let { Json.parseObject(it) } ?: return null
        val boardStr = m["board"] as? String ?: return null
        val rngStr = m["rng"] as? String ?: return null
        val board = decodeBoard(boardStr) ?: return null
        return GameState(
            board = board,
            rng = Rng.deserialize(rngStr),
            score = (m["score"] as? Double)?.toInt() ?: 0,
            highScore = (m["high"] as? Double)?.toInt() ?: 0,
            shuffles = (m["shuffles"] as? Double)?.toInt() ?: 0,
            hints = (m["hints"] as? Double)?.toInt() ?: 0,
        )
    }

    suspend fun saveGame(state: GameState) {
        context.dataStore.edit { prefs ->
            prefs[GAME] = Json.stringify(
                mapOf(
                    "board" to encodeBoard(state.board),
                    "rng" to state.rng.serialize(),
                    "score" to state.score,
                    "high" to state.highScore,
                    "shuffles" to state.shuffles,
                    "hints" to state.hints,
                )
            )
        }
    }

    suspend fun loadSettings(): Settings {
        val prefs = context.dataStore.data.first()
        val m = prefs[SETTINGS]?.let { Json.parseObject(it) } ?: return Settings()
        val defaults = Settings()
        return Settings(
            frameDelayMs = (m["frameDelayMs"] as? Double)?.toInt() ?: defaults.frameDelayMs,
            haptics = m["haptics"] as? Boolean ?: defaults.haptics,
            showHint = m["showHint"] as? Boolean ?: defaults.showHint,
            keepScreenOn = m["keepScreenOn"] as? Boolean ?: defaults.keepScreenOn,
        )
    }

    suspend fun saveSettings(s: Settings) {
        context.dataStore.edit { prefs ->
            prefs[SETTINGS] = Json.stringify(
                mapOf(
                    "frameDelayMs" to s.frameDelayMs,
                    "haptics" to s.haptics,
                    "showHint" to s.showHint,
                    "keepScreenOn" to s.keepScreenOn,
                )
            )
        }
    }

    // A settled board holds only gem types 0..6, so a fixed-length string of one
    // decimal digit per cell round-trips it exactly and compactly.
    private fun encodeBoard(b: Board): String =
        buildString(CELLS) { for (i in 0 until CELLS) append(('0' + b[i].toInt())) }

    private fun decodeBoard(s: String): Board? {
        if (s.length != CELLS) return null
        val cells = ByteArray(CELLS)
        for (i in 0 until CELLS) {
            val d = s[i] - '0'
            if (d !in 0 until TYPES) return null
            cells[i] = d.toByte()
        }
        return Board(cells)
    }

    private companion object {
        val GAME = stringPreferencesKey("game")
        val SETTINGS = stringPreferencesKey("settings")
    }
}
