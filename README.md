# 🚀 Space Dodger

[🇵🇱 Wersja Polska](#-wersja-polska-pl) | [🇬🇧 English Version](#-english-version-en)

---

## 🇵🇱 Wersja Polska (PL)

**Space Dodger** to dynamiczna, zręcznościowa gra 2D stworzona natywnie w **Jetpack Compose** (Android) renderowana w 60/120 FPS z wykorzystaniem natywnego `Canvas`.

### 🎮 O Grze
W **Space Dodger** wcielasz się w pilota statku kosmicznego przedzierającego się przez niebezpieczne pole asteroid. Twoim celem jest jak najdłuższe przetrwanie, omijanie przeszkód oraz zbieranie wartościowych przedmiotów i ulepszeń.

### 🌟 Główne Funkcje:
* ⚡ **Ultra Płynność (60–120 FPS):** Dedykowany silnik fizyki i renderowania zero-allocation na Compose `Canvas` bez zacięć Garbage Collectora.
* 🕹️ **Intuicyjne Sterowanie:** Sterowanie gestem przesuwania palcem po ekranie (Touch Drag).
* 🎯 **3 Poziomy Trudności:** 
  * `Normal` – Zbalansowane wyzwanie.
  * `Hard` – Szybsze asteroidy i gęstszy spawn.
  * `Insane` – Ekstremalny test refleksu.
* 🛡️ **Power-upy i Znajdźki:**
  * 🌟 **Złota Gwiazda (+10 pkt):** Zwiększa punktację i generuje błyszczące cząsteczki.
  * 🛡️ **Tarcza Ochronna (Shield):** Generuje pole siłowe absorbujące jedno uderzenie bez utraty życia.
  * ⏱️ **Spowolnienie Czasu (Slow Time):** Spowalnia ruch asteroid na 5 sekund.
* ✨ **Game Feel & Efekty:**
  * Efekt wstrząsu ekranu (Screen Shake) przy kolizjach.
  * Animowany płomień silnika i cząsteczki wybuchów.
* 🏆 **System Rekordów & Statystyk:** Śledzenie najlepszego wyniku (High Score), liczby rozegranych gier oraz łącznej liczby zebranych gwiazdek.
* ⏸️ **Menu Pauzy:** Możliwość wstrzymania gry w dowolnym momencie.

---

## 🇬🇧 English Version (EN)

**Space Dodger** is a fast-paced 2D arcade action game built natively with **Jetpack Compose** for Android, running smoothly at 60–120 FPS using custom Canvas rendering.

### 🎮 About the Game
In **Space Dodger**, you control a futuristic spaceship navigating through an intense asteroid belt. Your goal is to survive as long as possible, dodge incoming asteroids, and collect glowing power-ups and stars to maximize your score.

### 🌟 Key Features:
* ⚡ **Ultra Smooth (60–120 FPS):** Dedicated game engine with zero object allocations inside the rendering loop for jank-free 120 FPS performance.
* 🕹️ **Smooth Touch Drag Controls:** Drag left and right to control your spaceship seamlessly.
* 🎯 **3 Difficulty Modes:**
  * `Normal` – Balanced arcade challenge.
  * `Hard` – Faster asteroids and tighter spawn windows.
  * `Insane` – Ultimate reflex speed test.
* 🛡️ **Power-ups & Collectibles:**
  * 🌟 **Gold Star (+10 pts):** Boosts score with sparkling particle effects.
  * 🛡️ **Energy Shield:** Creates a protective bubble that absorbs 1 asteroid hit without losing a life.
  * ⏱️ **Time Slow:** Temporarily slows down all asteroids for 5 seconds.
* ✨ **Game Feel & Particles:**
  * Dynamic Screen Shake effect on taking damage.
  * Thruster flame particle trails and vibrant explosion effects.
* 🏆 **High Score & Lifetime Stats:** Tracks your High Score, total games played, and cumulative stars collected.
* ⏸️ **In-Game Pause:** Pause anytime without losing your game progress.

---

## 🏗️ Codebase Structure / Struktura Projektu

```text
com.example.spacedodger/
├── MainActivity.kt           # Edge-to-Edge Activity launcher
└── game/
    ├── Models.kt             # Data classes (GameState, Player, Asteroid, Collectible, Particle)
    ├── GameEngine.kt         # Game engine: 60FPS physics loop, collisions, spawners, particles
    └── SpaceDodgerGame.kt    # Canvas renderer + UI overlays (Main Menu, HUD, Pause, Game Over)
```

---

## 🛠️ Technical Specs / Wymagania Techniczne

* **Language / Język:** Kotlin 2.0+
* **UI Framework:** Jetpack Compose (Material 3)
* **Min SDK:** API 24 (Android 7.0+)
* **Target / Compile SDK:** API 35 (Android 15)
* **Build System:** Gradle 9.1 / AGP 8.7

---

## 🚀 How to Run / Jak Uruchomić

1. Open project in **Android Studio**:
   ```text
   E:\SpaceDodger
   ```
2. Connect your Android device via USB (ADB Enabled).
3. Run in terminal or click **Run (Shift + F10)**:
   ```bash
   ./gradlew installDebug
   ```
