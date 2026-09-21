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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.ok1cdj.kgems.R
import com.ok1cdj.kgems.data.Settings

@Composable
fun SettingsDialog(
    settings: Settings,
    onFrameDelay: (Int) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onShowHint: (Boolean) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onResetHighScore: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            TextMMD(text = stringResource(R.string.settings), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            TextMMD(text = stringResource(R.string.frame_delay_label), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            TextMMD(text = stringResource(R.string.frame_delay_sub), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentButton(stringResource(R.string.delay_250), settings.frameDelayMs == 250, Modifier.weight(1f)) { onFrameDelay(250) }
                SegmentButton(stringResource(R.string.delay_500), settings.frameDelayMs == 500, Modifier.weight(1f)) { onFrameDelay(500) }
                SegmentButton(stringResource(R.string.delay_off), settings.frameDelayMs == 0, Modifier.weight(1f)) { onFrameDelay(0) }
            }

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                label = stringResource(R.string.haptics_label),
                sub = stringResource(R.string.haptics_sub),
                checked = settings.haptics,
                onChange = onHaptics,
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                label = stringResource(R.string.show_hint_label),
                sub = stringResource(R.string.show_hint_sub),
                checked = settings.showHint,
                onChange = onShowHint,
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                label = stringResource(R.string.keep_screen_on_label),
                sub = stringResource(R.string.keep_screen_on_sub),
                checked = settings.keepScreenOn,
                onChange = onKeepScreenOn,
            )

            Spacer(Modifier.height(16.dp))
            DialogWideButton(stringResource(R.string.reset_high)) { onResetHighScore() }
            Spacer(Modifier.height(8.dp))
            DialogWideButton(stringResource(R.string.close), onClick = onDismiss)
        }
    }
}

/**
 * One option of a three-way choice; the active one is inverted (black fill,
 * white label) — the same selection idiom as a picked gem. Built from a plain
 * [Box] rather than [ButtonMMD] because MMD paints its own white surface, which
 * would hide the black fill and leave a white-on-white label.
 */
@Composable
private fun SegmentButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .background(if (selected) Color.Black else Color.White, shape)
            .border(1.dp, Color.Black, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TextMMD(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Color.Black,
        )
    }
}

@Composable
private fun DialogWideButton(text: String, onClick: () -> Unit) {
    ButtonMMD(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextMMD(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToggleRow(label: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            TextMMD(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            TextMMD(text = sub, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.Black,
                uncheckedThumbColor = Color.Black,
                uncheckedTrackColor = Color.White,
                uncheckedBorderColor = Color.Black,
            ),
        )
    }
}
