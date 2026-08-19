package com.pico.swan.colorstrike.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pico.swan.colorstrike.BuildConfig
import com.pico.swan.colorstrike.data.SharedPreferencesColorStrikeRepository
import com.pico.swan.colorstrike.domain.usecase.SESSION_DURATION_MS
import com.pico.swan.colorstrike.platform.DebugLaunchState

private object ColorStrikeModelStore {
    @Volatile private var model: ColorStrikeViewModel? = null

    fun get(context: Context, sessionDurationMs: Long): ColorStrikeViewModel =
        model ?: synchronized(this) {
            model ?: ColorStrikeViewModel(
                SharedPreferencesColorStrikeRepository(context.applicationContext),
                sessionDurationMs,
            ).also { model = it }
        }
}

@Composable
fun rememberSharedColorStrikeViewModel(
    sessionDurationMs: Long = if (BuildConfig.DEBUG) {
        DebugLaunchState.durationMs.coerceIn(1_000L, SESSION_DURATION_MS)
    } else {
        SESSION_DURATION_MS
    },
): ColorStrikeViewModel {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext, sessionDurationMs) {
        ColorStrikeModelStore.get(applicationContext, sessionDurationMs)
    }
}
