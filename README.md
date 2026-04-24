# 👸 Mikaela — Hytale Modding Contest Entry

Mikaela is a custom boss NPC built for a Hytale modding contest. She's a two-phase fighter with a handcrafted combat AI, a custom health bar HUD, and a set of abilities that escalate as the fight goes on. The whole thing is implemented as a server-side plugin using Hytale's ECS framework.

---

## 🗡️ What is this?

This plugin adds **Mikaela**, a fully original boss character, to a Hytale server. She has her own 3D model, animations, attack kit, and a dynamic HUD that shows players how the fight is going.

The design goal was to make her feel like a real boss encounter — not just an NPC with high health and a single attack. She reads the situation, mixes up her moves, and shifts into a second phase when she goes down for the first time.

---

## ⚔️ Boss Overview

### 🔄 Two-Phase Fight

- **Phase 1 — Mikaela** (1,400 HP): Ground-based brawler. Close-range melee attacks with a few mid-range specials.
- **Phase 2 — Archangel** (2,000 HP): Aerial powerhouse. Takes flight after Mikaela falls, switching between an aerial stance and a ground stance with a completely different attack kit.

The transition is handled automatically by the server on Mikaela's death.

---

## 🥊 Attack Kits

### Phase 1 — Mikaela

| Attack | Description | Cooldown | Weight | Range |
|--------|-------------|----------|--------|-------|
| ⬅️ **Swing Left** | Wide horizontal swing | 3s | 2 | 0–5 |
| ⬇️ **Swing Down** | Overhead slam | 2s | 2 | 0–5 |
| 💥 **Swing Down Combo** | Follow-up combo slam | 8s | 2 | 0–5 |
| 👊 **Punch** | Direct hit with strong knockback | 4s | 2 | 0–5 |
| 🤜 **Grab** | Pulls the player from mid range | 7s | 5 | 5–8 |
| 🌧️ **Rain Hands** | Projectiles fall from the sky above target | 30s | 5 | 0–20 |
| 💢 **AOE Jump** | Ground-slam that hits everything nearby | 10s | 5 | 0–1 |

### Phase 2 — Archangel

Archangel operates in two stances: **Aerial** (default) and **Ground** (after landing).

**Aerial stance** — attacks executed while airborne at altitude 5–7:

| Attack | Description | Cooldown | Weight | Range |
|--------|-------------|----------|--------|-------|
| 🌧️ **Rain Hands** | Aerial projectile rain | 30s | 2 | 0–20 |
| 💥 **Burst Projectile** | Rapid projectile burst | 6s | 2 | 0–20 |
| 🔵 **Big Projectile** | Heavy charged projectile | 4s | 2 | 0–20 |
| ✈️ **Levitate** | Transitions to ground stance | 25s | 2 | 0–20 |

**Ground stance** — attacks executed while landed:

| Attack | Description | Cooldown | Weight | Range |
|--------|-------------|----------|--------|-------|
| 🌧️ **Rain Hands** | Ground projectile rain | 60s | 1.95 | 0–20 |
| 💥 **Burst Projectile** | Rapid projectile burst | 8s | 2 | 0–20 |
| 🔵 **Big Projectile** | Heavy charged projectile | 5s | 2 | 0–20 |
| ⚔️ **AOE Sword** | Wide sword slam in all directions | 2s | 2 | 0–15 |
| 🗡️ **Throw Sword** | Hurls sword at the player | 4s | 2 | 0–20 |
| 🤺 **Swing Combo** | Multi-hit sword combo | 2s | 2 | 0–15 |
| 🛸 **Levitate** | Returns to aerial stance | 30s | 2 | 0–20 |

---

### ❤️ Health Bar HUD

A custom health bar appears when you get close enough and disappears when you move away. It changes color as the fight progresses:

- 🟡 **Yellow** — healthy
- 🟠 **Orange** — mid-fight
- 🔴 **Red** — nearly done

---

## 🔧 Technical Details

Built on Hytale's Entity Component System with:

- ⚙️ **CAE/CAO combat AI** — each attack has its own decision conditions (cooldowns, distance, randomness) feeding into a central evaluator. Archangel's CAE uses **ActionSets** to swap the full attack list when switching between aerial and ground stances.
- 🔗 **Custom interactions** — Rain Hands and regen reset are implemented as server-side custom interaction chains.
- 🔄 **ECS ticking systems** — proximity detection for the HUD, projectile spawning, and NPC respawn logic all run as independent systems.
- 📦 **Asset pack included** — the plugin ships with both Mikaela and Archangel's models, textures, and all animations bundled in.
