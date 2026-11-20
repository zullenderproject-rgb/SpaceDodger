package com.example.spacedodger.game

import androidx.compose.ui.graphics.Color

enum class GameState {
    START_MENU,
    HIGH_SCORES,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class Difficulty(val label: String, val speedMult: Float, val spawnIntervalMult: Float) {
    NORMAL("Normal", 1.0f, 1.0f),
    HARD("Hard", 1.35f, 0.75f),
    INSANE("Insane", 1.7f, 0.55f)
}

enum class CollectibleType {
    STAR,
    SHIELD,
    SLOW_TIME
}

class Player(
    var x: Float = 0f,
    var y: Float = 0f,
    val width: Float = 100f,
    val height: Float = 120f,
    var lives: Int = 3,
    var hasShield: Boolean = false,
    var shieldTimeLeft: Float = 0f
)

class Asteroid(
    val id: Long,
    var x: Float,
    var y: Float,
    val radius: Float,
    val speedY: Float,
    var rotation: Float = 0f,
    val rotationSpeed: Float
)

class Collectible(
    val id: Long,
    val type: CollectibleType,
    var x: Float,
    var y: Float,
    val radius: Float = 40f,
    val speedY: Float = 350f
)

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var alpha: Float = 1f,
    var radius: Float,
    var maxAge: Float,
    var currentAge: Float = 0f
)

class BackgroundStar(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speedY: Float,
    val alpha: Float
)
