/*
 * kGems — match-3 puzzle for the Mudita Kompakt
 * Copyright (C) 2026 Ondrej Kolonicny (OK1CDJ)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.ok1cdj.kgems

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ok1cdj.kgems.ui.AboutDialog
import com.ok1cdj.kgems.ui.GameScreen
import com.ok1cdj.kgems.ui.GameViewModel
import com.ok1cdj.kgems.ui.KGemsTheme
import com.ok1cdj.kgems.ui.SettingsDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KGemsTheme { App() } }
    }
}

@Composable
private fun App() {
    val vm: GameViewModel = viewModel()
    var showAbout by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Persist the resumable game whenever the app goes to the background — per the
    // spec, on pause rather than continuously (moves persist themselves too).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) vm.persist()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keep the display awake while playing, if enabled. keepScreenOn on the
    // window's view holds a wake lock only while the window is visible and needs
    // no permission — it clears itself when the app leaves the foreground.
    val view = LocalView.current
    LaunchedEffect(vm.keepScreenOn) { view.keepScreenOn = vm.keepScreenOn }

    // targetSdk 37 forces edge-to-edge, so inset the whole app below the system
    // bars — otherwise the header sits under the status bar and swallows taps.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding(),
    ) {
        GameScreen(vm, onAbout = { showAbout = true }, onSettings = { showSettings = true })
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    if (showSettings) {
        SettingsDialog(
            settings = vm.settings,
            onFrameDelay = vm::setFrameDelay,
            onHaptics = vm::setHaptics,
            onShowHint = vm::setShowHint,
            onKeepScreenOn = vm::setKeepScreenOn,
            onResetHighScore = vm::resetHighScore,
            onDismiss = { showSettings = false },
        )
    }
}
