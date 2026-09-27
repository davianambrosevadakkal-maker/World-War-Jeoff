package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.GamePreferences
import com.example.game.data.GraphicsPreset
import com.example.game.engine.DisplayCapabilities
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalSurface

@Composable
fun SettingsScreen(
  prefs: GamePreferences,
  displayCapabilities: DisplayCapabilities,
  onApplyRefreshRate: (Int) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var targetFps by remember { mutableIntStateOf(prefs.targetFps) }
  var selectedPreset by remember { mutableStateOf(prefs.graphicsPreset) }
  var fov by remember { mutableFloatStateOf(prefs.fieldOfView) }
  var lookSens by remember { mutableFloatStateOf(prefs.lookSensitivity) }
  var adsSens by remember { mutableFloatStateOf(prefs.adsSensitivity) }
  var haptics by remember { mutableStateOf(prefs.hapticsEnabled) }
  var telemetry by remember { mutableStateOf(prefs.showTelemetry) }
  var sfxVol by remember { mutableFloatStateOf(prefs.sfxVolume) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TacticalDarkBg)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      // Top Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              prefs.targetFps = targetFps
              prefs.graphicsPreset = selectedPreset
              prefs.fieldOfView = fov
              prefs.lookSensitivity = lookSens
              prefs.adsSensitivity = adsSens
              prefs.hapticsEnabled = haptics
              prefs.showTelemetry = telemetry
              prefs.sfxVolume = sfxVol
              onApplyRefreshRate(targetFps)
              onBack()
            },
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(TacticalSurface)
              .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "GRAPHICS & DISPLAY CONFIG",
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.Black
            )
            Text(
              text = "MOTOROLA EDGE 60 FUSION 120 FPS OPTIMIZATION",
              color = TacticalAmber,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Device Detected Badge
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x3306B6D4))
            .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.PhoneAndroid, contentDescription = "Device", tint = TacticalCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (displayCapabilities.isMotorolaEdge) "MOTO EDGE DETECTED" else displayCapabilities.deviceModel.take(20),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        // 1. Target Frame Rate (120 FPS)
        item {
          SettingsCard(title = "FRAME RATE TARGET (REFRESH RATE)", icon = Icons.Default.Speed) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = "Select display refresh rate target. Motorola Edge 60 Fusion pOLED panel supports ultra-smooth 120Hz refresh rate mode with low input latency.",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                listOf(60, 90, 120).forEach { fps ->
                  val isSelected = targetFps == fps
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isSelected) TacticalAmber else Color(0xFF131D31))
                      .border(1.5.dp, if (isSelected) TacticalAmber else Color(0xFF334155), RoundedCornerShape(8.dp))
                      .clickable {
                        targetFps = fps
                        prefs.targetFps = fps
                        onApplyRefreshRate(fps)
                      }
                      .padding(vertical = 12.dp)
                      .testTag("fps_option_$fps"),
                    contentAlignment = Alignment.Center
                  ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                        text = "$fps FPS",
                        color = if (isSelected) TacticalDarkBg else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                      )
                      Text(
                        text = when (fps) {
                          120 -> "MOTO 120Hz MODE"
                          90 -> "SMOOTH"
                          else -> "BALANCED"
                        },
                        color = if (isSelected) TacticalDarkBg else TacticalCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // 2. Graphics Presets
        item {
          SettingsCard(title = "GRAPHICS QUALITY PRESET", icon = Icons.Default.Bolt) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              GraphicsPreset.values().forEach { preset ->
                val isSelected = selectedPreset == preset
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0x33F59E0B) else Color(0xFF131D31))
                    .border(1.dp, if (isSelected) TacticalAmber else Color(0xFF1E293B), RoundedCornerShape(6.dp))
                    .clickable {
                      selectedPreset = preset
                      prefs.graphicsPreset = preset
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(
                      text = preset.displayName,
                      color = if (isSelected) TacticalAmber else Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "Ray density: ${preset.rayCount} slices  •  Max particles: ${preset.maxParticles}",
                      color = Color(0xFF94A3B8),
                      fontSize = 10.sp
                    )
                  }
                  if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = "Active", tint = TacticalAmber, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }

        // 3. Field of View & Controls
        item {
          SettingsCard(title = "CONTROLS & CAMERA FOV", icon = Icons.Default.Tune) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              // FOV Slider
              SliderSetting(
                label = "FIELD OF VIEW (FOV)",
                valueStr = "${fov.toInt()}°",
                value = fov,
                range = 70f..110f,
                onValueChange = {
                  fov = it
                  prefs.fieldOfView = it
                }
              )

              // Look Sensitivity
              SliderSetting(
                label = "HIPFIRE LOOK SENSITIVITY",
                valueStr = String.format("%.1fx", lookSens),
                value = lookSens,
                range = 0.5f..3.0f,
                onValueChange = {
                  lookSens = it
                  prefs.lookSensitivity = it
                }
              )

              // ADS Sensitivity
              SliderSetting(
                label = "ADS OPTIC SENSITIVITY",
                valueStr = String.format("%.1fx", adsSens),
                value = adsSens,
                range = 0.3f..2.0f,
                onValueChange = {
                  adsSens = it
                  prefs.adsSensitivity = it
                }
              )
            }
          }
        }

        // 4. Audio, Haptics & Telemetry
        item {
          SettingsCard(title = "AUDIO, HAPTICS & TELEMETRY", icon = Icons.Default.VolumeUp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              // SFX Volume
              SliderSetting(
                label = "SFX & GUNFIRE VOLUME",
                valueStr = "${(sfxVol * 100).toInt()}%",
                value = sfxVol,
                range = 0f..1f,
                onValueChange = {
                  sfxVol = it
                  prefs.sfxVolume = it
                }
              )

              // Haptic Recoil Toggle
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(text = "HAPTIC RECOIL FEEDBACK", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text(text = "Vibrate device on weapon fire and taking damage", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
                Switch(
                  checked = haptics,
                  onCheckedChange = {
                    haptics = it
                    prefs.hapticsEnabled = it
                  },
                  colors = SwitchDefaults.colors(checkedThumbColor = TacticalAmber, checkedTrackColor = Color(0x66F59E0B))
                )
              }

              // Telemetry Toggle
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(text = "LIVE 120 FPS TELEMETRY HUD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text(text = "Display real-time FPS, frame time (ms) and Moto high refresh mode", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
                Switch(
                  checked = telemetry,
                  onCheckedChange = {
                    telemetry = it
                    prefs.showTelemetry = it
                  },
                  colors = SwitchDefaults.colors(checkedThumbColor = TacticalAmber, checkedTrackColor = Color(0x66F59E0B))
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: @Composable () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = TacticalSurface),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = title, tint = TacticalCyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun SliderSetting(
  label: String,
  valueStr: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      Text(text = valueStr, color = TacticalAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = range,
      colors = SliderDefaults.colors(
        thumbColor = TacticalAmber,
        activeTrackColor = TacticalAmber,
        inactiveTrackColor = Color(0xFF334155)
      )
    )
  }
}
