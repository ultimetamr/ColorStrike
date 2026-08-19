package com.pico.swan.colorstrike

import com.pico.swan.colorstrike.ui.ColorStrikeScreen
import com.pico.swan.colorstrike.ui.COLOR_STRIKE_STAGE_ID
import com.pico.swan.colorstrike.spatial.ColorStrikeStage
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Immersion
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.Stage

fun mainApp(scope: SpatialAppScope) = with(scope) {
    DefaultWindowContainer {
        PicoTheme { ColorStrikeScreen() }
    }
    Stage(id = COLOR_STRIKE_STAGE_ID, immersion = Immersion.Default) {
        PicoTheme { ColorStrikeStage() }
    }
}
