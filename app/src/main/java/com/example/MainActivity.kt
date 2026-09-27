package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.game.audio.TacticalAudioSystem
import com.example.game.audio.TacticalHapticSystem
import com.example.game.data.GamePreferences
import com.example.game.engine.DisplayCapabilities
import com.example.game.engine.DisplayManagerHelper
import com.example.game.engine.GameEngine
import com.example.game.ui.GamePlayScreen
import com.example.game.ui.GunsmithScreen
import com.example.game.ui.MainMenuScreen
import com.example.game.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalDarkBg

enum class AppScreen {
  LOBBY,
  GAMEPLAY,
  GUNSMITH,
  SETTINGS
}

class MainActivity : ComponentActivity() {
  private lateinit var audioSystem: TacticalAudioSystem
  private lateinit var hapticSystem: TacticalHapticSystem
  private lateinit var preferences: GamePreferences
  private lateinit var displayCapabilities: DisplayCapabilities
  private lateinit var gameEngine: GameEngine

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // 1. Fullscreen Immersive Mode for true console/mobile FPS feel
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

    // 2. Initialize Game Services & Systems
    preferences = GamePreferences(this)
    audioSystem = TacticalAudioSystem()
    hapticSystem = TacticalHapticSystem(this)
    displayCapabilities = DisplayManagerHelper.getDisplayCapabilities(this)

    // 3. Apply 120 FPS high refresh mode (specifically tuned for Motorola Edge 60 Fusion)
    DisplayManagerHelper.applyTargetRefreshRate(this, preferences.targetFps)

    gameEngine = GameEngine(
      context = this,
      prefs = preferences,
      audio = audioSystem,
      haptics = hapticSystem
    )

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = TacticalDarkBg
        ) {
          VanguardAppRoot(
            engine = gameEngine,
            prefs = preferences,
            displayCapabilities = displayCapabilities,
            onApplyRefreshRate = { fps ->
              DisplayManagerHelper.applyTargetRefreshRate(this, fps)
            }
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
  }

  override fun onDestroy() {
    super.onDestroy()
    audioSystem.release()
  }
}

@Composable
fun VanguardAppRoot(
  engine: GameEngine,
  prefs: GamePreferences,
  displayCapabilities: DisplayCapabilities,
  onApplyRefreshRate: (Int) -> Unit
) {
  var currentScreen by remember { mutableStateOf(AppScreen.LOBBY) }

  // Back Navigation Handling
  BackHandler(enabled = currentScreen != AppScreen.LOBBY) {
    currentScreen = AppScreen.LOBBY
  }

  when (currentScreen) {
    AppScreen.LOBBY -> {
      MainMenuScreen(
        prefs = prefs,
        displayCapabilities = displayCapabilities,
        onStartMatch = {
          engine.initializeGame()
          currentScreen = AppScreen.GAMEPLAY
        },
        onOpenGunsmith = {
          currentScreen = AppScreen.GUNSMITH
        },
        onOpenSettings = {
          currentScreen = AppScreen.SETTINGS
        }
      )
    }

    AppScreen.GAMEPLAY -> {
      GamePlayScreen(
        engine = engine,
        onExitToLobby = {
          currentScreen = AppScreen.LOBBY
        },
        onOpenSettings = {
          currentScreen = AppScreen.SETTINGS
        }
      )
    }

    AppScreen.GUNSMITH -> {
      GunsmithScreen(
        prefs = prefs,
        onBack = {
          currentScreen = AppScreen.LOBBY
        }
      )
    }

    AppScreen.SETTINGS -> {
      SettingsScreen(
        prefs = prefs,
        displayCapabilities = displayCapabilities,
        onApplyRefreshRate = onApplyRefreshRate,
        onBack = {
          currentScreen = AppScreen.LOBBY
        }
      )
    }
  }
}
