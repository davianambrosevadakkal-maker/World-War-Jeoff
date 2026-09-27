package com.example.game.engine

data class Vec2(var x: Float, var y: Float)

data class SpawnPoint(val x: Float, val y: Float, val angle: Float, val isEnemy: Boolean)

class TacticalMap {
  val width = 24
  val height = 24

  // 0 = Empty floor
  // 1 = Tactical Concrete Wall
  // 2 = Hazard Shipping Container (Caution Stripes)
  // 3 = Cyber Military Server Terminal (Cyan glow)
  // 4 = Heavy Steel Blast Door
  // 5 = Weathered Brick with Rebar
  // 6 = Ammo Resupply Bunker
  val grid: Array<IntArray> = arrayOf(
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,1),
    intArrayOf(1,0,2,2,0,0,1,0,3,3,0,0,0,0,3,3,1,0,0,2,2,0,0,1),
    intArrayOf(1,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,0,0,4,4,0,0,1,1,0,0,0,0,1,1,0,0,4,4,0,0,0,1),
    intArrayOf(1,1,0,0,4,4,0,0,1,0,0,0,0,0,0,1,0,0,4,4,0,0,1,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,3,0,0,0,2,2,0,0,0,0,0,0,0,0,2,2,0,0,0,3,0,1),
    intArrayOf(1,0,3,0,0,0,2,2,0,0,5,5,5,5,0,0,2,2,0,0,0,3,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,5,0,0,5,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,0,0,6,6,0,0,0,0,0,0,0,0,0,0,0,0,6,6,0,0,0,1),
    intArrayOf(1,0,0,0,6,6,0,0,0,0,0,0,0,0,0,0,0,0,6,6,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,5,0,0,5,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,3,0,0,0,2,2,0,0,5,5,5,5,0,0,2,2,0,0,0,3,0,1),
    intArrayOf(1,0,3,0,0,0,2,2,0,0,0,0,0,0,0,0,2,2,0,0,0,3,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,1,0,0,4,4,0,0,1,0,0,0,0,0,0,1,0,0,4,4,0,0,1,1),
    intArrayOf(1,0,0,0,4,4,0,0,1,1,0,0,0,0,1,1,0,0,4,4,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,0,0,1),
    intArrayOf(1,0,2,2,0,0,1,0,3,3,0,0,0,0,3,3,1,0,0,2,2,0,0,1),
    intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
  )

  fun isWall(x: Float, y: Float): Boolean {
    val ix = x.toInt()
    val iy = y.toInt()
    if (ix !in 0 until width || iy !in 0 until height) return true
    return grid[iy][ix] > 0
  }

  fun getTile(x: Int, y: Int): Int {
    if (x !in 0 until width || y !in 0 until height) return 1
    return grid[y][x]
  }

  val playerSpawn = SpawnPoint(1.5f, 1.5f, 0.785f, false)

  val enemySpawns = listOf(
    SpawnPoint(21.5f, 21.5f, 3.92f, true),
    SpawnPoint(21.5f, 4.5f, 2.35f, true),
    SpawnPoint(4.5f, 21.5f, 5.49f, true),
    SpawnPoint(12.0f, 18.5f, 4.71f, true),
    SpawnPoint(16.5f, 12.5f, 3.14f, true)
  )

  val patrolWaypoints = listOf(
    Vec2(4f, 4f), Vec2(12f, 4f), Vec2(20f, 4f),
    Vec2(4f, 12f), Vec2(12f, 12f), Vec2(20f, 12f),
    Vec2(4f, 20f), Vec2(12f, 20f), Vec2(20f, 20f),
    Vec2(8f, 8f), Vec2(16f, 8f), Vec2(8f, 16f), Vec2(16f, 16f)
  )
}
