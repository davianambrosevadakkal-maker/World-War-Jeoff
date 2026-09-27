package com.example.game.engine

data class Vec2(var x: Float, var y: Float)

data class SpawnPoint(val x: Float, val y: Float, val angle: Float, val isEnemy: Boolean)

/**
 * Desert Warzone Arena: "Operation Sandstorm / Dust Outpost"
 * Open desert terrain with standalone buildings, desert watchtowers,
 * sun-bleached adobe villas, fortified sandbag compounds, and weathered stone arches.
 *
 * Tile Types:
 * 0 = Open Desert Sand
 * 1 = Desert Sandstone Fortress Perimeter & Sand Dune Cliffs
 * 2 = Sun-bleached Adobe / Stucco Village House Wall
 * 3 = Weathered Ancient Desert Stone Arch / Red Clay Masonry
 * 4 = Military Outpost Corrugated Desert Sheet-Metal Shed
 * 5 = Sandbag Trench & Low Bunker Fortification
 * 6 = Desert Cache / Desert Supply Depot
 */
class TacticalMap {
  val width = 24
  val height = 24

  val grid: Array<IntArray> = arrayOf(
    // Outer perimeter = Sandstone boundary cliffs & fortress perimeter (1)
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
    // North-West open desert with small compound (Building 1)
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,0,0,0,4,4,0,0,1),
    intArrayOf(1,0,2,2,2,0,0,0,0,0,2,0,2,0,0,0,0,0,0,4,4,0,0,1),
    intArrayOf(1,0,2,0,2,0,0,0,0,0,2,0,2,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,2,2,2,0,0,0,0,0,2,2,2,0,0,0,0,0,0,0,0,0,0,1),
    // Sandbag barricades in open desert path
    intArrayOf(1,0,0,0,0,0,0,5,5,0,0,0,0,0,0,5,5,0,0,0,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,5,0,0,0,0,0,0,0,0,5,0,0,0,0,0,0,1),
    // Central Desert Bazaar / Ruins Compound (Building 2 & Ancient Pillars)
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,4,4,0,0,0,0,3,3,0,0,0,0,3,3,0,0,0,0,2,2,2,1),
    intArrayOf(1,0,4,4,0,0,0,0,3,0,0,0,0,0,0,3,0,0,0,0,2,0,2,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,6,0,0,6,0,0,0,0,0,0,2,2,2,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,6,0,0,6,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,3,0,0,0,0,0,0,3,0,0,0,0,4,4,0,1),
    intArrayOf(1,0,2,2,2,0,0,0,3,3,0,0,0,0,3,3,0,0,0,0,4,4,0,1),
    intArrayOf(1,0,2,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
    // South Desert Ruins & Watchtower (Building 3 & Outposts)
    intArrayOf(1,0,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0,5,5,0,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,5,5,0,0,0,0,0,0,0,0,5,5,0,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,2,2,2,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,2,0,2,0,0,0,0,0,0,2,2,2,0,1),
    intArrayOf(1,0,4,4,0,0,0,0,0,0,2,0,2,0,0,0,0,0,0,2,0,2,0,1),
    intArrayOf(1,0,4,4,0,0,0,0,0,0,2,2,2,0,0,0,0,0,0,2,2,2,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
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
    SpawnPoint(22.0f, 22.0f, 3.92f, true),
    SpawnPoint(22.0f, 4.5f, 2.35f, true),
    SpawnPoint(4.5f, 22.0f, 5.49f, true),
    SpawnPoint(12.0f, 18.5f, 4.71f, true),
    SpawnPoint(16.5f, 8.5f, 3.14f, true)
  )

  val patrolWaypoints = listOf(
    Vec2(4.5f, 4.5f), Vec2(12.0f, 3.0f), Vec2(19.0f, 4.5f),
    Vec2(5.0f, 12.0f), Vec2(12.0f, 12.0f), Vec2(19.0f, 12.0f),
    Vec2(4.5f, 19.5f), Vec2(12.0f, 21.0f), Vec2(19.5f, 19.5f),
    Vec2(8.0f, 8.0f), Vec2(16.0f, 8.0f), Vec2(8.0f, 16.0f), Vec2(16.0f, 16.0f)
  )
}
