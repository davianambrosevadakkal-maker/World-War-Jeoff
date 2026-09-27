package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.game.weapons.WeaponArsenal
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBlue
import com.example.ui.theme.TacticalCrimson
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GameHudOverlay(
  engine: GameEngine,
  onMoveInput: (Float, Float) -> Unit,
  onLookInput: (Float, Float) -> Unit,
  onFireDown: () -> Unit,
  onFireUp: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxSize()) {

    // 1. Damage Blood Screen Vignette
    if (engine.playerHealth < 100) {
      val bloodAlpha = ((100 - engine.playerHealth) / 100f * 0.75f).coerceIn(0f, 0.75f)
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(Color.Transparent, Color(0xFF991B1B).copy(alpha = bloodAlpha)),
              radius = 800f
            )
          )
      )
    }

    // 2. Top Bar: Minimap, Score Header, Telemetry & Killfeed
    TopHudSection(
      engine = engine,
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    )

    // 3. Center Screen: Dynamic Reticle, Hitmarker, XP Popups
    CenterReticleSection(
      engine = engine,
      modifier = Modifier.align(Alignment.Center)
    )

    // 4. Killstreak Deploy Tray (Right Center)
    KillstreakTray(
      engine = engine,
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 16.dp)
    )

    // 5. Bottom Left: Virtual Movement Stick, Sprint & Slide Buttons
    MovementControlsSection(
      engine = engine,
      onMoveInput = onMoveInput,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 24.dp, bottom = 24.dp)
    )

    // 6. Bottom Right: Aim Look Swipe Zone, Fire Button, ADS, Reload, Weapon HUD
    CombatControlsSection(
      engine = engine,
      onLookInput = onLookInput,
      onFireDown = onFireDown,
      onFireUp = onFireUp,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 24.dp, bottom = 24.dp)
    )
  }
}

@Composable
private fun TopHudSection(
  engine: GameEngine,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    // Left: Tactical Minimap Radar
    MinimapRadar(engine = engine)

    // Center: Team Score & 120 FPS Telemetry
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Scoreboard Banner (TDM)
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xCC0F172A))
          .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
          .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Blue Team (Player)
        Text(
          text = "ALLIES",
          color = TacticalBlue,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = " ${engine.blueTeamScore} ",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Black
        )
        Text(
          text = " - ",
          color = Color(0xFF64748B),
          fontSize = 14.sp
        )
        Text(
          text = " ${engine.redTeamScore} ",
          color = TacticalCrimson,
          fontSize = 18.sp,
          fontWeight = FontWeight.Black
        )
        Text(
          text = "CRIMSON",
          color = TacticalCrimson,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Match Timer
      val mins = (engine.matchTimeRemaining / 60).toInt()
      val secs = (engine.matchTimeRemaining % 60).toInt()
      Text(
        text = String.format("%02d:%02d", mins, secs),
        color = Color(0xFF94A3B8),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace
      )

      // Live 120 FPS Motorola Telemetry
      if (engine.prefs.showTelemetry) {
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xAA064E3B))
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "FPS",
            tint = Color(0xFF34D399),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${engine.fpsDisplay} FPS [MOTO 120Hz HIGH REFRESH]",
            color = Color(0xFF6EE7B7),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // Right: Live Killfeed Cards
    Column(
      horizontalAlignment = Alignment.End,
      modifier = Modifier.width(220.dp)
    ) {
      engine.killfeed.take(3).forEach { feed ->
        Row(
          modifier = Modifier
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xAA0F172A))
            .padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = feed.killerName,
            color = if (feed.isPlayerKiller) TacticalCyan else TacticalBlue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = " [${feed.weaponName.take(10)}] ",
            color = if (feed.isHeadshot) TacticalGold else Color(0xFF94A3B8),
            fontSize = 9.sp
          )
          Text(
            text = feed.victimName,
            color = if (feed.isPlayerVictim) TacticalCrimson else Color(0xFFEF4444),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun MinimapRadar(
  engine: GameEngine,
  modifier: Modifier = Modifier
) {
  val radarSize = 88.dp
  Box(
    modifier = modifier
      .size(radarSize)
      .clip(CircleShape)
      .background(Color(0xCC090D16))
      .border(1.5.dp, Color(0xFF0284C7), CircleShape)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.width / 2f

      // Radar Concentric rings
      drawCircle(Color(0x2206B6D4), radius = radius * 0.7f, style = Stroke(1f))
      drawCircle(Color(0x2206B6D4), radius = radius * 0.35f, style = Stroke(1f))
      drawLine(Color(0x2206B6D4), Offset(center.x, 0f), Offset(center.x, size.height), 1f)
      drawLine(Color(0x2206B6D4), Offset(0f, center.y), Offset(size.width, center.y), 1f)

      // UAV Sweep line
      if (engine.uavActiveTimer > 0f) {
        val sweepAngle = (System.currentTimeMillis() % 2000L) / 2000f * 2 * PI
        val sweepX = center.x + cos(sweepAngle).toFloat() * radius
        val sweepY = center.y + sin(sweepAngle).toFloat() * radius
        drawLine(Color(0x8806B6D4), center, Offset(sweepX, sweepY), 2f)
      }

      // Enemy Bot Blips on Radar
      val scale = radius / 12f // Map radius scale
      for (bot in engine.bots) {
        if (!bot.isAlive) continue
        val dx = bot.x - engine.playerX
        val dy = bot.y - engine.playerY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 11f && engine.uavActiveTimer <= 0f) continue

        // Rotate relative to player look angle
        val relAngle = kotlin.math.atan2(dy, dx) - engine.playerAngle + (PI / 2).toFloat()
        val blipX = center.x + cos(relAngle) * (dist * scale).coerceAtMost(radius - 6f)
        val blipY = center.y + sin(relAngle) * (dist * scale).coerceAtMost(radius - 6f)

        val blipColor = if (bot.isEnemy) Color(0xFFEF4444) else Color(0xFF3B82F6)
        drawCircle(blipColor, radius = 3.5f, center = Offset(blipX, blipY))
      }

      // Player Arrow at Center
      val playerArrow = Path().apply {
        moveTo(center.x, center.y - 7f)
        lineTo(center.x + 5f, center.y + 6f)
        lineTo(center.x, center.y + 3f)
        lineTo(center.x - 5f, center.y + 6f)
        close()
      }
      drawPath(playerArrow, Color(0xFF06B6D4))
    }
  }
}

@Composable
private fun CenterReticleSection(
  engine: GameEngine,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // XP Popups Floating (+100 ELIMINATION)
    engine.xpPopups.take(2).forEach { xp ->
      Text(
        text = xp.text,
        color = TacticalGold,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 4.dp)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Reticle & Hitmarker
    Box(
      modifier = Modifier.size(60.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Dynamic Hipfire Crosshair (hidden during full ADS)
        if (engine.adsProgress < 0.65f) {
          val spread = (10f + (if (engine.isSprinting) 18f else 0f)) * (1f - engine.adsProgress)
          val lineLen = 9f
          val reticleColor = Color(0xCCFFFFFF)

          // 4 Ticks
          drawLine(reticleColor, Offset(center.x - spread - lineLen, center.y), Offset(center.x - spread, center.y), 2f)
          drawLine(reticleColor, Offset(center.x + spread, center.y), Offset(center.x + spread + lineLen, center.y), 2f)
          drawLine(reticleColor, Offset(center.x, center.y - spread - lineLen), Offset(center.x, center.y - spread), 2f)
          drawLine(reticleColor, Offset(center.x, center.y + spread), Offset(center.x, center.y + spread + lineLen), 2f)
        }

        // 2. Hitmarker (Body Shot = White X, Headshot = Red X with flare)
        engine.activeHitmarker?.let { hm ->
          val hitColor = if (hm.isHeadshot) Color(0xFFFF2222) else Color(0xFFFFFFFF)
          val stroke = if (hm.isHeadshot) 3.5f else 2.5f
          val size = if (hm.isHeadshot) 14f else 10f

          drawLine(hitColor, Offset(center.x - size, center.y - size), Offset(center.x + size, center.y + size), stroke)
          drawLine(hitColor, Offset(center.x + size, center.y - size), Offset(center.x - size, center.y + size), stroke)
        }
      }
    }
  }
}

@Composable
private fun KillstreakTray(
  engine: GameEngine,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 3 Kills: UAV Recon
    StreakButton(
      icon = Icons.Default.Radar,
      label = "UAV (3)",
      isReady = engine.currentStreak >= 3,
      onClick = { engine.activateKillstreak(3) }
    )

    // 5 Kills: Cruise Missile
    StreakButton(
      icon = Icons.Default.Flight,
      label = "MISSILE (5)",
      isReady = engine.currentStreak >= 5,
      onClick = { engine.activateKillstreak(5) }
    )

    // 7 Kills: Overwatch Chopper
    StreakButton(
      icon = Icons.Default.Security,
      label = "CHOPPER (7)",
      isReady = engine.currentStreak >= 7,
      onClick = { engine.activateKillstreak(7) }
    )
  }
}

@Composable
private fun StreakButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  isReady: Boolean,
  onClick: () -> Unit
) {
  IconButton(
    onClick = onClick,
    enabled = isReady,
    modifier = Modifier
      .size(44.dp)
      .clip(CircleShape)
      .background(if (isReady) Color(0xEEF59E0B) else Color(0x661E293B))
      .border(1.dp, if (isReady) TacticalGold else Color(0xFF334155), CircleShape)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (isReady) TacticalDarkBg else Color(0xFF64748B),
      modifier = Modifier.size(22.dp)
    )
  }
}

@Composable
private fun MovementControlsSection(
  engine: GameEngine,
  onMoveInput: (Float, Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var thumbOffsetX by remember { mutableFloatStateOf(0f) }
  var thumbOffsetY by remember { mutableFloatStateOf(0f) }
  val stickRadius = 65f

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 360 Virtual Joystick Base
    Box(
      modifier = Modifier
        .size(130.dp)
        .clip(CircleShape)
        .background(Color(0x550F172A))
        .border(1.5.dp, Color(0x8838BDF8), CircleShape)
        .pointerInput(Unit) {
          detectDragGestures(
            onDragEnd = {
              thumbOffsetX = 0f
              thumbOffsetY = 0f
              onMoveInput(0f, 0f)
            },
            onDragCancel = {
              thumbOffsetX = 0f
              thumbOffsetY = 0f
              onMoveInput(0f, 0f)
            }
          ) { change, dragAmount ->
            change.consume()
            val newX = thumbOffsetX + dragAmount.x
            val newY = thumbOffsetY + dragAmount.y
            val dist = sqrt(newX * newX + newY * newY)
            if (dist > stickRadius) {
              thumbOffsetX = (newX / dist) * stickRadius
              thumbOffsetY = (newY / dist) * stickRadius
            } else {
              thumbOffsetX = newX
              thumbOffsetY = newY
            }
            onMoveInput(thumbOffsetX / stickRadius, thumbOffsetY / stickRadius)
          }
        },
      contentAlignment = Alignment.Center
    ) {
      // Joystick Thumb Knob
      Box(
        modifier = Modifier
          .offset { IntOffset(thumbOffsetX.toInt(), thumbOffsetY.toInt()) }
          .size(54.dp)
          .clip(CircleShape)
          .background(Color(0xCC0284C7))
          .border(2.dp, Color(0xFF38BDF8), CircleShape)
      )
    }

    // Tactical Actions: Sprint Toggle & Tactical Slide
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      // Tactical Sprint
      IconButton(
        onClick = { engine.toggleSprint() },
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(if (engine.isSprinting) TacticalAmber else Color(0x880F172A))
          .border(1.dp, Color(0xFF64748B), CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.DirectionsRun,
          contentDescription = "Sprint",
          tint = if (engine.isSprinting) TacticalDarkBg else Color.White
        )
      }

      // Tactical Slide
      IconButton(
        onClick = { engine.triggerTacticalSlide() },
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(if (engine.isSliding) TacticalCyan else Color(0x880F172A))
          .border(1.dp, Color(0xFF64748B), CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.FastForward,
          contentDescription = "Slide",
          tint = if (engine.isSliding) TacticalDarkBg else Color.White
        )
      }
    }
  }
}

@Composable
private fun CombatControlsSection(
  engine: GameEngine,
  onLookInput: (Float, Float) -> Unit,
  onFireDown: () -> Unit,
  onFireUp: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.End
  ) {
    // Health & Armor Bar
    Row(
      modifier = Modifier
        .padding(bottom = 8.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xCC0F172A))
        .padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Health
      Text(text = "HP", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.width(4.dp))
      Box(
        modifier = Modifier
          .width(80.dp)
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0xFF334155))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(engine.playerHealth / 100f)
            .height(8.dp)
            .background(if (engine.playerHealth > 40) Color(0xFF22C55E) else Color(0xFFEF4444))
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Armor
      Text(text = "ARMOR", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.width(4.dp))
      Box(
        modifier = Modifier
          .width(50.dp)
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0xFF334155))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth((engine.playerArmor / 50f).coerceIn(0f, 1f))
            .height(8.dp)
            .background(Color(0xFF38BDF8))
        )
      }
    }

    // Equipment & Tactical Actions Row (Grenade, Stim, Weapon Switch)
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      // Frag Grenade
      IconButton(
        onClick = { engine.throwGrenade() },
        enabled = engine.grenadesRemaining > 0,
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xAA0F172A))
          .border(1.dp, Color(0xFF475569), CircleShape)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Bolt, contentDescription = "Grenade", tint = TacticalAmber, modifier = Modifier.size(20.dp))
          Text(text = "${engine.grenadesRemaining}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Stim Shot
      IconButton(
        onClick = { engine.useStimShot() },
        enabled = engine.stimsRemaining > 0,
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xAA0F172A))
          .border(1.dp, Color(0xFF475569), CircleShape)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Healing, contentDescription = "Stim", tint = TacticalGreen, modifier = Modifier.size(20.dp))
          Text(text = "${engine.stimsRemaining}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Weapon Cycle Button
      IconButton(
        onClick = {
          val currentIndex = WeaponArsenal.ALL_WEAPONS.indexOfFirst { it.id == engine.currentWeapon.id }
          val nextIndex = (currentIndex + 1) % WeaponArsenal.ALL_WEAPONS.size
          engine.switchWeapon(WeaponArsenal.ALL_WEAPONS[nextIndex].id)
        },
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xAA0F172A))
          .border(1.dp, Color(0xFF475569), CircleShape)
      ) {
        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Weapon", tint = TacticalCyan, modifier = Modifier.size(22.dp))
      }
    }

    // Ammo Counter & Weapon Name
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xCC0F172A))
        .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = engine.currentWeapon.name,
          color = TacticalAmber,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = engine.currentWeapon.caliber,
          color = Color(0xFF64748B),
          fontSize = 9.sp
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Text(
        text = "${engine.currentAmmo}",
        color = if (engine.currentAmmo <= 5) TacticalCrimson else Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = " / ${engine.reserveAmmo}",
        color = Color(0xFF94A3B8),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )

      Spacer(modifier = Modifier.width(10.dp))

      // Tactical Reload Button
      IconButton(
        onClick = { engine.startReload() },
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Color(0x55334155))
      ) {
        Icon(
          imageVector = Icons.Default.Autorenew,
          contentDescription = "Reload",
          tint = Color.White,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Primary Combat Actions: Look Touch Area, ADS & Primary Fire
    Row(
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // ADS Button (Aim Down Sights)
      IconButton(
        onClick = { engine.toggleAds() },
        modifier = Modifier
          .size(62.dp)
          .clip(CircleShape)
          .background(if (engine.isAimingDownSights) TacticalCyan else Color(0x990F172A))
          .border(2.dp, if (engine.isAimingDownSights) TacticalCyan else Color(0xFF475569), CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.ZoomIn,
          contentDescription = "ADS",
          tint = if (engine.isAimingDownSights) TacticalDarkBg else Color.White,
          modifier = Modifier.size(32.dp)
        )
      }

      // Primary Fire Trigger Button
      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
            )
          )
          .border(2.5.dp, Color(0xFFFCA5A5), CircleShape)
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = { onFireDown() },
              onDragEnd = { onFireUp() },
              onDragCancel = { onFireUp() }
            ) { _, dragAmount ->
              // Also allow aim adjustment while holding trigger!
              onLookInput(dragAmount.x * 0.04f, dragAmount.y * 0.04f)
            }
          },
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "FIRE",
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Black
        )
      }
    }
  }
}
