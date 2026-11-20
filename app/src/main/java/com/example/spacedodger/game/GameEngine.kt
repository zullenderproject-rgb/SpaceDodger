package com.example.spacedodger.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.hypot
import kotlin.random.Random

class GameEngine {

    var gameState by mutableStateOf(GameState.START_MENU)
        private set

    var difficulty by mutableStateOf(Difficulty.NORMAL)
        private set

    var score by mutableIntStateOf(0)
        private set

    var starsCollectedSession by mutableIntStateOf(0)
        private set

    var highScore by mutableIntStateOf(0)
        private set

    var totalGamesPlayed by mutableIntStateOf(0)
        private set

    var totalStarsCollected by mutableIntStateOf(0)
        private set

    var screenWidth by mutableFloatStateOf(1080f)
    var screenHeight by mutableFloatStateOf(1920f)

    // Trigger for Canvas redraw (updated once per frame, avoiding snapshot overhead for 100s of objects)
    var frameTick by mutableLongStateOf(0L)
        private set

    val player = Player()

    // Plain ArrayLists for high-performance zero-overhead 120 FPS game loop
    val asteroids = ArrayList<Asteroid>(64)
    val collectibles = ArrayList<Collectible>(32)
    val particles = ArrayList<Particle>(256)
    val backgroundStars = ArrayList<BackgroundStar>(128)

    var timeSlowTimer by mutableFloatStateOf(0f)
        private set

    var screenShakeAmount by mutableFloatStateOf(0f)
        private set

    private var nextEntityId = 1L
    private var lastAsteroidSpawnTime = 0f
    private var lastCollectibleSpawnTime = 0f

    fun initScreenSize(width: Float, height: Float) {
        if (screenWidth != width || screenHeight != height) {
            screenWidth = width
            screenHeight = height
            player.x = width / 2f
            player.y = height - 250f

            if (backgroundStars.isEmpty()) {
                initBackgroundStars()
            }
        }
    }

    private fun initBackgroundStars() {
        backgroundStars.clear()
        for (i in 0..70) {
            backgroundStars.add(
                BackgroundStar(
                    x = Random.nextFloat() * screenWidth,
                    y = Random.nextFloat() * screenHeight,
                    radius = Random.nextFloat() * 3.5f + 1f,
                    speedY = Random.nextFloat() * 150f + 50f,
                    alpha = Random.nextFloat() * 0.7f + 0.3f
                )
            )
        }
    }

    fun changeDifficulty(diff: Difficulty) {
        difficulty = diff
    }

    fun startGame() {
        score = 0
        starsCollectedSession = 0
        player.lives = 3
        player.hasShield = false
        player.x = screenWidth / 2f
        player.y = screenHeight - 250f
        asteroids.clear()
        collectibles.clear()
        particles.clear()
        timeSlowTimer = 0f
        screenShakeAmount = 0f
        totalGamesPlayed++
        gameState = GameState.PLAYING
    }

    fun pauseGame() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED
        }
    }

    fun resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING
        }
    }

    fun openHighScores() {
        gameState = GameState.HIGH_SCORES
    }

    fun openMenu() {
        gameState = GameState.START_MENU
    }

    fun movePlayerTo(targetX: Float) {
        if (gameState == GameState.PLAYING) {
            val clampedX = targetX.coerceIn(player.width / 2f, screenWidth - player.width / 2f)
            player.x = clampedX

            // Emit thruster particles (capped)
            if (particles.size < 180 && Random.nextFloat() < 0.5f) {
                particles.add(
                    Particle(
                        x = player.x + Random.nextFloat() * 20f - 10f,
                        y = player.y + player.height / 2f,
                        vx = Random.nextFloat() * 40f - 20f,
                        vy = Random.nextFloat() * 200f + 300f,
                        color = if (Random.nextBoolean()) Color(0xFFFF9800) else Color(0xFFFFEB3B),
                        radius = Random.nextFloat() * 5f + 2.5f,
                        maxAge = 0.25f
                    )
                )
            }
        }
    }

    fun update(dt: Float) {
        if (dt <= 0f) return

        // Update Background Stars always for background parallax
        val starCount = backgroundStars.size
        for (i in 0 until starCount) {
            val star = backgroundStars[i]
            star.y += star.speedY * dt
            if (star.y > screenHeight) {
                star.y = 0f
                star.x = Random.nextFloat() * screenWidth
            }
        }

        // Decay Screen Shake
        if (screenShakeAmount > 0f) {
            screenShakeAmount = (screenShakeAmount - dt * 25f).coerceAtLeast(0f)
        }

        if (gameState == GameState.PLAYING) {
            // Update Power-up Timers
            if (timeSlowTimer > 0f) {
                timeSlowTimer = (timeSlowTimer - dt).coerceAtLeast(0f)
            }

            // Spawn Logic
            lastAsteroidSpawnTime += dt
            val baseInterval = (1.2f - (score * 0.02f)).coerceAtLeast(0.35f)
            val spawnInterval = baseInterval * difficulty.spawnIntervalMult
            if (lastAsteroidSpawnTime >= spawnInterval) {
                lastAsteroidSpawnTime = 0f
                spawnAsteroid()
            }

            lastCollectibleSpawnTime += dt
            if (lastCollectibleSpawnTime >= 5.5f) {
                lastCollectibleSpawnTime = 0f
                spawnCollectible()
            }

            // Speed multiplier
            val speedMultiplier = (if (timeSlowTimer > 0f) 0.4f else 1.0f) * difficulty.speedMult

            // Update Asteroids
            var i = 0
            while (i < asteroids.size) {
                val asteroid = asteroids[i]
                asteroid.y += asteroid.speedY * speedMultiplier * dt
                asteroid.rotation += asteroid.rotationSpeed * dt

                // Check collision with player
                val dist = hypot(asteroid.x - player.x, asteroid.y - player.y)
                val collisionRadius = asteroid.radius + player.width * 0.35f
                if (dist < collisionRadius) {
                    spawnExplosion(asteroid.x, asteroid.y, Color(0xFFFF5722), 20)
                    asteroids.removeAt(i)
                    screenShakeAmount = 14f

                    if (player.hasShield) {
                        player.hasShield = false
                        spawnExplosion(player.x, player.y, Color(0xFF00E5FF), 15)
                    } else {
                        player.lives--
                        if (player.lives <= 0) {
                            gameState = GameState.GAME_OVER
                            if (score > highScore) {
                                highScore = score
                            }
                        }
                    }
                    continue
                }

                // Remove if off screen
                if (asteroid.y - asteroid.radius > screenHeight) {
                    asteroids.removeAt(i)
                    score += 1
                    continue
                }
                i++
            }

            // Update Collectibles
            var j = 0
            while (j < collectibles.size) {
                val item = collectibles[j]
                item.y += item.speedY * dt

                val dist = hypot(item.x - player.x, item.y - player.y)
                if (dist < item.radius + player.width * 0.4f) {
                    when (item.type) {
                        CollectibleType.STAR -> {
                            score += 10
                            starsCollectedSession++
                            totalStarsCollected++
                            spawnExplosion(item.x, item.y, Color(0xFFFFD700), 12)
                        }
                        CollectibleType.SHIELD -> {
                            player.hasShield = true
                            spawnExplosion(item.x, item.y, Color(0xFF00E5FF), 12)
                        }
                        CollectibleType.SLOW_TIME -> {
                            timeSlowTimer = 5.0f
                            spawnExplosion(item.x, item.y, Color(0xFFE040FB), 12)
                        }
                    }
                    collectibles.removeAt(j)
                    continue
                }

                if (item.y - item.radius > screenHeight) {
                    collectibles.removeAt(j)
                    continue
                }
                j++
            }

            // Update Particles
            var k = 0
            while (k < particles.size) {
                val p = particles[k]
                p.currentAge += dt
                if (p.currentAge >= p.maxAge) {
                    particles.removeAt(k)
                    continue
                }
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.alpha = 1f - (p.currentAge / p.maxAge)
                k++
            }
        }

        // Update frame tick to invalidate Canvas efficiently
        frameTick++
    }

    private fun spawnAsteroid() {
        if (asteroids.size >= 30) return // Cap max asteroids for performance
        val radius = Random.nextFloat() * 38f + 32f
        val x = Random.nextFloat() * (screenWidth - radius * 2) + radius
        val speedY = Random.nextFloat() * 250f + 300f + (score * 5f).coerceAtMost(400f)
        val rotSpeed = Random.nextFloat() * 120f - 60f

        asteroids.add(
            Asteroid(
                id = nextEntityId++,
                x = x,
                y = -radius,
                radius = radius,
                speedY = speedY,
                rotationSpeed = rotSpeed
            )
        )
    }

    private fun spawnCollectible() {
        if (collectibles.size >= 5) return
        val type = when (Random.nextFloat()) {
            in 0.0f..0.55f -> CollectibleType.STAR
            in 0.55f..0.8f -> CollectibleType.SHIELD
            else -> CollectibleType.SLOW_TIME
        }
        val radius = 35f
        val x = Random.nextFloat() * (screenWidth - radius * 2) + radius

        collectibles.add(
            Collectible(
                id = nextEntityId++,
                type = type,
                x = x,
                y = -radius
            )
        )
    }

    private fun spawnExplosion(x: Float, y: Float, color: Color, count: Int) {
        val actualCount = count.coerceAtMost(200 - particles.size)
        for (n in 0 until actualCount) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = Random.nextFloat() * 350f + 80f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = color,
                    radius = Random.nextFloat() * 7f + 2.5f,
                    maxAge = Random.nextFloat() * 0.35f + 0.15f
                )
            )
        }
    }
}
