package com.example.game.engine

import android.content.Context
import com.example.game.audio.TacticalAudioSystem
import com.example.game.audio.TacticalHapticSystem
import com.example.game.data.GamePreferences
import com.example.game.weapons.Weapon
import com.example.game.weapons.WeaponArsenal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class KillfeedEntry(
  val id: Long = System.currentTimeMillis(),
  val killerName: String,
  val victimName: String,
  val weaponName: String,
  val isHeadshot: Boolean,
  val isPlayerKiller: Boolean,
  val isPlayerVictim: Boolean
)

data class XpPopup(
  val text: String,
  val amount: Int,
  var timer: Float = 1.6f
)

data class HitmarkerState(
  val isHeadshot: Boolean,
  var timer: Float = 0.25f
)

class BotEntity(
  val id: Int,
  val name: String,
  var x: Float,
  var y: Float,
  var angle: Float,
  var isEnemy: Boolean,
  var health: Int = 100,
  val maxHealth: Int = 100,
  var isAlive: Boolean = true,
  var isFiring: Boolean = false,
  var deathAnimProgress: Float = 0f,
  var hitFlashTimer: Float = 0f,
  var shootCooldown: Float = 0f,
  var targetWaypointIndex: Int = 0
)

class GameEngine(
  private val context: Context,
  val prefs: GamePreferences,
  val audio: TacticalAudioSystem,
  val haptics: TacticalHapticSystem
) {
  private val engineScope = CoroutineScope(Dispatchers.Default)
  val map = TacticalMap()
  val raycastRenderer = RaycastRenderer()
  val weaponRenderer = WeaponRenderer()

  // Player state
  var playerX = 1.5f
  var playerY = 1.5f
  var playerAngle = 0.785f
  var playerPitch = 0f
  var playerHealth = 100
  val playerMaxHealth = 100
  var playerArmor = 50
  val playerMaxArmor = 50
  var lastDamageTime = 0L

  var currentWeapon: Weapon = WeaponArsenal.getWeaponById(prefs.selectedWeaponId)
  var currentAmmo = currentWeapon.currentMagCapacity
  var reserveAmmo = currentWeapon.currentMagCapacity * currentWeapon.totalReserveMags

  var isAimingDownSights = false
  var adsProgress = 0f // 0..1
  var isSprinting = false
  var isSliding = false
  var slideTimer = 0f
  var slideDirectionAngle = 0f
  var isCrouching = false
  var walkTime = 0f

  var grenadesRemaining = 2
  var stimsRemaining = 2
  var isThrowingGrenade = false

  var isReloading = false
  var reloadTimer = 0f
  var lastShotTime = 0L

  // Killstreak & Match Stats
  var playerKills = 0
  var playerDeaths = 0
  var playerHeadshots = 0
  var currentStreak = 0
  var blueTeamScore = 0
  var redTeamScore = 0
  val scoreLimit = 25
  var matchTimeRemaining = 300f // 5 mins
  var isMatchOver = false
  var isVictory = false

  // Killstreaks active
  var uavActiveTimer = 0f // Sweeps minimap
  var airstrikeActiveTimer = 0f

  val bots = mutableListOf<BotEntity>()
  val killfeed = mutableListOf<KillfeedEntry>()
  val xpPopups = mutableListOf<XpPopup>()
  var activeHitmarker: HitmarkerState? = null

  // Telemetry & FPS stats
  var fpsDisplay = 120
  var frameTimeMs = 8.3f
  private var frameCounter = 0
  private var fpsTimer = 0L

  fun initializeGame() {
    playerX = map.playerSpawn.x
    playerY = map.playerSpawn.y
    playerAngle = map.playerSpawn.angle
    playerHealth = playerMaxHealth
    playerArmor = playerMaxArmor
    currentWeapon = WeaponArsenal.getWeaponById(prefs.selectedWeaponId)
    currentAmmo = currentWeapon.currentMagCapacity
    reserveAmmo = currentWeapon.currentMagCapacity * currentWeapon.totalReserveMags

    playerKills = 0
    playerDeaths = 0
    playerHeadshots = 0
    currentStreak = 0
    blueTeamScore = 0
    redTeamScore = 0
    matchTimeRemaining = 300f
    isMatchOver = false
    isVictory = false

    killfeed.clear()
    xpPopups.clear()
    bots.clear()

    // Spawn tactical enemy combatants (Red Cell)
    val botNames = listOf("PHANTOM-7", "VIPER-3", "REAPER-9", "GHOST-4", "TITAN-2")
    for (i in 0 until 5) {
      val spawn = map.enemySpawns[i % map.enemySpawns.size]
      bots.add(
        BotEntity(
          id = i + 1,
          name = botNames[i],
          x = spawn.x + (Random.nextFloat() * 0.4f - 0.2f),
          y = spawn.y + (Random.nextFloat() * 0.4f - 0.2f),
          angle = spawn.angle,
          isEnemy = true
        )
      )
    }

    // Spawn 1 friendly AI squadmate (Blue Team)
    bots.add(
      BotEntity(
        id = 10,
        name = "SHADOW-LEAD",
        x = 4.5f,
        y = 4.5f,
        angle = 0.5f,
        isEnemy = false
      )
    )

    audio.initialize()
    audio.sfxVolume = prefs.sfxVolume
    haptics.isHapticsEnabled = prefs.hapticsEnabled
    audio.playTacticalRadioCallout("START")
  }

  fun update(dt: Float, moveStickX: Float, moveStickY: Float, lookStickX: Float, lookStickY: Float) {
    if (isMatchOver) return

    val currentTime = System.currentTimeMillis()

    // 1. Calculate Live FPS and Frame Time Telemetry
    frameCounter++
    if (currentTime - fpsTimer >= 500) {
      fpsDisplay = ((frameCounter * 1000f) / (currentTime - fpsTimer)).toInt()
      frameTimeMs = if (fpsDisplay > 0) 1000f / fpsDisplay else 8.3f
      frameCounter = 0
      fpsTimer = currentTime
    }

    // Match Timer
    matchTimeRemaining -= dt
    if (matchTimeRemaining <= 0f) {
      endMatch(blueTeamScore >= redTeamScore)
      return
    }

    // UAV Killstreak Timer
    if (uavActiveTimer > 0f) {
      uavActiveTimer -= dt
    }

    // 2. Aim Look input
    val sens = if (isAimingDownSights) prefs.adsSensitivity else prefs.lookSensitivity
    val lookDeltaX = lookStickX * sens * dt * 3.2f
    val lookDeltaY = lookStickY * sens * dt * 2.8f
    playerAngle += lookDeltaX
    playerPitch = (playerPitch - lookDeltaY * 180f).coerceIn(-180f, 180f)

    // Normalize playerAngle
    while (playerAngle < 0) playerAngle += (2 * PI).toFloat()
    while (playerAngle >= 2 * PI) playerAngle -= (2 * PI).toFloat()

    // 3. Movement & Tactical Sprint / Slide
    val isMoving = kotlin.math.abs(moveStickX) > 0.1f || kotlin.math.abs(moveStickY) > 0.1f

    if (isSliding) {
      slideTimer -= dt
      val slideSpeed = 6.5f
      val dx = cos(slideDirectionAngle) * slideSpeed * dt
      val dy = sin(slideDirectionAngle) * slideSpeed * dt
      tryMovePlayer(dx, dy)
      if (slideTimer <= 0f) {
        isSliding = false
      }
    } else if (isMoving) {
      val moveAngle = playerAngle + atan2(moveStickY, moveStickX) - (PI.toFloat() / 2f)
      val speedMultiplier = when {
        isSprinting -> 5.8f
        isAimingDownSights -> 2.2f
        isCrouching -> 2.0f
        else -> 3.6f
      } * currentWeapon.mobilitySpeedFactor

      val dx = cos(moveAngle) * speedMultiplier * dt
      val dy = sin(moveAngle) * speedMultiplier * dt
      tryMovePlayer(dx, dy)

      walkTime += dt
      if (walkTime % 0.45f < dt && !isSliding) {
        audio.playFootstep()
      }
    }

    // 4. Smooth ADS Transition
    val targetAds = if (isAimingDownSights) 1.0f else 0.0f
    adsProgress += (targetAds - adsProgress) * (14.0f * dt)

    // 5. Weapon Animations (sway, bobbing, recoil recovery)
    weaponRenderer.updateAnimations(
      dt = dt,
      isMoving = isMoving,
      isSprinting = isSprinting,
      isSliding = isSliding,
      adsProgress = adsProgress,
      lookDeltaX = lookDeltaX,
      lookDeltaY = lookDeltaY,
      walkTime = walkTime
    )

    // 6. Reload Logic
    if (isReloading) {
      reloadTimer -= dt
      weaponRenderer.reloadProgress = 1.0f - (reloadTimer / (currentWeapon.effectiveReloadMs / 1000f)).coerceIn(0f, 1f)

      if (reloadTimer <= 0f) {
        finishReload()
      }
    }

    // 7. Health Regeneration (Call of Duty health recovery system)
    if (currentTime - lastDamageTime > 3800L && playerHealth < playerMaxHealth) {
      playerHealth = (playerHealth + (30f * dt).toInt()).coerceAtMost(playerMaxHealth)
    }

    // 8. Hitmarker & XP Popup timers
    activeHitmarker?.let {
      it.timer -= dt
      if (it.timer <= 0f) activeHitmarker = null
    }

    val xpIter = xpPopups.iterator()
    while (xpIter.hasNext()) {
      val pop = xpIter.next()
      pop.timer -= dt
      if (pop.timer <= 0f) xpIter.remove()
    }

    // 9. Update 3D World Particles
    raycastRenderer.updateParticles(dt)

    // 10. Update AI Enemy Bots
    updateBots(dt)
  }

  private fun tryMovePlayer(dx: Float, dy: Float) {
    val radius = 0.32f
    val newX = playerX + dx
    val newY = playerY + dy

    // Wall collision with slide
    if (!map.isWall(newX + radius, playerY) && !map.isWall(newX - radius, playerY)) {
      playerX = newX
    }
    if (!map.isWall(playerX, newY + radius) && !map.isWall(playerX, newY - radius)) {
      playerY = newY
    }
  }

  fun triggerPlayerFire() {
    if (isReloading || isSprinting) {
      isSprinting = false
      if (isReloading) return
    }

    val currentTime = System.currentTimeMillis()
    if (currentTime - lastShotTime < currentWeapon.shotIntervalMs) return

    if (currentAmmo <= 0) {
      startReload()
      return
    }

    lastShotTime = currentTime
    currentAmmo--

    // Audio & Haptic Recoil
    audio.playGunfire(currentWeapon.id, currentWeapon.isSilenced)
    haptics.triggerRecoil(currentWeapon.effectiveRecoilKick)

    // Visual weapon kickback
    weaponRenderer.triggerRecoil(currentWeapon.effectiveRecoilKick)
    raycastRenderer.addEjectedShell(playerX, playerY, playerAngle)

    // Camera recoil kick
    playerPitch = (playerPitch + currentWeapon.recoilClimb * 120f).coerceAtMost(180f)

    // Raycast hit detection for bullets
    val pellets = currentWeapon.pelletsPerShot
    for (p in 0 until pellets) {
      val spread = if (isAimingDownSights) currentWeapon.spreadAngle * 0.3f else currentWeapon.spreadAngle
      val shotAngle = playerAngle + (Random.nextFloat() * 2f - 1f) * spread
      castBullet(shotAngle)
    }

    if (currentAmmo == 0) {
      startReload()
    }
  }

  private fun castBullet(angle: Float) {
    val cosA = cos(angle)
    val sinA = sin(angle)
    var dist = 0f
    val maxDist = 22f
    val step = 0.2f

    var hitBot: BotEntity? = null
    var isHeadshot = false

    while (dist < maxDist) {
      dist += step
      val cx = playerX + cosA * dist
      val cy = playerY + sinA * dist

      if (map.isWall(cx, cy)) {
        // Hit wall! Spark & Smoke particle
        raycastRenderer.addBulletImpact(cx, cy)
        break
      }

      // Check collision with enemy bots
      for (bot in bots) {
        if (!bot.isAlive || !bot.isEnemy) continue
        val bDist = sqrt((bot.x - cx) * (bot.x - cx) + (bot.y - cy) * (bot.y - cy))
        if (bDist < 0.45f) {
          hitBot = bot
          // Headshot if crosshair was aimed high
          isHeadshot = playerPitch > 25f || (Random.nextFloat() < 0.22f)
          break
        }
      }

      if (hitBot != null) break
    }

    if (hitBot != null) {
      damageBot(hitBot, isHeadshot)
    }
  }

  private fun damageBot(bot: BotEntity, isHeadshot: Boolean) {
    val dmg = if (isHeadshot) {
      (currentWeapon.baseDamage * currentWeapon.headshotMultiplier).toInt()
    } else {
      currentWeapon.baseDamage
    }

    bot.health -= dmg
    bot.hitFlashTimer = 0.15f

    // Hitmarker feedback
    activeHitmarker = HitmarkerState(isHeadshot)
    audio.playHitmarker(isHeadshot)

    if (bot.health <= 0) {
      bot.health = 0
      bot.isAlive = false
      onPlayerKill(bot, isHeadshot)
    }
  }

  private fun onPlayerKill(bot: BotEntity, isHeadshot: Boolean) {
    playerKills++
    currentStreak++
    blueTeamScore++
    audio.playKillConfirmed()

    if (isHeadshot) playerHeadshots++

    // Killfeed entry
    killfeed.add(
      0,
      KillfeedEntry(
        killerName = "YOU",
        victimName = bot.name,
        weaponName = currentWeapon.name,
        isHeadshot = isHeadshot,
        isPlayerKiller = true,
        isPlayerVictim = false
      )
    )

    // XP & Callouts
    val xp = if (isHeadshot) 125 else 100
    xpPopups.add(0, XpPopup(if (isHeadshot) "+125 HEADSHOT" else "+100 ELIMINATED", xp))
    prefs.addXp(xp)
    prefs.totalKills++
    if (isHeadshot) prefs.totalHeadshots++
    if (currentStreak > prefs.bestStreak) prefs.bestStreak = currentStreak

    // Killstreak unlock voice triggers
    when (currentStreak) {
      3 -> {
        xpPopups.add(0, XpPopup("UAV RECON READY!", 50))
        audio.playTacticalRadioCallout("UAV")
      }
      5 -> {
        xpPopups.add(0, XpPopup("CRUISE MISSILE READY!", 100))
        audio.playTacticalRadioCallout("AIRSTRIKE")
      }
      7 -> {
        xpPopups.add(0, XpPopup("OVERWATCH GUNNER READY!", 150))
        audio.playTacticalRadioCallout("OVERWATCH")
      }
    }

    // Check Victory
    if (blueTeamScore >= scoreLimit) {
      endMatch(true)
    }

    // Respawn bot after delay
    respawnBot(bot)
  }

  private fun respawnBot(bot: BotEntity) {
    engineScope.launch {
      delay(3500)
      if (!isMatchOver) {
        val spawn = map.enemySpawns.random()
        bot.x = spawn.x
        bot.y = spawn.y
        bot.health = bot.maxHealth
        bot.isAlive = true
        bot.deathAnimProgress = 0f
      }
    }
  }

  private fun updateBots(dt: Float) {
    for (bot in bots) {
      if (!bot.isAlive) {
        bot.deathAnimProgress = (bot.deathAnimProgress + 2f * dt).coerceAtMost(1.0f)
        continue
      }

      if (bot.hitFlashTimer > 0f) {
        bot.hitFlashTimer -= dt
      }

      val dx = playerX - bot.x
      val dy = playerY - bot.y
      val distToPlayer = sqrt(dx * dx + dy * dy)

      // Bot faces player if enemy and within 14 units
      if (bot.isEnemy && distToPlayer < 14f && hasLineOfSight(bot.x, bot.y, playerX, playerY)) {
        bot.angle = atan2(dy, dx)

        // Bot Firing logic
        bot.shootCooldown -= dt
        if (bot.shootCooldown <= 0f) {
          bot.isFiring = true
          bot.shootCooldown = (Random.nextFloat() * 0.8f + 0.6f)
          audio.playGunfire("kronos_74", false)

          // Chance to hit player based on difficulty & distance
          if (distToPlayer < 10f && Random.nextFloat() < 0.42f) {
            damagePlayer(18)
          }
        } else {
          bot.isFiring = false
        }

        // Flank/Close in on player
        if (distToPlayer > 3.5f) {
          val moveX = cos(bot.angle) * 2.2f * dt
          val moveY = sin(bot.angle) * 2.2f * dt
          if (!map.isWall(bot.x + moveX, bot.y)) bot.x += moveX
          if (!map.isWall(bot.x, bot.y + moveY)) bot.y += moveY
        }
      } else {
        // Patrol Waypoints
        bot.isFiring = false
        val wp = map.patrolWaypoints[bot.targetWaypointIndex]
        val wdx = wp.x - bot.x
        val wdy = wp.y - bot.y
        val wDist = sqrt(wdx * wdx + wdy * wdy)

        if (wDist < 1.0f) {
          bot.targetWaypointIndex = (bot.targetWaypointIndex + 1) % map.patrolWaypoints.size
        } else {
          bot.angle = atan2(wdy, wdx)
          val mx = cos(bot.angle) * 1.8f * dt
          val my = sin(bot.angle) * 1.8f * dt
          if (!map.isWall(bot.x + mx, bot.y)) bot.x += mx
          if (!map.isWall(bot.x, bot.y + my)) bot.y += my
        }
      }
    }
  }

  private fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
    val dist = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    val steps = (dist * 4).toInt().coerceAtLeast(1)
    val dx = (x2 - x1) / steps
    val dy = (y2 - y1) / steps
    var cx = x1
    var cy = y1
    for (i in 0 until steps) {
      cx += dx
      cy += dy
      if (map.isWall(cx, cy)) return false
    }
    return true
  }

  fun damagePlayer(damage: Int) {
    if (playerHealth <= 0 || isMatchOver) return
    lastDamageTime = System.currentTimeMillis()

    // Armor absorbs damage first
    if (playerArmor > 0) {
      val absorbed = damage.coerceAtMost(playerArmor)
      playerArmor -= absorbed
      val remaining = damage - absorbed
      playerHealth -= remaining
    } else {
      playerHealth -= damage
    }

    haptics.triggerDamage()

    if (playerHealth <= 0) {
      playerHealth = 0
      onPlayerDeath()
    }
  }

  private fun onPlayerDeath() {
    playerDeaths++
    currentStreak = 0
    redTeamScore++
    prefs.totalDeaths++

    killfeed.add(
      0,
      KillfeedEntry(
        killerName = "VIPER-3",
        victimName = "YOU",
        weaponName = "KRONOS-74",
        isHeadshot = false,
        isPlayerKiller = false,
        isPlayerVictim = true
      )
    )

    if (redTeamScore >= scoreLimit) {
      endMatch(false)
      return
    }

    // Respawn player
    engineScope.launch {
      delay(2000)
      if (!isMatchOver) {
        playerX = map.playerSpawn.x
        playerY = map.playerSpawn.y
        playerHealth = playerMaxHealth
        playerArmor = playerMaxArmor
        currentAmmo = currentWeapon.currentMagCapacity
      }
    }
  }

  fun triggerTacticalSlide() {
    if (!isSliding && !isReloading) {
      isSliding = true
      slideTimer = 0.55f
      slideDirectionAngle = playerAngle
      audio.playSlide()
      haptics.triggerRecoil(0.3f)
    }
  }

  fun toggleSprint() {
    if (!isAimingDownSights && !isReloading) {
      isSprinting = !isSprinting
    }
  }

  fun toggleAds() {
    isAimingDownSights = !isAimingDownSights
    if (isAimingDownSights) {
      isSprinting = false
    }
  }

  fun startReload() {
    if (isReloading || currentAmmo >= currentWeapon.currentMagCapacity || reserveAmmo <= 0) return
    isReloading = true
    isAimingDownSights = false
    reloadTimer = currentWeapon.effectiveReloadMs / 1000f
    audio.playReloadSound(0)
  }

  private fun finishReload() {
    val needed = currentWeapon.currentMagCapacity - currentAmmo
    val take = needed.coerceAtMost(reserveAmmo)
    currentAmmo += take
    reserveAmmo -= take
    isReloading = false
    weaponRenderer.reloadProgress = 0f
    audio.playReloadSound(2)
  }

  fun throwGrenade() {
    if (grenadesRemaining <= 0 || isThrowingGrenade) return
    grenadesRemaining--
    isThrowingGrenade = true

    audio.playTacticalRadioCallout("GRENADE")

    // Cook & throw grenade
    engineScope.launch {
      delay(1200)
      // Detonate 6 units forward
      val targetX = playerX + cos(playerAngle) * 6f
      val targetY = playerY + sin(playerAngle) * 6f

      audio.playExplosion()
      haptics.triggerExplosion()
      raycastRenderer.addExplosion(targetX, targetY)

      // Damage bots in blast radius
      for (bot in bots) {
        if (!bot.isAlive || !bot.isEnemy) continue
        val d = sqrt((bot.x - targetX) * (bot.x - targetX) + (bot.y - targetY) * (bot.y - targetY))
        if (d < 4.5f) {
          damageBot(bot, false)
        }
      }
      isThrowingGrenade = false
    }
  }

  fun useStimShot() {
    if (stimsRemaining <= 0 || playerHealth >= playerMaxHealth) return
    stimsRemaining--
    playerHealth = playerMaxHealth
    playerArmor = playerMaxArmor
    audio.playTacticalRadioCallout("STIM")
    haptics.triggerRecoil(0.4f)
    xpPopups.add(0, XpPopup("HEALTH RESTORED", 25))
  }

  fun activateKillstreak(tier: Int) {
    when (tier) {
      3 -> {
        // UAV Recon
        uavActiveTimer = 30f
        audio.playTacticalRadioCallout("UAV")
        xpPopups.add(0, XpPopup("UAV RECON ACTIVE (30s)", 50))
      }
      5 -> {
        // Precision Cruise Missile
        audio.playTacticalRadioCallout("AIRSTRIKE")
        xpPopups.add(0, XpPopup("AIRSTRIKE INBOUND!", 100))
        engineScope.launch {
          delay(1600)
          audio.playExplosion()
          haptics.triggerExplosion()
          // Blast living enemy bots
          for (bot in bots) {
            if (bot.isAlive && bot.isEnemy) {
              raycastRenderer.addExplosion(bot.x, bot.y)
              damageBot(bot, false)
            }
          }
        }
      }
      7 -> {
        // Overwatch Chopper Gunner
        audio.playTacticalRadioCallout("OVERWATCH")
        xpPopups.add(0, XpPopup("OVERWATCH DEPLOYED!", 150))
        engineScope.launch {
          for (i in 0 until 6) {
            delay(1000)
            val enemy = bots.find { it.isAlive && it.isEnemy }
            if (enemy != null) {
              audio.playGunfire("solaris_50", false)
              damageBot(enemy, true)
            }
          }
        }
      }
    }
  }

  fun switchWeapon(weaponId: String) {
    if (isReloading) return
    currentWeapon = WeaponArsenal.getWeaponById(weaponId)
    prefs.selectedWeaponId = weaponId
    currentAmmo = currentWeapon.currentMagCapacity
    reserveAmmo = currentWeapon.currentMagCapacity * currentWeapon.totalReserveMags
    audio.playReloadSound(1)
  }

  private fun endMatch(victory: Boolean) {
    isMatchOver = true
    isVictory = victory
    if (victory) {
      prefs.matchesWon++
      prefs.addXp(500)
      audio.playTacticalRadioCallout("VICTORY")
    } else {
      prefs.addXp(150)
      audio.playTacticalRadioCallout("DEFEAT")
    }
  }
}
