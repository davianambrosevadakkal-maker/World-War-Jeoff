package com.example.game.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.game.weapons.OpticType
import com.example.game.weapons.Weapon
import com.example.game.weapons.WeaponCamo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class WeaponRenderer {
  private val paint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.FILL
  }

  private val strokePaint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
  }

  // Animation states
  var swayX: Float = 0f
  var swayY: Float = 0f
  var bobX: Float = 0f
  var bobY: Float = 0f
  var recoilOffset: Float = 0f
  var recoilClimb: Float = 0f
  var reloadProgress: Float = 0f // 0 = idle, >0 = reloading
  var isFiringFlash: Boolean = false
  var flashIntensity: Float = 0f

  fun updateAnimations(
    dt: Float,
    isMoving: Boolean,
    isSprinting: Boolean,
    isSliding: Boolean,
    adsProgress: Float, // 0 = hipfire, 1 = full ADS
    lookDeltaX: Float,
    lookDeltaY: Float,
    walkTime: Float
  ) {
    // 1. Natural breathing & movement bobbing
    val bobFreq = if (isSprinting) 14f else 8f
    val bobAmount = if (isSprinting) 18f else if (isMoving) 10f else 3f
    val adsDampen = (1f - adsProgress * 0.85f)

    bobX = cos(walkTime * bobFreq) * bobAmount * adsDampen
    bobY = kotlin.math.abs(sin(walkTime * bobFreq)) * bobAmount * 1.2f * adsDampen

    // 2. Look inertia sway
    val targetSwayX = (-lookDeltaX * 14f).coerceIn(-40f, 40f) * adsDampen
    val targetSwayY = (-lookDeltaY * 14f).coerceIn(-30f, 30f) * adsDampen
    swayX += (targetSwayX - swayX) * 12f * dt
    swayY += (targetSwayY - swayY) * 12f * dt

    // 3. Recoil spring recovery
    recoilOffset = (recoilOffset - 8f * dt).coerceAtLeast(0f)
    recoilClimb = (recoilClimb - 12f * dt).coerceAtLeast(0f)

    // 4. Muzzle flash decay
    if (flashIntensity > 0f) {
      flashIntensity = (flashIntensity - 16f * dt).coerceAtLeast(0f)
      isFiringFlash = flashIntensity > 0.05f
    }
  }

  fun triggerRecoil(kick: Float) {
    recoilOffset = (recoilOffset + kick * 50f).coerceAtMost(55f)
    recoilClimb = (recoilClimb + kick * 35f).coerceAtMost(40f)
    flashIntensity = 1.0f
    isFiringFlash = true
  }

  fun renderWeapon(
    canvas: Canvas,
    screenWidth: Float,
    screenHeight: Float,
    weapon: Weapon,
    adsProgress: Float,
    isSprinting: Boolean,
    isSliding: Boolean
  ) {
    canvas.save()

    // Base gun anchor (Hipfire bottom-right vs ADS screen-center)
    val hipX = screenWidth * 0.68f
    val hipY = screenHeight * 0.76f

    // When ADS is active, sight aligns perfectly with center screen!
    val adsX = screenWidth * 0.50f
    val adsY = screenHeight * 0.58f

    var targetX = hipX + (adsX - hipX) * adsProgress + swayX + bobX
    var targetY = hipY + (adsY - hipY) * adsProgress + swayY + bobY - recoilClimb

    // Tactical sprint posture: raise weapon up and tilt
    if (isSprinting && adsProgress < 0.2f) {
      targetX += screenWidth * 0.06f
      targetY -= screenHeight * 0.08f
      canvas.rotate(-22f, targetX, targetY)
    }

    // Tactical slide: tilt weapon inward
    if (isSliding && adsProgress < 0.2f) {
      targetY += screenHeight * 0.05f
      canvas.rotate(14f, targetX, targetY)
    }

    // Reload animation: drop weapon and bring back up
    if (reloadProgress > 0f && reloadProgress < 1.0f) {
      val drop = sin(reloadProgress * PI.toFloat()) * screenHeight * 0.22f
      targetY += drop
    }

    // Draw ADS Scope Overlay / Peripheral Depth-of-Field if high ADS
    if (adsProgress > 0.2f) {
      renderAdsOpticVignette(canvas, screenWidth, screenHeight, weapon.selectedOptic, adsProgress)
    }

    // Draw First-Person 3D Weapon Geometry
    drawWeaponModel(
      canvas = canvas,
      cx = targetX,
      cy = targetY,
      weapon = weapon,
      adsProgress = adsProgress,
      scale = (screenHeight / 900f).coerceIn(0.7f, 1.4f)
    )

    // Draw Muzzle Flash Flare
    if (isFiringFlash && adsProgress < 0.95f) {
      drawMuzzleFlash(canvas, targetX, targetY - 140f, weapon)
    }

    canvas.restore()
  }

  private fun drawWeaponModel(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    weapon: Weapon,
    adsProgress: Float,
    scale: Float
  ) {
    val camo = weapon.selectedCamo
    val camoPrimary = camo.primaryColor.toInt()
    val camoAccent = camo.accentColor.toInt()

    val kickback = recoilOffset * scale

    // 1. Handguard & Barrel
    paint.color = Color.rgb(20, 24, 33)
    val barrelLength = when (weapon.id) {
      "solaris_50" -> 360f * scale
      "vortex_9" -> 180f * scale
      "goliath_12" -> 220f * scale
      else -> 260f * scale
    }
    val barrelTop = cy - barrelLength + kickback
    val barrelWidth = 24f * scale

    // Shaded metallic barrel
    paint.shader = LinearGradient(
      cx - barrelWidth / 2f, 0f, cx + barrelWidth / 2f, 0f,
      Color.rgb(30, 41, 59), Color.rgb(15, 23, 42),
      Shader.TileMode.CLAMP
    )
    canvas.drawRect(cx - barrelWidth / 2f, barrelTop, cx + barrelWidth / 2f, cy, paint)
    paint.shader = null

    // Muzzle Brake / Compensator / Suppressor
    if (weapon.selectedMuzzle.isSilenced) {
      // Cylindrical carbon suppressor
      paint.color = Color.rgb(15, 23, 42)
      canvas.drawRoundRect(
        cx - barrelWidth * 0.9f, barrelTop - 45f * scale,
        cx + barrelWidth * 0.9f, barrelTop,
        6f, 6f, paint
      )
    } else {
      // Crown muzzle brake with ventilation ports
      paint.color = Color.rgb(71, 85, 105)
      canvas.drawRect(
        cx - barrelWidth * 0.8f, barrelTop - 25f * scale,
        cx + barrelWidth * 0.8f, barrelTop,
        paint
      )
    }

    // 2. Weapon Receiver & Tactical Body with Camo Finish
    val receiverWidth = when (weapon.id) {
      "vortex_9" -> 50f * scale
      "goliath_12" -> 70f * scale
      else -> 60f * scale
    }
    val receiverHeight = 180f * scale
    val receiverTop = cy - 80f * scale + kickback

    // Camo Shader
    paint.color = camoPrimary
    canvas.drawRoundRect(
      cx - receiverWidth / 2f, receiverTop,
      cx + receiverWidth / 2f, receiverTop + receiverHeight,
      12f * scale, 12f * scale, paint
    )

    // Camo Accent Carbon/Gold Stripe
    paint.color = camoAccent
    canvas.drawRect(
      cx - receiverWidth * 0.45f, receiverTop + 30f * scale,
      cx + receiverWidth * 0.45f, receiverTop + 55f * scale,
      paint
    )

    // Picatinny Top Rail
    paint.color = Color.rgb(30, 41, 59)
    canvas.drawRect(
      cx - 16f * scale, receiverTop - 12f * scale,
      cx + 16f * scale, receiverTop,
      paint
    )

    // 3. Magazine / Drum
    val magWidth = if (weapon.id == "goliath_12") 85f * scale else 35f * scale
    val magHeight = 110f * scale
    val magY = receiverTop + receiverHeight * 0.7f

    // Magazine Drop Animation during reload
    val magDropOffset = if (reloadProgress in 0.15f..0.7f) {
      sin((reloadProgress - 0.15f) / 0.55f * PI.toFloat()) * 180f * scale
    } else 0f

    paint.color = Color.rgb(20, 24, 35)
    canvas.drawRoundRect(
      cx - magWidth / 2f, magY + magDropOffset,
      cx + magWidth / 2f, magY + magHeight + magDropOffset,
      8f * scale, 8f * scale, paint
    )

    // 4. Optic Sight / Sights
    drawOpticSight(
      canvas = canvas,
      cx = cx,
      cy = receiverTop - 10f * scale,
      optic = weapon.selectedOptic,
      adsProgress = adsProgress,
      scale = scale
    )
  }

  private fun drawOpticSight(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    optic: OpticType,
    adsProgress: Float,
    scale: Float
  ) {
    when (optic) {
      OpticType.IRON_SIGHTS -> {
        // Tactical Iron Sight post with illuminated tritium dots
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(cx - 5f * scale, cy - 35f * scale, cx + 5f * scale, cy, paint)
        // Green Tritium Night Dot
        paint.color = Color.rgb(34, 197, 94)
        canvas.drawCircle(cx, cy - 32f * scale, 3.5f * scale, paint)
      }
      OpticType.VIPER_REFLEX -> {
        // Red Dot Optic Housing
        val sightRadius = (36f * scale) + (adsProgress * 20f * scale)
        paint.color = Color.rgb(15, 23, 42)
        strokePaint.color = Color.rgb(51, 65, 85)
        strokePaint.strokeWidth = 5f * scale
        canvas.drawCircle(cx, cy - sightRadius * 0.6f, sightRadius, strokePaint)

        // Glass tint
        paint.color = Color.argb(45, 6, 182, 212)
        canvas.drawCircle(cx, cy - sightRadius * 0.6f, sightRadius - 4f, paint)

        // Glowing Red Reflex Dot
        paint.color = Color.rgb(239, 68, 68)
        val dotRadius = (4.5f * scale) * (1f + adsProgress * 0.5f)
        canvas.drawCircle(cx, cy - sightRadius * 0.6f, dotRadius, paint)
      }
      OpticType.APEX_HOLO -> {
        // Holographic Optic Rectangular Frame
        val w = (65f * scale) + (adsProgress * 30f * scale)
        val h = (50f * scale) + (adsProgress * 25f * scale)
        val top = cy - h - 5f

        paint.color = Color.rgb(15, 23, 42)
        strokePaint.color = Color.rgb(71, 85, 105)
        strokePaint.strokeWidth = 6f * scale
        canvas.drawRoundRect(cx - w / 2f, top, cx + w / 2f, top + h, 8f, 8f, strokePaint)

        // Cyan Holo Reticle Ring & Center Chevron
        val reticleCenterY = top + h / 2f
        strokePaint.color = Color.rgb(6, 182, 212)
        strokePaint.strokeWidth = 2.5f * scale
        canvas.drawCircle(cx, reticleCenterY, 14f * scale, strokePaint)
        // Center dot
        paint.color = Color.rgb(6, 182, 212)
        canvas.drawCircle(cx, reticleCenterY, 3f * scale, paint)
      }
      OpticType.RECON_4X, OpticType.THERMAL_SNIPER -> {
        // Heavy sniper optic scope bell
        val scopeRadius = (45f * scale) + (adsProgress * 35f * scale)
        strokePaint.color = Color.rgb(15, 23, 42)
        strokePaint.strokeWidth = 8f * scale
        canvas.drawCircle(cx, cy - scopeRadius, scopeRadius, strokePaint)

        // Amber reticle crosshairs
        strokePaint.color = Color.rgb(245, 158, 11)
        strokePaint.strokeWidth = 2f * scale
        canvas.drawLine(cx - scopeRadius, cy - scopeRadius, cx + scopeRadius, cy - scopeRadius, strokePaint)
        canvas.drawLine(cx, cy - scopeRadius * 2, cx, cy, strokePaint)
      }
    }
  }

  private fun renderAdsOpticVignette(
    canvas: Canvas,
    w: Float,
    h: Float,
    optic: OpticType,
    adsProgress: Float
  ) {
    val alpha = (adsProgress * 230).toInt().coerceIn(0, 230)
    val cx = w / 2f
    val cy = h / 2f

    if (optic == OpticType.THERMAL_SNIPER || optic == OpticType.RECON_4X) {
      // True Sniper Scope Blackout Mask with circular aperture
      val scopeRadius = (h * 0.44f).coerceAtLeast(100f)
      paint.shader = RadialGradient(
        cx, cy, scopeRadius,
        intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 10, 10, 15), Color.argb(alpha, 0, 0, 0)),
        floatArrayOf(0.75f, 0.95f, 1.0f),
        Shader.TileMode.CLAMP
      )
      canvas.drawRect(0f, 0f, w, h, paint)
      paint.shader = null

      // Crisp sniper reticle crosshair in center
      strokePaint.color = Color.argb(alpha, 255, 60, 40)
      strokePaint.strokeWidth = 2.5f
      canvas.drawLine(0f, cy, w, cy, strokePaint)
      canvas.drawLine(cx, 0f, cx, h, strokePaint)
    } else {
      // Peripheral Depth-of-Field Blur Vignette
      paint.shader = RadialGradient(
        cx, cy, w * 0.6f,
        intArrayOf(Color.TRANSPARENT, Color.argb((alpha * 0.6f).toInt(), 0, 0, 0)),
        floatArrayOf(0.4f, 1.0f),
        Shader.TileMode.CLAMP
      )
      canvas.drawRect(0f, 0f, w, h, paint)
      paint.shader = null
    }
  }

  private fun drawMuzzleFlash(canvas: Canvas, x: Float, y: Float, weapon: Weapon) {
    val flashSize = when (weapon.id) {
      "solaris_50" -> 130f
      "goliath_12" -> 110f
      "phantom_x" -> 85f
      else -> 75f
    }

    // Outer flame flare
    val outerColor = if (weapon.id == "phantom_x") Color.argb(220, 6, 182, 212) else Color.argb(230, 255, 140, 20)
    paint.shader = RadialGradient(
      x, y, flashSize,
      intArrayOf(Color.WHITE, outerColor, Color.TRANSPARENT),
      floatArrayOf(0.1f, 0.5f, 1.0f),
      Shader.TileMode.CLAMP
    )
    canvas.drawCircle(x, y, flashSize, paint)
    paint.shader = null

    // Multi-pointed starburst flare
    val starColor = if (weapon.id == "phantom_x") Color.rgb(186, 230, 253) else Color.rgb(255, 240, 180)
    paint.color = starColor
    val path = Path()
    for (i in 0 until 8) {
      val angle = (i * PI / 4).toFloat()
      val r = if (i % 2 == 0) flashSize * 1.3f else flashSize * 0.35f
      val px = x + cos(angle) * r
      val py = y + sin(angle) * r
      if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    canvas.drawPath(path, paint)
  }
}
