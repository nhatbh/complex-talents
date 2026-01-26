---
title: 'Fix Critical Gaps and Misalignments in Elemental Mage Talent System'
slug: 'fix-elemental-mage-gaps'
created: '2026-01-23'
status: 'review'
stepsCompleted: [1, 2, 3]
tech_stack:
  - 'Java 17'
  - 'Minecraft Forge 47.4.10'
  - 'Minecraft 1.20.1'
  - 'Gradle 8.x'
  - 'Iron\'s Spellbooks 3.4.0.11'
files_to_modify:
  - 'src/main/java/com/complextalents/elemental/superreaction/SuperReactionHandler.java'
  - 'src/main/java/com/complextalents/elemental/superreaction/SuperReactionTier.java'
  - 'src/main/java/com/complextalents/elemental/talents/mage/unleash/ElementalUnleashTalent.java'
  - 'src/main/java/com/complextalents/elemental/talents/mage/attunement/ElementalAttunementTalent.java'
  - 'src/main/java/com/complextalents/elemental/talents/mage/ward/ElementalWardTalent.java'
  - 'src/main/java/com/complextalents/elemental/talents/mage/conflux/ElementalConfluxTalent.java'
  - 'src/main/java/com/complextalents/elemental/superreaction/reactions/FireSuperReaction.java'
  - 'src/main/java/com/complextalents/elemental/superreaction/reactions/IceSuperReaction.java'
  - 'src/main/java/com/complextalents/elemental/superreaction/reactions/AquaSuperReaction.java'
  - 'src/main/java/com/complextalents/elemental/superreaction/reactions/LightningSuperReaction.java'
  - 'src/main/java/com/complextalents/elemental/superreaction/reactions/NatureSuperReaction.java'
  - 'src/main/java/com/complextalents/elemental/superreaction/reactions/EnderSuperReaction.java'
files_to_create:
  - 'src/main/java/com/complextalents/elemental/entity/ElementalOrbEntity.java'
  - 'src/main/java/com/complextalents/elemental/entity/LavaPoolEntity.java'
  - 'src/main/java/com/complextalents/elemental/entity/ScorchedEarthEntity.java'
  - 'src/main/java/com/complextalents/elemental/entity/HeartOfTheWildEntity.java'
  - 'src/main/java/com/complextalents/elemental/entity/StormCloudEntity.java'
code_patterns:
  - 'Event-Driven Forge Architecture'
  - 'Capability-based Player Data (TalentsCapabilities)'
  - 'Network Packet Synchronization (SimpleChannel)'
  - 'Static Registry Pattern (DeferredRegister)'
  - 'Entity Registration via EntityType Builder'
  - 'Attribute-based Mastery System'
  - 'Player Persistent Data (NBT CompoundTag)'
  - 'TalentBranches for Path Validation'
test_patterns: ['Manual In-Game Testing']
---

# Tech-Spec: Fix Critical Gaps and Misalignments in Elemental Mage Talent System

**Created:** 2026-01-23

## Overview

### Problem Statement

The Elemental Mage talent system implementation has critical discrepancies between the design specification and actual code implementation. A comprehensive verification report identified:

- **1 Critical Formula Error**: Mastery scaling uses wrong formula affecting ALL damage calculations
- **3 Missing Critical Features**: Basic reaction detonation, Resonant Cascade capstone, multiple TODOs
- **18 Missing Named Super-Reactions**: Generic implementations instead of unique tier-specific mechanics per element
- **1 Missing Entity**: ElementalOrbEntity for Reprisal talent visual effects

While the structural foundation is excellent (70% functionally complete), these gaps prevent the system from achieving design parity and delivering the intended player experience.

### Solution

Systematically fix all identified issues in priority order:
1. Fix mastery scaling formula to use correct tier-based calculation
2. Implement missing critical mechanics (basic detonation, Resonant Cascade)
3. Complete all TODO implementations (lingering field, overload, amplification, orb entity)
4. Refactor all 24 super-reactions to implement unique named mechanics per tier

### Scope

**In Scope:**
- Fix mastery scaling formula in SuperReactionHandler
- Implement basic reaction detonation in ElementalUnleashTalent
- Implement Resonant Cascade (Attunement Rank 4B) capstone
- Complete lingering field effect (Unleash Rank 2B)
- Complete multi-hit overload (Unleash Rank 3A)
- Complete damage amplification (Unleash Rank 3B)
- Create ElementalOrbEntity and implement Reprisal talent
- Refactor all 6 super-reaction classes to implement 24 unique named mechanics
- Add mastery scaling to Blackhole execute threshold
- Add TODO for Loop Casting Iron's Spellbooks integration

**Out of Scope:**
- Iron's Spellbooks integration implementation (TODO only)
- GUI for branch selection
- In-game talent tooltips
- Performance optimization
- Configuration file documentation
- Additional mod integrations beyond Iron's Spellbooks

## Context for Development

### Codebase Patterns

**Event-Driven Forge Architecture:**
- Capability-based player data persistence via `PlayerTalents` (TalentsCapabilities)
- Network packet synchronization using SimpleChannel with version "1"
- Event bus subscription for spell damage (`SpellDamageEvent`), entity updates
- Static registry pattern using `DeferredRegister` for talents, entities, attributes
- Forge event handlers with `@SubscribeEvent` annotation

**Talent System Structure:**
- 5-slot architecture: DEFINITION, HARMONY, CRESCENDO, RESONANCE, FINALE
- Branching talents use `TalentBranches.hasBranch(player, talentId, rank, PathChoice)` for path checking
- Resource system managed through Definition talents (Focus bar for Elemental Mage)
- Level-based arrays for scaling values (pattern: `VALUE[Math.min(level - 1, 3)]`)
- Array indices: level 1-4 maps to index 0-3

**Elemental System:**
- `ElementalStackManager` tracks stacks per entity using UUID-based HashMap
- Stack storage: `Map<UUID, Map<ElementType, ElementStack>>`
- Reactions triggered BEFORE new stack application (check existing elements first)
- Super-reactions consume ALL stacks on target after triggering
- Mastery attributes: general (`ELEMENTAL_MASTERY`) + element-specific (e.g., `FIRE_MASTERY`)
- Attribute values retrieved via `player.getAttributeValue(attribute.get())`

**Entity Creation Pattern (from BloomCoreEntity):**
- Extend `Entity` or `Projectile` base class
- Register via `DeferredRegister<EntityType<?>>` in entity class
- Use `EntityType.Builder` with category `MobCategory.MISC`
- Define synced data with `EntityDataAccessor<T>` and `SynchedEntityData`
- Implement `tick()` for behavior, `defineSynchedData()` for sync
- NBT serialization via `addAdditionalSaveData()` and `readAdditionalSaveData()`

**Damage Application:**
- Use `target.damageSources().playerAttack(caster)` for player-sourced damage
- Direct damage via `target.hurt(damageSource, amount)`
- Area damage uses AABB bounding boxes and `level.getEntitiesOfClass()`

**Player Persistent Data:**
- Store temporary buffs/flags in `player.getPersistentData()`
- Pattern: `player.getPersistentData().putBoolean("flag_name", true)`
- Include expiration timestamps for time-limited effects
- Clean up flags after consumption or expiration

**Super-Reaction Current Implementation:**
- Interface `SuperReaction` with `execute()` method
- Handler retrieves primary element (highest stack count)
- Base damage calculated in handler, passed to reaction implementation
- Reactions registered in static HashMap by element type
- Tier determines damage multiplier and effect intensity

### Files to Reference

| File | Purpose | Key Info |
| ---- | ------- | -------- |
| [IMPLEMENTATION_VERIFICATION_REPORT.md](IMPLEMENTATION_VERIFICATION_REPORT.md) | Complete analysis of all discrepancies with line numbers | Priority-ordered fix list with estimates |
| [SuperReactionHandler.java](src/main/java/com/complextalents/elemental/superreaction/SuperReactionHandler.java) | Main handler for super-reactions | Lines 156-166: wrong mastery formula; Line 132-150: damage calculation |
| [SuperReactionTier.java](src/main/java/com/complextalents/elemental/superreaction/SuperReactionTier.java) | Tier enum definitions | Need to add getScalingFactor() method |
| [ElementalUnleashTalent.java](src/main/java/com/complextalents/elemental/talents/mage/unleash/ElementalUnleashTalent.java) | Crescendo slot talent | Line 169: basic detonation TODO; Line 262: lingering field TODO; Line 272: overload TODO; Line 285: amplification TODO |
| [ElementalAttunementTalent.java](src/main/java/com/complextalents/elemental/talents/mage/attunement/ElementalAttunementTalent.java) | Harmony slot talent (Focus management) | Missing Resonant Cascade (Rank 4B); Lines 135-162: Loop Casting needs integration TODOs |
| [ElementalWardTalent.java](src/main/java/com/complextalents/elemental/talents/mage/ward/ElementalWardTalent.java) | Resonance slot talent | Line 198: Reprisal TODO for orb spawning |
| [ElementalConfluxTalent.java](src/main/java/com/complextalents/elemental/talents/mage/conflux/ElementalConfluxTalent.java) | Finale slot talent | Lines 299-312: Blackhole execute threshold needs mastery scaling |
| [FireSuperReaction.java](src/main/java/com/complextalents/elemental/superreaction/reactions/FireSuperReaction.java) | Fire super-reaction implementation | Example of generic implementation pattern to refactor |
| [ElementalReactionHandler.java](src/main/java/com/complextalents/elemental/ElementalReactionHandler.java) | Basic reaction handler | Reference for reaction trigger pattern; Lines 42-97: complete reaction flow |
| [BloomCoreEntity.java](src/main/java/com/complextalents/elemental/entity/BloomCoreEntity.java) | Bloom reaction entity | Pattern for creating custom entities with lifetime, particles, interactions |
| [MasteryAttributes.java](src/main/java/com/complextalents/elemental/attributes/MasteryAttributes.java) | Mastery attribute definitions | ELEMENTAL_MASTERY + 6 element-specific attributes; Base value 0.0 |
| [architecture.md](docs/architecture.md) | Project architecture documentation | Event-driven patterns, capability system, network architecture |

### Technical Decisions

1. **Mastery Scaling Formula**:
   - Use design spec formula: `Final_Effect = Base_Effect * (1 + Scaling_Factor * (Mastery - 1))`
   - Scaling factors by tier: TIER_1=0.25, TIER_2=0.50, TIER_3=0.75, TIER_4=1.00
   - Mastery base value is 1.0 (not 0) per design document
   - Apply both general and element-specific mastery additively within the formula

2. **Mastery Retrieval**:
   - Get general mastery: `player.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get())`
   - Get element-specific: `player.getAttributeValue(MasteryAttributes.{ELEMENT}_MASTERY.get())`
   - Default value is 0.0 from attribute definition, but formula treats base as 1.0
   - Need helper method to convert attribute value to mastery (attribute + 1.0)

3. **Basic Reaction Detonation**:
   - Determine which 2 elements present from stack map
   - Find appropriate `ElementalReaction` enum value
   - Call `ElementalReactionHandler.triggerReaction(target, reaction, elem1, elem2, player, baseDamage)`
   - Clear both stacks after triggering
   - Apply chain detonation if Path A selected

4. **Super-Reaction Mechanics**:
   - Each tier needs unique named implementation per design document
   - Tier-specific effects INSIDE each reaction class (not generic multipliers)
   - Examples: Fire L2 needs vortex pull + lava pool entity; Ice L3 needs time-stop effect
   - Use switch statements on tier for completely different behaviors

5. **Entity Creation**:
   - Follow `BloomCoreEntity` pattern for all new entities
   - Register in entity's own DeferredRegister (self-contained)
   - ElementalOrbEntity: orbital motion, collision detection, element-based particles
   - Zone entities (lava pool, scorched earth, etc.): stationary, pulse damage, limited lifetime
   - All entities need NBT serialization for persistence

6. **Amplification Damage**:
   - Store amplification in player persistent data before calling SuperReactionHandler
   - Key: "amplification_multiplier" (float)
   - SuperReactionHandler checks and applies in calculateBaseDamage()
   - Clear flag after reaction completes

7. **Overload Multi-Hit**:
   - Store jump count and decay in player persistent data
   - Schedule delayed damage applications (use TickEvent or custom scheduler)
   - Each hit reduces damage by decay percentage

8. **Lingering Field**:
   - Create AreaEffectCloud or custom zone entity at reaction location
   - Store duration from talent config (8/10/12/15 seconds based on level)
   - Pulse every 1-2 seconds to reapply random element stacks to enemies in radius

9. **Iron's Spellbooks Integration**:
   - Add structured TODO comments with clear integration points
   - Mark locations where spell cast events should be hooked
   - Document expected behavior: instant cast, 200% CDR (3x faster), 500% mana regen (6x)
   - User will implement integration later

## Implementation Plan

### Tasks

#### 🔴 CRITICAL PRIORITY (Immediate Fixes - Core Functionality Broken)

**TASK 1: Fix Mastery Scaling Formula**
- **File**: `SuperReactionHandler.java:156-166, 142`
- **Action**:
  1. Modify `getMasteryBonus()` to retrieve mastery attribute value (not talent level)
  2. Add `getScalingFactor()` method to `SuperReactionTier` enum (0.25, 0.50, 0.75, 1.00)
  3. Implement correct formula: `(1 + scalingFactor * (generalMastery - 1) + scalingFactor * (specificMastery - 1))`
  4. Update all damage calculation call sites to use new formula
- **Estimated**: 3-4 hours

**TASK 2: Implement Basic Reaction Detonation**
- **File**: `ElementalUnleashTalent.java:169`
- **Action**:
  1. Replace TODO with logic to determine which 2 elements are present
  2. Call `ElementalReactionHandler.triggerReaction(element1, element2, target, player, baseDamage)`
  3. Clear both element stacks after detonation
  4. Apply talent modifiers (Chain Detonation, etc.) if applicable
- **Estimated**: 2-3 hours

**TASK 3: Implement Resonant Cascade (Attunement Rank 4B)**
- **File**: `ElementalAttunementTalent.java`
- **Action**:
  1. In `checkOverflowTrigger()`, set persistent data flag `resonant_cascade_active` when overflow triggers
  2. Add expiration timestamp (10 second window)
  3. In `SuperReactionHandler`, check for flag before damage calculation
  4. If active: apply 2x damage multiplier + apply all 6 elements to target after reaction
  5. Consume the buff after use
- **Estimated**: 3-4 hours

**TASK 4: Implement Lingering Field Effect (Unleash Rank 2B)**
- **File**: `ElementalUnleashTalent.java:262`
- **Action**:
  1. Create zone entity or AreaEffectCloud at reaction location
  2. Store decay time from talent config (8/10/12/15 seconds)
  3. Every 1-2 seconds, reapply random element stacks to entities in zone radius
  4. Clean up zone entity when duration expires
- **Estimated**: 4-5 hours

**TASK 5: Implement Multi-Hit Overload (Unleash Rank 3A)**
- **File**: `ElementalUnleashTalent.java:272`
- **Action**:
  1. Pass jump count (2/3/4/5) to SuperReactionHandler
  2. Apply reaction damage multiple times to same target
  3. Apply decay per jump (50%/40%/30%/20% less damage each hit)
  4. Schedule hits with small delay (0.1-0.2s between hits) for visual feedback
- **Estimated**: 2-3 hours

**TASK 6: Implement Damage Amplification (Unleash Rank 3B)**
- **File**: `ElementalUnleashTalent.java:285`
- **Action**:
  1. Calculate amplification percentage based on Focus consumed (cap at 50% max Focus)
  2. Store amplification multiplier in player persistent data
  3. In SuperReactionHandler, read amplification multiplier and apply to damage
  4. Clear amplification data after reaction completes
- **Estimated**: 2 hours

---

#### 🟡 HIGH PRIORITY (Major Features Missing)

**TASK 7: Create ElementalOrbEntity**
- **Files**: New file `elemental/entity/ElementalOrbEntity.java`, register in entity registry
- **Action**:
  1. Create custom entity class extending Projectile or AbstractHurtingProjectile
  2. Implement orbital motion around player (sin/cos calculation for rotation)
  3. Add collision detection for enemies
  4. On hit: apply element stack + deal damage with mastery scaling
  5. Lifetime: 30 seconds
  6. Particle effects based on element type
- **Estimated**: 4-5 hours

**TASK 8: Implement Reprisal Orb Spawning (Ward Rank 3B)**
- **File**: `ElementalWardTalent.java:198`
- **Action**:
  1. On successful block with Path B, spawn 2/3/4/5 ElementalOrbEntity instances
  2. Assign random different elements to each orb
  3. Set orb damage: `baseDamage * (1 + 0.5 * (Mastery - 1))`
  4. Initialize orbital behavior around player
- **Estimated**: 2 hours

**TASK 9: Refactor Fire Super-Reactions (4 Tiers)**
- **File**: `FireSuperReaction.java`
- **Action**:
  1. **Tier 1 - Conflagration**: Keep explosion, add 2.5s burn DOT effect
  2. **Tier 2 - Incinerating Maw**: Add vortex pull (vector towards target) before explosion, spawn lava pool entity (4s duration)
  3. **Tier 3 - Solar Judgment**: Add meteor particle trail, calculate missing HP damage component, spawn Scorched Earth zone (3.75s, DOT to entities inside)
  4. **Tier 4 - Ignition**: Replace immediate explosion with 8-second fuse mechanic, apply glowing effect, detonate for 5% max HP + flat damage with particle countdown
- **Estimated**: 5-6 hours

**TASK 10: Refactor Ice Super-Reactions (4 Tiers)**
- **File**: `IceSuperReaction.java`
- **Action**:
  1. **Tier 1 - Frostburst**: Cold wave particle effect + freeze with mastery-scaled duration (1.5s base)
  2. **Tier 2 - Shattering Prism**: Transform target visual (crystal overlay), on next hit shatter for 150% AoE damage with mastery scaling
  3. **Tier 3 - Stasis Field**: Apply time-stop effect (Slowness 255, Weakness 255) in massive radius, mastery-scaled duration (1.5s base)
  4. **Tier 4 - Cryo-Shatter**: Apply debuff converting damage to 125% bonus Poise damage for 10s (requires Poise system hook or damage type conversion)
- **Estimated**: 6-7 hours

**TASK 11: Refactor Aqua Super-Reactions (4 Tiers)**
- **File**: `AquaSuperReaction.java`
- **Action**:
  1. **Tier 1 - Tidal Surge**: Wave particles + knockback + 20% slow (2.5s, mastery-scaled)
  2. **Tier 2 - Tsunami**: Full-screen wave effect, strip potion effects, spawn slow field (17% slow, 2.67s, mastery-scaled)
  3. **Tier 3 - Aegis of Leviathan**: Apply transformation buff to player (8s), grant 12.5% speed, apply 5% damage taken debuff to nearby enemies, mastery-scaled
  4. **Tier 4 - The Great Flood**: Spawn persistent arena flood zone (60s), applies 14% slow to enemies, 30% speed to player, mastery-scaled
- **Estimated**: 5-6 hours

**TASK 12: Refactor Lightning Super-Reactions (4 Tiers)**
- **File**: `LightningSuperReaction.java`
- **Action**:
  1. **Tier 1 - Chain Lightning**: Fix chain count to 4 enemies (currently 3), mastery-scaled damage
  2. **Tier 2 - Thunderclap**: Apply Lightning Rod debuff, spawn scheduled task to chain to 5 enemies every 0.5s, mastery-scaled
  3. **Tier 3 - Planar Storm**: Spawn storm cloud entity above target, on next spell cast by player discharge to 5 enemies for 37.5% spell damage, mastery-scaled
  4. **Tier 4 - Superconductor**: Apply pulsing lightning (every 0.5s) + 50% spell damage amplification debuff, mastery-scaled
- **Estimated**: 6-7 hours

**TASK 13: Refactor Nature Super-Reactions (4 Tiers)**
- **File**: `NatureSuperReaction.java`
- **Action**:
  1. **Tier 1 - Grasping Thorns**: Thorns particles + 1.5s root + bleed DOT, mastery-scaled
  2. **Tier 2 - Jungle's Embrace**: Spawn jungle zone (5s), apply damage + 1s root + silence effect, mastery-scaled durations
  3. **Tier 3 - Avatar of the Wild**: Spawn Heart of the Wild entity at location (similar to BloomCore), pulse damage + root every 1s for 5s, mastery-scaled
  4. **Tier 4 - Verdant Crucible**: Apply spore emission debuff (20s), target emits damaging spores in radius, apply bleed to hit enemies, mastery-scaled
- **Estimated**: 5-6 hours

**TASK 14: Refactor Ender Super-Reactions (4 Tiers)**
- **File**: `EnderSuperReaction.java`
- **Action**:
  1. **Tier 1 - Void Touched**: Apply brand particle effect + 12.5% damage/armor reduction debuff (4s), mastery-scaled
  2. **Tier 2 - Reality Fracture**: Teleport target to void dimension or apply invisibility + invulnerability (1.33s exile), track accumulated damage, apply on return with bleed, mastery-scaled
  3. **Tier 3 - Null Singularity**: Spawn pulling sphere entity, apply Unraveling debuff (25% damage taken, no healing), mastery-scaled
  4. **Tier 4 - Unraveling Nexus**: Spawn rift entity, apply Unraveling debuff to all enemies in range (20% damage taken, no healing, 1% true damage on hit), mastery-scaled
- **Estimated**: 6-8 hours

---

#### 🟢 MEDIUM PRIORITY (Polish & Completeness)

**TASK 15: Add Mastery Scaling to Blackhole Execute**
- **File**: `ElementalConfluxTalent.java:299-312`
- **Action**:
  1. Replace static threshold array with mastery calculation
  2. Use formula: `0.05f * (1 + 0.2f * (masteryValue - 1))`
  3. Retrieve mastery value from player attributes
- **Estimated**: 1 hour

**TASK 16: Add Loop Casting Integration TODOs**
- **File**: `ElementalAttunementTalent.java:135-162`
- **Action**:
  1. Add structured TODO comment for instant cast integration
  2. Add structured TODO comment for cooldown reduction (200% = 3x faster)
  3. Add structured TODO comment for mana regeneration boost (500% = 6x)
  4. Include clear hook points and expected behavior
- **Estimated**: 30 minutes

---

### Acceptance Criteria

**AC1: Mastery Scaling Formula Corrected**
- GIVEN a player with Elemental Mastery = 2
- WHEN they trigger a Tier 1 super-reaction (scaling factor 0.25)
- THEN damage should be `baseDamage * (1 + 0.25 * (2 - 1))` = `baseDamage * 1.25`

**AC2: Basic Reaction Detonation Works**
- GIVEN a target with exactly 2 different element stacks
- WHEN player activates Elemental Unleash and detonates
- THEN the appropriate basic reaction triggers (e.g., Fire + Aqua = Vaporize)
- AND both element stacks are consumed

**AC3: Resonant Cascade Implemented**
- GIVEN player has Attunement Rank 4 Path B selected
- WHEN Overflow triggers (random chance on super-reaction)
- THEN player gains Resonant Cascade buff for 10 seconds
- AND next super-reaction deals 200% damage
- AND applies all 6 element stacks to target after reaction

**AC4: Lingering Field Effect Works**
- GIVEN player has Unleash Rank 2 Path B selected
- WHEN player detonates a super-reaction
- THEN a zone spawns at the reaction location
- AND persists for 8/10/12/15 seconds (based on level)
- AND reapplies random element stacks to enemies inside every 1-2 seconds

**AC5: Multi-Hit Overload Works**
- GIVEN player has Unleash Rank 3 Path A selected
- WHEN player detonates a super-reaction
- THEN damage applies 2/3/4/5 times to the same target
- AND each hit deals 50%/40%/30%/20% less damage than previous

**AC6: Damage Amplification Works**
- GIVEN player has Unleash Rank 3 Path B selected
- AND player has 100 Focus (50% of 150 max = 75 Focus consumed)
- WHEN player detonates a super-reaction
- THEN damage is amplified by percentage based on Focus consumed
- AND Focus is consumed proportionally

**AC7: Elemental Orbs Spawn and Function**
- GIVEN player has Ward Rank 3 Path B selected
- WHEN player successfully blocks damage
- THEN 2/3/4/5 elemental orbs spawn
- AND orbs orbit player for 30 seconds
- AND orbs apply element stacks on enemy collision
- AND orbs deal `baseDamage * (1 + 0.5 * (Mastery - 1))` damage

**AC8-13: All 24 Super-Reactions Have Unique Mechanics**
- GIVEN player triggers Fire Tier 4 super-reaction (Ignition)
- WHEN the reaction activates
- THEN target becomes a living bomb with 8-second fuse
- AND target has glowing particle effect
- AND detonates after 8 seconds for 5% max HP + flat damage
- *(Repeat for all 24 tier-specific mechanics)*

**AC14: Blackhole Execute Uses Mastery Scaling**
- GIVEN player has Conflux Rank 4 Path A selected
- AND player has Elemental Mastery = 3
- WHEN enemy is below `0.05 * (1 + 0.2 * (3 - 1))` = 7% HP
- THEN enemy is executed at Conflux center

**AC15: Loop Casting Has Clear Integration Points**
- GIVEN Loop Casting is activated
- WHEN inspecting the code
- THEN clear TODO comments exist for Iron's Spellbooks integration
- AND integration points are documented for instant cast, cooldown reduction, mana regen

## Additional Context

### Dependencies

**Minecraft Forge**:
- Entity registration system for new entities (ElementalOrbEntity, zone entities)
- Attribute system for mastery values
- Event bus for damage calculations

**Iron's Spellbooks** (Optional - for Loop Casting):
- Spell casting events
- Cooldown management API
- Mana regeneration system

**Existing Systems**:
- `ElementalReactionHandler` for basic reactions
- `ElementalStackManager` for stack tracking
- `TalentBranches` for path validation
- Player persistent data for buff tracking

### Testing Strategy

**Manual Testing by User**:
- Test each priority tier separately
- Verify mastery scaling at different mastery values (1, 2, 3, 5)
- Test all 24 super-reaction mechanics individually
- Test talent branch combinations (Path A vs Path B)
- Verify entity spawning (orbs, zones, special effects)
- Test edge cases (0 Focus, max Resonance, execute threshold)

**Recommended Test Sequence**:
1. Critical fixes first: mastery formula, basic detonation, Resonant Cascade
2. Complete TODOs: lingering field, overload, amplification
3. Entity creation: ElementalOrbEntity + Reprisal
4. Super-reactions element-by-element (Fire → Ice → Aqua → Lightning → Nature → Ender)
5. Polish: Blackhole mastery scaling

### Notes

- **Design Document Reference**: Original request embedded in verification report (lines 11-12) contains full talent system specification
- **Formula Base**: Mastery has a base value of 1 (not 0) per design document
- **Performance**: Zone entities and orb entities may need cleanup logic to prevent memory leaks
- **Particle Density**: Super-reaction particle counts may need config options for performance
- **Poise System**: Ice Tier 4 (Cryo-Shatter) requires Poise damage conversion - may need custom damage type or hook into Iron's Spellbooks Poise system
- **True Damage**: Ender Tier 4 (Unraveling Nexus) requires 1% true damage - bypass armor/resistance
- **Integration Timing**: Loop Casting integration TODOs should be implemented when testing with Iron's Spellbooks mod available
