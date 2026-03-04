---
title: 'Berserker - Rage Engine Skill Module'
slug: 'berserker-rage-engine'
created: '2026-02-05'
status: 'ready-for-dev'
stepsCompleted: [1, 2, 3, 4]
tech_stack:
  - 'Java 17'
  - 'Minecraft Forge 47.4.10'
  - 'Minecraft 1.20.1'
  - 'NBT for data serialization'
files_to_modify:
  - 'src/main/java/com/complextalents/network/PacketHandler.java'
  - 'src/main/java/com/complextalents/TalentsMod.java'
  - 'src/main/java/com/complextalents/skill/SkillRegistry.java'
files_to_create:
  - 'src/main/java/com/complextalents/impl/bsk/BerserkerData.java'
  - 'src/main/java/com/complextalents/impl/bsk/ClientBerserkerData.java'
  - 'src/main/java/com/complextalents/impl/bsk/domain/RageZone.java'
  - 'src/main/java/com/complextalents/impl/bsk/domain/RageCalculator.java'
  - 'src/main/java/com/complextalents/impl/bsk/events/RendingFuryEventHandler.java'
  - 'src/main/java/com/complextalents/impl/bsk/events/ChainLashEventHandler.java'
  - 'src/main/java/com/complextalents/impl/bsk/events/UltimateEventHandler.java'
  - 'src/main/java/com/complextalents/impl/bsk/events/BerserkerStateHandler.java'
  - 'src/main/java/com/complextalents/impl/bsk/skill/ChainLashSkill.java'
  - 'src/main/java/com/complextalents/impl/bsk/skill/RecklessOnslaughtUltimate.java'
  - 'src/main/java/com/complextalents/impl/bsk/origin/BerserkerOrigin.java'
  - 'src/main/java/com/complextalents/network/bsk/BerserkerSyncPacket.java'
  - 'src/main/java/com/complextalents/network/bsk/RendingStrikePacket.java'
  - 'src/main/java/com/complextalents/network/bsk/ChainLashVisualPacket.java'
code_patterns:
  - 'ConcurrentHashMap<UUID, Type> for thread-safe per-player data storage'
  - 'Static methods with auto-sync: modifyState() calls syncToClient() automatically'
  - 'Clamped values: all setters use Math.max(0, Math.min(MAX, value))'
  - 'Time-based decay: track last hit time, compare with current game time'
  - 'SkillBuilder fluent API: .nature().targeting().maxRange().scaledCooldown().onActive().register()'
  - 'OriginBuilder with scaledStat arrays: new double[]{lvl1, lvl2, lvl3, lvl4, lvl5}'
  - 'PacketHandler auto-incrementing packetId for registration'
  - 'Packet triad: encode(FriendlyByteBuf), decode(FriendlyByteBuf), handle(Supplier<Context>)'
  - 'Event handlers with @Mod.EventBusSubscriber, check level.isClientSide() first'
  - 'EventPriority.HIGH for damage calculations to override other mods'
  - 'Tick processing: use (tickCount % N) for performance optimization'
  - 'Server-side movement: teleportTo() for final position, visual packet for interpolation'
test_patterns:
  - 'Manual in-game testing'
  - 'No existing unit tests in codebase'
---

# Tech-Spec: Berserker - Rage Engine Skill Module

**Created:** 2026-02-05

## Overview

### Problem Statement

Need to implement a new Origin (Berserker) with a unique Rage-based resource system that creates high-risk, high-reward gameplay centered around HP manipulation and maintaining an optimal "Sweet Spot" (70-90% Rage).

### Solution

Create a new skill module `impl/bsk/` (Berserker) with:
- **BerserkerData** class tracking Rage (0-100), Combo stacks, Ultimate state, and HP-based penalties
- **Rending Fury Passive** - Combo-based AoE slam triggering at 70%+ Rage with 3 stacks
- **Chain Lash Skill** - 12-block range pull generating +15 Rage based on bounding box comparison
- **Reckless Onslaught Ultimate** - Attack Speed buff with healing-to-absorption conversion and infinite Rending Strikes
- Custom absorption system with stackable tracking
- Server-side movement (following Sword Dance pattern) for Chain Lash pull mechanics

### Scope

**In Scope:**

**Resource System (BerserkerData):**
- Rage tracking (0-100, max capacity scales 100/150/200/300 with Origin level)
- Rage generation from Light Attack hits: base 3/5/7/10 + 2/3/4/5 per additional enemy hit
- HP penalty: 2% max HP lost per 10 Rage held
- Rage decay: -5% per second after 4 seconds out of combat
- Combat timeout tracking
- Combo counter (0-3 stacks, resets on damage taken)
- Last hit time tracking for decay calculation

**Rage Zones:**
- Weakened (0-29%): Damage 30/35/40/50%, Attack Speed -10%
- Warmup (30-69%): Damage scales linearly from Weakened penalty to 100% (formula: `WeakenedDmg + (100% - WeakenedDmg) * ((Rage - 30) / 39)`), HP penalty 5-10%
- Sweet Spot (70-90%): Damage 100% + bonus (1% per 1% max HP lost from Rage penalty), Attack Speed +10%, Current HP burns 2% of Max HP per tick
- Overheated (100%): Damage 30%, cannot gain Rage, Max HP suppressed, drains over 5 seconds then resets. **Ultimate Override:** While Ultimate active, Rage caps at 99% - never enters Overheated during Ultimate.

**Rending Fury Passive:**
- Combo counter max 3 stacks
- Gains stack on hitting enemy (successful AttackEntityEvent)
- Resets to 0 on taking damage from any source (LivingHurtEvent at EventPriority.LOW)
- **Exception:** Rending Strike's 5% HP cost does NOT reset combo (self-damage exception)
- At 70%+ Rage + 3 stacks: next attack triggers Rending Strike
- Rending Strike: 5-block radius 180° arc AoE slam (forward-facing cone), 150% weapon damage, consumes 15 Rage, consumes 5% Current HP (rounded up)
- Fatal damage protection: Uses `EventPriority.HIGHEST` with temporary same-tick immunity flag - sets health to 1 if damage would be fatal, protects against multi-hit damage sources

**Chain Lash Skill:**
- Range: 12 blocks
- Cooldown: 12/11/10/9/8 seconds (scaled with Origin level)
- On hit: pulls player and target toward each other based on bounding box size
- Target > Player: player pulled to target
- Target < Player: target pulled to player
- Equal: meet in middle
- **Collision Handling:** Uses `teleportTo()` with raytrace check - pull fails if path goes through unbreakable blocks
- Generates +15 Rage on hit
- **Rage Overflow:** If Rage gain would exceed max capacity, excess is wasted (capped at 100%)

**Reckless Onslaught Ultimate:**
- Activation requires 80%+ Rage (Sweet Spot)
- Cooldown: 60/55/50/45/40 seconds (scaled with Origin level)
- Max duration cap: 20 seconds (prevents infinite loops with healing)
- On activation: instantly deal damage equal to 50% of Current Max HP (not attribute reduction)
- Gain 60/70/80/100% Attack Speed via attribute modifier
- **Cannot Overheat while active:** Rage caps at 99% during Ultimate, never enters Overheated state
- All healing converted to custom Absorption Hearts (stackable, up to 100% Max Health)
- **Infinite Loop Prevention:** Absorption overflow to Rage is capped at 95% (cannot extend Ultimate beyond reasonable duration)
- Absorption overflow converts to Rage: HP Overhealed * 2/4/6/8 (capped at 95% Rage max)
- Infinite Combo: every hit triggers Rending Strike
- During Ultimate: Rending Strike GENERATES 10% Rage (instead of consuming), still costs 5% Current HP
- **Rage Drain Formula:** `Rage = max(0, Rage - (0.1 + (Rage/100)^2))` per tick (~10 sec duration at full Rage)
- The Ultimate ends when Rage hits 0 or 20 seconds elapses

**Berserker Origin:**
- Stat scaling for all damage values
- Rage capacity tiers
- Cooldown modifiers

**Out of Scope:**
- GUI for Rage meter (can reuse existing HUD patterns later)
- Other skills beyond the three specified
- Talent tree integration beyond basic slot assignment
- Visual effects renderers (can add later following YinYangRenderer pattern)

## Context for Development

### Codebase Patterns

**Reference Implementation (yygm module):**

The Yin Yang Grandmaster (yygm) module provides the primary reference pattern:

```
src/main/java/com/complextalents/impl/yygm/
├── skill/                    # Skill implementations
│   ├── SwordDanceSkill.java
│   └── EightFormationBattleArraySkill.java
├── effect/                   # Status effects
│   └── YinYangEffects.java
├── events/                   # Event handlers
│   └── SwordDanceEventHandler.java
├── state/                    # State management
│   └── YinYangStateManager.java
├── origin/                   # Origin integration
│   └── YinYangGrandmasterOrigin.java
├── client/renderer/          # Client-side rendering
│   └── YinYangRenderer.java
└── EquilibriumData.java      # Resource management
```

**Resource Data Pattern (EquilibriumData):**
- Server-side `ConcurrentHashMap<UUID, Integer>` for tracking
- Static methods for get/set/clear operations
- `syncToClient()` method using packets
- Max stacks, decay, and timeout tracking
- Thread-safe operations

**Skill Registration Pattern (SkillBuilder):**
```java
BuiltSkill skill = SkillBuilder.create("complextalents", "skill_name")
    .nature(SkillNature.ACTIVE)
    .targeting(TargetType.ENTITY)
    .maxRange(12.0)
    .scaledCooldown(new double[]{30.0, 25.0, 20.0, 15.0})
    .onActive(SkillClass::executeMethod)
    .build();
```

**Movement/Physics Pattern (SwordDanceEventHandler):**
- Server-side `teleportTo()` for final position enforcement
- Visual packet sent to client for smooth interpolation
- Tick-based processing with `TickEvent.ServerTickEvent`
- NBT storage for dash state tracking

**Network Packet Pattern:**
- Encode/decode using `FriendlyByteBuf`
- `handle()` method with `NetworkEvent.Context`
- Use `PacketHandler.sendTo()` and `PacketHandler.sendToNearly()`

### Files to Reference

| File | Purpose |
| ---- | ------- |
| src/main/java/com/complextalents/impl/yygm/EquilibriumData.java | ConcurrentHashMap storage, auto-sync, time-based decay pattern |
| src/main/java/com/complextalents/impl/yygm/skill/SwordDanceSkill.java | SkillBuilder usage, NBT state tracking for temporary data |
| src/main/java/com/complextalents/impl/yygm/events/SwordDanceEventHandler.java | Movement/physics pattern, tick processing, teleportTo() usage |
| src/main/java/com/complextalents/impl/yygm/events/YYGMHarmonizationHandler.java | AttackEntityEvent hook for damage bonuses/combo system |
| src/main/java/com/complextalents/impl/yygm/origin/YinYangGrandmasterOrigin.java | OriginBuilder pattern, scaledStat arrays, event bus subscriber |
| src/main/java/com/complextalents/network/PacketHandler.java | Packet registration with auto-incrementing packetId |
| src/main/java/com/complextalents/network/yygm/EquilibriumSyncPacket.java | Packet triad structure: encode/decode/handle |
| src/main/java/com/complextalents/skill/SkillBuilder.java | Fluent API for skill registration |
| src/main/java/com/complextalents/origin/OriginBuilder.java | Fluent API for origin registration |
| src/main/java/com/complextalents/origin/OriginManager.java | getOriginStat() for scaled value retrieval |

**Key Imports Required:**
```java
// Core
import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.origin.OriginBuilder;
import com.complextalents.origin.OriginManager;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;

// Data structures
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Minecraft/Forge
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.fml.common.Mod;
```
| src/main/java/com/complextalents/impl/yygm/skill/SwordDanceSkill.java | SkillBuilder pattern, NBT state tracking |
| src/main/java/com/complextalents/impl/yygm/events/SwordDanceEventHandler.java | Movement/physics pattern, tick processing |
| src/main/java/com/complextalents/impl/yygm/origin/YinYangGrandmasterOrigin.java | Origin registration and stat scaling |
| src/main/java/com/complextalents/network/PacketHandler.java | Packet registration location |
| src/main/java/com/complextalents/skill/SkillRegistry.java | Skill registration location |
| src/main/java/com/complextalents/origin/OriginBuilder.java | Origin builder pattern |

### Technical Decisions

**Architecture Pattern: Hybrid Domain-Data-Sync (Approved via Party Mode)**

```
┌─────────────────────────────────────────────────────────────┐
│                    BERSERKER MODULE                          │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────┐    ┌──────────────────────────────┐   │
│  │  Domain Layer   │    │      Event Handlers          │   │
│  ├─────────────────┤    ├──────────────────────────────┤   │
│  │ RageZone (enum) │◄───┤ RendingFuryEventHandler      │   │
│  │ - WEAKENED      │    │ - AttackEntityEvent hook     │   │
│  │ - WARMUP        │    │ - Check combo + rage        │   │
│  │ - SWEET_SPOT    │    │ - Trigger AoE slam          │   │
│  │ - OVERHEATED    │    │                              │   │
│  └────────┬────────┘    └──────────────────────────────┘   │
│           │                                                    │
│  ┌────────▼────────┐    ┌──────────────────────────────┐   │
│  │ RageCalculator  │    │   ChainLashEventHandler      │   │
│  │ - getZone()     │    │ - Server-side pull physics   │   │
│  │ - getDmgMult()  │    │ - Visual packet to client    │   │
│  │ - getASMod()    │    └──────────────────────────────┘   │
│  │ - getHPPenalty()│                                      │
│  └────────┬────────┘    ┌──────────────────────────────┐   │
│           │             │  UltimateEventHandler        │   │
│  ┌────────▼────────┤    │ - LivingHealEvent hook       │   │
│  │ BerserkerData   │    │ - Convert to absorption     │   │
│  │ ├─ rage         │    │ - Overflow → Rage           │   │
│  │ ├─ comboStacks  │    │ - Infinite Rending mode     │   │
│  │ ├─ ultActive    │    └──────────────────────────────┘   │
│  │ └─ absorption   │                                      │
│  └────────┬────────┘    ┌──────────────────────────────┐   │
│           │             │  BerserkerStateHandler       │   │
│  ┌────────▼────────┤    │ - ServerTickEvent            │   │
│  │ Network Layer   │    │ - Rage decay                │   │
│  ├─────────────────┤    │ - Zone transitions          │   │
│  │ SyncPacket      │    │ - HP burn in Sweet Spot     │   │
│  │ - rage          │    └──────────────────────────────┘   │
│  │ - zone          │                                      │
│  │ - combo         │    ┌──────────────────────────────┐   │
│  │ - ultActive     │    │  Origin Integration          │   │
│  └─────────────────┘    │  BerserkerOrigin.java        │   │
│                         │  - Stat scaling              │   │
│                         │  - Skill registration        │   │
│                         └──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**Key Architectural Principles:**
1. **Rage is source of truth** - All derived values calculated on-demand via `RageCalculator`
2. **Zone is a function of Rage** - Not stored, computed from current Rage value
3. **Event handlers are thin** - They coordinate, don't contain business logic
4. **Data layer is dumb** - Just `ConcurrentHashMap` storage + sync methods
5. **Single sync packet** - Aggregate everything to minimize network traffic

**File Structure:**
```
src/main/java/com/complextalents/impl/bsk/
├── BerserkerData.java              # Resource storage (like EquilibriumData)
├── domain/
│   ├── RageZone.java               # Enum with stat calculations
│   └── RageCalculator.java         # Static methods for derived values
├── events/
│   ├── RendingFuryEventHandler.java    # Combo + AoE slam
│   ├── ChainLashEventHandler.java      # Pull physics
│   ├── UltimateEventHandler.java       # Heal conversion, infinite combo
│   └── BerserkerStateHandler.java      # Tick processing, decay
├── skill/
│   ├── ChainLashSkill.java        # SkillBuilder registration
│   └── RecklessOnslaughtUltimate.java
└── origin/
    └── BerserkerOrigin.java       # OriginBuilder registration
```

**Specific Implementation Decisions:**
1. **Module Name:** `bsk` (abbreviation of Berserker) - keeps package names shorter like yygm
2. **Resource Tracking:** Dedicated `BerserkerData` class with `ConcurrentHashMap<UUID, Integer>` for server-side tracking
3. **Server-Authoritative Design:** ALL logic executes on server side, client is purely a display/render target
4. **Client Data Class:** `ClientBerserkerData` stores last-synced values for HUD rendering only, NEVER modifies game state
5. **Data Cleanup:** `cleanup(UUID)` method clears all data on logout/origin change to prevent memory leaks
6. **HP Terminology:** "Max HP attribute" = permanent stat reduction, "Current HP" = actual health points, "HP damage" = reduces Current HP only
7. **Absorption System:** Custom tracking separate from vanilla absorption, synced via BerserkerSyncPacket, rendered via client override
8. **Movement:** Server-side `teleportTo()` with visual packets (SwordDance pattern) for Chain Lash
9. **Damage Calculation:** Hook into `AttackEntityEvent` for Rage generation and Rending Strike detection
10. **HP Penalty:** Applied as damage to Current HP when in Sweet Spot (2% Max HP per tick), calculated from current Rage
11. **Fatal Damage Protection:** Use `EventPriority.HIGHEST` on `LivingHurtEvent` with temporary immunity flag for same-tick protection
12. **Rage Decay:** Track last combat time, apply decay after 4 seconds (80 ticks) of no damage dealt/taken
13. **Ultimate Rage Drain:** Formula: `Rage = max(0, Rage - (0.1 + (Rage/100)^2))` per tick (accelerating decay, ~10 seconds at full)
14. **Combo System:** Simple counter with timestamp, resets on any damage received via `LivingHurtEvent` at `EventPriority.LOW`
15. **Ultimate Overheated Override:** While Ultimate is active, player cannot enter Overheated state. Rage caps at 99% during Ultimate.

**Server-Client Split Pattern:**
```
SERVER SIDE (Authoritative):
├── BerserkerData.java          - All state, all calculations
├── domain/                     - Business logic (zone, damage calc)
├── events/                     - ALL game logic, state changes
├── skill/                      - Skill execution
└── syncToClient()              - Push state to client after any change

CLIENT SIDE (Display Only):
├── ClientBerserkerData.java    - Read-only cache for HUD
├── BerserkerSyncPacket.handle() - Update cache from server
└── (future) BerserkerRenderer  - Render rage meter, combo counter
```

## Implementation Plan

### Tasks

#### Phase 1: Core Resource System

- [ ] **Task 1:** Create BerserkerData resource class
  - File: `src/main/java/com/complextalents/impl/bsk/BerserkerData.java`
  - Action: Implement Rage tracking (0-100), Combo counter (0-3), Ultimate state, combat timeout
  - Notes: Follow EquilibriumData pattern with ConcurrentHashMap and sync methods

- [ ] **Task 2:** Create Berserker network sync packet
  - File: `src/main/java/com/complextalents/network/bsk/BerserkerSyncPacket.java`
  - Action: Packet syncing Rage, Combo stacks, Ultimate active state, Max HP penalty
  - Notes: Include all values needed for client-side HUD rendering

- [ ] **Task 3:** Create BerserkerState enum and manager
  - File: `src/main/java/com/complextalents/impl/bsk/domain/RageZone.java`
  - Action: Define WEAKENED (0-29%), WARMUP (30-69%), SWEET_SPOT (70-90%), OVERHEATED (100%) enum with stat modifiers
  - File: `src/main/java/com/complextalents/impl/bsk/domain/RageCalculator.java`
  - Action: Static methods for damage scaling (linear interpolation in Warmup), zone transitions
  - Notes: Use `domain/` package, not `state/` - Rage is source of truth, zone is computed

- [ ] **Task 3.5:** Implement player data cleanup
  - File: `src/main/java/com/complextalents/impl/bsk/BerserkerData.java`
  - Action: Add `cleanup(UUID)` method that clears all ConcurrentHashMap entries
  - Hook into: `PlayerEvent.PlayerLoggedOutEvent` and Origin change events
  - Notes: Follow EquilibriumData.cleanup() pattern - prevents memory leaks and stale state

#### Phase 2: Rage Zone Mechanics

- [ ] **Task 4:** Implement Rage zone stat calculations
  - File: `src/main/java/com/complextalents/impl/bsk/domain/RageCalculator.java`
  - Action: Calculate damage multiplier, attack speed modifier, HP penalty per zone
  - Notes: Weakened: 30/35/40/50% dmg, -10% AS; Warmup: linear interpolation to 100%; Sweet Spot: 100% + (HP lost * bonus%), +10% AS

- [ ] **Task 5:** Implement Rage decay system
  - File: `src/main/java/com/complextalents/impl/bsk/events/BerserkerStateHandler.java`
  - Action: Track last combat time (game ticks), apply -5% per second after 80 ticks (4 sec) out of combat
  - Notes: Combat = dealing damage (AttackEntityEvent) OR taking damage (LivingHurtEvent, excluding self-damage)
  - Event Priority: Use `EventPriority.NORMAL` for tick processing

- [ ] **Task 6:** Implement HP penalty from Rage (Sweet Spot burn)
  - File: `src/main/java/com/complextalents/impl/bsk/events/BerserkerStateHandler.java`
  - Action: In Sweet Spot (70-90% Rage), apply 2% of Max HP as damage to Current HP each tick
  - Notes: Can reduce Current HP but not below 1. Use `player.hurt()` with custom damage source

#### Phase 3: Rending Fury Passive

- [ ] **Task 7:** Create Combo system
  - File: `src/main/java/com/complextalents/impl/bsk/BerserkerData.java`
  - Action: Track combo stacks (0-3), add `pendingRendingStrike` flag, `fatalImmunityTicks` counter
  - Notes: Gain stack on successful hit, reset on damage taken (except self-damage from Rending Strike)

- [ ] **Task 8:** Create Rending Strike event handler
  - File: `src/main/java/com/complextalents/impl/bsk/events/RendingFuryEventHandler.java`
  - Action: Hook into `AttackEntityEvent` at `EventPriority.NORMAL`, check Rage >= 70% and Combo == 3, set pending flag
  - Notes: On next attack with pending flag: trigger 5-block radius 180° arc AoE, 150% weapon damage, consume 15 Rage, consume 5% Current HP (rounded up)

- [ ] **Task 9:** Implement fatal damage protection (multi-hit safe)
  - File: `src/main/java/com/complextalents/impl/bsk/events/RendingFuryEventHandler.java`
  - Action: Use `EventPriority.HIGHEST` on `LivingHurtEvent` with same-tick immunity flag
  - Notes: Before applying HP cost, check if fatal. If yes, set health to 1 and set `fatalImmunityTicks = 1`. Subsequent damage in same tick checks immunity flag first

- [ ] **Task 10:** Create Rending Strike visual packet
  - File: `src/main/java/com/complextalents/network/bsk/RendingStrikePacket.java`
  - Action: Client-side 5-block radius 180° arc AoE visual effect
  - Notes: Can add renderer later

#### Phase 4: Chain Lash Skill

- [ ] **Task 11:** Create ChainLashSkill
  - File: `src/main/java/com/complextalents/impl/bsk/skill/ChainLashSkill.java`
  - Action: Implement 12-block range pull based on bounding box comparison
  - Notes: Use SkillBuilder, targeting ENTITY, maxRange 12.0, scaledCooldown 12/11/10/9/8

- [ ] **Task 12:** Implement pull physics with collision handling
  - File: `src/main/java/com/complextalents/impl/bsk/skill/ChainLashSkill.java`
  - Action: Compare bounding box sizes, raytrace for collision, pull smaller entity toward larger
  - Notes: If path blocked by unbreakable blocks, pull fails. Target > Player: player to target; Target < Player: target to player; Equal: meet middle
  - Collision: Use `ClipContext.Block.COLLIDER` with `level.clip()` to check path

- [ ] **Task 13:** Create Chain Lash movement handler
  - File: `src/main/java/com/complextalents/impl/bsk/events/ChainLashEventHandler.java`
  - Action: Server-side `teleportTo()` for final position, visual packet for smooth interpolation
  - Notes: Follow SwordDanceEventHandler pattern

- [ ] **Task 14:** Implement Chain Lash Rage generation with overflow cap
  - File: `src/main/java/com/complextalents/impl/bsk/skill/ChainLashSkill.java`
  - Action: Add +15 Rage on successful hit, cap at max capacity (100%)
  - Notes: Call `BerserkerData.addRage()` which clamps to max, then sync

#### Phase 5: Reckless Onslaught Ultimate

- [ ] **Task 15:** Create RecklessOnslaughtUltimate
  - File: `src/main/java/com/complextalents/impl/bsk/skill/RecklessOnslaughtUltimate.java`
  - Action: Implement Ultimate activation requiring 80%+ Rage, max duration 20 seconds
  - Notes: Use SkillBuilder with activation validation, scaledCooldown 60/55/50/45/40

- [ ] **Task 16:** Implement activation sacrifice (Current HP damage)
  - File: `src/main/java/com/complextalents/impl/bsk/skill/RecklessOnslaughtUltimate.java`
  - Action: On activation, deal damage equal to 50% of Current Max HP to player
  - Notes: This is Current HP damage, not attribute reduction. Triggers Sweet Spot damage bonus via HP lost

- [ ] **Task 17:** Create custom Absorption tracking
  - File: `src/main/java/com/complextalents/impl/bsk/BerserkerData.java`
  - Action: Track custom absorption hearts (float, 0 to Max HP), sync in BerserkerSyncPacket
  - Notes: Separate from vanilla absorption. ClientBerserkerData stores for HUD rendering. Up to 100% Max Health cap

- [ ] **Task 18:** Implement healing-to-absorption conversion with overflow cap
  - File: `src/main/java/com/complextalents/impl/bsk/events/UltimateEventHandler.java`
  - Action: Hook into `LivingHealEvent` at `EventPriority.HIGH`, cancel heal, convert to custom absorption
  - Notes: Overflow Rage = HP Overhealed * 2/4/6/8 (level-scaled), but capped at 95% Rage to prevent infinite Ultimate
  - Priority: HIGH to override vanilla healing

- [ ] **Task 19:** Implement Infinite Combo mode with Overheated override
  - File: `src/main/java/com/complextalents/impl/bsk/events/UltimateEventHandler.java`
  - Action: Every hit triggers Rending Strike, which GENERATES 10% Rage (instead of consuming)
  - Notes: Still costs 5% Current HP per hit. Rage caps at 99% during Ultimate (never enters Overheated)

- [ ] **Task 20:** Implement Ultimate Rage drain with max duration
  - File: `src/main/java/com/complextalents/impl/bsk/events/UltimateEventHandler.java`
  - Action: Per-tick drain formula: `Rage = max(0, Rage - (0.1 + (Rage/100)^2))`, track duration, end at 0 Rage or 20 seconds
  - Notes: ~10 second duration at full Rage. Use `EventPriority.NORMAL` on tick handler
  - Notes: Prevent overheating during Ultimate

#### Phase 6: Origin and Integration

- [ ] **Task 21:** Create Berserker Origin
  - File: `src/main/java/com/complextalents/impl/bsk/origin/BerserkerOrigin.java`
  - Action: Register Origin with stat scaling, Rage capacity tiers, skill assignment
  - Notes: Use OriginBuilder pattern, add isBerserker() helper method

- [ ] **Task 22:** Implement ClientBerserkerData
  - File: `src/main/java/com/complextalents/impl/bsk/ClientBerserkerData.java`
  - Action: Create read-only cache with fields: rage, comboStacks, zone, absorption, ultActive, ultDuration
  - Notes: Store synced values from BerserkerSyncPacket, used by BerserkerRenderer (future)

- [ ] **Task 23:** Register skills and Origin
  - File: `src/main/java/com/complextalents/TalentsMod.java`
  - Action: Initialize BerserkerData, register all skills, register Berserker Origin
  - Notes: Follow existing initialization pattern

- [ ] **Task 24:** Register packets with proper IDs
  - File: `src/main/java/com/complextalents/network/PacketHandler.java`
  - Action: Register BerserkerSyncPacket, RendingStrikePacket, ChainLashVisualPacket
  - Notes: Use auto-incrementing packetId, add to register() method

- [ ] **Task 25:** Implement debug commands
  - File: `src/main/java/com/complextalents/impl/bsk/commands/BerserkerDebugCommand.java`
  - Action: Create `/berserker rage <player> <amount>`, `/berserker combo <player> <stacks>`, `/berserker ultimate <player>`, `/berserker debug <player>`
  - Notes: Use Commands.command() registration pattern, server-side only

### Acceptance Criteria

#### Resource System

- [ ] **AC1:** Given a Berserker player, when they deal damage, then Rage increases by base (3/5/7/10) + per-enemy bonus (2/3/4/5)
- [ ] **AC2:** Given a Berserker with 50 Rage, when 4 seconds pass without combat, then Rage decays by 5% per second
- [ ] **AC3:** Given a Berserker with 80 Rage, when calculating stats, then Max HP is reduced by 16% (2% per 10 Rage)
- [ ] **AC4:** Given a Berserker at 100 Rage, when entering Overheated state, then cannot gain more Rage and drains over 5 seconds

#### Rage Zones

- [ ] **AC5:** Given a Berserker with 15% Rage, when attacking, then damage is reduced to base penalty (30/35/40/50%) and attack speed -10%
- [ ] **AC6:** Given a Berserker with 80% Rage (Sweet Spot), when attacking, then damage is 100% + bonus based on HP lost
- [ ] **AC7:** Given a Berserker in Sweet Spot, when a second passes, then Max HP burns an additional 2%
- [ ] **AC8:** Given a Berserker at 100% Rage, when entering Overheated, then damage drops to 30% and Rage is suppressed

#### Rending Fury

- [ ] **AC9:** Given a Berserker hitting enemies, when they hit 3 times without taking damage, then combo counter is 3
- [ ] **AC10:** Given a Berserker with 3 combo stacks, when they take damage, then combo resets to 0
- [ ] **AC11:** Given a Berserker with 70%+ Rage and 3 combo stacks, when they attack, then Rending Strike triggers (180° AoE, 150% dmg)
- [ ] **AC12:** Given Rending Strike triggering, when it would kill the player (5% HP cost), then health is set to 1 instead
- [ ] **AC13:** Given Rending Strike triggering, when execution completes, then 15 Rage is consumed and combo is retained

#### Chain Lash

- [ ] **AC14:** Given a Berserker using Chain Lash on a larger enemy, when it hits, then player is pulled toward enemy
- [ ] **AC15:** Given a Berserker using Chain Lash on a smaller enemy, when it hits, then enemy is pulled toward player
- [ ] **AC16:** Given Chain Lash hitting, when execution completes, then +15 Rage is added and synced

#### Reckless Onslaught Ultimate

- [ ] **AC17:** Given a Berserker with 80%+ Rage, when activating Ultimate, then instantly loses 50% of Max HP
- [ ] **AC18:** Given Ultimate active, when player would receive healing, then it converts to Absorption (up to 100% Max HP)
- [ ] **AC19:** Given Ultimate active with full absorption, when overhealed, then excess converts to Rage (HP * 2/4/6/8)
- [ ] **AC20:** Given Ultimate active, when any hit lands, then Rending Strike triggers and GENERATES 10% Rage
- [ ] **AC21:** Given Ultimate active, when Rage reaches 0, then Ultimate ends

#### Origin

- [ ] **AC22:** Given a player selecting Berserker Origin, when checking stats, then Rage capacity is 100 (scales with level)
- [ ] **AC23:** Given a Berserker at Origin level 2, when checking Rage capacity, then max is 150

#### Edge Cases (Adversarial Review Coverage)

- [ ] **AC24:** Given a Berserker logging out with 50 Rage, when they rejoin, then Rage is 0 (cleanup worked)
- [ ] **AC25:** Given a Berserker with 1 HP taking fatal damage from Rending Strike, when thorns also hits in same tick, then health is set to 1 (multi-hit protection)
- [ ] **AC26:** Given a Berserker during Ultimate at 99% Rage, when Rending Strike generates 10% Rage, then Rage remains at 99% (Overheated override)
- [ ] **AC27:** Given a Berserker during Ultimate receiving continuous healing, when absorption overflows, then Rage caps at 95% (infinite loop prevention)
- [ ] **AC28:** Given a Berserker at 95% Rage who would gain 20 Rage, when processed, then Rage is 100% (overflow wasted)
- [ ] **AC29:** Given a Berserker with 3 combo stacks triggering Rending Strike, when 5% HP cost is applied, then combo remains at 3 (self-damage exception)
- [ ] **AC30:** Given Chain Lash targeting an enemy behind a wall, when executed, then pull fails (collision check worked)
- [ ] **AC31:** Given a Berserker at 69% Rage who gains 2 Rage, when crossing into Sweet Spot, then HP burn begins immediately (boundary test)

## Additional Context

### Dependencies

**Minecraft/Forge Systems:**
- Forge Capability system for skill data
- Forge EventBus for attack, heal, and tick events
- ServerPlayer for player interaction
- TickEvent.ServerTickEvent for per-tick processing
- AttackEntityEvent for combo gain and Rending Strike detection (`EventPriority.NORMAL`)
- LivingHurtEvent for damage taken (combo reset) and fatal protection (`EventPriority.LOW` for reset, `EventPriority.HIGHEST` for protection)
- LivingHealEvent for healing-to-absorption conversion (`EventPriority.HIGH`)
- PlayerEvent.PlayerLoggedOutEvent for data cleanup (`EventPriority.NORMAL`)
- NBT CompoundTag for data serialization
- FriendlyByteBuf for network packets
- Attributes system for damage and attack speed modifiers (using `AttributeModifier`)

**Project Systems:**
- SkillBuilder for skill registration
- OriginBuilder for Origin registration
- SkillRegistry for skill storage
- PacketHandler for network distribution
- TalentsMod.LOGGER for debug logging

**External Requirements:**
- None (pure Minecraft Forge mod)

### Testing Strategy

**Manual Testing Steps:**

**Phase 1: Resource System**
1. Hit enemy - verify Rage generation
2. Wait 5 seconds without combat - verify Rage decay
3. Check Max HP at various Rage levels - verify penalty
4. Reach 100% Rage - verify Overheated state

**Phase 2: Rending Fury**
1. Hit 3 times without taking damage - verify combo 3
2. Take damage - verify combo reset
3. Get 70%+ Rage and 3 combo, attack - verify Rending Strike
4. Test fatal damage protection - verify health set to 1

**Phase 3: Chain Lash**
1. Use Chain Lash on large mob - verify player pulled
2. Use Chain Lash on small mob - verify mob pulled
3. Verify +15 Rage on hit

**Phase 4: Ultimate**
1. Get 80%+ Rage, activate Ultimate - verify 50% Max HP sacrifice
2. Receive healing during Ultimate - verify absorption conversion
3. Overheal with full absorption - verify Rage conversion
4. Land hits during Ultimate - verify infinite Rending Strike with Rage generation
5. Wait for Rage drain - verify Ultimate ends at 0

**Debug Commands Needed:**
- `/berserker rage <player> <amount>` - Set Rage value
- `/berserker combo <player> <stacks>` - Set combo stacks
- `/berserker ultimate <player>` - Toggle Ultimate
- `/berserker absorption <player> <amount>` - Set absorption
- `/berserker debug <player>` - Dump full Berserker data

**Known Testing Limitations:**
- No automated unit tests (manual only)
- No CI/CD pipeline
- Testing requires full Minecraft client/server

### Notes

**Implementation Risks:**
- **Performance Impact:** Per-tick Rage decay and HP burn could cause lag with many players
  - Mitigation: Use efficient data structures, batch operations
- **State Synchronization:** Client-server desync possible with complex Rage states
  - Mitigation: Frequent sync packets, dirty flag pattern
- **HP Penalty Confusion:** Players may not understand why HP is decreasing
  - Mitigation: Clear visual feedback, damage source indication
- **Fatal Damage Edge Cases:** Multiple damage sources in same tick could bypass protection
  - Mitigation: Use `EventPriority.HIGHEST` with same-tick immunity flag (addressed in spec)
- **Infinite Ultimate Duration:** Continuous healing during Ultimate could extend it indefinitely
  - Mitigation: Max duration cap (20 sec) and Rage overflow cap at 95% (addressed in spec)
- **Memory Leaks:** Player data not cleaned up on logout/origin change
  - Mitigation: Task 3.5 implements cleanup() method (addressed in spec)

**Known Limitations:**
- No GUI for Rage meter (TODO)
- No visual effects for Rending Strike/Chain Lash (TODO)
- Single Origin implementation
- No integration with other mod systems yet

**Balance Notes:**
- Rage generation values may need tuning based on playtesting
- HP penalty from Rage creates tension between damage and survivability
- Sweet Spot is the optimal zone - skilled players stay here
- Overheated is punishing - forces players to manage Rage carefully
- Ultimate is high-risk/high-reward - 50% Max HP sacrifice for massive DPS

**Future Enhancements:**
- Rage bar HUD with zone indicators
- Combo counter visual display
- Rending Strike particle effects
- Chain Lash chain visualization
- Ultimate activation visual effect
- Sound effects for all abilities
- Config file for balance values

---

## Adversarial Review Findings (Addressed)

This spec was reviewed through adversarial analysis. All 24 findings have been addressed:

**Critical (4) - All Addressed:**
- F1: Player data cleanup → Task 3.5 added
- F2: Multi-hit fatal damage bypass → Task 9 updated with same-tick immunity flag
- F3: Overheated vs Ultimate contradiction → Spec clarified: Rage caps at 99% during Ultimate
- F4: Infinite loop with healing → Max duration 20s + overflow cap at 95% Rage added

**High (7) - All Addressed:**
- F5: HP terminology → Clarified Max HP attribute vs Current HP vs HP damage
- F6: Event priorities → Specified for all handlers (NORMAL, LOW, HIGH, HIGHEST)
- F7: Absorption system → Task 17 details custom tracking with sync packet
- F8: AoE radius → Specified as 5-block radius 180° arc
- F9: Chain Lash collision → Task 12 adds raytrace collision check
- F10: Rage overflow → Task 14 specifies cap at 100%
- F11: Exponential drain formula → Task 20: `Rage = max(0, Rage - (0.1 + (Rage/100)^2))`

**Medium (7) - All Addressed:**
- F12: ClientBerserkerData → Task 22 added
- F13: Rage generation formula → Clarified per-enemy bonus with no explicit limit (natural cap at 100%)
- F14: Sweet Spot boundary → AC31 tests 69% → 70% transition
- F15: Combat timeout → Specified: damage dealt OR taken, 80 ticks (4 sec)
- F16: Rending Strike combo retention → AC29 tests self-damage exception
- F17: Debug commands → Task 25 added

**Low (6) - All Addressed:**
- F18: Package naming → Unified to `domain/` for RageZone/RageCalculator
- F19: Cooldown values → Added for all skills
- F20: Attack speed method → Specified as AttributeModifier
- F21: Next attack detection → Task 7 adds `pendingRendingStrike` flag
- F22: File naming → Consistent EventHandler naming
- F23: Warmup scaling → Linear interpolation formula provided
