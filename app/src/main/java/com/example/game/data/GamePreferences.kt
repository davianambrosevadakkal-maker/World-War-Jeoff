package com.example.game.data

import android.content.Context
import android.content.SharedPreferences

enum class GraphicsPreset(val displayName: String, val rayCount: Int, val maxParticles: Int, val dynamicLighting: Boolean) {
  LOW("Performance (60 FPS)", 160, 60, false),
  MEDIUM("Balanced", 220, 120, true),
  HIGH("High Quality", 300, 200, true),
  ULTRA("Ultra (120 FPS Ready)", 380, 300, true),
  INSANE("Extreme Tactical (120 FPS)", 460, 450, true)
}

class GamePreferences(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("vanguard_fps_prefs", Context.MODE_PRIVATE)

  var targetFps: Int
    get() = prefs.getInt("target_fps", 120) // Default to 120 FPS as requested!
    set(value) = prefs.edit().putInt("target_fps", value).apply()

  var graphicsPreset: GraphicsPreset
    get() {
      val name = prefs.getString("graphics_preset", GraphicsPreset.ULTRA.name) ?: GraphicsPreset.ULTRA.name
      return try { GraphicsPreset.valueOf(name) } catch (_: Exception) { GraphicsPreset.ULTRA }
    }
    set(value) = prefs.edit().putString("graphics_preset", value.name).apply()

  var fieldOfView: Float
    get() = prefs.getFloat("fov", 85f)
    set(value) = prefs.edit().putFloat("fov", value).apply()

  var lookSensitivity: Float
    get() = prefs.getFloat("sensitivity_look", 1.2f)
    set(value) = prefs.edit().putFloat("sensitivity_look", value).apply()

  var adsSensitivity: Float
    get() = prefs.getFloat("sensitivity_ads", 0.8f)
    set(value) = prefs.edit().putFloat("sensitivity_ads", value).apply()

  var showTelemetry: Boolean
    get() = prefs.getBoolean("show_telemetry", true)
    set(value) = prefs.edit().putBoolean("show_telemetry", value).apply()

  var hapticsEnabled: Boolean
    get() = prefs.getBoolean("haptics_enabled", true)
    set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

  var sfxVolume: Float
    get() = prefs.getFloat("sfx_volume", 0.9f)
    set(value) = prefs.edit().putFloat("sfx_volume", value).apply()

  var selectedWeaponId: String
    get() = prefs.getString("selected_weapon", "kronos_74") ?: "kronos_74"
    set(value) = prefs.edit().putString("selected_weapon", value).apply()

  // Player Career Stats
  var playerLevel: Int
    get() = prefs.getInt("player_level", 1)
    set(value) = prefs.edit().putInt("player_level", value).apply()

  var playerXp: Int
    get() = prefs.getInt("player_xp", 0)
    set(value) = prefs.edit().putInt("player_xp", value).apply()

  var totalKills: Int
    get() = prefs.getInt("total_kills", 0)
    set(value) = prefs.edit().putInt("total_kills", value).apply()

  var totalDeaths: Int
    get() = prefs.getInt("total_deaths", 0)
    set(value) = prefs.edit().putInt("total_deaths", value).apply()

  var totalHeadshots: Int
    get() = prefs.getInt("total_headshots", 0)
    set(value) = prefs.edit().putInt("total_headshots", value).apply()

  var bestStreak: Int
    get() = prefs.getInt("best_streak", 0)
    set(value) = prefs.edit().putInt("best_streak", value).apply()

  var matchesWon: Int
    get() = prefs.getInt("matches_won", 0)
    set(value) = prefs.edit().putInt("matches_won", value).apply()

  fun addXp(amount: Int): Boolean {
    val current = playerXp + amount
    var level = playerLevel
    val xpForNext = level * 1000
    var leveledUp = false
    if (current >= xpForNext) {
      level++
      leveledUp = true
    }
    prefs.edit()
      .putInt("player_xp", current)
      .putInt("player_level", level)
      .apply()
    return leveledUp
  }
}
