# Origins and Skills Guide

This document provides a descriptive overview of each origin's playstyle and skills. For precise formulas, scaling arrays, and internal logic, see the [Skill Mechanics Technical Reference](SKILL_MECHANICS.md).

---

## 1. Assassin
**The Invisible Predator.** A high-mobility origin that rewards patience and precise strikes.

### Gameplay
- **Stealth Mastery**: Use **Shadow Walk** to vanish from sight. Be careful—moving too close to enemies will drain your stealth gauge rapidly. Entering stealth forces enemies to lose track of you immediately.
- **Execution**: Strike from the shadows—ideally from behind—to deal massive backstab damage. A successful backstab consumes **25%** of your gauge, while a normal strike consumes **50%**.
- **Escape**: After breaking stealth with an attack, you gain a massive speed boost to disengage and wait for your gauge to refill. Recovery is slowed by half while not in stealth.

### Key Stats
- **Stealth Speed**: +15% to +40% (Base Multiplier)
- **Backstab Multiplier**: +30% to +150% (Ambush Buff)
- **Gauge Size**: 400 to 1000

---

## 2. Dark Mage
**The Soul Reaper.** A high-risk, infinite-scaling glass cannon that trades health for absolute power.

### Gameplay
- **Soul Harvest**: Every kill feeds your soul. Souls have no cap—the more you kill, the stronger you become.
### Blood Pact [Toggle]
Trades life force for immense power. While active, HP is drained, but the mage gains massive Spell Power, Crit Chance, and Mana Regeneration based on harvested Souls.
- **Exponential Growth**: Drain doubles every 20s. Soul-based effects scale by 1.5x every 20s.
- **Auto-Shutoff**: Skill deactivates automatically if HP reaches critical levels.
- **Death Defy**: Your **Phylactery** allows you to cheat death. If you take fatal damage while holding souls, you'll be saved at 1 HP. While the cheat-death itself is free, actual death (when the skill is on cooldown) results in a **30% loss** of your soul collection.

### Key Stats
- **Blood Pact HP Drain**: 8% to 4% per second (Initial Rate)
- **Mana Regeneration**: 1.0 + (souls / 200) per second.
- **Death Defy Cooldown**: 5 Minutes.

---

## 3. Elemental Mage
**The Master of Resonance.** An evocation specialist that builds power through sustained elemental damage.

### Gameplay
- **Resonance**: Dealing elemental damage generates Resonance Echoes. 
- **Convergence**: Use your skill to consume stored elemental echoes, regaining mana and gaining a 10s window of guaranteed spell criticals.
- **Elemental Reactions**: Combining different elements on a target (e.g., Fire + Ice for Melt) triggers powerful bonus effects and damage. Each reaction costs 25 Resonance.
- **Nature Cores**: Created via Water + Nature reactions. Strike them with Fire or Lightning to cause massive explosions.
- **Overwhelming Power**: Dealing massive single-hit spell damage (10/30/50+) triggers high-tier "OP" reactions like Miniature Suns, Sandstorms, or Water Columns.
- **Infinite Scaling**: Dealing elemental damage increases your power in that element. Maintaining balance between elements makes you grow stronger faster.

### Key Stats
- **Mana Restore**: Up to 40 + (EM * 25) per echo.
- **Crit Bonuses**: Up to +20% Crit Chance per echo and +50% Crit Damage.

---

## 4. High Priest
**The Divine Commander.** A tactical support and area-denial specialist who demands perfect positioning.

### Gameplay
- **State of Grace**: You are a beacon of hope as long as you are untouched. Taking **any** damage removes your "Grace," stripping you of your cast speed and healing bonuses for 30 seconds.
- **Infinite Scaling (Faith)**: Earn **Faith** by casting Holy spells while in your "Grace" state. Faith permanently increases your Max Mana and grants bonus Holy Power.
- **The Beacon**: Control the **Seraphic Echo**, a divine orb. Moving the orb through allies heals and shields them; moving it through enemies burns and slows them. **Moving the orb from a stationary state** deals 1.5x damage and shielding.
- **Divine Pull**: Consume Command stacks to turn the orb into a gravity well, pulling all nearby enemies into its center for a delayed holy explosion.

### Key Stats
- **Healing Potency**: +30% to +125%
- **Overheal Conversion**: 30% to 75% turned into absorption shields.
- **Beacon Influence**: 48-block control range.

---

## 5. Warrior
**The Combat Maestro.** A frontline juggernaut who thrives on the "Style" of battle.

### Gameplay
- **Style Rank**: Keep attacking to build your Style from D up to SSS. Higher ranks provide massive damage multipliers. At SSS, you gain a one-time "SSS Shield" that negates a single hit of damage—breaking it will reset your Style significantly.
- **Retribution**: Charge **Challenger's Retribution** to adopt a defensive stance. You will taunt all nearby enemies and absorb 100% of incoming damage. 
- **Counter-Attack**: Release the charge to unleash an AoE burst that reflects a portion of all damage absorbed. If your shield breaks during the charge, the counter-attack fails—don't overextend!

### Key Stats
- **Reflect Multiplier**: 50% to 160% of absorbed damage.
- **SSS Damage Max**: 1.5x Multiplier.
- **Shield HP**: Scales with Style Rank and your Max Health.
