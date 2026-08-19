package com.pico.swan.colorstrike.domain.usecase

import com.pico.swan.colorstrike.domain.model.Difficulty
import com.pico.swan.colorstrike.domain.model.PostureMode
import com.pico.swan.colorstrike.domain.model.SafeTargetZone
import com.pico.swan.colorstrike.domain.model.SpatialPoint
import com.pico.swan.colorstrike.domain.model.TargetCue
import com.pico.swan.colorstrike.domain.model.TargetSpawn

const val SESSION_DURATION_MS = 90_000L
const val COMBO_STEP_MS = 30_000L

val SAFE_TARGET_ZONES = mapOf(
    PostureMode.STANDING to SafeTargetZone(PostureMode.STANDING, -0.55f, 0.55f, 1.05f, 1.55f, 1.10f, 1.45f),
    PostureMode.SEATED to SafeTargetZone(PostureMode.SEATED, -0.45f, 0.45f, 0.75f, 1.15f, 1.10f, 1.45f),
)

private val STANDING_POINTS = listOf(
    SpatialPoint(-0.38f, 1.18f, -1.22f), SpatialPoint(0f, 1.32f, -1.30f),
    SpatialPoint(0.38f, 1.18f, -1.22f), SpatialPoint(-0.22f, 1.48f, -1.25f),
    SpatialPoint(0.22f, 1.48f, -1.25f),
)
private val SEATED_POINTS = listOf(
    SpatialPoint(-0.32f, 0.83f, -1.22f), SpatialPoint(0f, 0.96f, -1.30f),
    SpatialPoint(0.32f, 0.83f, -1.22f), SpatialPoint(-0.18f, 1.10f, -1.25f),
    SpatialPoint(0.18f, 1.10f, -1.25f),
)

/** Four fixed cue phrases; complexity increases only by phrase length. */
val FIXED_COMBOS = listOf(
    listOf(TargetCue.LEFT_RED, TargetCue.RIGHT_BLUE),
    listOf(TargetCue.LEFT_RED, TargetCue.RIGHT_BLUE, TargetCue.BOTH_YELLOW),
    listOf(TargetCue.RIGHT_BLUE, TargetCue.LEFT_RED, TargetCue.BOTH_YELLOW),
    listOf(TargetCue.BOTH_YELLOW, TargetCue.LEFT_RED, TargetCue.RIGHT_BLUE),
)

fun safeZoneFor(posture: PostureMode): SafeTargetZone = checkNotNull(SAFE_TARGET_ZONES[posture])

fun combinationLevel(elapsedMs: Long): Int = (elapsedMs / COMBO_STEP_MS).toInt().coerceIn(0, 2)

fun cueFor(sequenceIndex: Int, elapsedMs: Long): TargetCue {
    val combo = FIXED_COMBOS[(sequenceIndex / (combinationLevel(elapsedMs) + 2)) % FIXED_COMBOS.size]
    return combo[sequenceIndex % combo.size]
}

fun spawnTarget(
    sequenceIndex: Int,
    elapsedMs: Long,
    posture: PostureMode,
    difficulty: Difficulty,
    nowMs: Long,
): TargetSpawn {
    val points = if (posture == PostureMode.SEATED) SEATED_POINTS else STANDING_POINTS
    val point = points[sequenceIndex % points.size]
    check(safeZoneFor(posture).contains(point)) { "Unsafe target candidate: $point" }
    return TargetSpawn(
        id = sequenceIndex.toLong(), cue = cueFor(sequenceIndex, elapsedMs), point = point,
        openedAtMs = nowMs, expiresAtMs = nowMs + difficulty.targetIntervalMs,
    )
}

fun isSessionComplete(elapsedMs: Long): Boolean = elapsedMs >= SESSION_DURATION_MS
