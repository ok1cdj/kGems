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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import com.ok1cdj.kgems.BuildConfig
import com.ok1cdj.kgems.R

private const val GITHUB_URL = "https://github.com/ok1cdj/kGems"
private const val COFFEE_URL = "https://www.buymeacoffee.com/ok1cdj"

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    // Bound the card height so it never runs off-screen; only the text scrolls.
    MmdDialog(onDismiss = onDismiss, modifier = Modifier.heightIn(max = 520.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            TextMMD(text = stringResource(R.string.about_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            TextMMD(text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME), fontSize = 13.sp)
            TextMMD(text = stringResource(R.string.about_author), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            TextMMD(text = stringResource(R.string.about_desc), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            TextMMD(text = stringResource(R.string.about_license), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            TextMMD(text = stringResource(R.string.about_credits), fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))
        MmdButton(stringResource(R.string.about_github), modifier = Modifier.fillMaxWidth()) { openUrl(context, GITHUB_URL) }
        Spacer(Modifier.height(8.dp))
        MmdButton(stringResource(R.string.about_coffee), modifier = Modifier.fillMaxWidth()) { openUrl(context, COFFEE_URL) }
        Spacer(Modifier.height(8.dp))
        MmdButton(stringResource(R.string.close), modifier = Modifier.fillMaxWidth(), onClick = onDismiss)
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
