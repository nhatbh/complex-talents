# Yin Yang Grandmaster - Skills Guide

## Origin Overview

### The Tao of Harmony

*"The laws of the universe dictate the flow of battle."*

The Yin Yang Grandmaster is a **rhythm-based melee origin** that embodies the philosophical concept of harmony through balance. Combat revolves around the eternal dance between Yin and Yang - striking elemental gates in precise alternating patterns while maintaining inner Equilibrium.

**Max Level:** 5

---

### Core Mechanics

#### The Gate System

When a Yin Yang Grandmaster melee attacks an enemy:

1. The **Harmonized** effect is applied to the target
2. Two gates spawn at random compass directions:
   - **Gold Gate (Yang)** - representing light, active, masculine energy
   - **Silver Gate (Yin)** - representing dark, passive, feminine energy
3. Gates appear at one of 8 compass directions: N, NE, E, SE, S, SW, W, NW
4. Must strike gates in **alternating order** (Yang→Yin→Yang or Yin→Yang→Yin)
5. Visual bagua (eight trigrams) circles appear under the entity's feet

#### Hit Results

| Result | Condition | Damage | Equilibrium Effect |
|--------|-----------|--------|-------------------|
| **True Gate** | Hit correct gate type in sequence | True damage + bonus | Tracks pair completion |
| **False Gate** | Hit wrong gate type | **ZERO** | Lose ALL stacks + Discord |
| **Empty Gate** | Hit direction with no gate | Reduced (body penalty) | Lose 1 stack |
| **Body Hit** | No harmonized target | Reduced (body penalty) | None |

#### Discord (Punishment)

When hitting the wrong gate type:
- Lose **all** Equilibrium stacks
- Apply **Nausea** and **Weakness** for 15 seconds
- Wrong gate enters cooldown before respawning

---

## Resource: Equilibrium
- **Maximum Stacks**: 8
- **Gain**: Complete Yin+Yang gate pairs (hit one of each type in alternating order)
- **Lost entirely**: Wrong gate type, 10 seconds of inactivity
- **Lost 1 stack**: Hitting an empty direction (no gate)
- **Effect**: Each stack grants bonus true damage on gate hits

---

## Active Skills

### Sword Dance

*A high-speed dash that strikes gates along its path.*

**Mechanics:**
- Dash 8 blocks horizontally in the direction you're facing
- Activates gates based on dash angle relative to target
- Does not lose Equilibrium on empty gate hit
- Lose all Equilibrium when hitting the wrong gate

**Dash Scenarios:**

| Scenario | Description | Gates Hit |
|----------|-------------|-----------|
| Through Target | Dash path passes through target | Up to 2 gates (start + end angles) |
| Near Target | Dash ends near target | 1 gate (end angle) |
| Miss | Dash doesn't reach target | 0 gates |

**Cooldown Refunds:**
- Both gates correct: **100% refund** (free cast)
- One gate correct: **25% refund**

| Level | Cooldown |
|-------|----------|
| 1 | 30 seconds |
| 2 | 25 seconds |
| 3 | 20 seconds |
| 4 | 15 seconds |

---

## Ultimate Skills

### Eight Formation Battle Array

*Summons eight gates in all directions, surrounding your target in an inescapable formation. Completing the pattern unlocks devastating power.*

**Mechanics:**
- Creates 8 gates at all compass directions (N, NE, E, SE, S, SW, W, NW)
- Random pattern of 4 Yang (Gold) and 4 Yin (Silver) gates
- **Gates do NOT respawn** after being hit
- Must complete all 8 gates correctly to trigger Yin Yang Annihilation
- Hitting the wrong gate immediately clears the effect
- Requires 8 stacks of Equilibrium (does NOT consume them)
- Cannot activate if target already has Exposed effect

**Reward: Yin Yang Annihilation**
- All attacks deal amplified true damage from **any angle**
- No gate restrictions
- Duration equals remaining Exposed time

| Level | Duration | Cooldown |
|-------|----------|----------|
| 1 | 15 seconds | 180 seconds (3 min) |
| 2 | 20 seconds | 160 seconds |
| 3 | 25 seconds | 140 seconds |
| 4 | 25 seconds | 120 seconds |
| 5 | 30 seconds | 100 seconds |

---

## Passive Mechanics

### True Damage Multiplier

All gate hits deal true damage based on origin level.

| Level | Multiplier |
|-------|------------|
| 1 | 1.5x |
| 2 | 1.7x |
| 3 | 2.0x |
| 4 | 2.5x |
| 5 | 3.0x |

---

### Equilibrium Bonus Damage

Each Equilibrium stack adds bonus true damage percentage to gate hits.

| Level | Bonus per Stack | Max Bonus (8 stacks) |
|-------|-----------------|----------------------|
| 1 | +5% | +40% |
| 2 | +8% | +64% |
| 3 | +12% | +96% |
| 4 | +15% | +120% |
| 5 | +15% | +120% |

---

### Gate Cooldown

Time before a hit gate respawns in a new direction.

| Level | Cooldown |
|-------|----------|
| 1 | 2.0 seconds (40 ticks) |
| 2 | 1.75 seconds (35 ticks) |
| 3 | 1.5 seconds (30 ticks) |
| 4 | 1.25 seconds (25 ticks) |
| 5 | 1.0 second (20 ticks) |

---

### Body Hit Penalty

Hitting from wrong angle (no gate) deals reduced damage.

| Level | Damage Reduction | Damage Dealt |
|-------|------------------|--------------|
| 1 | 90% | 10% |
| 2 | 85% | 15% |
| 3 | 80% | 20% |
| 4 | 75% | 25% |
| 5 | 70% | 30% |

---

## Combat Effects

### Harmonized Effect (Standard Combat)
- **Duration**: 10 seconds
- **Gates**: 2 (1 Yin, 1 Yang) at random compass directions
- **Rule**: Must alternate gate types (Yin→Yang→Yin or Yang→Yin→Yang)
- **Gates respawn** after hitting a different gate type

### Exposed Effect (Ultimate)
- **Duration**: 15-30 seconds (based on level)
- **Gates**: 8 (4 Yin, 4 Yang) at all compass directions
- **Rule**: Must hit all 8 gates, correct types only
- **Gates do NOT respawn** - permanently disappear when hit
- **Wrong gate** = effect cleared immediately

### Yin Yang Annihilation (Ultimate Reward)
- **Duration**: Remaining Exposed time
- **Effect**: All attacks deal amplified true damage from any direction
- **No gate restrictions**

---

## Gate Visuals and Indicators

### Gate Colors
- **Yang (Gold)**: #FFD700 - Represents active energy
- **Yin (Silver)**: #C0C0C0 - Represents passive energy
- **Discord**: Dark particles on wrong gate hit
- **Exposed/Ultimate Gates**: Red-tinted overlay

### Compass Directions

| Code | Direction | Angle Range |
|------|-----------|-------------|
| 0 | South | 337.5° - 22.5° |
| 1 | Southwest | 292.5° - 337.5° |
| 2 | West | 247.5° - 292.5° |
| 3 | Northwest | 202.5° - 247.5° |
| 4 | North | 157.5° - 202.5° |
| 5 | Northeast | 112.5° - 157.5° |
| 6 | East | 67.5° - 112.5° |
| 7 | Southeast | 22.5° - 67.5° |

### Bagua Display
- Visual circles appear under harmonized entities
- Shows gate positions for all nearby YYGM players
- Multiple YYGM players can have independent gates on the same target

---

## Skill Interaction Summary

| Skill | Consumes Equilibrium? | Can Build Equilibrium? | Loses Equilibrium on Wrong Gate? |
|-------|----------------------|------------------------|----------------------------------|
| Basic Attack | No | Yes | Yes (all stacks) |
| Sword Dance | No | Yes | No |
| Eight Formation | No (requires 8) | Yes | Yes (all stacks) |
| Annihilation | No | No | No (no gates to hit) |

---

## Advanced Mechanics

### Smart AoE Target Selection
- YYGM uses damage caching to prefer targeting recently hit enemies
- Helps maintain gate focus during multi-target combat
- 10-second cache window for target preference

### Gate Respawn System
- Hit gates enter cooldown before respawning at a **new random direction**
- New gates spawn in unused compass slots when available
- When all 8 slots have been used, the slot pool resets
- This prevents gate clustering and rewards positional awareness

### Equilibrium Decay
- **10 seconds** of inactivity causes total Equilibrium loss
- Timer resets on any gate hit
- Cooldown and timer displayed on HUD

### Cooldown Refund Mechanic (Sword Dance)
When hitting correct gates via basic attack while Sword Dance is on cooldown:
- **50% refund** of remaining cooldown (once per cooldown cycle)
- Encourages mixing basic attacks with dash usage

### Damage Formula

**True Damage (Gate Hit):**
```
finalDamage = baseDamage × (1 + (trueDamageMultiplier - 1) + (equilibrium × equilibriumBonusPercent))
```

**Body Hit Penalty:**
```
finalDamage = baseDamage × (1 - bodyHitDamageReduction)
```

**Example at Level 5 with 8 Equilibrium:**
- Base: 10 damage
- True Damage Multiplier: 2.5x (adds +1.5x)
- Equilibrium Bonus: 8 × 15% = +1.2x
- **Final: 10 × (1 + 1.5 + 1.2) = 37 damage**

---
