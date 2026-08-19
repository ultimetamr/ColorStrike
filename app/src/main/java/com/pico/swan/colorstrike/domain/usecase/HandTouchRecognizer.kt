package com.pico.swan.colorstrike.domain.usecase

import com.pico.swan.colorstrike.domain.model.SpatialPoint
import com.pico.swan.colorstrike.domain.model.TargetCue
import com.pico.swan.colorstrike.domain.model.TargetSpawn
import kotlin.math.sqrt

sealed interface HandTouchOutcome {
    data class Submit(val targetId: Long, val cue: TargetCue) : HandTouchOutcome
    data object WaitingForBothHands : HandTouchOutcome
    data object BothHandsOutOfSync : HandTouchOutcome
}

/** Converts tracked hand contact points into one discrete game input per target. */
class HandTouchRecognizer(
    private val hitRadiusMeters: Float = 0.17f,
    private val bothHandsWindowMs: Long = 300L,
) {
    private var targetId: Long? = null
    private var armed = false
    private var leftWasInside = false
    private var rightWasInside = false
    private var leftEnteredAtMs: Long? = null
    private var rightEnteredAtMs: Long? = null

    fun update(
        target: TargetSpawn?,
        leftContactPoints: List<SpatialPoint>?,
        rightContactPoints: List<SpatialPoint>?,
        nowMs: Long,
    ): HandTouchOutcome? {
        if (target == null) {
            reset()
            return null
        }

        val leftInside = leftContactPoints?.any { it.distanceTo(target.point) <= hitRadiusMeters } == true
        val rightInside = rightContactPoints?.any { it.distanceTo(target.point) <= hitRadiusMeters } == true

        if (target.id != targetId) {
            targetId = target.id
            armed = !leftInside && !rightInside
            leftWasInside = leftInside
            rightWasInside = rightInside
            leftEnteredAtMs = null
            rightEnteredAtMs = null
            return null
        }

        val requiredTrackingAvailable = when (target.cue) {
            TargetCue.LEFT_RED -> leftContactPoints != null
            TargetCue.RIGHT_BLUE -> rightContactPoints != null
            TargetCue.BOTH_YELLOW -> leftContactPoints != null && rightContactPoints != null
        }
        if (!requiredTrackingAvailable) {
            armed = false
            leftWasInside = leftInside
            rightWasInside = rightInside
            leftEnteredAtMs = null
            rightEnteredAtMs = null
            return null
        }

        if (!armed) {
            if (!leftInside && !rightInside) armed = true
            leftWasInside = leftInside
            rightWasInside = rightInside
            return null
        }

        val leftEntered = leftInside && !leftWasInside
        val rightEntered = rightInside && !rightWasInside
        leftWasInside = leftInside
        rightWasInside = rightInside

        return when (target.cue) {
            TargetCue.LEFT_RED -> when {
                leftEntered -> submitAndDisarm(target.id, TargetCue.LEFT_RED)
                rightEntered -> submitAndDisarm(target.id, TargetCue.RIGHT_BLUE)
                else -> null
            }
            TargetCue.RIGHT_BLUE -> when {
                rightEntered -> submitAndDisarm(target.id, TargetCue.RIGHT_BLUE)
                leftEntered -> submitAndDisarm(target.id, TargetCue.LEFT_RED)
                else -> null
            }
            TargetCue.BOTH_YELLOW -> recognizeBothHands(target.id, leftEntered, rightEntered, nowMs)
        }
    }

    private fun recognizeBothHands(
        activeTargetId: Long,
        leftEntered: Boolean,
        rightEntered: Boolean,
        nowMs: Long,
    ): HandTouchOutcome? {
        if (leftEntered) leftEnteredAtMs = nowMs
        if (rightEntered) rightEnteredAtMs = nowMs
        val leftAt = leftEnteredAtMs
        val rightAt = rightEnteredAtMs
        if (leftAt != null && rightAt != null) {
            return if (kotlin.math.abs(leftAt - rightAt) <= bothHandsWindowMs) {
                submitAndDisarm(activeTargetId, TargetCue.BOTH_YELLOW)
            } else {
                armed = false
                HandTouchOutcome.BothHandsOutOfSync
            }
        }
        return if (leftEntered || rightEntered) HandTouchOutcome.WaitingForBothHands else null
    }

    private fun submitAndDisarm(activeTargetId: Long, cue: TargetCue): HandTouchOutcome.Submit {
        armed = false
        return HandTouchOutcome.Submit(activeTargetId, cue)
    }

    private fun reset() {
        targetId = null
        armed = false
        leftWasInside = false
        rightWasInside = false
        leftEnteredAtMs = null
        rightEnteredAtMs = null
    }
}

private fun SpatialPoint.distanceTo(other: SpatialPoint): Float {
    val dx = xMeters - other.xMeters
    val dy = yMeters - other.yMeters
    val dz = zMeters - other.zMeters
    return sqrt(dx * dx + dy * dy + dz * dz)
}
