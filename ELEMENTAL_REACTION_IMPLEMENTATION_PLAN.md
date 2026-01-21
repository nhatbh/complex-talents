# Elemental Reaction System Implementation Plan

## Overview
This plan outlines the implementation of a comprehensive Elemental Reaction system that integrates with the existing talent system. The system enables reactive combat through elemental stacking and reactions when different elements combine.

## Phase 1: Foundation & Core Architecture

### 1.1 Elemental Stack System
**Goal**: Create the foundation for tracking elemental effects on entities

- Create `ElementType` enum with six elements (Fire, Aqua, Lightning, Ice, Nature, Ender)
- Implement `ElementalStack` data structure to track:
  - Element type
  - Stack duration/expiration
  - Source player/spell information
  - Stack strength/power
- Create `ElementalStackManager` capability to attach to LivingEntity
  - Store active elemental stacks per entity
  - Handle stack application, refresh, and expiration
  - Provide methods to query active stacks
- Implement particle effect system for visual feedback
  - Map each element to distinctive particle effects
  - Render particles above entities with active stacks
  - Update particles on tick based on active stacks

### 1.2 Configuration System
**Goal**: Make all system values configurable for balance tuning

- Create `ElementalReactionConfig` class with sections for:
  - **Core Reaction Multipliers**: Base damage multipliers for each reaction
  - **Mastery Scaling**: General and Specific scaling constants
  - **Duration Values**: Stack durations, debuff durations, DoT timings
  - **Secondary Effects**: Resistance reductions, movement penalties, etc.
- Integrate with existing mod config system
- Provide sensible defaults based on prompt specifications

## Phase 2: Custom Attributes & Talent Integration

### 2.1 Mastery Attribute System
**Goal**: Create the attribute framework for reaction scaling

- Create seven new custom attributes:
  - `ElementalMastery` (general bonus to all reactions)
  - `FireMastery`, `AquaMastery`, `LightningMastery`, `IceMastery`, `NatureMastery`, `EnderMastery` (element-specific bonuses)
- Register attributes with Minecraft's attribute system
- Attach all attributes to players on login/respawn
- Implement diminishing returns formulas:
  - General Mastery: More gradual scaling (e.g., `bonus = mastery / (mastery + scalingConstant)`)
  - Specific Mastery: More aggressive scaling with higher peak values

### 2.2 Talent System Updates
**Goal**: Integrate mastery attributes into talent progression

- Modify existing "Elemental Mastery" talent to grant attribute points instead of percentage bonuses
- Create new element-specific mastery talents (e.g., "Pyromancer", "Cryomancer")
- Update talent descriptions to reflect attribute point grants
- Ensure talent point allocation properly modifies player attributes

## Phase 3: Spell Detection & Element Mapping

### 3.1 Spell Damage Event Hooking
**Goal**: Intercept spell damage to identify elements

- Hook into Iron's Spellbooks primary damage event
- Create `SpellElementDetector` utility class
- Implement spell source inspection logic:
  - Extract spell object from damage source
  - Identify spell's magic school attribute
  - Handle both Iron's Spellbooks and T.O's Spellbooks namespaces

### 3.2 Element Mapping Logic
**Goal**: Map spell schools to ElementType

- Create `SpellToElementMapper` with mapping rules:
  - `fire_spell_power` → Fire
  - `traveloptics:aqua_spell_power` → Aqua
  - `lightning_spell_power` → Lightning
  - `ice_spell_power` → Ice
  - `nature_spell_power` → Nature
  - `ender_spell_power` → Ender
- Support fallback mapping by spell ID for unmapped schools
- Allow configuration for custom spell→element mappings

## Phase 4: Reaction Detection & Triggering

### 4.1 Reaction Detection System
**Goal**: Identify when reactions should occur

- Create `ElementalReactionDetector` class
- On spell damage event:
  - Query target's `ElementalStackManager` for active stacks
  - Compare incoming element with existing stacks
  - Determine if a valid reaction exists
  - Check reaction compatibility matrix

### 4.2 Reaction Compatibility Matrix
**Goal**: Define which elements can react together

- Create reaction mapping table:
  - Fire + Aqua → Vaporize
  - Fire + Ice → Melt
  - Fire + Lightning → Overloaded
  - Aqua + Lightning → Electro-Charged
  - Aqua + Ice → Frozen
  - Ice + Lightning → Superconduct
  - Nature + Fire → Burning
  - Nature + Aqua → Bloom
  - Ender + [Any Element] → Ender-specific reactions
- Handle directional reactions (e.g., Fire triggers vs Aqua triggers for Vaporize)

## Phase 5: Core Reaction Implementation

### 5.1 Damage Calculation Framework
**Goal**: Create unified damage calculation system

- Implement `ReactionDamageCalculator` class with formula:
  ```
  baseDamage = triggeringSpellDamage * baseReactionMultiplier
  generalBonus = elementalMastery / (elementalMastery + generalScalingConstant)
  specificBonus = specificMastery / (specificMastery + specificScalingConstant)
  finalDamage = baseDamage * (1 + generalBonus) * (1 + specificBonus)
  ```
- Support both instant damage and DoT calculations
- Handle average spell power for secondary reactions (Bloom cores)

### 5.2 Core Amplifying Reactions
**Goal**: Implement high-damage burst reactions

- **Vaporize (Fire + Aqua)**
  - Calculate 2.0x (Fire trigger) or 1.5x (Aqua trigger) damage
  - Apply instant damage
  - Create steam cloud particle effect (3s duration, 3-block radius)
  - Implement 25% ranged miss chance debuff for entities in cloud

- **Melt (Fire + Ice)**
  - Calculate 2.0x (Fire trigger) or 1.5x (Ice trigger) damage
  - Apply instant damage
  - Apply Frostbite debuff (6s, -15% armor attribute modifier)

- **Overloaded (Fire + Lightning)**
  - Calculate 1.2x damage in AoE (4-block radius)
  - Apply knockback vector to affected entities
  - Apply Stagger debuff (0.5s action interruption)

### 5.3 Core Sustain Reactions
**Goal**: Implement DoT and crowd control reactions

- **Electro-Charged (Aqua + Lightning)**
  - Create DoT effect (10s duration, 1s tick rate, 0.3x damage per tick)
  - Apply Conductive debuff: flag for next Lightning hit to crit

- **Frozen (Aqua + Ice)**
  - Apply immobilization effect (1.5s-4.5s based on spell power)
  - Apply Brittle debuff: track for first critical hit to Shatter (1.5x crit damage)
  - Handle mobility prevention (no movement, rotation allowed)

- **Superconduct (Ice + Lightning)**
  - Calculate 0.8x damage in small AoE (3-block radius)
  - Apply physical resistance reduction debuff (12s, -40%)
  - Affect all entities in radius

### 5.4 Nature Reactions
**Goal**: Implement nature-based reactions with secondary triggers

- **Burning (Nature + Fire)**
  - Create DoT effect (8s duration, 1s tick rate, 0.5x damage per tick)
  - Implement Panic AI behavior (30% chance per second to flee randomly)

- **Bloom (Nature + Aqua)**
  - Spawn Nature Core entity at target location
  - Set 6s fuse timer
  - Implement core collision detection with Fire/Lightning spells
  - On timeout: despawn with particle effect
  - On trigger: execute secondary reaction

- **Hyperbloom (Bloom Core + Lightning)**
  - Spawn 3-5 tracking projectile entities
  - Target nearby enemies (8-block radius)
  - Calculate 1.5x average spell damage per projectile hit
  - Apply Vulnerable debuff (5s, +20% damage taken)

- **Burgeon (Bloom Core + Fire)**
  - Calculate 2.0x average spell damage in large AoE (6-block radius)
  - Create Smoldering Gloom zone entity (4s duration)
  - Zone applies slow (30%) and DoT to entities within

## Phase 6: Ender Reaction Implementation

### 6.1 Ender Utility Reactions
**Goal**: Implement controlled chaos and utility effects

- **Unstable Ward (Ender + Fire/Aqua/Lightning/Ice)**
  - Spawn collectible shard entity with matching element
  - On pickup: apply buff (60% element resistance, 20% damage amp, 8s duration)
  - Track spell cast counter per player
  - On 3rd spell: trigger small AoE explosion (2-block radius, 0.5x damage)

- **Rift Pull (Ender + Lightning)**
  - Calculate 1.2x Lightning damage
  - Calculate vector from target to caster (8-block pull distance)
  - Apply velocity to target entity
  - Apply Spatial Instability debuff (2s, movement prevention, 50% speed reduction)

- **Singularity (Ender + Fire)**
  - Spawn gravity well entity (6s duration, 5-block radius)
  - Apply inward velocity vector to entities each tick
  - Track and nullify non-player projectiles entering radius
  - Apply small DoT to entities within (0.1x damage per second)

### 6.2 Ender Debuff Reactions
**Goal**: Implement powerful debilitation effects

- **Fracture (Ender + Ice)**
  - Calculate 1.0x Ice damage
  - Apply Fracture debuff (5s duration)
  - Implement damage modification: 25% chance to ignore, 25% increased damage on hit
  - Hook into damage calculation events for target

- **Withering Seed (Ender + Nature)**
  - Apply 10s debuff
  - Implement damage reduction modifier (15% less damage dealt)
  - Hook into target's damage dealt events
  - On damage: apply small Nature damage burst to target, heal caster

- **Decrepit Grasp (Ender + Aqua)**
  - Calculate 0.8x Aqua damage
  - Apply Decrepitude debuff (8s duration)
  - Implement attack speed reduction (30% modifier)
  - Prevent all healing: intercept healing events for target
  - On first heal attempt: convert to Nature damage burst

## Phase 7: Status Effects & Debuffs

### 7.1 Custom Effect Registry
**Goal**: Create all custom status effects

- Implement custom MobEffect classes for:
  - Frostbite (armor reduction)
  - Stagger (action interrupt)
  - Conductive (crit flag)
  - Brittle (shatter bonus)
  - Panic (AI behavior modifier)
  - Vulnerable (damage amplification)
  - Spatial Instability (mobility prevention)
  - Fracture (variable damage)
  - Decrepitude (attack speed, heal prevention)
  - Withering (damage reduction, life siphon)
- Register all effects with Forge/Fabric registry
- Create visual indicators (icon, color, particles)

### 7.2 Effect Behavior Implementation
**Goal**: Implement special effect mechanics

- Implement armor/resistance modification system
- Create action interrupt system for Stagger
- Implement critical hit tracking for Conductive/Brittle
- Create AI behavior override for Panic
- Implement healing interception for Decrepitude
- Create damage modification hooks for Fracture, Vulnerable, Withering

## Phase 8: Visual & Audio Feedback

### 8.1 Particle Systems
**Goal**: Create distinctive visual feedback

- Design particle effects for each element type
- Implement stack indication particles (entity overhead)
- Create reaction trigger particles (burst effects)
- Implement zone particles (steam cloud, Smoldering Gloom, Singularity)
- Create trail particles (Hyperbloom projectiles)

### 8.2 Sound Effects
**Goal**: Provide audio feedback for reactions

- Source/create sound effects for:
  - Each element stack application
  - Each reaction trigger
  - Special effects (Frozen shattering, Singularity pull, etc.)
- Register sounds with Minecraft sound system
- Implement sound playback on reaction triggers

## Phase 9: Integration & Polish

### 9.1 Event System Integration
**Goal**: Ensure proper event handling throughout

- Hook all damage calculation events
- Implement proper event priority for mod compatibility
- Handle event cancellation appropriately
- Ensure proper cleanup on entity death/removal

### 9.2 Networking & Synchronization
**Goal**: Sync system state between client and server

- Create network packets for:
  - Stack application/removal
  - Reaction triggers
  - Effect application
  - Particle spawning
- Implement client-side prediction where appropriate
- Ensure server authority for all damage calculations

### 9.3 Data Persistence
**Goal**: Save elemental state across sessions

- Implement capability serialization for ElementalStackManager
- Save active stacks on entity save
- Restore stacks on entity load
- Handle world unload/reload gracefully

## Phase 10: Testing & Balance

### 10.1 Unit Testing
**Goal**: Verify core system functionality

- Test element detection and mapping
- Test stack application and expiration
- Test reaction detection logic
- Test damage calculation formulas
- Test mastery scaling curves

### 10.2 Integration Testing
**Goal**: Verify system interaction with mods

- Test with Iron's Spellbooks spells
- Test with T.O's Spellbooks spells
- Test with multiple players
- Test with various entity types
- Test edge cases (simultaneous reactions, stack refresh timing)

### 10.3 Balance Tuning
**Goal**: Adjust values for gameplay feel

- Test damage scaling at various mastery levels
- Tune reaction multipliers for power balance
- Adjust debuff durations and intensities
- Balance Ender reactions for utility vs power
- Iterate based on playtesting feedback

## Phase 11: Documentation & Deployment

### 11.1 Code Documentation
**Goal**: Document system for maintainability

- Add JavaDoc comments to all public APIs
- Document configuration options
- Create architecture overview documentation
- Document event hooks and extension points

### 11.2 User Documentation
**Goal**: Help players understand the system

- Create in-game tooltips for effects
- Document all reactions and their triggers
- Explain mastery system mechanics
- Provide strategy guides for element combinations

### 11.3 Deployment Preparation
**Goal**: Prepare for release

- Final configuration review
- Performance profiling and optimization
- Mod compatibility testing
- Create migration path from old talent system if needed

---

## Implementation Notes

### Dependencies
- Iron's Spellbooks (spell damage events, magic schools)
- T.O's Spellbooks (additional spells)
- Existing talent system (attribute integration)

### Technical Considerations
- Use capabilities for per-entity data storage
- Leverage Minecraft's attribute system for mastery
- Use event bus for spell interception
- Client-server synchronization for visual effects
- Performance optimization for particle systems

### Priority Order
1. Foundation (Phase 1-2): Core systems and attributes
2. Detection (Phase 3-4): Spell identification and reaction triggering
3. Core Reactions (Phase 5): Implement most-used reactions first
4. Advanced Reactions (Phase 6): Ender and complex mechanics
5. Polish (Phase 7-9): Effects, feedback, integration
6. Release (Phase 10-11): Testing and documentation
