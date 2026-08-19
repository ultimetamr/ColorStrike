package com.pico.swan.colorstrike.domain.model

enum class PostureMode(val label: String) { STANDING("站姿"), SEATED("坐姿") }

enum class Difficulty(val label: String, val targetIntervalMs: Long) {
    CALM("轻松", 2_800L), STANDARD("标准", 2_350L), RHYTHM("节奏", 2_000L)
}

enum class TargetCue(
    val label: String,
    val colorName: String,
    val shapeName: String,
    val handHint: String,
    val controllerHint: String,
) {
    LEFT_RED("左手", "红色", "圆形", "左手图标", "左键"),
    RIGHT_BLUE("右手", "蓝色", "方形", "右手图标", "右键"),
    BOTH_YELLOW("双手", "黄色", "三角形", "双手图标", "双键同时")
}

enum class GamePhase { CALIBRATION, TUTORIAL, TRAINING, PAUSED, RESULT }

enum class InputSource { HANDS, CONTROLLER, PANEL_FALLBACK }

data class SpatialPoint(val xMeters: Float, val yMeters: Float, val zMeters: Float)

data class SafeTargetZone(
    val posture: PostureMode,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minDistance: Float,
    val maxDistance: Float,
    val headExclusionRadius: Float = 0.30f,
) {
    fun contains(point: SpatialPoint): Boolean {
        val forwardDistance = -point.zMeters
        val distanceFromHead = kotlin.math.sqrt(
            point.xMeters * point.xMeters + point.yMeters * point.yMeters + point.zMeters * point.zMeters,
        )
        return point.xMeters in minX..maxX && point.yMeters in minY..maxY &&
            forwardDistance in minDistance..maxDistance && distanceFromHead > headExclusionRadius
    }
}

data class TargetSpawn(
    val id: Long,
    val cue: TargetCue,
    val point: SpatialPoint,
    val openedAtMs: Long,
    val expiresAtMs: Long,
)

data class ColorStats(val correct: Int = 0, val attempts: Int = 0) {
    val accuracyPercent: Int get() = if (attempts == 0) 0 else (correct * 100 / attempts)
}

data class SessionSummary(
    val finishedAtEpochMs: Long,
    val posture: PostureMode,
    val difficulty: Difficulty,
    val score: Int,
    val bestCombo: Int,
    val stats: Map<TargetCue, ColorStats>,
)
