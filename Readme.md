# 🎭 PropHuntOptimized

**PropHuntOptimized** is a modern, high‑performance **Prop Hunt / Block Morph** minigame plugin for Minecraft servers.
Designed for smooth visuals, competitive gameplay, and immersive disguises using advanced block display mechanics.

---

## 🧩 Game Overview

One player becomes the **Hunter** while the others become **Seekers (Props)**.
Seekers disguise themselves as blocks in the arena and try to survive until time runs out.

🏆 **Hunter Wins** → All seekers are eliminated
🏆 **Seekers Win** → Time runs out

---

## ✨ Core Features

### 🧱 Advanced Block Morph System

* Players visually transform into **real Minecraft blocks**
* Block moves **smoothly with the player**
* Block **never rotates** on any axis
* Player can still **walk, jump, and rotate camera freely**
* Morph ends only if:

  * Player morphs into another block
  * Player looks into the sky with a spyglass
  * Player dies

### 🌟 Visual Effects (NEW)

* Morph cloud + magical sound
* Demorph smoke effect
* Fake block hit particles when hunter attacks a disguised player
* Smooth interpolation for natural block sliding
* Anti‑spam morph cooldown

### 🧱 Smart Block Connections

If a player morphs into:

* Walls
* Fences
* Panes

Their disguise **visually connects** to nearby blocks just like real Minecraft blocks.

---

## 🎮 Game Flow

1. Players join the arena lobby
2. Auto countdown starts when minimum players join
3. Roles are assigned randomly
4. **Seekers spawn in arena first** to hide
5. Hunter waits in lobby with blindness
6. Hiding timer ends → Hunter released
7. Main game timer begins
8. Game ends when all seekers are found or time expires

---

## ⚔ Roles

### 🟥 Hunter

* Starts in lobby
* Has temporary blindness during hiding phase
* Receives wooden sword
* Must find all disguised players

### 🟩 Seekers (Props)

* Spawn in arena at game start
* Use spyglass to morph into blocks
* Get food + fireworks
* Must survive until timer ends

---

## 🛡 Arena Protections

During a match:

* ❌ No block breaking
* ❌ No mob spawning
* ❌ No portal usage
* ❌ No leaving arena bounds
* ❌ No item dropping exploits

Players remain in **Adventure Mode** at all times.

---

## 🎯 Commands

| Command             | Description                 |
| ------------------- | --------------------------- |
| `/mb pos1`          | Set arena corner position 1 |
| `/mb pos2`          | Set arena corner position 2 |
| `/mb create <name>` | Create arena                |
| `/mb setlobby`      | Set lobby location          |
| `/mb setspawn`      | Set seeker spawn location   |
| `/mb join <arena>`  | Join game                   |
| `/mb <arena> leave` | Leave current game          |
| `/mb reload`        | Reload config & arena files |
| `/mb help`          | Show help menu              |

✅ Full **Tab Completion** supported

---

## 🧠 Smart Game Mechanics

* Prevents double joining
* Countdown cancels if players leave
* Leave item only before match start
* Inventories cleared at start & end
* Death auto‑spectator mode
* Automatic game reset
* BossBar shows remaining seekers + time left

---

## 💾 Data Storage

All arena data is saved in:

```
/plugins/PropHuntOptimized/arenas/
```

Each arena has its own **YML file** storing:

* Arena positions
* Lobby spawn
* Game spawn
* Arena settings

---

## ⚙ Configuration Example

```yml
min-players: 2
max-players: 10
countdown-seconds: 30
hiding-seconds: 20
game-time-seconds: 300
```

---

## 🔌 Requirements

* Paper / Spigot 1.20+
* ProtocolLib (recommended for best visual sync)
* Vault + EssentialsX (optional for rewards)

---

## 🚀 Installation Guide

1. Place **PropHuntOptimized.jar** in `/plugins`
2. Start server
3. Set arena with pos1 & pos2
4. Create arena
5. Set lobby and spawn
6. Players can now join and play!

---

## 📺 Creator Channels

Support development ❤️

[![YouTube](https://img.shields.io/badge/YouTube-NotY215-red?style=for-the-badge\&logo=youtube)](https://www.youtube.com/@NotY215)

[![Movies Channel](https://img.shields.io/badge/YouTube-NotY215_Movies-darkred?style=for-the-badge\&logo=youtube)](https://www.youtube.com/@noty215_movies)

---

## 💬 Support & Updates

Found a bug or want a feature?
Leave a comment on the YouTube channels above.

---

**Made with ❤️ for epic Prop Hunt servers**
