package com.example.spacedodger.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Reusable Path instances outside DrawScope to prevent 100% of GC pressure & stutter
private val reusableShipPath = Path()
private val reusableStarPath = Path()
private val reusableAsteroidPath = Path()

@Composable
fun SpaceDodgerGameScreen() {
    val engine = remember { GameEngine() }

    // Game loop running at 60 / 120 FPS
    LaunchedEffect(Unit) {
        var lastTimeNanos = System.nanoTime()
        while (true) {
            withFrameNanos { frameTimeNanos ->
                val dt = ((frameTimeNanos - lastTimeNanos) / 1_000_000_000f).coerceAtMost(0.04f)
                lastTimeNanos = frameTimeNanos
                engine.update(dt)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A1A))
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    engine.initScreenSize(size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    engine.movePlayerTo(engine.player.x + dragAmount.x)
                }
            }
    ) {
        // High-performance direct Canvas renderer
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Reading engine.frameTick ensures smooth frame invalidation without Compose state tracking overhead
            @Suppress("UNUSED_VARIABLE")
            val tick = engine.frameTick

            val shakeOffsetX = if (engine.screenShakeAmount > 0f) {
                (Random.nextFloat() - 0.5f) * engine.screenShakeAmount * 2f
            } else 0f
            val shakeOffsetY = if (engine.screenShakeAmount > 0f) {
                (Random.nextFloat() - 0.5f) * engine.screenShakeAmount * 2f
            } else 0f

            translate(shakeOffsetX, shakeOffsetY) {
                // 1. Draw Starfield
                val stars = engine.backgroundStars
                val starCount = stars.size
                for (i in 0 until starCount) {
                    val star = stars[i]
                    drawCircle(
                        color = Color.White.copy(alpha = star.alpha),
                        radius = star.radius,
                        center = Offset(star.x, star.y)
                    )
                }

                // 2. Draw Collectibles
                val collectibles = engine.collectibles
                val collectibleCount = collectibles.size
                for (i in 0 until collectibleCount) {
                    drawCollectible(collectibles[i])
                }

                // 3. Draw Asteroids
                val asteroids = engine.asteroids
                val asteroidCount = asteroids.size
                for (i in 0 until asteroidCount) {
                    drawAsteroid(asteroids[i])
                }

                // 4. Draw Particles
                val particles = engine.particles
                val particleCount = particles.size
                for (i in 0 until particleCount) {
                    val p = particles[i]
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha),
                        radius = p.radius,
                        center = Offset(p.x, p.y)
                    )
                }

                // 5. Draw Player
                if (engine.gameState == GameState.PLAYING || engine.gameState == GameState.PAUSED) {
                    drawPlayerShip(engine.player)
                }
            }
        }

        // HUD Overlay
        if (engine.gameState == GameState.PLAYING || engine.gameState == GameState.PAUSED) {
            GameHUD(
                score = engine.score,
                lives = engine.player.lives,
                hasShield = engine.player.hasShield,
                slowTimeTimer = engine.timeSlowTimer,
                onPause = { engine.pauseGame() }
            )
        }

        // Start Menu Overlay
        AnimatedVisibility(
            visible = engine.gameState == GameState.START_MENU,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            StartMenuOverlay(
                highScore = engine.highScore,
                difficulty = engine.difficulty,
                onSelectDifficulty = { engine.changeDifficulty(it) },
                onStartGame = { engine.startGame() },
                onOpenHighScores = { engine.openHighScores() }
            )
        }

        // High Scores Overlay
        AnimatedVisibility(
            visible = engine.gameState == GameState.HIGH_SCORES,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HighScoresOverlay(
                highScore = engine.highScore,
                gamesPlayed = engine.totalGamesPlayed,
                totalStars = engine.totalStarsCollected,
                onBackToMenu = { engine.openMenu() }
            )
        }

        // Pause Menu Overlay
        AnimatedVisibility(
            visible = engine.gameState == GameState.PAUSED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PauseMenuOverlay(
                onResume = { engine.resumeGame() },
                onRestart = { engine.startGame() },
                onMenu = { engine.openMenu() }
            )
        }

        // Game Over Overlay
        AnimatedVisibility(
            visible = engine.gameState == GameState.GAME_OVER,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GameOverOverlay(
                finalScore = engine.score,
                starsCollected = engine.starsCollectedSession,
                highScore = engine.highScore,
                onRestart = { engine.startGame() },
                onMenu = { engine.openMenu() }
            )
        }
    }
}

private fun DrawScope.drawPlayerShip(player: Player) {
    val px = player.x
    val py = player.y
    val w = player.width
    val h = player.height

    // Draw Shield Bubble if Active
    if (player.hasShield) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x8000E5FF), Color(0x2000E5FF), Color.Transparent),
                center = Offset(px, py),
                radius = w * 0.9f
            ),
            center = Offset(px, py),
            radius = w * 0.85f
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            center = Offset(px, py),
            radius = w * 0.85f,
            style = Stroke(width = 4f)
        )
    }

    // Reuse path object to avoid allocations
    reusableShipPath.reset()
    reusableShipPath.moveTo(px, py - h * 0.5f) // Nose
    reusableShipPath.lineTo(px + w * 0.45f, py + h * 0.35f) // Right wing
    reusableShipPath.lineTo(px + w * 0.2f, py + h * 0.25f)
    reusableShipPath.lineTo(px, py + h * 0.45f) // Tail
    reusableShipPath.lineTo(px - w * 0.2f, py + h * 0.25f)
    reusableShipPath.lineTo(px - w * 0.45f, py + h * 0.35f) // Left wing
    reusableShipPath.close()

    drawPath(
        path = reusableShipPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF00E5FF), Color(0xFF1A237E)),
            startY = py - h * 0.5f,
            endY = py + h * 0.45f
        )
    )

    drawPath(
        path = reusableShipPath,
        color = Color(0xFF80DEEA),
        style = Stroke(width = 3.5f)
    )

    // Cockpit Glow
    drawCircle(
        color = Color(0xFF00F5FF),
        radius = 12f,
        center = Offset(px, py - h * 0.1f)
    )
}

private fun DrawScope.drawAsteroid(asteroid: Asteroid) {
    rotate(degrees = asteroid.rotation, pivot = Offset(asteroid.x, asteroid.y)) {
        val r = asteroid.radius
        reusableAsteroidPath.reset()

        val points = 8
        for (i in 0 until points) {
            val angle = (i.toFloat() / points) * Math.PI.toFloat() * 2f
            val jaggedRadius = if (i % 2 == 0) r else r * 0.82f
            val x = asteroid.x + cos(angle) * jaggedRadius
            val y = asteroid.y + sin(angle) * jaggedRadius
            if (i == 0) reusableAsteroidPath.moveTo(x, y) else reusableAsteroidPath.lineTo(x, y)
        }
        reusableAsteroidPath.close()

        drawPath(
            path = reusableAsteroidPath,
            color = Color(0xFF4E342E)
        )
        drawPath(
            path = reusableAsteroidPath,
            color = Color(0xFF8D6E63),
            style = Stroke(width = 3f)
        )

        // Craters
        drawCircle(
            color = Color(0xFF3E2723),
            radius = r * 0.25f,
            center = Offset(asteroid.x - r * 0.3f, asteroid.y - r * 0.2f)
        )
        drawCircle(
            color = Color(0xFF3E2723),
            radius = r * 0.2f,
            center = Offset(asteroid.x + r * 0.25f, asteroid.y + r * 0.3f)
        )
    }
}

private fun DrawScope.drawCollectible(item: Collectible) {
    val x = item.x
    val y = item.y
    val r = item.radius

    when (item.type) {
        CollectibleType.STAR -> {
            reusableStarPath.reset()
            val points = 5
            val outerRadius = r
            val innerRadius = r * 0.45f
            for (i in 0 until points * 2) {
                val currentRadius = if (i % 2 == 0) outerRadius else innerRadius
                val angle = (i.toFloat() / (points * 2)) * Math.PI.toFloat() * 2f - Math.PI.toFloat() / 2f
                val px = x + cos(angle) * currentRadius
                val py = y + sin(angle) * currentRadius
                if (i == 0) reusableStarPath.moveTo(px, py) else reusableStarPath.lineTo(px, py)
            }
            reusableStarPath.close()

            drawPath(reusableStarPath, color = Color(0xFFFFD700))
            drawPath(reusableStarPath, color = Color.White, style = Stroke(width = 2f))
        }

        CollectibleType.SHIELD -> {
            drawCircle(color = Color(0xFF00E5FF), radius = r, center = Offset(x, y))
            drawCircle(color = Color.White, radius = r, center = Offset(x, y), style = Stroke(width = 3f))
        }

        CollectibleType.SLOW_TIME -> {
            drawCircle(color = Color(0xFFE040FB), radius = r, center = Offset(x, y))
            drawCircle(color = Color.White, radius = r, center = Offset(x, y), style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun GameHUD(
    score: Int,
    lives: Int,
    hasShield: Boolean,
    slowTimeTimer: Float,
    onPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 45.dp, start = 20.dp, end = 20.dp)
    ) {
        // Top Bar: Score, Lives & Pause Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score Display
            Column {
                Text(
                    text = "SCORE",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$score",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Right side: Lives & Pause
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.padding(end = 12.dp)) {
                    repeat(3) { index ->
                        val isAlive = index < lives
                        Text(
                            text = if (isAlive) "❤️" else "🖤",
                            fontSize = 22.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                // Pause Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0x661A237E), CircleShape)
                        .border(1.dp, Color(0xFF00E5FF), CircleShape)
                        .clickable { onPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏸️",
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Active Power-up Badges
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 85.dp)
        ) {
            if (hasShield) {
                Surface(
                    color = Color(0xCC00E5FF),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "🛡️ SHIELD ACTIVE",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (slowTimeTimer > 0f) {
                Surface(
                    color = Color(0xCCE040FB),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val formattedSeconds = ((slowTimeTimer * 10).toInt() / 10f)
                    Text(
                        text = "⏱️ SLOW TIME (${formattedSeconds}s)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StartMenuOverlay(
    highScore: Int,
    difficulty: Difficulty,
    onSelectDifficulty: (Difficulty) -> Unit,
    onStartGame: () -> Unit,
    onOpenHighScores: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2090A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "🚀 SPACE DODGER",
                color = Color(0xFF00E5FF),
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Dodge asteroids & collect power-ups!",
                color = Color.LightGray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Difficulty Selector
            Text(
                text = "DIFFICULTY",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Difficulty.entries.forEach { diff ->
                    val isSelected = diff == difficulty
                    Surface(
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable { onSelectDifficulty(diff) }
                    ) {
                        Text(
                            text = diff.label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Play Game Button
            Button(
                onClick = onStartGame,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "🎮 PLAY GAME",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // High Scores / Stats Button
            Button(
                onClick = onOpenHighScores,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "🏆 HIGH SCORES & STATS",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HighScoresOverlay(
    highScore: Int,
    gamesPlayed: Int,
    totalStars: Int,
    onBackToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2090A1A)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141738)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "🏆 STATS & HIGH SCORE",
                    color = Color(0xFFFFD700),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                StatRow(label = "Best Score", value = "$highScore")
                StatRow(label = "Games Played", value = "$gamesPlayed")
                StatRow(label = "Stars Collected", value = "⭐ $totalStars")

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onBackToMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "BACK TO MENU",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 16.sp)
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PauseMenuOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2090A1A)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141738)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "⏸️ GAME PAUSED",
                    color = Color(0xFF00E5FF),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "RESTART", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "MAIN MENU", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    finalScore: Int,
    starsCollected: Int,
    highScore: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2090A1A)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141738)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "💥 GAME OVER",
                    color = Color(0xFFFF5722),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "FINAL SCORE", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "$finalScore",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "STARS", color = Color.Gray, fontSize = 12.sp)
                        Text(text = "⭐ $starsCollected", color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "BEST", color = Color.Gray, fontSize = 12.sp)
                        Text(text = "🏆 $highScore", color = Color(0xFF00E5FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "PLAY AGAIN",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "MAIN MENU",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
