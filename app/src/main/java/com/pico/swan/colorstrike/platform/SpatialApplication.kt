package com.pico.swan.colorstrike.platform

import android.app.Application
import com.pico.swan.colorstrike.mainApp
import com.pico.spatial.ui.foundation.dsl.launch

class SpatialApplication : Application() {
    override fun onCreate() { super.onCreate(); launch(::mainApp) }
}
