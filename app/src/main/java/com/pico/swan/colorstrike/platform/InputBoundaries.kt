package com.pico.swan.colorstrike.platform

/** Extension points keep gesture, controller, and re-calibration paths equivalent. */
interface HandInput { fun isLeftAvailable(): Boolean; fun isRightAvailable(): Boolean }
interface ControllerInput { fun submitLeft(); fun submitRight(); fun submitBoth() }
interface GrabInteractable { fun onTargetTouch(targetId: Long) }
interface AudioCue { fun correct(); fun softMiss() }
interface Haptics { fun confirm(); fun softReject() }
interface TutorialStep { val id: String; val copy: String }
interface PauseMenu
interface ScreenshotExporter { fun requestCapture() }
