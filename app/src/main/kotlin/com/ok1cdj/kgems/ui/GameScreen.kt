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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.ok1cdj.kgems.R
import com.ok1cdj.kgems.core.EMPTY
import com.ok1cdj.kgems.core.SIZE
import com.ok1cdj.kgems.core.colOf
import com.ok1cdj.kgems.core.rowOf

@Composable
fun GameScreen(vm: GameViewModel, onAbout: () -> Unit, onSettings: () -> Unit) {
    // One short haptic when a move completes (respects the Haptics setting;
    // performHapticFeedback needs no VIBRATE permission).
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(vm.moveTick) {
        if (vm.moveTick > 0 && vm.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var confirmNew by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Header: title · settings gear.
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextMMD(text = stringResource(R.string.app_name), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            SettingsButton(onClick = onSettings)
        }

        // Score line.
        Box(
            modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            TextMMD(
                text = stringResource(R.string.stats, vm.score, vm.highScore, vm.shuffles),
                fontSize = 15.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        BoardCanvas(vm)

        Spacer(Modifier.weight(1f))

        // Controls — New Game · Hint · About.
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameButton(stringResource(R.string.new_game), modifier = Modifier.weight(1f)) {
                if (vm.score > 0) confirmNew = true else vm.newGame()
            }
            if (vm.showHint) {
                GameButton(stringResource(R.string.hint), enabled = !vm.busy, modifier = Modifier.weight(1f), onClick = vm::showHint)
            }
            GameButton(stringResource(R.string.about), modifier = Modifier.weight(1f), onClick = onAbout)
        }
    }

    if (confirmNew) {
        ConfirmDialog(
            title = stringResource(R.string.new_game_confirm_title),
            body = stringResource(R.string.new_game_confirm_body, vm.score),
            confirmLabel = stringResource(R.string.new_game_confirm_ok),
            onConfirm = { confirmNew = false; vm.newGame() },
            onDismiss = { confirmNew = false },
        )
    }
}

// --- board rendering ---------------------------------------------------------

private const val GRID_COLOR = 0xFFB0B0B0
private const val HINT_INSET = 0.14f

@Composable
private fun BoardCanvas(vm: GameViewModel) {
    val board = vm.board
    val selected = vm.selected
    val hint = vm.hint
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(1f)
            .pointerInput(vm) {
                val cell = size.width / SIZE.toFloat()
                detectTapGestures { off ->
                    val col = (off.x / cell).toInt()
                    val row = (off.y / cell).toInt()
                    if (col in 0 until SIZE && row in 0 until SIZE) {
                        vm.onCellTap(row * SIZE + col)
                    } else {
                        vm.clearSelection()
                    }
                }
            },
    ) {
        val cell = size.width / SIZE
        // Gems first — drawGem fills the whole cell, so the grid must go on top.
        with(GemGlyphs) {
            for (i in 0 until SIZE * SIZE) {
                // Intermediate cascade frames carry EMPTY (-1) holes — leave them
                // blank (the white canvas shows through); the grid is drawn on top.
                if (board[i] == EMPTY) continue
                drawGem(
                    type = board[i].toInt(),
                    topLeft = Offset(colOf(i) * cell, rowOf(i) * cell),
                    cellSize = cell,
                    inverted = i == selected,
                )
            }
        }

        // 1px interior grid + a heavier outer frame.
        val grid = Color(GRID_COLOR)
        val line = 1.dp.toPx()
        for (k in 1 until SIZE) {
            drawLine(grid, Offset(k * cell, 0f), Offset(k * cell, size.height), line)
            drawLine(grid, Offset(0f, k * cell), Offset(size.width, k * cell), line)
        }
        drawRect(Color.Black, style = Stroke(width = 2.dp.toPx()))

        // Hint: hollow ring on each of the two swap cells.
        hint?.let { m ->
            val inset = cell * HINT_INSET
            for (idx in intArrayOf(m.a, m.b)) {
                val x = colOf(idx) * cell
                val y = rowOf(idx) * cell
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(x + inset, y + inset),
                    size = Size(cell - 2 * inset, cell - 2 * inset),
                    style = Stroke(width = cell * 0.09f),
                )
            }
        }
    }
}

// --- shared button + dialog --------------------------------------------------

@Composable
private fun GameButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ButtonMMD(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .height(56.dp)
            .border(1.dp, if (enabled) Color.Black else Color.Gray, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextMMD(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            TextMMD(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            TextMMD(text = body, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogButton(stringResource(R.string.cancel), modifier = Modifier.weight(1f), onClick = onDismiss)
                DialogButton(confirmLabel, modifier = Modifier.weight(1f), onClick = onConfirm)
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ButtonMMD(
        onClick = onClick,
        modifier = modifier.height(56.dp).border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextMMD(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
