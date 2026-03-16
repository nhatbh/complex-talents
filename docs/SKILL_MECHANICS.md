# Complex Talents: Skill Mechanics [Technical Reference]

This document provides a deep dive into the internal mechanics, scaling formulas, and in-game behaviors of all Origin skills.

---

## 🗡️ Assassin

### Passives

#### Expose Weakness
Melee attacks from behind apply the `Exposed` effect.
- **Damage Amplification**: `[30%, 40%, 50%, 60%, 80%]`
- **Duration**: `[8s, 10s, 12s, 14s, 16s]`

#### The Disengage
Gaining distance after a strike grants a movement speed boost.
- **Move Speed Bonus**: `[30%, 45%, 60%, 75%, 100%]`
- **Duration**: `[1.5s, 1.5s, 2.0s, 2.0s, 2.5s]`

### Active Skill: Shadow Walk
Stealth-based infiltration and high-burst execution.

- **Channel Time**: 1.0s
- **Aggro Control**: Upon entering stealth, all mobs currently targeting the player will immediately drop their target.
- **Stealth Gauge**:
    - **Proximity Drain**: Linear scaling between 10.0 and 3.0 blocks. At 3.0 blocks or less, drain is 2.0 pts/tick.
    - **Recovery**: Recovers while active but away from mobs (`recovery/20.0` per tick). Recovers at **half rate** (50%) when not in stealth.
- **Stealth Breaks**:
    - **Taking Damage**: Immediately removes stealth. Damage taken is multiplied (1.5x down to 1.0x).
- **Backstab Mechanics**:
    - **Detection**: Checks if the player is behind the target's look vector.
    - **Gauge Cost**: A successful Backstab consumes **25%** of the Max Gauge. A normal strike while stealthed consumes **50%**.
    - **Ambush Buff**: A successful backstab grants a duration-based damage bonus to the backstab hit and subsequent hits.

#### Scaling & Formulas
| Level | Max Gauge | Recovery (pts/s) | Movespeed (Mult) | Damage Penalty (Taken) | Backstab Buff | Buff Duration | Visibility Mult |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | 400 | 5 | 1.15x | 1.5x | +30% | 4s | 0.10 |
| 2 | 500 | 7 | 1.20x | 1.4x | +50% | 5s | 0.08 |
| 3 | 600 | 10 | 1.25x | 1.3x | +70% | 6s | 0.06 |
| 4 | 800 | 15 | 1.30x | 1.2x | +100% | 8s | 0.04 |
| 5 | 1000 | 20 | 1.40x | 1.0x | +150% | 10s | 0.02 |

---

## 💀 Dark Mage

### Passives

#### Soul Siphon
Killing non-player entities harvests their souls.
- **Soul Gain Formula**: `Souls = (3 * √(MobMaxHP / 10) - 5) × (0.9 to 1.1)`. (Minimum 0). Linear below 40 HP.
- **Limit**: Uncapped (Infinite scaling).

#### Death Defy (Phylactery)
- **Trigger**: Fatal damage while holding at least 1.0 Souls.
- **Effect**: Damage is cancelled, HP is set to 1.0. 
- **Cost**: **Free** (No soul consumption).
- **Cooldown**: 300s (5 minutes).
- **Death Penalty**: Actual death (Phylactery on cooldown) results in a **30% loss** of all current souls.

### Active Skill: Blood Pact (Toggle)
Trades life force for immense power.
- **Exponential Drain**: The health drain **doubles every 20 seconds** of continuous activation (`2^(elapsedSeconds/20)`).
- **Soul Power Scaling**: For every doubling of health drain, all soul-based effects (Crit, Damage, and Mana Regen) are scaled by **1.5x** (`1.5^(elapsedSeconds/20)`).
- **Safety Rail**: Auto-deactivates if HP drops to 1.0 or less.
- **Mana Regeneration**: `(1.0 + (souls / 200.0)) * multiplier` additional mana/tick.
- **Activation Cost**: Requires at least 20% HP.

#### Scaling & Formulas
| Level | HP Drain (Base) | Cast Speed | Damage per Soul | Crit per Soul |
| :--- | :--- | :--- | :--- | :--- |
| 1 | 8.0%/s | +10% | +0.05% | +0.08% |
| 2 | 7.0%/s | +20% | +0.10% | +0.10% |
| 3 | 6.0%/s | +30% | +0.15% | +0.12% |
| 4 | 5.0%/s | +40% | +0.20% | +0.14% |
| 5 | 4.0%/s | +50% | +0.25% | +0.16% |

---

## ☄️ Elemental Mage

### Resource: Elemental Resonance
- **Elemental Mastery**: A weighted sum of all spell powers (40% top, 20% second, 15% third, 10% 4th/5th, 5% 6th).

### Active Skill: Harmonic Convergence
Consumes Resonance Echoes for mana restoration and critical hyper-scaling.
- **Mana Restore**: Instantly restores Mana: `echoes * (MANA_BASE + (elemental_mastery * MANA_MULT))`.
- **The "Crit Window"**: The **next spell cast within 10 seconds** gains massive Crit Chance and Crit Damage bonuses based on Echoes consumed.

### Elemental Reactions
Triggering a reaction costs **25 Resonance** and grants **1 Resonance Echo**.
- **Standard Matrix**:
    - **Melt** (Fire + Ice): 4.0 base damage.
    - **Vaporize** (Fire + Aqua): 3.0 base damage.
    - **Freeze**: Immobilizes.
    - **Overloaded**: Explosive knockback.
    - **Bloom**: Spawns a Nature Core (Detonate with Fire/Lightning).
    - **Ender Reactions**: Voidfire, Fracture, Spring, Flux.
- **Overwhelming Power (OP)**:
    - High-damage spells (>10 DMG) trigger tiered reactions.
    - **Fire**: Ignite (T1) -> Scorch (T2) -> Supernova (T3: Miniature Sun).
    - **Ice**: Frozen Touch (T1) -> Glacial Aura (T2) -> Absolute Zero (T3).
    - **Aqua**: Splash (T1) -> Violent Splash (T2) -> The Deluge (T3).
    - **Lightning**: Arcing Bolt (T1) -> Chain Surge (T2) -> The Thundergod (T3).
    - **Nature**: Parasitic Seed (T1) -> Spore Burst (T2) -> Sandstorm (T3).

### Infinite Scaling: Elemental Power
The Elemental Mage grows stronger by dealing damage, but must maintain balance.
- **Power Gain**: `Delta = sqrt(Damage) / 1000`.
- **The Balance Metric (B)**: Based on the variance of power across the 5 schools.
    - **High Balance**: Fast power gain, near-zero decay in other schools.
    - **Low Balance (One-Tricking)**: Slow power gain, rapid decay in unused schools.
- **Elemental Mastery**: A weighted sum of all 5 elemental powers (40% top, 20% second, 15% third, 10% 4th, 5% 5th).
- **Yields**: Directly adds to the corresponding Spell Power attribute (e.g., +1.0 Power = +100% Spell Power).

#### Scaling & Formulas
| Level | Mana (Base/Echo) | Mana (EM Scale) | Crit Chance/Echo | Crit Dmg (Base) | Crit Dmg (Scale) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | 10.0 | 5.0 | +10% | +25% | +15% |
| 2 | 15.0 | 8.0 | +12% | +30% | +20% |
| 3 | 20.0 | 12.0 | +15% | +35% | +25% |
| 4 | 25.0 | 16.0 | +17% | +40% | +30% |
| 5 | 40.0 | 25.0 | +20% | +50% | +40% |

---

## ⚖️ High Priest

### Passives

#### Grace of the Seraphim
Binary state of divine focus.
- **Binary State**: Buff is lost entirely upon taking any damage. Recovery takes **30 seconds**.
- **Yields**: Cast Time Reduction, Healing Potency, and Overheal-to-Absorption conversion.

#### Command
Passive generation for ability usage.
- **Max Stacks**: 10.
- **Generation Interval**: Scaling from 10s down to 5s per stack.

### Active Skill: Seraphic Echo
Orbital beacon control for team management and area denial.
- **Hover Bonus**: Moving the beacon while it is stationary increases damage/shielding by **1.5x**.
- **Command Pull**: Pulls enemies in 8-block radius. applies delayed damage and Slowness.
- **Path Effects**: Allies gain Absorption/Speed; Enemies take Holy damage/Slowness.

### Infinite Scaling: Faith
Faith is the High Priest's long-term progression resource.
- **Generation**: Gained when spending mana on Holy spells while **Grace** is active.
- **Uncapped**: There is no maximum limit to Faith.
- **Benefits**:
    - **Max Mana**: Scaled by level (`faith * [0.1, 0.15, 0.2, 0.25, 0.3]`).
    - **Divine Retribution**: While Grace is active, grants `+0.01%` Holy Spell Power per Faith point.

#### Scaling & Formulas
| Level | Healing Potency | Overheal Conv. | Absorp. Duration | Command Gen | Command CD |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | +30% | 30% | 30s | 200 ticks | 10s |
| 2 | +50% | 40% | 40s | 180 ticks | 9s |
| 3 | +70% | 50% | 50s | 160 ticks | 8s |
| 4 | +90% | 60% | 60s | 140 ticks | 7s |
| 5 | +125% | 75% | 75s | 100 ticks | 5s |
| (Note) | | | | | (Command CD is for active pull) |

---

## 🛡️ Warrior

### Passives

#### Style Rank (D -> SSS)
Combat performance system based on varied, continuous aggression.
- **Damage Multipliers**: 
    - D(0.7x), C(0.85x), B(1.0x), A(1.1x), S(1.3x), SS(1.4x), SSS(1.5x).
- **SSS Shield**: Grants a 1-hit invulnerability shield at SSS. Style resets to 900 on break.
- **Decay**: Style decays rapidly when remaining out of combat.

### Active Skill: Challenger's Retribution
Taunt-based damage absorption and explosive counter-attacks.
- **Taunt**: Forced aggro in scaling radius.
- **Absorption**: While charging, takes **0 damage** (stored in shield).
- **Shield Breaking**: If incoming hit > Shield HP, charge is canceled (Stagger).
- **Release**: Unleashes `storedDamage * reflectPercent` AoE damage and knockback.

#### Scaling & Formulas
| Level | Taunt Range | Shield Base HP | Reflect % | Max SSS Multiplier | Cooldown |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | 5 blocks | 5 HP | 50% | 1.1x | 45s |
| 2 | 6 blocks | 8 HP | 75% | 1.2x | 40s |
| 3 | 7 blocks | 12 HP | 100% | 1.3x | 35s |
| 4 | 8 blocks | 16 HP | 125% | 1.4x | 30s |
| 5 | 10 blocks | 20 HP | 160% | 1.5x | 25s |
