package com.example.game.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.data.GamePreferences
import com.example.game.engine.DisplayCapabilities
import com.example.game.weapons.WeaponArsenal
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBlue
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MainMenuScreen(
  prefs: GamePreferences,
  displayCapabilities: DisplayCapabilities,
  onStartMatch: () -> Unit,
  onOpenGunsmith: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val activeWeapon = WeaponArsenal.getWeaponById(prefs.selectedWeaponId)

  // Animated background scanner line
  var scanAngle by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    while (true) {
      scanAngle = (scanAngle + 0.03f) % (2 * PI).toFloat()
      delay(16)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TacticalDarkBg)
  ) {
    // Military Carbon Tactical Grid Background
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Dark radial glow
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFF1E293B), Color(0xFF080C14)),
          center = Offset(w * 0.5f, h * 0.5f),
          radius = w * 0.8f
        )
      )

      // Tactical Grid Lines
      val step = 45f
      var x = 0f
      while (x < w) {
        drawLine(Color(0x1538BDF8), Offset(x, 0f), Offset(x, h), 1f)
        x += step
      }
      var y = 0f
      while (y < h) {
        drawLine(Color(0x1538BDF8), Offset(0f, y), Offset(w, y), 1f)
        y += step
      }
    }

    // Top Header: Title & Player Rank Card
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 28.dp, vertical = 20.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Game Brand Title
      Column {
        Text(
          text = "VANGUARD OPS",
          color = Color.White,
          fontSize = 28.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontFamily = FontFamily.SansSerif
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "WARZONE TACTICAL FPS",
            color = TacticalAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
          )
          Spacer(modifier = Modifier.width(12.dp))
          // 120 FPS Badge
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0x44059669))
              .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Bolt, contentDescription = "120 FPS", tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = if (displayCapabilities.isMotorolaEdge) "MOTO 120Hz MODE" else "120 FPS ACTIVE",
              color = Color(0xFF6EE7B7),
              fontSize = 9.sp,
              fontWeight = FontWeight.Black
            )
          }
        }
      }

      // Player Career Card
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(TacticalSurface)
          .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.MilitaryTech,
          contentDescription = "Rank",
          tint = TacticalGold,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "SPEC-OPS PRESTIGE",
            color = TacticalGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
          )
          Text(
            text = "LVL ${prefs.playerLevel}  •  K/D: ${calculateKd(prefs.totalKills, prefs.totalDeaths)}",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // Main Content: Left Action Cards & Right 3D Weapon Showcase
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 28.dp)
        .padding(top = 80.dp, bottom = 24.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Action Menu Column
      Column(
        modifier = Modifier.width(300.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. Primary Action: DEPLOY TO WARZONE
        Button(
          onClick = onStartMatch,
          modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("deploy_button"),
          colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Deploy", tint = TacticalDarkBg, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "DEPLOY: TDM WARZONE",
              color = TacticalDarkBg,
              fontSize = 16.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp
            )
          }
        }

        // 2. GUNSMITH: Arsenal Lab
        MenuCard(
          icon = Icons.Default.Security,
          title = "GUNSMITH ARSENAL",
          subtitle = "${activeWeapon.name} [CUSTOMIZED]",
          badge = "5 WEAPONS",
          onClick = onOpenGunsmith
        )

        // 3. SETTINGS & 120 FPS
        MenuCard(
          icon = Icons.Default.Speed,
          title = "120 FPS & GRAPHICS",
          subtitle = "${prefs.targetFps} FPS Target • ${prefs.graphicsPreset.displayName}",
          badge = if (displayCapabilities.isMotorolaEdge) "MOTO READY" else "HIGH REFRESH",
          onClick = onOpenSettings
        )
      }

      // Right: Active Weapon Showcase
      Card(
        modifier = Modifier
          .weight(1f)
          .padding(start = 24.dp)
          .height(230.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column {
              Text(
                text = activeWeapon.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
              )
              Text(
                text = "${activeWeapon.codeName}  •  ${activeWeapon.caliber}",
                color = TacticalAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // Camo Tag
            Text(
              text = activeWeapon.selectedCamo.displayName,
              color = TacticalCyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x3306B6D4))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          // Weapon Lore Quote
          Text(
            text = activeWeapon.lore,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            lineHeight = 16.sp
          )

          // Key Stats Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            StatItem(label = "DAMAGE", value = "${activeWeapon.baseDamage}")
            StatItem(label = "FIRE RATE", value = "${activeWeapon.fireRateRpm} RPM")
            StatItem(label = "MAGAZINE", value = "${activeWeapon.currentMagCapacity} RNDS")
            StatItem(label = "OPTIC", value = activeWeapon.selectedOptic.displayName)
          }
        }
      }
    }
  }
}

@Composable
private fun MenuCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  badge: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(TacticalSurface)
      .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(imageVector = icon, contentDescription = title, tint = TacticalCyan, modifier = Modifier.size(24.dp))
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = badge,
          color = TacticalAmber,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0x33F59E0B))
            .padding(horizontal = 4.dp, vertical = 1.dp)
        )
      }
      Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 10.sp)
    }
  }
}

@Composable
private fun StatItem(label: String, value: String) {
  Column {
    Text(text = label, color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
  }
}

private fun calculateKd(kills: Int, deaths: Int): String {
  if (deaths == 0) return if (kills == 0) "1.00" else "$kills.00"
  return String.format("%.2f", kills.toFloat() / deaths.toFloat())
}
