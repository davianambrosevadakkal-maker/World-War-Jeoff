package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.game.weapons.MagazineType
import com.example.game.weapons.MuzzleType
import com.example.game.weapons.OpticType
import com.example.game.weapons.Weapon
import com.example.game.weapons.WeaponArsenal
import com.example.game.weapons.WeaponCamo
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalDarkBg
import com.example.ui.theme.TacticalGold
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant

@Composable
fun GunsmithScreen(
  prefs: GamePreferences,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedWeapon by remember {
    mutableStateOf(WeaponArsenal.getWeaponById(prefs.selectedWeaponId))
  }

  var selectedTab by remember { mutableIntStateOf(0) }
  val tabTitles = listOf("OPTICS", "MUZZLE", "MAGAZINE", "CAMOS")

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
      // Top Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBack,
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
              text = "GUNSMITH WEAPON LAB",
              color = Color.White,
              fontSize = 20.sp,
              fontWeight = FontWeight.Black
            )
            Text(
              text = "NON-COPY TACTICAL ARSENAL",
              color = TacticalAmber,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Equip Button
        Button(
          onClick = {
            prefs.selectedWeaponId = selectedWeapon.id
            onBack()
          },
          colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.testTag("equip_weapon_button")
        ) {
          Icon(Icons.Default.Check, contentDescription = "Equip", tint = TacticalDarkBg, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "EQUIP LOADOUT", color = TacticalDarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Weapon Selector Carousel (All 5 non-copy guns)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(WeaponArsenal.ALL_WEAPONS) { weapon ->
          val isCurrent = weapon.id == selectedWeapon.id
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isCurrent) TacticalSurfaceVariant else TacticalSurface)
              .border(1.5.dp, if (isCurrent) TacticalAmber else Color(0xFF334155), RoundedCornerShape(8.dp))
              .clickable { selectedWeapon = weapon }
              .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = weapon.name,
                color = if (isCurrent) TacticalAmber else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = weapon.codeName,
                color = Color(0xFF94A3B8),
                fontSize = 9.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Main Split Pane: Left Stat Overview & Right Attachment Pickers
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        // Left: Weapon Specs & Live Ballistics
        Card(
          modifier = Modifier
            .weight(0.42f)
            .fillMaxHeight(),
          colors = CardDefaults.cardColors(containerColor = TacticalSurface),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = selectedWeapon.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
              )
              Text(
                text = "${selectedWeapon.caliber}  •  ${selectedWeapon.category.name}",
                color = TacticalAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = selectedWeapon.lore,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                lineHeight = 14.sp
              )
            }

            // Stat Meters
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              StatMeter("DAMAGE", selectedWeapon.baseDamage / 120f, "${selectedWeapon.baseDamage}")
              StatMeter("FIRE RATE", selectedWeapon.fireRateRpm / 1000f, "${selectedWeapon.fireRateRpm} RPM")
              StatMeter("CAPACITY", selectedWeapon.currentMagCapacity / 60f, "${selectedWeapon.currentMagCapacity}")
              StatMeter("MOBILITY", selectedWeapon.mobilitySpeedFactor / 1.2f, "${(selectedWeapon.mobilitySpeedFactor * 100).toInt()}%")
              StatMeter("CONTROL", (1.0f - selectedWeapon.effectiveRecoilKick), "${((1.0f - selectedWeapon.effectiveRecoilKick) * 100).toInt()}%")
            }
          }
        }

        // Right: Attachment Tabs (Optics, Muzzle, Magazine, Camo)
        Card(
          modifier = Modifier
            .weight(0.58f)
            .fillMaxHeight(),
          colors = CardDefaults.cardColors(containerColor = TacticalSurface),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
          Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // Category Tabs
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0B0F19))
                .padding(4.dp),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) TacticalAmber else Color.Transparent)
                    .clickable { selectedTab = index }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = title,
                    color = if (isSelected) TacticalDarkBg else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attachment Option List
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              when (selectedTab) {
                0 -> {
                  // Optics
                  items(OpticType.values()) { optic ->
                    val isEquipped = selectedWeapon.selectedOptic == optic
                    AttachmentRow(
                      title = optic.displayName,
                      desc = "Zoom: ${optic.zoomFactor}x  •  Reflex reticle optic",
                      isEquipped = isEquipped,
                      onSelect = { selectedWeapon.selectedOptic = optic }
                    )
                  }
                }
                1 -> {
                  // Muzzle
                  items(MuzzleType.values()) { muzzle ->
                    val isEquipped = selectedWeapon.selectedMuzzle == muzzle
                    AttachmentRow(
                      title = muzzle.displayName,
                      desc = if (muzzle.isSilenced) "Suppresses radar blip & gunfire sound" else "Reduces vertical recoil by ${(muzzle.recoilReduction * 100).toInt()}%",
                      isEquipped = isEquipped,
                      onSelect = { selectedWeapon.selectedMuzzle = muzzle }
                    )
                  }
                }
                2 -> {
                  // Magazine
                  items(MagazineType.values()) { mag ->
                    val isEquipped = selectedWeapon.selectedMag == mag
                    AttachmentRow(
                      title = mag.displayName,
                      desc = "Capacity: ${(selectedWeapon.baseMagSize * mag.capacityMultiplier).toInt()} rounds",
                      isEquipped = isEquipped,
                      onSelect = { selectedWeapon.selectedMag = mag }
                    )
                  }
                }
                3 -> {
                  // Camos
                  items(WeaponCamo.values()) { camo ->
                    val isEquipped = selectedWeapon.selectedCamo == camo
                    AttachmentRow(
                      title = camo.displayName,
                      desc = "Special military finish: ${camo.patternStyle}",
                      isEquipped = isEquipped,
                      accentColor = Color(camo.accentColor),
                      onSelect = { selectedWeapon.selectedCamo = camo }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AttachmentRow(
  title: String,
  desc: String,
  isEquipped: Boolean,
  accentColor: Color? = null,
  onSelect: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(if (isEquipped) Color(0x33F59E0B) else Color(0xFF131D31))
      .border(1.dp, if (isEquipped) TacticalAmber else Color(0xFF1E293B), RoundedCornerShape(6.dp))
      .clickable(onClick = onSelect)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (accentColor != null) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(accentColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = title,
          color = if (isEquipped) TacticalAmber else Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Text(text = desc, color = Color(0xFF94A3B8), fontSize = 10.sp)
    }

    if (isEquipped) {
      Text(
        text = "EQUIPPED",
        color = TacticalAmber,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black
      )
    }
  }
}

@Composable
private fun StatMeter(label: String, progress: Float, valueStr: String) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = label, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
      Text(text = valueStr, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
    Spacer(modifier = Modifier.height(2.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(Color(0xFF1E293B))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(progress.coerceIn(0f, 1f))
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(TacticalAmber)
      )
    }
  }
}
