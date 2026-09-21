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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ok1cdj.kgems.R

// Header icon sizing — generous touch targets for the Kompakt's e-ink panel,
// where small glyphs are hard to hit reliably.
private val TOUCH_TARGET = 48.dp
private val ICON = 34.dp

/** The About (ⓘ) button: the crisp ic_info vector in a 48dp touch target. */
@Composable
fun InfoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(TOUCH_TARGET).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = stringResource(R.string.about),
            modifier = Modifier.size(ICON),
        )
    }
}

/** The Settings (gear) button in a 48dp touch target. */
@Composable
fun SettingsButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(TOUCH_TARGET).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = stringResource(R.string.settings),
            modifier = Modifier.size(ICON),
        )
    }
}
