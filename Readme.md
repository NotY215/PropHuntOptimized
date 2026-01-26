# PropHuntOptimized

PropHuntOptimized is a fully-featured **Prop Hunt plugin** for Minecraft 1.21.x servers. Players can morph into blocks, hide from hunters, and compete in a fun hide-and-seek style minigame.

## 🎯 Features

- Morph into any block visible in survival using a spyglass.
- Hunter vs Seeker gameplay:
    - Hunter finds seekers within 10 minutes.
    - Seekers hide and morph to avoid detection.
- Hiding countdown before game start.
- Single arena support with lobby and spawn setup.
- Crossplay-ready morph system (Java & Bedrock clients via Geyser).
- Hunter receives a wooden sword; seekers get spyglass, food, rockets, and invisible armor.
- Boss bars and countdowns for both hiding and main game.
- Diamond to leave system.
- Inventory cleared on start and end.
- End commands configurable in plugin config.
- Adventure mode enforcement and portal blocking.
- Automatic game end on hunter or seeker death.
- Big broadcast when game starts.

---

## 📦 Requirements

- **Minecraft 1.21.x**
- **Spigot or Paper**
- **Vault** (optional for economy rewards)
- **EssentialsX** (optional for economy rewards)
- **ProtocolLib** (required for BlockDisplay morphs)

---

## ⚙️ Installation

1. Place the `PropHuntOptimized.jar` in your server's `plugins/` folder.
2. Ensure **Vault**, **ProtocolLib**, and optionally **EssentialsX** are installed.
3. Start or reload your server to generate default config.
4. Set up your arena using commands below.

---

## 🛠 Commands

| Command | Description |
|---------|-------------|
| `/mb help` | Show help and command suggestions |
| `/mb <arena> join` | Join the game |
| `/mb <arena> leave` | Leave the game (requires diamond) |
| `/mb <arena> setlobby` | Set lobby location |
| `/mb <arena> setspawn` | Set seeker spawn location |

> Players automatically receive a **diamond to leave** after joining. During the game, the diamond is removed to prevent leaving early.

---

## 📝 Setup Example

1. `/mb pos1` and `/mb pos2` – mark arena corners
2. `/mb create <arena>` – create arena
3. `/mb <arena> setlobby` – set the lobby location
4. `/mb <arena> setspawn` – set the seeker spawn
5. Players join via `/mb <arena> join`

---

## 🎮 Gameplay Flow

1. Players join arena lobby.
2. Countdown starts when minimum players are met.
3. Seekers hide while hunters receive 30 seconds of blindness.
4. Seekers morph using spyglass; blocks remain static, player can rotate/move.
5. Game ends when hunter finds all seekers or timer runs out, or when hunter/seeker dies.
6. Rewards or commands run via config-defined end commands.

---

## ⚡ Config Options

- `min-players`: Minimum players to start game
- `max-players`: Maximum players
- `countdown-seconds`: Countdown before game starts
- `blindness-seconds`: Duration of hunter blindness
- `game-time-seconds`: Total duration of game
- `end-commands`: List of commands to run on game end, supports `%winner%` and `%loser%` placeholders

---

## 🛡 Notes

- Currently **single arena only**.
- Multi-arena support is planned.
- Crossplay is supported via **BlockDisplay** morphs.
- Mob spawning is blocked in arena regions.
- Players cannot break blocks or use portals in arena.

---

## 📌 Author

- **NotY215**
