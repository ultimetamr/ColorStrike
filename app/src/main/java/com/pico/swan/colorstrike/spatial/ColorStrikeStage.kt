package com.pico.swan.colorstrike.spatial

import android.util.Log
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.hand.HandJoint.Index.INDEX_TIP
import com.pico.spatial.tracking.hand.HandJoint.Index.PALM
import com.pico.spatial.tracking.hand.HandPose
import com.pico.spatial.tracking.hand.HandTrackingProvider
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.haptic.controllerHapticFeedback
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.swan.colorstrike.domain.model.GamePhase
import com.pico.swan.colorstrike.domain.model.InputSource
import com.pico.swan.colorstrike.domain.model.SpatialPoint
import com.pico.swan.colorstrike.domain.model.TargetCue
import com.pico.swan.colorstrike.domain.usecase.HandTouchOutcome
import com.pico.swan.colorstrike.domain.usecase.HandTouchRecognizer
import com.pico.swan.colorstrike.ui.ColorStrikeEvent
import com.pico.swan.colorstrike.ui.rememberSharedColorStrikeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

private const val STAGE_PANEL = "color-strike-stage-panel"
private const val TARGET_PANEL = "color-strike-target-panel"
private const val HAND_LOG_TAG = "ColorStrikeHand"

private val StageTriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/** Full Space Stage with visible targets and direct palm/index-fingertip touch input. */
@Composable
fun ColorStrikeStage() {
    val model = rememberSharedColorStrikeViewModel()
    val state by model.uiState.collectAsState()
    val handTrackingProvider = remember { HandTrackingProvider() }
    val touchRecognizer = remember { HandTouchRecognizer() }
    val rootEntity = remember { Entity() }
    var targetEntity by remember { mutableStateOf<Entity?>(null) }

    DisposableEffect(handTrackingProvider) {
        val result = handTrackingProvider.start()
        Log.i(HAND_LOG_TAG, "providerStart=$result support=${handTrackingProvider.supportState}")
        model.onEvent(ColorStrikeEvent.TrackingChanged(leftAvailable = false, rightAvailable = false))
        onDispose {
            handTrackingProvider.stop()
            Log.i(HAND_LOG_TAG, "providerStopped")
        }
    }

    LaunchedEffect(handTrackingProvider, rootEntity, model) {
        var lastAvailability: Pair<Boolean, Boolean>? = null
        handTrackingProvider.dataFlow.collect { data ->
            val availability = (data.left != null) to (data.right != null)
            if (availability != lastAvailability) {
                lastAvailability = availability
                model.onEvent(ColorStrikeEvent.TrackingChanged(availability.first, availability.second))
                Log.i(HAND_LOG_TAG, "tracking left=${availability.first} right=${availability.second}")
            }

            val currentState = model.uiState.value
            val target = currentState.activeTarget.takeIf { currentState.phase == GamePhase.TRAINING }
            val outcome = touchRecognizer.update(
                target = target,
                leftContactPoints = data.left?.contactPointsIn(rootEntity),
                rightContactPoints = data.right?.contactPointsIn(rootEntity),
                nowMs = System.currentTimeMillis(),
            )
            when (outcome) {
                is HandTouchOutcome.Submit -> {
                    Log.i(HAND_LOG_TAG, "touch target=${outcome.targetId} cue=${outcome.cue}")
                    model.onEvent(ColorStrikeEvent.SubmitCue(outcome.cue, InputSource.HANDS, outcome.targetId))
                }
                HandTouchOutcome.WaitingForBothHands -> model.onEvent(ColorStrikeEvent.WaitingForBothHands)
                HandTouchOutcome.BothHandsOutOfSync -> model.onEvent(ColorStrikeEvent.BothHandsOutOfSync)
                null -> Unit
            }
        }
    }

    LaunchedEffect(handTrackingProvider) {
        while (true) {
            if (handTrackingProvider.supportState != DataProvider.SupportState.SUPPORTED) {
                model.onEvent(ColorStrikeEvent.TrackingChanged(leftAvailable = false, rightAvailable = false))
            }
            delay(500L)
        }
    }

    LaunchedEffect(state.phase, state.activeTarget?.id, targetEntity) {
        targetEntity?.components?.get(TransformComponent::class.java)?.apply {
            val target = state.activeTarget
            if (state.phase == GamePhase.TRAINING && target != null) {
                setPosition(Vector3(target.point.xMeters, target.point.yMeters, target.point.zMeters))
                setScaleVector(Vector3(1f))
            } else {
                setScaleVector(Vector3(0.001f))
            }
        }
    }

    SpatialView(
        initial = { content, attachments ->
            attachChild(rootEntity, attachments.entity(STAGE_PANEL), Vector3(0f, 1.78f, -1.15f))
            targetEntity = attachments.entity(TARGET_PANEL)?.also { rootEntity.addChild(it) }
            content.addEntity(rootEntity)
        },
        attachments = {
            AttachmentPanel(id = STAGE_PANEL) {
                StageControlPanel(
                    remainingSeconds = state.remainingMs / 1_000,
                    feedback = state.feedback,
                    targetId = state.activeTarget?.id,
                    onControllerCue = { cue, targetId ->
                        model.onEvent(ColorStrikeEvent.SubmitCue(cue, InputSource.CONTROLLER, targetId))
                    },
                )
            }
            AttachmentPanel(id = TARGET_PANEL) {
                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    state.activeTarget?.takeIf { state.phase == GamePhase.TRAINING }?.let {
                        StageCueTarget(it.cue)
                    }
                }
            }
        },
    )
}

@Composable
private fun StageControlPanel(
    remainingSeconds: Long,
    feedback: String,
    targetId: Long?,
    onControllerCue: (TargetCue, Long?) -> Unit,
) {
    Box(Modifier.size(760.dp, 250.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Color Strike · 剩余 ${remainingSeconds}s", style = PicoTheme.typography.titleLarge)
            Text(feedback, style = PicoTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TargetCue.entries.forEach { cue ->
                    Button(onClick = { onControllerCue(cue, targetId) }) { Text(cue.controllerHint) }
                }
            }
            Text("手势：用对应手的手掌或食指尖直接触碰目标", style = PicoTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StageCueTarget(cue: TargetCue) {
    val color = when (cue) {
        TargetCue.LEFT_RED -> Color(0xFFE85555) // design-style: fixed-figma-color game cue red
        TargetCue.RIGHT_BLUE -> Color(0xFF4B8BFF) // design-style: fixed-figma-color game cue blue
        TargetCue.BOTH_YELLOW -> Color(0xFFFFC843) // design-style: fixed-figma-color game cue yellow
    }
    val shape = when (cue) {
        TargetCue.LEFT_RED -> CircleShape
        TargetCue.RIGHT_BLUE -> RoundedCornerShape(12.dp)
        TargetCue.BOTH_YELLOW -> StageTriangleShape
    }
    val label = when (cue) {
        TargetCue.LEFT_RED -> "●\n左手"
        TargetCue.RIGHT_BLUE -> "■\n右手"
        TargetCue.BOTH_YELLOW -> "▲\n双手"
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(220.dp)
            .clip(shape)
            .spatialHoverEffect(enabled = true)
            .clickable(
                enabled = false,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {},
            )
            .controllerHapticFeedback(interactionSource = interactionSource)
            .background(color)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PicoTheme.typography.titleLarge)
    }
}

private fun HandPose.contactPointsIn(rootEntity: Entity): List<SpatialPoint> = listOf(PALM, INDEX_TIP).map { index ->
    val position = rootEntity.convertPositionFrom(this[index].position, null)
    SpatialPoint(position.x, position.y, position.z)
}

private fun attachChild(root: Entity, entity: Entity?, position: Vector3) {
    entity?.apply {
        components[TransformComponent::class.java]?.setPosition(position)
        root.addChild(this)
    }
}
