package com.example.game.engine

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager

data class DisplayCapabilities(
  val deviceModel: String,
  val isMotorolaEdge: Boolean,
  val currentRefreshRate: Float,
  val maxRefreshRate: Float,
  val supportedRefreshRates: List<Int>,
  val is120HzSupported: Boolean,
  val is144HzSupported: Boolean
)

object DisplayManagerHelper {
  private const val TAG = "DisplayManagerHelper"

  fun getDisplayCapabilities(activity: Activity): DisplayCapabilities {
    val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      activity.display
    } else {
      @Suppress("DEPRECATION")
      activity.windowManager.defaultDisplay
    }

    val currentRate = display?.refreshRate ?: 60f
    val supportedRates = mutableSetOf<Int>()
    var maxRate = currentRate

    val modes = display?.supportedModes ?: emptyArray()
    for (mode in modes) {
      val rateInt = mode.refreshRate.toInt()
      supportedRates.add(rateInt)
      if (mode.refreshRate > maxRate) {
        maxRate = mode.refreshRate
      }
    }

    // Default to at least 60
    if (supportedRates.isEmpty()) {
      supportedRates.add(60)
    }

    val manufacturer = Build.MANUFACTURER.lowercase()
    val model = Build.MODEL.lowercase()
    val isMoto = manufacturer.contains("motorola") || model.contains("edge") || model.contains("moto")
    val fullDeviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

    val has120 = supportedRates.any { it in 118..125 } || maxRate >= 118f
    val has144 = supportedRates.any { it in 140..148 } || maxRate >= 140f

    // Even on emulators, allow 120 FPS emulation if configured
    val sortedRates = (supportedRates + listOf(60, 90, 120)).sorted().distinct()

    return DisplayCapabilities(
      deviceModel = fullDeviceName,
      isMotorolaEdge = isMoto,
      currentRefreshRate = currentRate,
      maxRefreshRate = maxRate,
      supportedRefreshRates = sortedRates,
      is120HzSupported = has120 || true, // Allow 120 FPS target selection everywhere
      is144HzSupported = has144
    )
  }

  fun applyTargetRefreshRate(activity: Activity, targetFps: Int) {
    try {
      val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.display
      } else {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay
      }

      val modes = display?.supportedModes ?: emptyArray()
      var targetMode: Display.Mode? = null

      // Find closest mode that matches or exceeds targetFps
      for (mode in modes) {
        val rate = mode.refreshRate.toInt()
        if (rate == targetFps || (targetFps == 120 && rate in 118..125) || (targetFps == 90 && rate in 88..92)) {
          targetMode = mode
          break
        }
      }

      // If exact not found, find closest
      if (targetMode == null && modes.isNotEmpty()) {
        targetMode = modes.minByOrNull { kotlin.math.abs(it.refreshRate - targetFps) }
      }

      val window = activity.window
      val params = window.attributes

      if (targetMode != null) {
        params.preferredDisplayModeId = targetMode.modeId
        Log.i(TAG, "Applied preferredDisplayModeId=${targetMode.modeId} for ${targetMode.refreshRate}Hz")
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && targetFps > 0) {
        // Hint to Android compositor for high frame rate
        try {
          params.preferredRefreshRate = targetFps.toFloat()
        } catch (e: Exception) {
          Log.w(TAG, "Could not set preferredRefreshRate: ${e.message}")
        }
      }

      window.attributes = params
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } catch (e: Exception) {
      Log.e(TAG, "Error applying refresh rate: ${e.message}")
    }
  }
}
