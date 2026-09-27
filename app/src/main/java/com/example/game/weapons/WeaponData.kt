package com.example.game.weapons

import androidx.compose.ui.graphics.Color

enum class WeaponCategory {
  ASSAULT_RIFLE,
  SUBMACHINE_GUN,
  SNIPER_RIFLE,
  SHOTGUN,
  EXPERIMENTAL
}

enum class OpticType(val displayName: String, val zoomFactor: Float, val reticleColor: Color) {
  IRON_SIGHTS("Tactical Irons", 1.15f, Color(0xFFE2E8F0)),
  VIPER_REFLEX("Viper Reflex Dot", 1.30f, Color(0xFFEF4444)),
  APEX_HOLO("Apex Holo-Reticle", 1.45f, Color(0xFF06B6D4)),
  RECON_4X("Recon 4x Optic", 2.0f, Color(0xFF10B981)),
  THERMAL_SNIPER("Thermal 8x Scope", 3.2f, Color(0xFFFFD700))
}

enum class MuzzleType(val displayName: String, val recoilReduction: Float, val isSilenced: Boolean) {
  FACTORY_BARREL("Factory Barrel", 0.0f, false),
  TACTICAL_COMPENSATOR("Vortex Compensator", 0.35f, false),
  SHADOW_SUPPRESSOR("Shadow Suppressor", 0.15f, true)
}

enum class MagazineType(val displayName: String, val capacityMultiplier: Float, val reloadTimeMultiplier: Float) {
  STANDARD("Standard Tactical Mag", 1.0f, 1.0f),
  EXTENDED_DRUM("Extended High-Cap Drum", 1.5f, 1.25f),
  SPEED_LOADER("Lightweight Speed Mag", 0.9f, 0.75f)
}

enum class WeaponCamo(val displayName: String, val primaryColor: Long, val accentColor: Long, val patternStyle: String) {
  MATTE_BLACK("Tactical Black", 0xFF1E293B, 0xFF475569, "SOLID"),
  DESERT_DIGITAL("Desert Digital Camo", 0xFFB45309, 0xFFFDE68A, "CAMO"),
  APEX_CARBON("Apex Carbon Fiber", 0xFF0F172A, 0xFF06B6D4, "CARBON"),
  DAMASCUS("Damascus Titanium", 0xFF312E81, 0xFFA855F7, "DAMASCUS"),
  CYBER_NEON("Cyberpunk Neon", 0xFF09090B, 0xFFEC4899, "NEON"),
  ROYAL_GOLD("Royal Gold Spec-Ops", 0xFFB45309, 0xFFFFD700, "GOLD")
}

data class Weapon(
  val id: String,
  val name: String,
  val codeName: String,
  val category: WeaponCategory,
  val lore: String,
  val caliber: String,
  val baseDamage: Int,
  val fireRateRpm: Int,
  val baseMagSize: Int,
  val totalReserveMags: Int,
  val reloadDurationMs: Long,
  val recoilKickback: Float,
  val recoilClimb: Float,
  val spreadAngle: Float,
  val pelletsPerShot: Int = 1,
  val muzzleVelocity: Float = 400f,
  val mobilitySpeedFactor: Float = 1.0f,
  val headshotMultiplier: Float = 1.8f,
  // Customizable Loadout
  var selectedOptic: OpticType = OpticType.IRON_SIGHTS,
  var selectedMuzzle: MuzzleType = MuzzleType.FACTORY_BARREL,
  var selectedMag: MagazineType = MagazineType.STANDARD,
  var selectedCamo: WeaponCamo = WeaponCamo.MATTE_BLACK
) {
  val currentMagCapacity: Int
    get() = (baseMagSize * selectedMag.capacityMultiplier).toInt()

  val effectiveReloadMs: Long
    get() = (reloadDurationMs * selectedMag.reloadTimeMultiplier).toLong()

  val effectiveRecoilKick: Float
    get() = recoilKickback * (1.0f - selectedMuzzle.recoilReduction)

  val shotIntervalMs: Long
    get() = (60_000L / fireRateRpm)

  val isSilenced: Boolean
    get() = selectedMuzzle.isSilenced
}

object WeaponArsenal {
  val ALL_WEAPONS: List<Weapon> = listOf(
    Weapon(
      id = "kronos_74",
      name = "KRONOS-74 Tactical",
      codeName = "COMBAT CARBINE",
      category = WeaponCategory.ASSAULT_RIFLE,
      lore = "Next-gen tactical carbine engineered for Tier-1 assault operations. Features dual-stage gas-piston, monolithic Picatinny rail, and balanced recoil recovery.",
      caliber = "6.8x43mm SPC Tungsten",
      baseDamage = 34,
      fireRateRpm = 720,
      baseMagSize = 30,
      totalReserveMags = 4,
      reloadDurationMs = 2100L,
      recoilKickback = 0.22f,
      recoilClimb = 0.035f,
      spreadAngle = 0.04f,
      mobilitySpeedFactor = 1.0f,
      selectedOptic = OpticType.VIPER_REFLEX
    ),
    Weapon(
      id = "vortex_9",
      name = "VORTEX-9 Vector PDW",
      codeName = "SUBMACHINE GUN",
      category = WeaponCategory.SUBMACHINE_GUN,
      lore = "Ultra-compact personal defense weapon with an articulated vector counter-recoil system. Dominates close-quarters with a blistering 980 RPM fire rate.",
      caliber = "9x21mm Sub-Sonic AP",
      baseDamage = 22,
      fireRateRpm = 980,
      baseMagSize = 36,
      totalReserveMags = 5,
      reloadDurationMs = 1750L,
      recoilKickback = 0.16f,
      recoilClimb = 0.025f,
      spreadAngle = 0.06f,
      mobilitySpeedFactor = 1.15f,
      selectedOptic = OpticType.APEX_HOLO
    ),
    Weapon(
      id = "solaris_50",
      name = "SOLARIS-50 AMR",
      codeName = "ANTI-MATERIEL RIFLE",
      category = WeaponCategory.SNIPER_RIFLE,
      lore = "Heavy anti-materiel bolt-action platform with carbon-wrapped fluted barrel and high-magnification optic. Devastating kinetic shockwave guarantees single-shot takedowns.",
      caliber = "12.7x99mm Depleted Core",
      baseDamage = 115,
      fireRateRpm = 52,
      baseMagSize = 5,
      totalReserveMags = 4,
      reloadDurationMs = 3200L,
      recoilKickback = 0.55f,
      recoilClimb = 0.12f,
      spreadAngle = 0.015f,
      mobilitySpeedFactor = 0.82f,
      selectedOptic = OpticType.THERMAL_SNIPER
    ),
    Weapon(
      id = "goliath_12",
      name = "GOLIATH-12 Breacher",
      codeName = "TACTICAL SHOTGUN",
      category = WeaponCategory.SHOTGUN,
      lore = "Heavy motorized drum-fed tactical breacher. Cleans out rooms, corridors, and fortified bunker chokepoints with brutal kinetic buckshot fragmentation.",
      caliber = "12-Gauge Magnum (8 Pellets)",
      baseDamage = 18,
      pelletsPerShot = 8,
      fireRateRpm = 250,
      baseMagSize = 10,
      totalReserveMags = 4,
      reloadDurationMs = 2800L,
      recoilKickback = 0.40f,
      recoilClimb = 0.08f,
      spreadAngle = 0.12f,
      mobilitySpeedFactor = 0.92f,
      selectedOptic = OpticType.IRON_SIGHTS
    ),
    Weapon(
      id = "phantom_x",
      name = "PHANTOM-X Gauss",
      codeName = "RAIL-CARBINE HYBRID",
      category = WeaponCategory.EXPERIMENTAL,
      lore = "Classified prototype weapon using superconducting electromagnetic rails to launch cobalt flechettes at Mach 6. Leaves an ionized cyan tracer trail.",
      caliber = "4.5mm Cobalt Flechette",
      baseDamage = 48,
      fireRateRpm = 540,
      baseMagSize = 24,
      totalReserveMags = 4,
      reloadDurationMs = 2400L,
      recoilKickback = 0.28f,
      recoilClimb = 0.04f,
      spreadAngle = 0.025f,
      mobilitySpeedFactor = 0.95f,
      selectedOptic = OpticType.APEX_HOLO
    )
  )

  fun getWeaponById(id: String): Weapon {
    return ALL_WEAPONS.find { it.id == id } ?: ALL_WEAPONS.first()
  }
}
