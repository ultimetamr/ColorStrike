package com.pico.swan.colorstrike.platform

import android.os.Bundle
import com.pico.spatial.ui.platform.stub.SpatialLaunchActivity

class LaunchActivity : SpatialLaunchActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        DebugLaunchState.scenario = intent?.getStringExtra("colorstrike_debug_scenario")
        DebugLaunchState.durationMs = intent?.getLongExtra("colorstrike_debug_duration_ms", 90_000L) ?: 90_000L
        super.onCreate(savedInstanceState)
    }
}

object DebugLaunchState {
    @Volatile var scenario: String? = null
    @Volatile var durationMs: Long = 90_000L
}
