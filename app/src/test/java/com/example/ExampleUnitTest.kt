package com.example

import com.example.game.engine.TacticalMap
import com.example.game.weapons.OpticType
import com.example.game.weapons.WeaponArsenal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun weaponsArsenal_hasUniqueWeapons() {
    val weapons = WeaponArsenal.ALL_WEAPONS
    assertEquals(5, weapons.size)

    val kronos = WeaponArsenal.getWeaponById("kronos_74")
    assertNotNull(kronos)
    assertEquals("KRONOS-74 Tactical", kronos.name)
    assertTrue(kronos.baseDamage > 0)
    assertTrue(kronos.fireRateRpm > 0)

    val vortex = WeaponArsenal.getWeaponById("vortex_9")
    assertEquals("VORTEX-9 Vector PDW", vortex.name)
    assertEquals(980, vortex.fireRateRpm)

    val solaris = WeaponArsenal.getWeaponById("solaris_50")
    assertEquals(115, solaris.baseDamage)

    val goliath = WeaponArsenal.getWeaponById("goliath_12")
    assertEquals(8, goliath.pelletsPerShot)

    val phantom = WeaponArsenal.getWeaponById("phantom_x")
    assertEquals("PHANTOM-X Gauss", phantom.name)
  }

  @Test
  fun tacticalMap_boundariesAndSpawnsValid() {
    val map = TacticalMap()
    assertEquals(24, map.width)
    assertEquals(24, map.height)

    // Outer borders should be walls
    assertTrue(map.isWall(0.5f, 0.5f))
    assertTrue(map.isWall(23.5f, 23.5f))

    // Player spawn should not be wall
    assertFalse(map.isWall(map.playerSpawn.x, map.playerSpawn.y))
  }
}
