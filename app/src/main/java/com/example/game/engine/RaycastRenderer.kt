package com.example.game.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.game.data.GraphicsPreset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

data class RayHit(
  val distance: Float,
  val wallType: Int,
  val isVertical: Boolean,
  val wallX: Float, // Texture coordinate [0..1]
  val mapX: Int,
  val mapY: Int
)

data class Particle(
  var x: Float,
  var y: Float,
  var z: Float,
  var vx: Float,
  var vy: Float,
  var vz: Float,
  var life: Float, // 1.0 down to 0.0
  var maxLife: Float,
  val color: Int,
  val size: Float,
  val isShell: Boolean = false
)

class RaycastRenderer {
  private val paint = Paint().apply {
    isAntiAlias = false
    style = Paint.Style.FILL
  }

  private val smoothPaint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.FILL
  }

  private val strokePaint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
  }

  val particles = mutableListOf<Particle>()

  fun addBulletImpact(x: Float, y: Float, z: Float = 0.5f) {
    // Bullet impact dust cloud & sparks in desert
    for (i in 0 until 12) {
      val angle = (Math.random() * 2 * PI).toFloat()
      val speed = (Math.random() * 2.5f + 0.8f).toFloat()
      particles.add(
        Particle(
          x = x,
          y = y,
          z = z,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          vz = (Math.random() * 2.2f).toFloat(),
          life = 1.0f,
          maxLife = (Math.random() * 0.45f + 0.25f).toFloat(),
          // Desert dust sand puff + sparks
          color = if (Math.random() > 0.4) Color.rgb(217, 169, 115) else Color.rgb(245, 158, 11),
          size = (Math.random() * 6f + 3f).toFloat()
        )
      )
    }
  }

  fun addExplosion(x: Float, y: Float) {
    for (i in 0 until 40) {
      val angle = (Math.random() * 2 * PI).toFloat()
      val speed = (Math.random() * 5.0f + 2.0f).toFloat()
      particles.add(
        Particle(
          x = x,
          y = y,
          z = 0.5f,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          vz = (Math.random() * 4.0f).toFloat(),
          life = 1.0f,
          maxLife = (Math.random() * 0.8f + 0.4f).toFloat(),
          color = if (i % 2 == 0) Color.rgb(255, 80, 20) else Color.rgb(234, 179, 8),
          size = (Math.random() * 10f + 6f).toFloat()
        )
      )
    }
  }

  fun addEjectedShell(playerX: Float, playerY: Float, playerAngle: Float) {
    val ejectAngle = playerAngle + (PI.toFloat() / 2f) + (Math.random() * 0.2f - 0.1f).toFloat()
    val speed = 2.5f
    particles.add(
      Particle(
        x = playerX,
        y = playerY,
        z = 0.5f,
        vx = cos(ejectAngle) * speed,
        vy = sin(ejectAngle) * speed,
        vz = 1.8f,
        life = 1.0f,
        maxLife = 0.6f,
        color = Color.rgb(245, 180, 50), // Brass gold
        size = 5f,
        isShell = true
      )
    )
  }

  fun updateParticles(dt: Float) {
    val iter = particles.iterator()
    while (iter.hasNext()) {
      val p = iter.next()
      p.life -= dt / p.maxLife
      p.x += p.vx * dt
      p.y += p.vy * dt
      p.z += p.vz * dt
      p.vz -= 6.0f * dt // Gravity

      if (p.z < 0f) {
        p.z = 0f
        p.vz = -p.vz * 0.4f // Bounce
        p.vx *= 0.7f
        p.vy *= 0.7f
      }

      if (p.life <= 0f) {
        iter.remove()
      }
    }
  }

  fun renderScene(
    canvas: Canvas,
    screenWidth: Float,
    screenHeight: Float,
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    pitchOffset: Float, // Up/down look offset
    fovRad: Float,
    map: TacticalMap,
    bots: List<BotEntity>,
    muzzleFlashIntensity: Float, // 0..1 point light
    preset: GraphicsPreset
  ) {
    val numRays = preset.rayCount
    val halfFov = fovRad / 2f
    val horizonY = screenHeight / 2f + pitchOffset
    val rayStep = screenWidth / numRays.toFloat()

    // 1. Draw Scorching Desert Sky & Endless Sand Dunes
    renderDesertSkyAndSand(canvas, screenWidth, screenHeight, horizonY, playerAngle, fovRad, muzzleFlashIntensity)

    // Raycast Depth Buffer for Bot occlusion
    val zBuffer = FloatArray(numRays)

    // 2. Cast Rays & Render Desert Buildings and Fortifications
    for (i in 0 until numRays) {
      val rayScreenX = i.toFloat() / numRays.toFloat()
      val rayAngle = playerAngle - halfFov + (rayScreenX * fovRad)
      val rayCos = cos(rayAngle)
      val raySin = sin(rayAngle)

      val hit = castRay(playerX, playerY, rayCos, raySin, map)
      val correctedDist = hit.distance * cos(rayAngle - playerAngle)
      zBuffer[i] = correctedDist

      val wallHeight = (screenHeight / (correctedDist.coerceAtLeast(0.1f))) * 0.95f
      val wallTop = horizonY - (wallHeight / 2f)
      val wallBottom = horizonY + (wallHeight / 2f)

      drawWallSlice(
        canvas = canvas,
        screenX = i * rayStep,
        sliceWidth = rayStep + 0.8f,
        top = wallTop,
        bottom = wallBottom,
        hit = hit,
        dist = correctedDist,
        muzzleFlash = muzzleFlashIntensity
      )
    }

    // 3. Render 3D Perspective Combatants (Bots)
    renderBots(
      canvas = canvas,
      screenWidth = screenWidth,
      screenHeight = screenHeight,
      horizonY = horizonY,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      fovRad = fovRad,
      numRays = numRays,
      zBuffer = zBuffer,
      bots = bots
    )

    // 4. Render 3D World Particles (Dust, Sand puffs, Shells, Sparks)
    renderWorldParticles(
      canvas = canvas,
      screenWidth = screenWidth,
      screenHeight = screenHeight,
      horizonY = horizonY,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      fovRad = fovRad
    )
  }

  private fun renderDesertSkyAndSand(
    canvas: Canvas,
    w: Float,
    h: Float,
    horizon: Float,
    playerAngle: Float,
    fovRad: Float,
    muzzleFlash: Float
  ) {
    // Scorching Open Desert Sky: Deep vibrant desert azure fading down to warm haze on horizon
    paint.shader = LinearGradient(
      0f, 0f, 0f, horizon.coerceAtLeast(1f),
      Color.rgb(30, 85, 155),   // Vivid Desert Sky Blue
      Color.rgb(238, 195, 140),  // Dusty Sunlit Atmospheric Horizon Haze
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, w, horizon, paint)
    paint.shader = null

    // Blazing Desert Sun (Dynamic panoramic projection based on player angle)
    val sunWorldAngle = 1.2f // North-East desert sun
    var sunRelAngle = sunWorldAngle - playerAngle
    while (sunRelAngle > PI) sunRelAngle -= (2 * PI).toFloat()
    while (sunRelAngle < -PI) sunRelAngle += (2 * PI).toFloat()

    val halfFov = fovRad / 2f
    if (sunRelAngle in (-halfFov - 0.3f)..(halfFov + 0.3f)) {
      val sunScreenX = ((sunRelAngle + halfFov) / fovRad) * w
      val sunScreenY = horizon * 0.32f

      // Solar Corona / Sun Flare
      smoothPaint.color = Color.argb(45, 255, 240, 180)
      canvas.drawCircle(sunScreenX, sunScreenY, 80f, smoothPaint)
      smoothPaint.color = Color.argb(120, 255, 245, 210)
      canvas.drawCircle(sunScreenX, sunScreenY, 40f, smoothPaint)
      smoothPaint.color = Color.rgb(255, 255, 240)
      canvas.drawCircle(sunScreenX, sunScreenY, 20f, smoothPaint)
    }

    // Distant Desert Dunes Silhouette on Horizon
    val dunePath = Path()
    dunePath.moveTo(0f, horizon)
    val duneSteps = 16
    val dStepX = w / duneSteps
    for (s in 0..duneSteps) {
      val x = s * dStepX
      val duneWave = sin((x * 0.015f) + playerAngle * 2f) * 14f + cos((x * 0.008f)) * 8f
      dunePath.lineTo(x, horizon - 12f + duneWave)
    }
    dunePath.lineTo(w, horizon)
    dunePath.close()

    smoothPaint.color = Color.rgb(214, 158, 96) // Distant warm sand dune rim
    canvas.drawPath(dunePath, smoothPaint)

    // Desert Sand Floor: Warm sun-bleached golden desert sand dunes fading into distance
    val flashAdd = (muzzleFlash * 60).toInt()
    val sandColorHorizon = Color.rgb(
      (210 + flashAdd / 2).coerceAtMost(255),
      (165 + flashAdd / 2).coerceAtMost(255),
      (110 + flashAdd / 2).coerceAtMost(255)
    )
    val sandColorForeground = Color.rgb(
      (180 + flashAdd).coerceAtMost(255),
      (135 + flashAdd).coerceAtMost(255),
      (85 + flashAdd).coerceAtMost(255)
    )

    paint.shader = LinearGradient(
      0f, horizon, 0f, h,
      sandColorHorizon,
      sandColorForeground,
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, horizon, w, h, paint)
    paint.shader = null

    // Wind-swept sand ripple wave contours on floor
    smoothPaint.color = Color.argb(32, 138, 94, 52)
    val numDuneRipples = 8
    for (g in 1..numDuneRipples) {
      val t = (g.toFloat() / (numDuneRipples + 1))
      val y = horizon + (h - horizon) * (t * t)
      canvas.drawLine(0f, y, w, y, smoothPaint)
    }
  }

  private fun castRay(
    startX: Float,
    startY: Float,
    rayCos: Float,
    raySin: Float,
    map: TacticalMap
  ): RayHit {
    var mapX = startX.toInt()
    var mapY = startY.toInt()

    val deltaDistX = if (rayCos == 0f) 1e30f else kotlin.math.abs(1f / rayCos)
    val deltaDistY = if (raySin == 0f) 1e30f else kotlin.math.abs(1f / raySin)

    var sideDistX: Float
    var sideDistY: Float

    val stepX = if (rayCos < 0) {
      sideDistX = (startX - mapX) * deltaDistX
      -1
    } else {
      sideDistX = (mapX + 1.0f - startX) * deltaDistX
      1
    }

    val stepY = if (raySin < 0) {
      sideDistY = (startY - mapY) * deltaDistY
      -1
    } else {
      sideDistY = (mapY + 1.0f - startY) * deltaDistY
      1
    }

    var hit = false
    var isVertical = false
    var wallType = 1
    var steps = 0
    val maxSteps = 40

    while (!hit && steps < maxSteps) {
      if (sideDistX < sideDistY) {
        sideDistX += deltaDistX
        mapX += stepX
        isVertical = false
      } else {
        sideDistY += deltaDistY
        mapY += stepY
        isVertical = true
      }

      val tile = map.getTile(mapX, mapY)
      if (tile > 0) {
        hit = true
        wallType = tile
      }
      steps++
    }

    val perpWallDist = if (!isVertical) {
      (mapX - startX + (1 - stepX) / 2f) / rayCos
    } else {
      (mapY - startY + (1 - stepY) / 2f) / raySin
    }

    val wallX = if (!isVertical) {
      startY + perpWallDist * raySin
    } else {
      startX + perpWallDist * rayCos
    }
    val normWallX = wallX - floor(wallX)

    return RayHit(
      distance = perpWallDist.coerceAtLeast(0.05f),
      wallType = wallType,
      isVertical = isVertical,
      wallX = normWallX,
      mapX = mapX,
      mapY = mapY
    )
  }

  private fun drawWallSlice(
    canvas: Canvas,
    screenX: Float,
    sliceWidth: Float,
    top: Float,
    bottom: Float,
    hit: RayHit,
    dist: Float,
    muzzleFlash: Float
  ) {
    // Natural desert sun illumination & warm atmospheric dust haze
    val maxDist = 20f
    val fog = (1f - (dist / maxDist)).coerceIn(0.18f, 1.0f)
    // Strong sunlight cast from one axis (North/South vs East/West)
    val sunAngleFactor = if (hit.isVertical) 0.85f else 1.0f

    // Muzzle flash boost
    val flashBoost = muzzleFlash * (1f - (dist / 8f)).coerceAtLeast(0f) * 0.6f
    val finalLight = (fog * sunAngleFactor + flashBoost).coerceIn(0.15f, 1.35f)

    var baseR: Int
    var baseG: Int
    var baseB: Int

    when (hit.wallType) {
      2 -> {
        // Sun-Bleached Adobe / Stucco Village House (Classic Middle Eastern / Desert Warzone building)
        // Sand-colored plaster stucco with wooden beams and recessed window slits
        val isWindow = (hit.wallX in 0.38f..0.62f) && (top + (bottom - top) * 0.35f < (top + bottom) / 2f && top + (bottom - top) * 0.55f > (top + bottom) / 2f)
        val isWoodBeam = ((bottom - top) > 100f) && (hit.wallX in 0.20f..0.26f || hit.wallX in 0.74f..0.80f)
        if (isWindow) {
          // Dark recessed sniper aperture / window
          baseR = 38; baseG = 28; baseB = 22
        } else if (isWoodBeam) {
          // Weathered desert cedar support beam
          baseR = 101; baseG = 67; baseB = 33
        } else {
          // Warm clay stucco / adobe sand plaster
          val textureVariation = ((hit.wallX * 24f).toInt() % 2 == 0)
          if (textureVariation) {
            baseR = 228; baseG = 196; baseB = 152
          } else {
            baseR = 216; baseG = 182; baseB = 138
          }
        }
      }
      3 -> {
        // Weathered Ancient Desert Stone Pillars & Clay Brick Archway
        val brickRow = ((top + bottom) * 0.04f).toInt() % 2 == 0
        val isMortar = hit.wallX in 0.48f..0.52f || brickRow
        if (isMortar) {
          baseR = 160; baseG = 125; baseB = 95 // Clay mortar
        } else {
          baseR = 195; baseG = 110; baseB = 75 // Sun-baked terracotta & desert brick
        }
      }
      4 -> {
        // Desert Outpost Corrugated Metal Shed / Military Checkpoint
        // Corrugated vertical iron ribs with desert dust and olive-drab paint
        val rib = (hit.wallX * 16f).toInt() % 2 == 0
        if (rib) {
          baseR = 168; baseG = 152; baseB = 120 // Sunlit corrugated zinc
        } else {
          baseR = 130; baseG = 118; baseB = 92  // Rib shadow
        }
      }
      5 -> {
        // Sandbag Bunker & Trench Barricade
        val bagRow = ((top + bottom) * 0.08f).toInt() % 2 == 0
        val bagSeam = hit.wallX in 0.30f..0.34f || hit.wallX in 0.66f..0.70f
        if (bagSeam || bagRow) {
          baseR = 130; baseG = 100; baseB = 65 // Sandbag seam shadow
        } else {
          baseR = 190; baseG = 155; baseB = 105 // Burlap sandbag cloth
        }
      }
      6 -> {
        // Desert Ammo Depot & Supply Cache
        val isStripe = hit.wallX in 0.45f..0.55f
        if (isStripe) {
          baseR = 245; baseG = 158; baseB = 11 // Yellow caution band
        } else {
          baseR = 85; baseG = 95; baseB = 65   // Desert camouflage olive
        }
      }
      else -> {
        // Sandstone Boundary Cliffs & Massive Desert Fortress Perimeter
        val strataLayer = ((top + bottom) * 0.025f).toInt() % 3
        when (strataLayer) {
          0 -> { baseR = 210; baseG = 160; baseB = 110 } // Natural sandstone
          1 -> { baseR = 190; baseG = 140; baseB = 95 }  // Darker sedimentary band
          else -> { baseR = 225; baseG = 175; baseB = 125 } // Light silica layer
        }
      }
    }

    // Apply lighting, sun angle & muzzle flash
    val r = (baseR * finalLight).toInt().coerceIn(0, 255)
    val g = (baseG * finalLight).toInt().coerceIn(0, 255)
    val b = (baseB * finalLight).toInt().coerceIn(0, 255)

    paint.color = Color.rgb(r, g, b)
    canvas.drawRect(screenX, top, screenX + sliceWidth, bottom, paint)
  }

  private fun renderBots(
    canvas: Canvas,
    screenWidth: Float,
    screenHeight: Float,
    horizonY: Float,
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    fovRad: Float,
    numRays: Int,
    zBuffer: FloatArray,
    bots: List<BotEntity>
  ) {
    // Sort bots by distance descending (Painter's algorithm)
    val sortedBots = bots.filter { it.isAlive || it.deathAnimProgress < 1.0f }
      .map { bot ->
        val dx = bot.x - playerX
        val dy = bot.y - playerY
        val dist = sqrt(dx * dx + dy * dy)
        Pair(bot, dist)
      }
      .sortedByDescending { it.second }

    val halfFov = fovRad / 2f

    for ((bot, dist) in sortedBots) {
      if (dist < 0.2f) continue

      val dx = bot.x - playerX
      val dy = bot.y - playerY
      var botAngle = atan2(dy, dx) - playerAngle

      // Normalize angle to [-PI, PI]
      while (botAngle > PI) botAngle -= (2 * PI).toFloat()
      while (botAngle < -PI) botAngle += (2 * PI).toFloat()

      if (botAngle < -halfFov - 0.2f || botAngle > halfFov + 0.2f) continue

      // Screen X projection
      val screenX = ((botAngle + halfFov) / fovRad) * screenWidth
      val botHeight = (screenHeight / dist) * 0.85f
      val botWidth = botHeight * 0.55f
      val botTop = horizonY - (botHeight / 2f) + (bot.deathAnimProgress * botHeight * 0.4f)
      val botBottom = botTop + botHeight * (1f - bot.deathAnimProgress * 0.5f)

      // Depth buffer check (sample center slice)
      val centerRayIndex = ((screenX / screenWidth) * numRays).toInt().coerceIn(0, numRays - 1)
      if (zBuffer[centerRayIndex] < dist - 0.2f) {
        continue // Occluded behind building/wall
      }

      // Draw Desert Combatant Soldier
      drawTacticalBot(
        canvas = canvas,
        cx = screenX,
        top = botTop,
        bottom = botBottom,
        width = botWidth,
        bot = bot,
        dist = dist
      )
    }
  }

  private fun drawTacticalBot(
    canvas: Canvas,
    cx: Float,
    top: Float,
    bottom: Float,
    width: Float,
    bot: BotEntity,
    dist: Float
  ) {
    val h = bottom - top
    val isRed = bot.isEnemy
    val alpha = ((1.0f - bot.deathAnimProgress) * 255).toInt().coerceIn(0, 255)

    // Damage flash
    val isFlashing = bot.hitFlashTimer > 0f

    // 1. Desert Combat Boots & Cargo Pants (Khaki / Desert camo)
    smoothPaint.color = if (isFlashing) Color.argb(alpha, 255, 255, 255) else Color.argb(alpha, 120, 95, 65)
    canvas.drawRect(cx - width * 0.35f, top + h * 0.6f, cx - width * 0.05f, bottom, smoothPaint)
    canvas.drawRect(cx + width * 0.05f, top + h * 0.6f, cx + width * 0.35f, bottom, smoothPaint)

    // 2. Tactical Plate Carrier Vest (Hostile Red Cell vs Allied Desert Blue)
    val vestColor = if (isFlashing) {
      Color.argb(alpha, 255, 100, 100)
    } else if (isRed) {
      Color.argb(alpha, 185, 28, 28) // Hostile Crimson
    } else {
      Color.argb(alpha, 29, 78, 216) // Friendly Blue
    }
    smoothPaint.color = vestColor
    canvas.drawRoundRect(
      cx - width * 0.4f, top + h * 0.25f,
      cx + width * 0.4f, top + h * 0.65f,
      width * 0.1f, width * 0.1f,
      smoothPaint
    )

    // 3. Combat Helmet & Sand Goggles / Visor
    smoothPaint.color = Color.argb(alpha, 160, 130, 90) // Desert Tan Helmet
    canvas.drawCircle(cx, top + h * 0.16f, width * 0.25f, smoothPaint)

    // Tinted Sand Goggles
    val visorColor = if (isRed) Color.argb(alpha, 239, 68, 68) else Color.argb(alpha, 6, 182, 212)
    smoothPaint.color = visorColor
    canvas.drawRoundRect(
      cx - width * 0.16f, top + h * 0.14f,
      cx + width * 0.16f, top + h * 0.20f,
      2f, 2f,
      smoothPaint
    )

    // 4. Weapon & Muzzle Flash
    smoothPaint.color = Color.argb(alpha, 40, 45, 55)
    canvas.drawRect(cx, top + h * 0.38f, cx + width * 0.65f, top + h * 0.45f, smoothPaint)

    if (bot.isFiring && bot.isAlive) {
      smoothPaint.color = Color.argb(230, 255, 200, 50)
      canvas.drawCircle(cx + width * 0.68f, top + h * 0.41f, width * 0.25f, smoothPaint)
      smoothPaint.color = Color.argb(255, 255, 255, 255)
      canvas.drawCircle(cx + width * 0.68f, top + h * 0.41f, width * 0.12f, smoothPaint)
    }

    // 5. Overhead Tag & Health Bar
    if (bot.isAlive && dist < 14f) {
      val barWidth = width * 0.9f
      val barHeight = 6f
      val barY = top - 18f

      smoothPaint.color = Color.argb(160, 0, 0, 0)
      canvas.drawRect(cx - barWidth / 2f, barY, cx + barWidth / 2f, barY + barHeight, smoothPaint)

      val hpRatio = (bot.health.toFloat() / bot.maxHealth.toFloat()).coerceIn(0f, 1f)
      smoothPaint.color = if (isRed) Color.rgb(239, 68, 68) else Color.rgb(34, 197, 94)
      canvas.drawRect(cx - barWidth / 2f, barY, cx - barWidth / 2f + (barWidth * hpRatio), barY + barHeight, smoothPaint)

      paint.color = if (isRed) Color.rgb(254, 202, 202) else Color.rgb(186, 230, 253)
      paint.textSize = (width * 0.24f).coerceIn(16f, 24f)
      paint.textAlign = Paint.Align.CENTER
      canvas.drawText(bot.name, cx, barY - 4f, paint)
      paint.textAlign = Paint.Align.LEFT
    }
  }

  private fun renderWorldParticles(
    canvas: Canvas,
    screenWidth: Float,
    screenHeight: Float,
    horizonY: Float,
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    fovRad: Float
  ) {
    val halfFov = fovRad / 2f
    for (p in particles) {
      val dx = p.x - playerX
      val dy = p.y - playerY
      val dist = sqrt(dx * dx + dy * dy)
      if (dist < 0.1f) continue

      var angle = atan2(dy, dx) - playerAngle
      while (angle > PI) angle -= (2 * PI).toFloat()
      while (angle < -PI) angle += (2 * PI).toFloat()

      if (angle < -halfFov || angle > halfFov) continue

      val sx = ((angle + halfFov) / fovRad) * screenWidth
      val pHeight = (screenHeight / dist)
      val sy = horizonY - (p.z - 0.5f) * pHeight

      val renderSize = (p.size * (1f / dist) * 1.5f).coerceIn(2f, 25f)
      val alpha = (p.life * 255).toInt().coerceIn(0, 255)

      smoothPaint.color = (p.color and 0x00FFFFFF) or (alpha shl 24)
      canvas.drawCircle(sx, sy, renderSize, smoothPaint)
    }
  }
}
