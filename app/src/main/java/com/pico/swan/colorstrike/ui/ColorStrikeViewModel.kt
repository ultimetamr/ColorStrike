package com.pico.swan.colorstrike.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pico.swan.colorstrike.data.ColorStrikeRepository
import com.pico.swan.colorstrike.domain.model.ColorStats
import com.pico.swan.colorstrike.domain.model.GamePhase
import com.pico.swan.colorstrike.domain.model.InputSource
import com.pico.swan.colorstrike.domain.model.SessionSummary
import com.pico.swan.colorstrike.domain.model.TargetCue
import com.pico.swan.colorstrike.domain.usecase.SESSION_DURATION_MS
import com.pico.swan.colorstrike.domain.usecase.spawnTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ColorStrikeViewModel(
    private val repository: ColorStrikeRepository,
    private val sessionDurationMs: Long = SESSION_DURATION_MS,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ColorStrikeUiState())
    val uiState: StateFlow<ColorStrikeUiState> = _uiState.asStateFlow()
    private var trainingStartedAtMs = 0L
    private var pausedAtMs = 0L
    private var pausedDurationMs = 0L

    init { viewModelScope.launch { _uiState.update { it.copy(recentSessions = repository.recentCount()) } } }

    fun onEvent(event: ColorStrikeEvent) = when (event) {
        is ColorStrikeEvent.SelectPosture -> _uiState.update { it.copy(posture = event.posture) }
        is ColorStrikeEvent.SelectDifficulty -> _uiState.update { it.copy(difficulty = event.difficulty) }
        is ColorStrikeEvent.SelectInput -> _uiState.update { it.copy(inputSource = event.source) }
        ColorStrikeEvent.BeginTutorial -> _uiState.update { it.copy(phase = GamePhase.TUTORIAL, tutorialStep = 0) }
        ColorStrikeEvent.NextTutorial -> _uiState.update { it.copy(tutorialStep = (it.tutorialStep + 1).coerceAtMost(2)) }
        ColorStrikeEvent.StartTraining -> startTraining()
        is ColorStrikeEvent.SubmitCue -> submit(event.cue, event.source, event.targetId)
        ColorStrikeEvent.Tick -> tick()
        ColorStrikeEvent.Pause -> {
            if (_uiState.value.phase == GamePhase.TRAINING) {
                pausedAtMs = System.currentTimeMillis()
                _uiState.update { it.copy(phase = GamePhase.PAUSED) }
            }
            Unit
        }
        ColorStrikeEvent.Resume -> {
            if (_uiState.value.phase == GamePhase.PAUSED) {
                pausedDurationMs += System.currentTimeMillis() - pausedAtMs
                _uiState.update { it.copy(phase = GamePhase.TRAINING) }
            }
            Unit
        }
        ColorStrikeEvent.Reset -> startTraining()
        is ColorStrikeEvent.TrackingChanged -> _uiState.update {
            it.copy(missingLeftHand = !event.leftAvailable, missingRightHand = !event.rightAvailable)
        }
        ColorStrikeEvent.WaitingForBothHands -> _uiState.update {
            it.copy(feedback = "已检测到一只手，请让双手同时触碰黄色三角。")
        }
        ColorStrikeEvent.BothHandsOutOfSync -> _uiState.update {
            it.copy(feedback = "双手不同步：请收回双手，再同时触碰黄色三角。")
        }
        ColorStrikeEvent.BackToCalibration -> _uiState.update { ColorStrikeUiState(recentSessions = it.recentSessions) }
    }

    private fun startTraining() {
        trainingStartedAtMs = System.currentTimeMillis()
        pausedDurationMs = 0L
        val state = _uiState.value
        _uiState.update {
            it.copy(
                phase = GamePhase.TRAINING,
                remainingMs = sessionDurationMs,
                sequenceIndex = 0,
                score = 0,
                combo = 0,
                bestCombo = 0,
                neonSegments = 0,
                stats = TargetCue.entries.associateWith { ColorStats() },
                activeTarget = spawnTarget(0, 0L, state.posture, state.difficulty, trainingStartedAtMs),
                feedback = "开始：用对应手的手掌或食指尖触碰前方目标。",
            )
        }
    }

    private fun tick() {
        if (_uiState.value.phase != GamePhase.TRAINING) return
        val now = System.currentTimeMillis()
        val elapsed = now - trainingStartedAtMs - pausedDurationMs
        if (elapsed >= sessionDurationMs) {
            finish()
            return
        }
        val state = _uiState.value
        if (now >= (state.activeTarget?.expiresAtMs ?: Long.MAX_VALUE)) {
            _uiState.update { it.copy(combo = 0, feedback = "错过也没关系，下一个目标已出现。") }
            nextTarget(elapsed, now)
        } else {
            _uiState.update { it.copy(remainingMs = (sessionDurationMs - elapsed).coerceAtLeast(0L)) }
        }
    }

    private fun submit(cue: TargetCue, source: InputSource, targetId: Long? = null) {
        val state = _uiState.value
        if (state.phase != GamePhase.TRAINING) return
        if (cue == TargetCue.BOTH_YELLOW && (state.missingLeftHand || state.missingRightHand) && source == InputSource.HANDS) {
            _uiState.update { it.copy(feedback = "双手追踪不同步：可切换到手柄双键，或重新校准。") }
            return
        }
        val target = state.activeTarget ?: return
        if (targetId != null && target.id != targetId) return
        val correct = cue == target.cue
        val changedStats = state.stats.toMutableMap()
        val prior = changedStats.getValue(target.cue)
        changedStats[target.cue] = prior.copy(
            correct = prior.correct + if (correct) 1 else 0,
            attempts = prior.attempts + 1,
        )
        val combo = if (correct) state.combo + 1 else 0
        _uiState.update {
            it.copy(
                score = it.score + if (correct) 10 + combo else 0,
                combo = combo,
                bestCombo = maxOf(it.bestCombo, combo),
                neonSegments = if (correct) (it.neonSegments + 1).coerceAtMost(12) else it.neonSegments,
                stats = changedStats,
                inputSource = source,
                feedback = if (correct) "命中！霓虹线路点亮。" else "这次手部不匹配，收回手再试下一个。",
            )
        }
        val now = System.currentTimeMillis()
        nextTarget(now - trainingStartedAtMs - pausedDurationMs, now)
    }

    private fun nextTarget(elapsed: Long, now: Long) {
        val index = _uiState.value.sequenceIndex + 1
        _uiState.update {
            it.copy(sequenceIndex = index, activeTarget = spawnTarget(index, elapsed, it.posture, it.difficulty, now))
        }
    }

    private fun finish() {
        val state = _uiState.value
        val summary = SessionSummary(
            System.currentTimeMillis(), state.posture, state.difficulty, state.score, state.bestCombo, state.stats,
        )
        viewModelScope.launch {
            repository.save(summary)
            Log.i("ColorStrikeTest", "phase=result saved=true recentSessions=${repository.recentCount()}")
            _uiState.update {
                it.copy(
                    phase = GamePhase.RESULT,
                    activeTarget = null,
                    remainingMs = 0L,
                    recentSessions = repository.recentCount(),
                    feedback = "完成 90 秒色彩反应游戏。",
                )
            }
        }
    }
}
