package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class TacticalAudioSystem {
  private val sampleRate = 22050
  private var isInitialized = false
  private var audioTrack: AudioTrack? = null
  private val soundQueue = ConcurrentLinkedQueue<ShortArray>()
  private var isRunning = false
  private val audioScope = CoroutineScope(Dispatchers.Default)

  var isMuted = false
  var sfxVolume = 1.0f

  fun initialize() {
    if (isInitialized) return
    try {
      val minBufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )

      val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

      val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

      val format = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .build()

      audioTrack = AudioTrack.Builder()
        .setAudioAttributes(attributes)
        .setAudioFormat(format)
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrack?.play()
      isRunning = true
      isInitialized = true

      // Start audio mixing loop
      audioScope.launch {
        runAudioLoop()
      }
    } catch (e: Exception) {
      Log.e("TacticalAudio", "Error initializing AudioTrack: ${e.message}")
    }
  }

  private fun runAudioLoop() {
    val mixBuffer = ShortArray(512)
    val floatMixer = FloatArray(512)

    val activeSounds = mutableListOf<SoundPlayback>()

    while (isRunning) {
      // Dequeue newly requested sounds
      while (true) {
        val sound = soundQueue.poll() ?: break
        if (activeSounds.size < 12) {
          activeSounds.add(SoundPlayback(sound, 0))
        }
      }

      if (activeSounds.isEmpty()) {
        try {
          Thread.sleep(8)
        } catch (_: InterruptedException) {
          break
        }
        continue
      }

      // Mix active sounds into float buffer
      floatMixer.fill(0f)
      val iterator = activeSounds.iterator()
      while (iterator.hasNext()) {
        val playback = iterator.next()
        val data = playback.data
        var readIndex = playback.index

        for (i in 0 until 512) {
          if (readIndex < data.size) {
            floatMixer[i] += data[readIndex].toFloat()
            readIndex++
          } else {
            break
          }
        }
        playback.index = readIndex
        if (readIndex >= data.size) {
          iterator.remove()
        }
      }

      // Convert and clamp to PCM 16-bit
      val volume = if (isMuted) 0f else sfxVolume
      for (i in 0 until 512) {
        val sample = (floatMixer[i] * volume).coerceIn(-32767f, 32767f).toInt()
        mixBuffer[i] = sample.toShort()
      }

      audioTrack?.write(mixBuffer, 0, 512)
    }
  }

  private class SoundPlayback(val data: ShortArray, var index: Int)

  private fun playRawSound(samples: ShortArray) {
    if (isMuted || !isInitialized) return
    soundQueue.offer(samples)
  }

  // --- Tactical Sound Generators ---

  fun playGunfire(weaponId: String, isSilenced: Boolean) {
    audioScope.launch {
      val durationMs = if (isSilenced) 80 else when (weaponId) {
        "solaris_50" -> 350
        "goliath_12" -> 220
        "vortex_9" -> 90
        "phantom_x" -> 160
        else -> 140
      }
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val env = exp(-t * (if (isSilenced) 35f else 18f))

        val noise = (Random.nextFloat() * 2f - 1f)

        val soundVal: Float = when {
          isSilenced -> {
            // Soft gas puff + high click
            val pop = sin(2f * PI.toFloat() * 120f * t)
            (pop * 0.4f + noise * 0.6f) * env * 22000f
          }
          weaponId == "solaris_50" -> {
            // Massive heavy sniper cannon blast: sub bass 65Hz + punchy noise
            val sub = sin(2f * PI.toFloat() * 65f * t) * (1f - t)
            val crack = noise * exp(-t * 40f)
            (sub * 0.65f + crack * 0.35f) * env * 32000f
          }
          weaponId == "goliath_12" -> {
            // Shotgun blast: heavy chaotic transient + thunder
            val kick = sin(2f * PI.toFloat() * 90f * t)
            (kick * 0.5f + noise * 0.5f) * env * 30000f
          }
          weaponId == "phantom_x" -> {
            // Sci-fi railgun ionization: high frequency chirp down + electro crackle
            val freq = 1400f * exp(-t * 20f) + 180f
            val tone = sin(2f * PI.toFloat() * freq * t)
            (tone * 0.7f + noise * 0.3f) * env * 26000f
          }
          weaponId == "vortex_9" -> {
            // Snappy rapid SMG crack
            val punch = sin(2f * PI.toFloat() * 160f * t)
            (punch * 0.4f + noise * 0.6f) * env * 26000f
          }
          else -> {
            // Kronos-74 standard assault rifle punch
            val punch = sin(2f * PI.toFloat() * 130f * t)
            val mechanical = sin(2f * PI.toFloat() * 450f * t) * exp(-t * 30f)
            (punch * 0.5f + mechanical * 0.2f + noise * 0.3f) * env * 28000f
          }
        }
        buffer[i] = soundVal.coerceIn(-32767f, 32767f).toInt().toShort()
      }
      playRawSound(buffer)
    }
  }

  fun playHitmarker(isHeadshot: Boolean) {
    audioScope.launch {
      val durationMs = if (isHeadshot) 65 else 40
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val env = (1f - (i.toFloat() / numSamples))

        val sample = if (isHeadshot) {
          // Sharp metallic ping (1600Hz + 2400Hz harmonics)
          val s1 = sin(2f * PI.toFloat() * 1600f * t)
          val s2 = sin(2f * PI.toFloat() * 2400f * t) * 0.5f
          (s1 + s2) * env * 28000f
        } else {
          // Iconic tactical hitmarker "tik" (880Hz crisp beep)
          val s = sin(2f * PI.toFloat() * 950f * t)
          s * env * 24000f
        }
        buffer[i] = sample.coerceIn(-32767f, 32767f).toInt().toShort()
      }
      playRawSound(buffer)
    }
  }

  fun playKillConfirmed() {
    audioScope.launch {
      val durationMs = 120
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val env = (1f - (i.toFloat() / numSamples))
        // Rising tactical chime: 520Hz to 840Hz
        val freq = 520f + 320f * (i.toFloat() / numSamples)
        val s = sin(2f * PI.toFloat() * freq * t)
        buffer[i] = (s * env * 26000f).toInt().toShort()
      }
      playRawSound(buffer)
    }
  }

  fun playReloadSound(stage: Int) {
    audioScope.launch {
      val durationMs = 70
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val env = exp(-t * 40f)
        val noise = Random.nextFloat() * 2f - 1f

        val toneFreq = when (stage) {
          0 -> 600f // Mag out clink
          1 -> 400f // Mag in snap
          else -> 800f // Bolt release rack
        }
        val click = sin(2f * PI.toFloat() * toneFreq * t) * 0.4f + noise * 0.6f
        buffer[i] = (click * env * 22000f).toInt().toShort()
      }
      playRawSound(buffer)
    }
  }

  fun playExplosion() {
    audioScope.launch {
      val durationMs = 500
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val env = exp(-t * 6f)
        val noise = (Random.nextFloat() * 2f - 1f)
        val sub = sin(2f * PI.toFloat() * 45f * t) * 0.7f

        val s = (sub + noise * 0.5f) * env * 32000f
        buffer[i] = s.coerceIn(-32767f, 32767f).toInt().toShort()
      }
      playRawSound(buffer)
    }
  }

  fun playTacticalRadioCallout(type: String) {
    audioScope.launch {
      // Radio squelch / 2-tone military alert
      val durationMs = 180
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)

      val half = numSamples / 2
      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val freq = if (i < half) 740f else 1100f
        val tone = sin(2f * PI.toFloat() * freq * t)
        val radioNoise = (Random.nextFloat() * 0.15f)
        val env = 1f - (i.toFloat() / numSamples) * 0.3f
        buffer[i] = ((tone * 0.85f + radioNoise) * env * 20000f).toInt().toShort()
      }
      playRawSound(buffer)
    }
  }

  fun playFootstep() {
    audioScope.launch {
      val durationMs = 45
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val env = exp(-t * 50f)
        val noise = (Random.nextFloat() * 2f - 1f)
        val thump = sin(2f * PI.toFloat() * 85f * t)
        buffer[i] = ((thump * 0.5f + noise * 0.5f) * env * 14000f).toInt().toShort()
      }
      playRawSound(buffer)
    }
  }

  fun playSlide() {
    audioScope.launch {
      val durationMs = 260
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val env = (1f - (i.toFloat() / numSamples)) * (if (i < 500) i / 500f else 1f)
        val noise = (Random.nextFloat() * 2f - 1f)
        buffer[i] = (noise * env * 18000f).toInt().toShort()
      }
      playRawSound(buffer)
    }
  }

  fun release() {
    isRunning = false
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (_: Exception) {}
    audioTrack = null
    isInitialized = false
  }
}
