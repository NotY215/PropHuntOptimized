# 🎭 PropHuntOptimized

A high‑performance **Prop Hunt / Block Morph** minigame plugin for Minecraft servers.
Built for smooth gameplay, clean mechanics, and cross‑version visual disguises.

---

## 🔥 Core Game Concept

One player becomes the **Hunter** and the rest become **Seekers (Props)**.
Seekers disguise themselves as blocks in the map while the Hunter tries to find and eliminate them before time runs out.

If all seekers are found → **Hunter Wins**
If time runs out → **Seekers Win**

---

## ✨ Main Features

### 🧱 Block Morph System

* Players transform into **real-looking blocks**
* Blocks **move with the player**
* Blocks **never rotate** (always placed position)
* Player can **move and rotate camera freely**
* Morph stays until:

  * Player changes block
  * Player looks into the sky using a spyglass
  * Player dies

### 🎯 Game Flow

* Lobby waiting system
* Auto game start when minimum players join
* Countdown cancels if players leave
* Hiding phase before hunter is released
* BossBar timer with remaining seekers count
* Automatic win detection

### ⚔ Roles

**Hunter**

* Spawns in lobby during hiding phase
* Has blindness until hiding ends
* Receives sword kit

**Seekers**

* Spawn in arena at start
* Use spyglass to morph into blocks
* Get food + rockets

---

## 🛡 Arena Protection

While a game is running:

* ❌ No block breaking
* ❌ No mob spawning
* ❌ No portal usage
* ❌ No arena griefing

---

## 🎮 Commands

| Command             | Description              |
| ------------------- | ------------------------ |
| `/mb create <name>` | Create arena             |
| `/mb pos1`          | Set arena position 1     |
| `/mb pos2`          | Set arena position 2     |
| `/mb setlobby`      | Set lobby spawn          |
| `/mb setspawn`      | Set game spawn           |
| `/mb join <arena>`  | Join game                |
| `/mb leave`         | Leave current game       |
| `/mb reload`        | Reload all arena configs |
| `/mb help`          | Show help menu           |

✔ Full tab completion included

---

## 🧠 Smart Mechanics

* Players cannot join twice
* Countdown stops if player count drops
* Leave item only before game start
* Inventories cleared on game start & end
* Death auto switches player to spectator
* Game auto resets after finish

---

## 💾 Data Saving

All arenas are saved in:

```
/plugins/PropHuntOptimized/arenas/
```

Each arena uses its own **YAML file** storing:

* Positions
* Lobby
* Spawn
* Settings

---

## 📺 Creator Channels

Support the project ❤️

[![YouTube](https://img.shields.io/badge/YouTube-NotY215-red?style=for-the-badge\&logo=youtube)](https://www.youtube.com/@NotY215)

[![Movies Channel](https://img.shields.io/badge/YouTube-NotY215_Movies-darkred?style=for-the-badge\&logo=youtube)](https://www.youtube.com/@noty215_movies)

---

## ⚙ Recommended Settings

```yml
min-players: 2
countdown-seconds: 30
hiding-seconds: 20
game-time-seconds: 300
```

---

## 🧩 Requirements

* Paper / Spigot 1.20+
* ProtocolLib (recommended for best disguise sync)

---

## 🚀 Installation

1. Drop plugin into `/plugins`
2. Start server
3. Create arena
4. Set positions & spawns
5. Players can now join and play!

---

## 💬 Support

If you find bugs or want new features, reach out via YouTube comments.

---

**Made with ❤️ for fun Prop Hunt servers**
