# 👸 Mikaela — Hytale Modding Contest Entry

Mikaela is a custom boss NPC built for a Hytale modding contest. She's a two-phase fighter with a handcrafted combat AI, a custom health bar HUD, and a set of abilities that escalate as the fight goes on. The whole thing is implemented as a server-side plugin using Hytale's ECS framework.

---

## 🗡️ What is this?

This plugin adds **Mikaela**, a fully original boss character, to a Hytale server. She has her own 3D model, animations, attack kit, and a dynamic HUD that shows players how the fight is going.

The design goal was to make her feel like a real boss encounter — not just an NPC with high health and a single attack. She reads the situation, mixes up her moves, and shifts into a second phase when she goes down for the first time.

---

## ⚔️ Boss Overview

### 🔄 Two-Phase Fight

Mikaela starts in her base form. When she's defeated, she respawns in a more aggressive second phase with access to a different set of combat actions. The transition is handled automatically by the server.

### 🥊 Attack Kit

She has seven abilities spread across two categories:

**Normal attacks** — her bread and butter, used frequently to keep pressure on the player:

| Attack | Description | Cooldown | Weight | Range |
|--------|-------------|----------|--------|-------|
| ⬅️ **Swing Left** | Wide horizontal swing | 3s | 2 | 0–5 |
| ⬇️ **Swing Down** | Overhead slam | 2s | 2 | 0–5 |
| 💥 **Swing Down Combo** | Follow-up combo version of the slam | 5s | 3 | 0–5 |
| 👊 **Punch** | Direct hit with strong knockback | 3s | 2 | 0–5 |

**Special abilities** — less frequent, but harder to avoid and more punishing:

| Attack | Description | Cooldown | Weight | Range |
|--------|-------------|----------|--------|-------|
| 🤜 **Grab** | Pulls the player toward her from mid range | 12s | 5 | 7–8 |
| 🌧️ **Rain Hands** | Projectiles fall from the sky above the target *(only activates below 75 HP)* | 15s | 5 | 0–20 |
| 💢 **AOE Jump** | Ground-slam that hits everything nearby | 15s | 5 | 0–3 |

The AI uses a utility-based system — each attack competes for priority every tick based on cooldown, distance, and a randomness factor. Special abilities have a much higher weight, so when they finally come off cooldown they tend to win the evaluation.

### ❤️ Health Bar HUD

There's a custom health bar that appears when you get close enough to Mikaela and disappears when you move away. It changes color as the fight progresses:

- 🟡 **Yellow** — she's healthy
- 🟠 **Orange** — mid-fight
- 🔴 **Red** — nearly done

---

## 🔧 Technical Details

Built on Hytale's Entity Component System with:

- ⚙️ **CAE/CAO combat AI** — each attack has its own decision conditions (cooldowns, distance, randomness) that feed into a central evaluator. The AI picks attacks based on weighted utility scores, so her behavior never feels like a fixed script.
- 🔗 **Custom interactions** — the Rain Hands attack and regen reset are implemented as server-side custom interaction chains.
- 🔄 **ECS ticking systems** — proximity detection for the HUD, projectile spawning, and NPC respawn logic all run as independent systems.
- 📦 **Asset pack included** — the plugin ships with Mikaela's model, texture, and all animations bundled in.

**Stack:** Java 25 · Gradle · Hytale `2026.03.26-89796e57b`

---

## 🚀 Build & Run

```bash
# Build the plugin JAR
./gradlew shadowJar

# Download the server and run with the plugin loaded
./gradlew runServer

# Run with an already-downloaded server
./gradlew runServerJar
```

Output JAR: `build/libs/MikaelaPlugin-1.0.0.jar`

---

## 📁 Project Structure

```
src/main/
├── java/com/hytalezx/mikaela/
│   ├── MikaelaPlugin.java              # Plugin entry point
│   ├── Config/                         # Boss registry and per-boss config
│   ├── Systems/                        # ECS ticking systems (HUD, projectiles, respawn)
│   ├── Interactions/                   # Custom interaction logic
│   └── UI/                             # Boss health bar HUD
└── resources/
    ├── Server/NPC/                     # Roles, combat AI (CAE/CAO), interactions
    ├── Server/Projectiles/             # Rain Hands projectile
    ├── Server/Entity/Effects/          # Invulnerable/Vulnerable status effects
    └── Common/NPC/Mikaela/             # Model, texture, animations
```

---

## 📄 License

MIT — do whatever you want with it.
