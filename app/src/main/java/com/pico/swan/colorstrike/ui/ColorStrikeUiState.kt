package com.pico.swan.colorstrike.ui

import com.pico.swan.colorstrike.domain.model.ColorStats
import com.pico.swan.colorstrike.domain.model.Difficulty
import com.pico.swan.colorstrike.domain.model.GamePhase
import com.pico.swan.colorstrike.domain.model.InputSource
import com.pico.swan.colorstrike.domain.model.PostureMode
import com.pico.swan.colorstrike.domain.model.TargetCue
import com.pico.swan.colorstrike.domain.model.TargetSpawn

data class ColorStrikeUiState(
    val phase: GamePhase = GamePhase.CALIBRATION,
    val posture: PostureMode = PostureMode.STANDING,
    val difficulty: Difficulty = Difficulty.STANDARD,
    val inputSource: InputSource = InputSource.HANDS,
    val tutorialStep: Int = 0,
    val remainingMs: Long = 90_000L,
    val activeTarget: TargetSpawn? = null,
    val sequenceIndex: Int = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val stats: Map<TargetCue, ColorStats> = TargetCue.entries.associateWith { ColorStats() },
    val neonSegments: Int = 0,
    val feedback: String = "先选择模式，再开始安全的色彩反应游戏。",
    val missingLeftHand: Boolean = false,
    val missingRightHand: Boolean = false,
    val recentSessions: Int = 0,
)

sealed interface ColorStrikeEvent {
    data class SelectPosture(val posture: PostureMode) : ColorStrikeEvent
    data class SelectDifficulty(val difficulty: Difficulty) : ColorStrikeEvent
    data class SelectInput(val source: InputSource) : ColorStrikeEvent
    data object BeginTutorial : ColorStrikeEvent
    data object NextTutorial : ColorStrikeEvent
    data object StartTraining : ColorStrikeEvent
    data class SubmitCue(
        val cue: TargetCue,
        val source: InputSource = InputSource.PANEL_FALLBACK,
        val targetId: Long? = null,
    ) : ColorStrikeEvent
    data object Tick : ColorStrikeEvent
    data object Pause : ColorStrikeEvent
    data object Resume : ColorStrikeEvent
    data object Reset : ColorStrikeEvent
    data class TrackingChanged(val leftAvailable: Boolean, val rightAvailable: Boolean) : ColorStrikeEvent
    data object WaitingForBothHands : ColorStrikeEvent
    data object BothHandsOutOfSync : ColorStrikeEvent
    data object BackToCalibration : ColorStrikeEvent
}
