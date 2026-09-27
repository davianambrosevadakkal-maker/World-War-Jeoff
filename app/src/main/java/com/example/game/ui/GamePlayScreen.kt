package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.game.engine.GameEngine
import kotlin.math.PI

@Composable
fun GamePlayScreen(
  engine: GameEngine,
  onExitToLobby: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  var moveStickX by remember { mutableFloatStateOf(0f) }
  var moveStickY by remember { mutableFloatStateOf(0f) }
  var lookStickX by remember { mutableFloatStateOf(0f) }
  var lookStickY by remember { mutableFloatStateOf(0f) }
  var isFiring by remember { mutableStateOf(false) }

  // Game Loop driven by hardware VSYNC (Choreographer via withFrameNanos)
  LaunchedEffect(Unit) {
    var lastNano = System.nanoTime()
    while (true) {
      withFrameNanos { nowNano ->
        val dt = ((nowNano - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastNano = nowNano

        // Update physics, bots, particles, and player
        engine.update(
          dt = dt,
          moveStickX = moveStickX,
          moveStickY = moveStickY,
          lookStickX = lookStickX,
          lookStickY = lookStickY
        )

        // Continuous automatic fire if trigger held
        if (isFiring) {
          engine.triggerPlayerFire()
        }

        // Decay touch look impulse
        lookStickX *= 0.6f
        lookStickY *= 0.6f
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    // 1. Hardware-Accelerated 3D Render Surface
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          // Free-look touch swipe across background
          detectDragGestures { change, dragAmount ->
            change.consume()
            lookStickX += dragAmount.x * 0.08f
            lookStickY += dragAmount.y * 0.08f
          }
        }
    ) {
      val w = size.width
      val h = size.height
      val nativeCanvas = drawContext.canvas.nativeCanvas

      // Convert FOV to radians
      val fovRad = (engine.prefs.fieldOfView * (PI / 180f)).toFloat()

      // 3D Raycast Scene Render
      engine.raycastRenderer.renderScene(
        canvas = nativeCanvas,
        screenWidth = w,
        screenHeight = h,
        playerX = engine.playerX,
        playerY = engine.playerY,
        playerAngle = engine.playerAngle,
        pitchOffset = engine.playerPitch,
        fovRad = fovRad,
        map = engine.map,
        bots = engine.bots,
        muzzleFlashIntensity = engine.weaponRenderer.flashIntensity,
        preset = engine.prefs.graphicsPreset
      )

      // 3D First-Person Weapon Render
      engine.weaponRenderer.renderWeapon(
        canvas = nativeCanvas,
        screenWidth = w,
        screenHeight = h,
        weapon = engine.currentWeapon,
        adsProgress = engine.adsProgress,
        isSprinting = engine.isSprinting,
        isSliding = engine.isSliding
      )
    }

    // 2. Tactical HUD Overlay
    if (!engine.isMatchOver) {
      GameHudOverlay(
        engine = engine,
        onMoveInput = { x, y ->
          moveStickX = x
          moveStickY = y
        },
        onLookInput = { lx, ly ->
          lookStickX += lx
          lookStickY += ly
        },
        onFireDown = {
          isFiring = true
          engine.triggerPlayerFire()
        },
        onFireUp = {
          isFiring = false
        },
        onOpenSettings = onOpenSettings
      )
    } else {
      // 3. Game Over Screen
      GameOverScreen(
        engine = engine,
        onPlayAgain = {
          engine.initializeGame()
        },
        onReturnToLobby = onExitToLobby
      )
    }
  }
}
