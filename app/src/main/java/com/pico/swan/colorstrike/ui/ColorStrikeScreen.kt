package com.pico.swan.colorstrike.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pico.swan.colorstrike.BuildConfig
import com.pico.swan.colorstrike.data.ColorStrikeRepository
import com.pico.swan.colorstrike.domain.model.Difficulty
import com.pico.swan.colorstrike.domain.model.GamePhase
import com.pico.swan.colorstrike.domain.model.InputSource
import com.pico.swan.colorstrike.domain.model.PostureMode
import com.pico.swan.colorstrike.domain.model.TargetCue
import com.pico.swan.colorstrike.domain.usecase.SESSION_DURATION_MS
import com.pico.swan.colorstrike.platform.DebugLaunchState
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.haptic.controllerHapticFeedback
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.platform.containers.closeStage
import com.pico.spatial.ui.platform.containers.openStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val COLOR_STRIKE_STAGE_ID = "ColorStrikeStage"

private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun ColorStrikeScreen(model: ColorStrikeViewModel? = null) {
    val context = LocalContext.current
    val scenario = if (BuildConfig.DEBUG) DebugLaunchState.scenario else null
    val requestedDuration = if (BuildConfig.DEBUG) {
        DebugLaunchState.durationMs.coerceIn(1_000L, SESSION_DURATION_MS)
    } else SESSION_DURATION_MS
    val resolved = model ?: rememberSharedColorStrikeViewModel(requestedDuration)
    val state by resolved.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.phase) {
        while (state.phase == GamePhase.TRAINING) {
            delay(250L)
            resolved.onEvent(ColorStrikeEvent.Tick)
        }
        // Keep the active Stage open for pause, result, and replay screens. Closing it here
        // removes the app's visible spatial container before the result can be read.
    }
    LaunchedEffect(scenario) {
        if (scenario != null) Log.i("ColorStrikeTest", "scenario=$scenario durationMs=$requestedDuration")
        when (scenario) {
            "tutorial" -> resolved.onEvent(ColorStrikeEvent.BeginTutorial)
            "training" -> {
                resolved.onEvent(ColorStrikeEvent.SelectPosture(PostureMode.SEATED))
                resolved.onEvent(ColorStrikeEvent.StartTraining)
                CoroutineScope(Dispatchers.Default).launch { context.openStage(COLOR_STRIKE_STAGE_ID) }
            }
            "full-flow" -> runDebugFlow(context, resolved)
        }
    }
    DisposableEffect(Unit) {
        onDispose { CoroutineScope(Dispatchers.Default).launch { closeStage() } }
    }

    when (state.phase) {
        GamePhase.CALIBRATION -> Calibration(state, resolved::onEvent)
        GamePhase.TUTORIAL -> Tutorial(state, resolved::onEvent)
        GamePhase.TRAINING -> Training(state, resolved::onEvent)
        GamePhase.PAUSED -> Pause(resolved::onEvent)
        GamePhase.RESULT -> Result(state, resolved::onEvent)
    }
}

private suspend fun runDebugFlow(context: Context, model: ColorStrikeViewModel) {
    model.onEvent(ColorStrikeEvent.SelectPosture(PostureMode.SEATED))
    model.onEvent(ColorStrikeEvent.BeginTutorial)
    Log.i("ColorStrikeTest", "phase=tutorial posture=seated")
    delay(250L)
    model.onEvent(ColorStrikeEvent.NextTutorial)
    model.onEvent(ColorStrikeEvent.NextTutorial)
    model.onEvent(ColorStrikeEvent.StartTraining)
    Log.i("ColorStrikeTest", "phase=training")
    CoroutineScope(Dispatchers.Default).launch { context.openStage(COLOR_STRIKE_STAGE_ID) }
    delay(300L)
    model.uiState.value.activeTarget?.cue?.let {
        model.onEvent(ColorStrikeEvent.SubmitCue(it, InputSource.PANEL_FALLBACK))
    }
    model.onEvent(ColorStrikeEvent.Pause)
    Log.i("ColorStrikeTest", "phase=paused")
    delay(250L)
    model.onEvent(ColorStrikeEvent.Resume)
    Log.i("ColorStrikeTest", "phase=resumed")
    delay(250L)
    model.onEvent(ColorStrikeEvent.Pause)
    model.onEvent(ColorStrikeEvent.Reset)
    Log.i("ColorStrikeTest", "phase=reset-training")
    delay(300L)
    model.uiState.value.activeTarget?.cue?.let {
        model.onEvent(ColorStrikeEvent.SubmitCue(it, InputSource.CONTROLLER))
    }
}

@Composable
private fun Surface(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize()) { content() }

@Composable
private fun Calibration(state: ColorStrikeUiState, event: (ColorStrikeEvent) -> Unit) = Surface {
    Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Header("色彩拳击", "90 秒色彩反应游戏，不是专业拳击训练")
        Text("只做前方舒适范围内的触碰。没有动作评分、热量或身体评价。", style = PicoTheme.typography.bodyLarge)
        Selector("姿势", PostureMode.entries, state.posture, { it.label }) { event(ColorStrikeEvent.SelectPosture(it)) }
        Selector("难度", Difficulty.entries, state.difficulty, { "${it.label} · 安全节奏" }) { event(ColorStrikeEvent.SelectDifficulty(it)) }
        Selector("输入", InputSource.entries, state.inputSource, {
            when (it) {
                InputSource.HANDS -> "手部触碰（推荐）"
                InputSource.CONTROLLER -> "手柄"
                InputSource.PANEL_FALLBACK -> "面板回退"
            }
        }) { event(ColorStrikeEvent.SelectInput(it)) }
        Text(
            if (state.posture == PostureMode.SEATED) "坐姿已启用：目标整体降低，仍可完整游玩。"
            else "站姿：目标仅在胸前至肩部舒适区。",
            style = PicoTheme.typography.bodyMedium,
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = { event(ColorStrikeEvent.BeginTutorial) }) { Text("学习三条规则（约 10 秒）") }
    }
}

@Composable
private fun Tutorial(state: ColorStrikeUiState, event: (ColorStrikeEvent) -> Unit) = Surface {
    val context = LocalContext.current
    val cue = TargetCue.entries[state.tutorialStep]
    Column(
        Modifier.fillMaxSize().padding(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Header("看颜色，也看形状和图标", "第 ${state.tutorialStep + 1}/3 条规则")
        CueTarget(cue, large = true, onClick = {})
        Text("${cue.colorName}${cue.shapeName} + ${cue.handHint}：用${cue.label}手掌或食指尖触碰", style = PicoTheme.typography.titleLarge)
        Text("手柄回退：${cue.controllerHint}。黄色三角目标请双键同时按。", style = PicoTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.tutorialStep < 2) {
                Button(onClick = { event(ColorStrikeEvent.NextTutorial) }) { Text("下一条") }
            } else {
                Button(onClick = {
                    CoroutineScope(Dispatchers.Default).launch { context.openStage(COLOR_STRIKE_STAGE_ID) }
                    event(ColorStrikeEvent.StartTraining)
                }) { Text("开始 90 秒") }
            }
            Button(onClick = { event(ColorStrikeEvent.BackToCalibration) }) { Text("返回校准") }
        }
    }
}

@Composable
private fun Training(state: ColorStrikeUiState, event: (ColorStrikeEvent) -> Unit) = Surface {
    val target = state.activeTarget
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Header("色彩反应中", "剩余 ${state.remainingMs / 1000}s · 连击 ${state.combo} · 分数 ${state.score}")
            Button(onClick = { event(ColorStrikeEvent.Pause) }) { Text("暂停") }
        }
        NeonLine(state.neonSegments)
        Text(state.feedback, style = PicoTheme.typography.bodyLarge)
        if (state.missingLeftHand || state.missingRightHand) {
            Text("手部追踪不稳定：可切换手柄或重新校准。", style = PicoTheme.typography.bodyMedium)
        }
        Spacer(Modifier.weight(1f))
        target?.let {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("目标在前方安全区：${it.point.xMeters}, ${it.point.yMeters}, ${it.point.zMeters} m", style = PicoTheme.typography.labelMedium)
                CueTarget(it.cue, large = true, onClick = { event(ColorStrikeEvent.SubmitCue(it.cue, state.inputSource, it.id)) })
                Text("在全空间直接用手掌或食指尖触碰；手柄回退可使用下方按钮。", style = PicoTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TargetCue.entries.forEach { cue ->
                        Button(onClick = { event(ColorStrikeEvent.SubmitCue(cue, InputSource.PANEL_FALLBACK)) }) { Text(cue.controllerHint) }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Pause(event: (ColorStrikeEvent) -> Unit) = Surface {
    Column(
        Modifier.fillMaxSize().padding(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Header("已暂停", "可以恢复、重置，或返回校准。")
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { event(ColorStrikeEvent.Resume) }) { Text("继续") }
            Button(onClick = { event(ColorStrikeEvent.Reset) }) { Text("重置本局") }
            Button(onClick = { event(ColorStrikeEvent.BackToCalibration) }) { Text("退出到校准") }
        }
    }
}

@Composable
private fun Result(state: ColorStrikeUiState, event: (ColorStrikeEvent) -> Unit) = Surface {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header("本局完成", "色彩反应记录已保存在本机")
        Text("最高连击 ${state.bestCombo} · 分数 ${state.score}", style = PicoTheme.typography.titleLarge)
        TargetCue.entries.forEach { cue ->
            val item = state.stats.getValue(cue)
            Text("${cue.colorName}${cue.shapeName} ${cue.label}：${item.accuracyPercent}%（${item.correct}/${item.attempts}）", style = PicoTheme.typography.bodyLarge)
        }
        Text("这是反应游戏结果，不构成身体能力或健康评价。", style = PicoTheme.typography.bodyMedium)
        Button(onClick = { event(ColorStrikeEvent.BackToCalibration) }) { Text("再来一局") }
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column {
        Text(title, style = PicoTheme.typography.displaySmall)
        Text(subtitle, style = PicoTheme.typography.bodyLarge)
    }
}

@Composable
private fun CueTarget(cue: TargetCue, large: Boolean, onClick: () -> Unit) {
    val size = if (large) 190.dp else 76.dp
    val color = when (cue) {
        TargetCue.LEFT_RED -> Color(0xFFE85555) // design-style: fixed-figma-color game cue red
        TargetCue.RIGHT_BLUE -> Color(0xFF4B8BFF) // design-style: fixed-figma-color game cue blue
        TargetCue.BOTH_YELLOW -> Color(0xFFFFC843) // design-style: fixed-figma-color game cue yellow
    }
    val shape = when (cue) {
        TargetCue.LEFT_RED -> CircleShape
        TargetCue.RIGHT_BLUE -> RoundedCornerShape(8.dp)
        TargetCue.BOTH_YELLOW -> TriangleShape
    }
    val label = when (cue) {
        TargetCue.LEFT_RED -> "●\n左手"
        TargetCue.RIGHT_BLUE -> "■\n右手"
        TargetCue.BOTH_YELLOW -> "▲\n双手"
    }
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(size).clip(shape)
            .spatialHoverEffect(enabled = true)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .controllerHapticFeedback(interactionSource = interaction)
            .background(color),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = PicoTheme.typography.titleLarge) }
}

@Composable
private fun NeonLine(segments: Int) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    repeat(12) {
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(8.dp))
                .background(
                    if (it < segments) Color(0xFF54F0D0) // design-style: fixed-figma-color neon success line
                    else Color(0x3344FFFF), // design-style: fixed-figma-color inactive neon line
                ),
        )
    }
}

@Composable
private fun <T> Selector(label: String, options: Iterable<T>, selected: T, text: (T) -> String, select: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = PicoTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { item ->
                Button(onClick = { select(item) }) { Text((if (item == selected) "● " else "○ ") + text(item)) }
            }
        }
    }
}

class ColorStrikeViewModelFactory(
    private val repository: ColorStrikeRepository,
    private val sessionDurationMs: Long = SESSION_DURATION_MS,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = ColorStrikeViewModel(repository, sessionDurationMs) as T
}
