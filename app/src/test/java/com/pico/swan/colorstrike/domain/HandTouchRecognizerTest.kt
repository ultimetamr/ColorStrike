package com.pico.swan.colorstrike.domain

import com.pico.swan.colorstrike.domain.model.SpatialPoint
import com.pico.swan.colorstrike.domain.model.TargetCue
import com.pico.swan.colorstrike.domain.model.TargetSpawn
import com.pico.swan.colorstrike.domain.usecase.HandTouchOutcome
import com.pico.swan.colorstrike.domain.usecase.HandTouchRecognizer
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import org.junit.Test

class HandTouchRecognizerTest {
    private val outside = listOf(SpatialPoint(0.8f, 1.0f, -1.2f))
    private val inside = listOf(SpatialPoint(0.02f, 1.21f, -1.19f))

    @Test
    fun leftTargetAcceptsLeftPalmOrFingerEntry() {
        val recognizer = HandTouchRecognizer()
        val target = target(TargetCue.LEFT_RED)

        assertNull(recognizer.update(target, outside, outside, 0L))
        val outcome = assertIs<HandTouchOutcome.Submit>(recognizer.update(target, inside, outside, 50L))

        assertEquals(TargetCue.LEFT_RED, outcome.cue)
        assertEquals(target.id, outcome.targetId)
    }

    @Test
    fun wrongHandIsReportedAsItsOwnCue() {
        val recognizer = HandTouchRecognizer()
        val target = target(TargetCue.LEFT_RED)

        recognizer.update(target, outside, outside, 0L)
        val outcome = assertIs<HandTouchOutcome.Submit>(recognizer.update(target, outside, inside, 50L))

        assertEquals(TargetCue.RIGHT_BLUE, outcome.cue)
    }

    @Test
    fun yellowTargetRequiresBothHandsWithinSyncWindow() {
        val recognizer = HandTouchRecognizer(bothHandsWindowMs = 300L)
        val target = target(TargetCue.BOTH_YELLOW)

        recognizer.update(target, outside, outside, 0L)
        assertEquals(
            HandTouchOutcome.WaitingForBothHands,
            recognizer.update(target, inside, outside, 100L),
        )
        val outcome = assertIs<HandTouchOutcome.Submit>(recognizer.update(target, inside, inside, 330L))

        assertEquals(TargetCue.BOTH_YELLOW, outcome.cue)
    }

    @Test
    fun yellowTargetRejectsUnsynchronisedHandsWithoutAdvancing() {
        val recognizer = HandTouchRecognizer(bothHandsWindowMs = 300L)
        val target = target(TargetCue.BOTH_YELLOW)

        recognizer.update(target, outside, outside, 0L)
        recognizer.update(target, inside, outside, 100L)

        assertEquals(
            HandTouchOutcome.BothHandsOutOfSync,
            recognizer.update(target, inside, inside, 450L),
        )
    }

    @Test
    fun trackingRecoveryInsideTargetDoesNotCreateFalseHit() {
        val recognizer = HandTouchRecognizer()
        val target = target(TargetCue.LEFT_RED)

        recognizer.update(target, outside, outside, 0L)
        assertNull(recognizer.update(target, null, outside, 50L))
        assertNull(recognizer.update(target, inside, outside, 100L))
        assertNull(recognizer.update(target, outside, outside, 150L))

        assertIs<HandTouchOutcome.Submit>(recognizer.update(target, inside, outside, 200L))
    }

    private fun target(cue: TargetCue) = TargetSpawn(
        id = 42L,
        cue = cue,
        point = SpatialPoint(0f, 1.2f, -1.2f),
        openedAtMs = 0L,
        expiresAtMs = 2_000L,
    )
}
