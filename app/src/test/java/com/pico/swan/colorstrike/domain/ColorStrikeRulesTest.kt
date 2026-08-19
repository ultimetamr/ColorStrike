package com.pico.swan.colorstrike.domain

import com.pico.swan.colorstrike.domain.model.Difficulty
import com.pico.swan.colorstrike.domain.model.PostureMode
import com.pico.swan.colorstrike.domain.usecase.COMBO_STEP_MS
import com.pico.swan.colorstrike.domain.usecase.SESSION_DURATION_MS
import com.pico.swan.colorstrike.domain.usecase.SAFE_TARGET_ZONES
import com.pico.swan.colorstrike.domain.usecase.combinationLevel
import com.pico.swan.colorstrike.domain.usecase.isSessionComplete
import com.pico.swan.colorstrike.domain.usecase.spawnTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorStrikeRulesTest {
    @Test fun everyFixedSpawnIsOutsideHeadAndInsideItsSafeZone() {
        PostureMode.entries.forEach { posture ->
            repeat(24) { index ->
                val spawn = spawnTarget(index, index * 2_000L, posture, Difficulty.STANDARD, 1_000L)
                assertTrue(SAFE_TARGET_ZONES.getValue(posture).contains(spawn.point), "$posture #$index")
            }
        }
    }
    @Test fun seatedTargetsAreLowerButRemainFullyPlayable() {
        val seated = spawnTarget(1, 0, PostureMode.SEATED, Difficulty.CALM, 0)
        val standing = spawnTarget(1, 0, PostureMode.STANDING, Difficulty.CALM, 0)
        assertTrue(seated.point.yMeters < standing.point.yMeters)
        assertTrue(SAFE_TARGET_ZONES.getValue(PostureMode.SEATED).contains(seated.point))
    }
    @Test fun complexityChangesEveryThirtySecondsWithoutSpeedMutation() {
        assertEquals(0, combinationLevel(0)); assertEquals(1, combinationLevel(COMBO_STEP_MS)); assertEquals(2, combinationLevel(COMBO_STEP_MS * 2))
        assertFalse(isSessionComplete(SESSION_DURATION_MS - 1)); assertTrue(isSessionComplete(SESSION_DURATION_MS))
    }
}
