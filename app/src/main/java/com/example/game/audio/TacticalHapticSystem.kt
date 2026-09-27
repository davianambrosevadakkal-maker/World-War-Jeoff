package com.example.game.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class TacticalHapticSystem(context: Context) {
  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  var isHapticsEnabled = true

  fun triggerRecoil(intensity: Float = 0.5f) {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val amp = (intensity * 255).toInt().coerceIn(40, 255)
        val effect = VibrationEffect.createOneShot(25L, amp)
        vibrator.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(25L)
      }
    } catch (_: Exception) {}
  }

  fun triggerDamage() {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 45, 30, 60), intArrayOf(0, 180, 0, 240), -1)
        vibrator.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(80L)
      }
    } catch (_: Exception) {}
  }

  fun triggerExplosion() {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createOneShot(180L, 255)
        vibrator.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(180L)
      }
    } catch (_: Exception) {}
  }
}
