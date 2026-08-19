# ColorStrike project guidance

ColorStrike is a Chinese, 90-second colour-reaction game for PICO OS 6. It is a Full Space Stage experience opened from a small control panel. It is not boxing instruction, health assessment, or exercise prescription.

- `domain/`: safe-zone definitions, target combinations, scoring, session rules, and the pure `HandTouchRecognizer`.
- `data/`: local recent-session persistence boundary.
- `ui/`: state, shared process model, ViewModel, SpatialUI surfaces, and simulator/controller fallbacks.
- `spatial/`: Stage `SpatialView`, visible target attachment, `HandTrackingProvider`, palm/index-fingertip coordinate conversion, and touch routing.
- `platform/`: launch and PICO app lifecycle.

Safety is non-negotiable: target candidates must use `SafeTargetZone`, remain forward and outside the head exclusion radius. Do not add body scoring, calories, combat, networking, or runtime AI.

All 2D interfaces use `com.pico.spatial.ui.*` inside `PicoTheme`; Material/Material3 are forbidden. The launcher control window keeps system `Material.Regular` glass and its root adds no background. Build with `..\gradlew.bat -p . :app:testDebugUnitTest :app:assembleDebug` while Java 21 is selected.

Hand input is real Full Space tracking, not a click alias. `ColorStrikeStage` starts and stops `HandTrackingProvider`, converts `PALM` and `INDEX_TIP` poses into the Stage root space, and feeds `HandTouchRecognizer`. Red/blue accept the corresponding hand, yellow requires both hands within 300 ms, and tracking recovery must leave the target before re-arming. Controller/panel buttons remain equivalent fallbacks. Use `ColorStrikeHand` logcat entries to verify provider availability and physical touches on a device.

Current real-device baseline: debug APK installs and starts on Swan `PB314XHGKC160016G`; hand provider reports `SUCCESS / SUPPORTED`; unit tests and `assembleDebug` pass; no crash was detected in the launch monitoring window. Physical hand joint frames and touch logs still require a user to place hands in the headset camera view.

<!-- pico-cli:plugin-context:pico-spatial-agentic-tools:start -->
## Plugin Context

Also read `./PICO-SPATIAL-AGENTIC-TOOLS.AGENTS.md` for PICO Spatial plugin guidance.
<!-- pico-cli:plugin-context:pico-spatial-agentic-tools:end -->
