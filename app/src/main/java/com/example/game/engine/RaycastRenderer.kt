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
    for (i in 0 until 10) {
      val angle = (Math.random() * 2 * PI).toFloat()
      val speed = (Math.random() * 2.5f + 1.0f).toFloat()
      particles.add(
        Particle(
          x = x,
          y = y,
          z = z,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          vz = (Math.random() * 2.0f).toFloat(),
          life = 1.0f,
          maxLife = (Math.random() * 0.3f + 0.2f).toFloat(),
          color = if (Math.random() > 0.3) Color.rgb(255, 200, 50) else Color.rgb(255, 100, 30),
          size = 4f
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
          color = if (i % 2 == 0) Color.rgb(255, 80, 20) else Color.rgb(255, 220, 40),
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

    // 1. Draw Atmospheric Ceiling & Tactical Industrial Floor
    renderCeilingAndFloor(canvas, screenWidth, screenHeight, horizonY, muzzleFlashIntensity)

    // Raycast Depth Buffer for Bot occlusion
    val zBuffer = FloatArray(numRays)

    // 2. Cast Rays & Render Walls
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

    // 3. Render 3D Perspective Bots
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

    // 4. Render 3D World Particles
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

  private fun renderCeilingAndFloor(
    canvas: Canvas,
    w: Float,
    h: Float,
    horizon: Float,
    muzzleFlash: Float
  ) {
    // Ceiling: Dark industrial compound with subtle metallic blue tint & glowing light conduits
    paint.shader = LinearGradient(
      0f, 0f, 0f, horizon.coerceAtLeast(1f),
      Color.rgb(10, 15, 26),
      Color.rgb(18, 24, 38),
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, w, horizon, paint)
    paint.shader = null

    // Ceiling industrial light strips
    smoothPaint.color = Color.argb(40, 6, 182, 212)
    val lightY = horizon * 0.35f
    canvas.drawRect(w * 0.2f, lightY, w * 0.8f, lightY + 3f, smoothPaint)

    // Floor: Tactical reinforced bunker floor with depth fog
    val flashAdd = (muzzleFlash * 50).toInt()
    val floorColorTop = Color.rgb(
      (22 + flashAdd).coerceAtMost(255),
      (28 + flashAdd).coerceAtMost(255),
      (40 + flashAdd).coerceAtMost(255)
    )
    val floorColorBottom = Color.rgb(
      (12 + flashAdd / 2).coerceAtMost(255),
      (15 + flashAdd / 2).coerceAtMost(255),
      (22 + flashAdd / 2).coerceAtMost(255)
    )

    paint.shader = LinearGradient(
      0f, horizon, 0f, h,
      floorColorTop,
      floorColorBottom,
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, horizon, w, h, paint)
    paint.shader = null

    // Perspective floor tactical grid lines
    smoothPaint.color = Color.argb(25, 59, 130, 246)
    val numGridLines = 7
    for (g in 1..numGridLines) {
      val t = (g.toFloat() / (numGridLines + 1))
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
    // Sector illumination & distance fog
    val maxDist = 16f
    val fog = (1f - (dist / maxDist)).coerceIn(0.12f, 1.0f)
    val sideDarken = if (hit.isVertical) 0.8f else 1.0f

    // Dynamic muzzle flash adds direct local lighting!
    val flashBoost = muzzleFlash * (1f - (dist / 8f)).coerceAtLeast(0f) * 0.6f
    val finalLight = (fog * sideDarken + flashBoost).coerceIn(0.1f, 1.3f)

    // Base texture color palette per tile type
    var baseR: Int
    var baseG: Int
    var baseB: Int

    when (hit.wallType) {
      2 -> {
        // Caution Shipping Container (Diagonal yellow / black hazard stripes)
        val stripe = ((hit.wallX * 8f) + (top * 0.02f)).toInt() % 2 == 0
        if (stripe) {
          baseR = 234; baseG = 179; baseB = 8 // Caution Yellow
        } else {
          baseR = 30; baseG = 41; baseB = 59 // Dark Steel
        }
      }
      3 -> {
        // Cyber Tech Server Terminal (Glowing cyan circuitry strips)
        val isStrip = hit.wallX in 0.45f..0.55f || hit.wallX in 0.85f..0.95f
        if (isStrip) {
          baseR = 6; baseG = 182; baseB = 212 // Bright Neon Cyan
        } else {
          baseR = 15; baseG = 23; baseB = 42 // Deep Navy Matrix
        }
      }
      4 -> {
        // Heavy Armor Blast Bulkhead (Steel plate with rivets)
        val isBorder = hit.wallX < 0.08f || hit.wallX > 0.92f
        if (isBorder) {
          baseR = 100; baseG = 116; baseB = 139
        } else {
          baseR = 51; baseG = 65; baseB = 85
        }
      }
      5 -> {
        // Weathered Brick / Rebar
        val brickY = ((top + bottom) * 0.05f).toInt() % 2 == 0
        val isMortar = hit.wallX in 0.48f..0.52f || brickY
        if (isMortar) {
          baseR = 120; baseG = 80; baseB = 70
        } else {
          baseR = 180; baseG = 83; baseB = 60
        }
      }
      6 -> {
        // Tactical Ammo Resupply Crate (Military green with white cross)
        baseR = 40; baseG = 75; baseB = 55
      }
      else -> {
        // Reinforced Tactical Concrete
        val panel = hit.wallX in 0.04f..0.96f
        if (panel) {
          baseR = 71; baseG = 85; baseB = 105
        } else {
          baseR = 30; baseG = 41; baseB = 59 // Seam
        }
      }
    }

    // Apply lighting & muzzle flash
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
        continue // Occluded behind wall!
      }

      // Draw Tactical Soldier Sprite
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

    // 1. Legs
    smoothPaint.color = if (isFlashing) Color.argb(alpha, 255, 255, 255) else Color.argb(alpha, 30, 41, 59)
    canvas.drawRect(cx - width * 0.35f, top + h * 0.6f, cx - width * 0.05f, bottom, smoothPaint)
    canvas.drawRect(cx + width * 0.05f, top + h * 0.6f, cx + width * 0.35f, bottom, smoothPaint)

    // 2. Torso with Tactical Vest
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

    // 3. Combat Helmet & Glowing Visor
    smoothPaint.color = Color.argb(alpha, 15, 23, 42) // Carbon helmet
    canvas.drawCircle(cx, top + h * 0.16f, width * 0.25f, smoothPaint)

    // Visor HUD Glow
    val visorColor = if (isRed) Color.argb(alpha, 239, 68, 68) else Color.argb(alpha, 6, 182, 212)
    smoothPaint.color = visorColor
    canvas.drawRoundRect(
      cx - width * 0.16f, top + h * 0.14f,
      cx + width * 0.16f, top + h * 0.20f,
      2f, 2f,
      smoothPaint
    )

    // 4. Weapon Barrel & Muzzle Flash
    smoothPaint.color = Color.argb(alpha, 51, 65, 85)
    canvas.drawRect(cx, top + h * 0.38f, cx + width * 0.65f, top + h * 0.45f, smoothPaint)

    if (bot.isFiring && bot.isAlive) {
      // Bot Muzzle Flash Flare
      smoothPaint.color = Color.argb(230, 255, 200, 50)
      canvas.drawCircle(cx + width * 0.68f, top + h * 0.41f, width * 0.25f, smoothPaint)
      smoothPaint.color = Color.argb(255, 255, 255, 255)
      canvas.drawCircle(cx + width * 0.68f, top + h * 0.41f, width * 0.12f, smoothPaint)
    }

    // 5. Tactical Overhead Callout & Health Gauge (if alive and close)
    if (bot.isAlive && dist < 12f) {
      val barWidth = width * 0.9f
      val barHeight = 6f
      val barY = top - 18f

      // Health background
      smoothPaint.color = Color.argb(160, 0, 0, 0)
      canvas.drawRect(cx - barWidth / 2f, barY, cx + barWidth / 2f, barY + barHeight, smoothPaint)

      // Health fill
      val hpRatio = (bot.health.toFloat() / bot.maxHealth.toFloat()).coerceIn(0f, 1f)
      smoothPaint.color = if (isRed) Color.rgb(239, 68, 68) else Color.rgb(34, 197, 94)
      canvas.drawRect(cx - barWidth / 2f, barY, cx - barWidth / 2f + (barWidth * hpRatio), barY + barHeight, smoothPaint)

      // Bot Name Tag
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
