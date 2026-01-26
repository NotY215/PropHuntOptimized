# PropHuntOptimized

A high‑performance **Prop Hunt** minigame plugin for **Minecraft Spigot/Paper 1.21.x** servers, designed with **cross‑play support (Geyser Bedrock compatible)** in mind.

Players hide as blocks. One hunter must find them before time runs out.

---

## ✨ Core Features

* 🎭 Players morph into **real Minecraft blocks** using a Spyglass
* 🧱 Uses **Block Display entities** (smooth visuals, crossplay friendly)
* 🕵️ Random **Hunter vs Seekers** role system
* ⏳ Configurable **lobby countdown** and **game timer**
* 🌫 Hunter gets **temporary blindness** at game start
* 📊 Live **BossBar** showing seekers remaining & time left
* 🚫 Adventure mode enforced (no block breaking)
* 🚪 Portals disabled inside arenas
* 🎒 Custom kits for Hunter and Seekers
* 💰 Supports **Vault** economy rewards
* ⚙ Fully configurable win commands

---

## 🗺 Arena Setup Commands (Admin)

| Command                | Description               |
| ---------------------- | ------------------------- |
| `/mb pos1`             | Set arena position 1      |
| `/mb pos2`             | Set arena position 2      |
| `/mb create <arena>`   | Create a new arena        |
| `/mb <arena> setlobby` | Set lobby spawn for arena |

---

## 🎮 Player Commands

| Command             | Description         |
| ------------------- | ------------------- |
| `/mb join <arena>`  | Join an arena lobby |
| `/mb <arena> leave` | Leave the arena     |

---

## 🎭 Morph Controls

| Action                            | Result                         |
| --------------------------------- | ------------------------------ |
| Right‑click Spyglass on a block   | Morph into that block          |
| Right‑click Spyglass into the sky | Return to normal form          |
| Death                             | Disguise removed automatically |
| Morph again                       | Previous disguise replaced     |

❗ Players **cannot morph into air or portal blocks**

---

## ⚔ Game Flow

1. Players join lobby
2. Countdown starts when minimum players reached
3. Game begins
4. One random player becomes **Hunter**
5. Seekers hide while Hunter is blinded
6. Hunter must find all Seekers before time runs out

### 🏆 Win Conditions

* Hunter wins if all seekers are found
* Seekers win if timer runs out

---

## 🎒 Kits

### Hunter

* Wooden Sword
* 30s Blindness at start

### Seekers

* Spyglass (undroppable)
* Food
* Firework Rockets (flight power 1)
* Leather Armor (hidden while morphed)

---

## 🔐 Permissions (LuckPerms)

| Permission       | Default | Description  |
| ---------------- | ------- | ------------ |
| `prophunt.admin` | OP      | Setup arenas |
| `prophunt.join`  | TRUE    | Join games   |
| `prophunt.leave` | TRUE    | Leave games  |

---

## ⚙ Config Options (`config.yml`)

```yml
countdown-seconds: 60
min-players: 2
max-players: 10
game-time-seconds: 600
blindness-seconds: 30

end-commands:
  - "eco give %winner% 100"
  - "say %winner% won Prop Hunt!"
```

### Placeholders

| Placeholder | Meaning             |
| ----------- | ------------------- |
| `%winner%`  | Winning player name |
| `%loser%`   | Losing player name  |

---

## 🔌 Dependencies

| Plugin      | Required | Purpose                   |
| ----------- | -------- | ------------------------- |
| Vault       | ✅        | Economy rewards           |
| EssentialsX | Optional | Economy backend           |
| Geyser      | Optional | Bedrock crossplay support |

---

## 🧠 Technical Notes

* Uses **BlockDisplay entities** instead of packet tricks
* Fully compatible with **Java & Bedrock players**
* No ProtocolLib required
* Designed for performance and stability

---

## 👤 Author

**NotY215**
