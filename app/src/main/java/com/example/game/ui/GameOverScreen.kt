package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBlue
import com.example.ui.theme.TacticalCrimson
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalSurface

@Composable
fun GameOverScreen(
  engine: GameEngine,
  onPlayAgain: () -> Unit,
  onReturnToLobby: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isVictory = engine.isVictory

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TacticalDarkBg),
    contentAlignment = Alignment.Center
  ) {
    // Background gradient glow
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = if (isVictory) {
              listOf(Color(0x33F59E0B), Color(0xFF080C14))
            } else {
              listOf(Color(0x33EF4444), Color(0xFF080C14))
            },
            radius = 700f
          )
        )
    )

    Card(
      modifier = Modifier
        .width(520.dp)
        .padding(16.dp),
      colors = CardDefaults.cardColors(containerColor = TacticalSurface),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isVictory) TacticalGold else TacticalCrimson)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Banner Header: VICTORY / DEFEAT
        Text(
          text = if (isVictory) "WARZONE VICTORY" else "MISSION DEFEAT",
          color = if (isVictory) TacticalGold else TacticalCrimson,
          fontSize = 28.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontFamily = FontFamily.SansSerif
        )

        Text(
          text = if (isVictory) "TARGET SCORE REACHED  •  CRIMSON CELL ELIMINATED" else "CRIMSON CELL SECURED VICTORY",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Match Score Display
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B0F19))
            .padding(horizontal = 24.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "ALLIES ", color = TacticalBlue, fontSize = 14.sp, fontWeight = FontWeight.Black)
          Text(text = "${engine.blueTeamScore}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
          Text(text = "  -  ", color = Color(0xFF64748B), fontSize = 18.sp)
          Text(text = "${engine.redTeamScore}", color = TacticalCrimson, fontSize = 24.sp, fontWeight = FontWeight.Black)
          Text(text = " CRIMSON", color = TacticalCrimson, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Personal Combat Record Breakdown
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          StatBox("KILLS", "${engine.playerKills}", TacticalAmber)
          StatBox("DEATHS", "${engine.playerDeaths}", Color(0xFF94A3B8))
          StatBox("K/D RATIO", calculateKd(engine.playerKills, engine.playerDeaths), TacticalGold)
          StatBox("HEADSHOTS", "${engine.playerHeadshots}", Color(0xFFEF4444))
          StatBox("BEST STREAK", "${engine.currentStreak}", TacticalBlue)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          OutlinedButton(
            onClick = onReturnToLobby,
            modifier = Modifier.weight(1f).height(46.dp).testTag("lobby_button"),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
          ) {
            Icon(Icons.Default.ViewList, contentDescription = "Lobby", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "LOBBY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          Button(
            onClick = onPlayAgain,
            modifier = Modifier.weight(1f).height(46.dp).testTag("play_again_button"),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber)
          ) {
            Icon(Icons.Default.Replay, contentDescription = "Play Again", tint = TacticalDarkBg, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "PLAY AGAIN", color = TacticalDarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF0F172A))
      .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(text = label, color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
  }
}

private fun calculateKd(kills: Int, deaths: Int): String {
  if (deaths == 0) return if (kills == 0) "1.00" else "$kills.00"
  return String.format("%.2f", kills.toFloat() / deaths.toFloat())
}
