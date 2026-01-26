---
title: 'Complete Talent System Refactor to Modifier-Based Architecture'
slug: 'complete-talent-modifier-refactor'
created: '2026-01-24'
status: 'review'
stepsCompleted: [1, 2, 3]
tech_stack:
  - 'Java 17'
  - 'Minecraft Forge 47.4.10'
  - 'Minecraft 1.20.1'
  - 'Gradle 8.x Build System'
files_to_modify:
  - 'src/main/java/com/complextalents/talent/Talent.java'
  - 'src/main/java/com/complextalents/talent/TalentRegistry.java'
  - 'src/main/java/com/complextalents/capability/PlayerTalents.java'
  - 'src/main/java/com/complextalents/capability/PlayerTalentsImpl.java'
  - 'src/main/java/com/complextalents/command/TalentCommand.java'
  - 'src/main/java/com/complextalents/network/SyncTalentsPacket.java'
  - 'src/main/java/com/complextalents/network/TalentActivationPacket.java'
files_to_create:
  - 'src/main/java/com/complextalents/talent/TalentContext.java'
  - 'src/main/java/com/complextalents/talent/TalentModifier.java'
  - 'src/main/java/com/complextalents/talent/TalentNode.java'
  - 'src/main/java/com/complextalents/talent/TalentTree.java'
  - 'src/main/java/com/complextalents/talent/TalentExecutor.java'
  - 'src/main/java/com/complextalents/capability/ITalentData.java'
  - 'src/main/java/com/complextalents/capability/TalentData.java'
code_patterns:
  - 'Forge Capability System for player data'
  - 'Static Registry pattern (DeferredRegister)'
  - 'Event-driven architecture (@SubscribeEvent)'
  - 'NBT serialization for persistence'
  - 'Network packets via SimpleChannel'
  - 'Command registration via Brigadier'
  - 'ResourceLocation for IDs'
test_patterns:
  - 'Manual in-game testing only'
  - 'No unit test framework currently'
---

# Tech-Spec: Complete Talent System Refactor to Modifier-Based Architecture

**Created:** 2026-01-24

## Overview

### Problem Statement

The current talent system uses inheritance-based branching with rank checks scattered throughout implementation code. This creates:
- Complex if-else chains checking ranks and branches (`if (rank >= 3 && branch == A)`)
- Duplicated behavior across talent variations
- Difficulty adding new branches or ranks
- Tight coupling between base talent and its variations
- Hard-to-track state mutations across different ranks
- Multiple inheritance hierarchies (ActiveTalent, PassiveTalent, HybridTalent, Branching variants)

The system needs a complete refactor to a modifier-based architecture where talents are composed of a base behavior plus a pipeline of modifiers.

### Solution

Complete replacement of the talent system with a modifier-based architecture:
- Single base `Talent` class - no type hierarchy
- Base talents define immutable core mechanics (Rank 1)
- Each rank/branch adds modifiers that adjust parameters in a TalentContext
- Active behavior is resolved by folding all modifiers along the unlocked path
- No behavior replacement, only augmentation through composition
- Tree structure defines valid paths, not inheritance

### Scope

**In Scope:**
- Replace entire talent system architecture
- Create new Talent base class with context-based execution
- Create TalentContext class to carry all state
- Create TalentModifier interface and implementations
- Create TalentNode tree structure for paths
- Implement TalentTree for path management
- Replace TalentRegistry with new registration system
- Refactor all existing elemental mage talents
- Update PlayerTalents capability for new system
- Simplify talent types to just behavior flags in context

**Out of Scope:**
- GUI changes (will adapt to new structure)
- Network packet structure changes (reuse existing)
- Save format changes (migrate on load)
- Other mod integrations
- Non-elemental talents (if any exist)

## Context for Development

### Codebase Patterns

**Current Talent Architecture (TO BE REPLACED):**
- Complex hierarchy: Talent → ActiveTalent/PassiveTalent/HybridTalent → Branching variants
- Branch selection stored in `TalentBranches` utility class with PathChoice enum
- 5-slot system: DEFINITION, HARMONY, CRESCENDO, RESONANCE, FINALE
- Player data in `PlayerTalents` capability
- Network sync via `SyncTalentsPacket`
- Static talent instances registered in `ElementalTalents`

**What We're Keeping:**
- 5-slot system (just metadata in context now)
- PlayerTalents capability interface (refactored implementation)
- Network packets (same structure, different data)
- Forge event system
- Resource management system

**What We're Replacing:**
- All talent base classes and hierarchy
- TalentBranches static utility → TalentTree instances
- Static talent registration → Tree-based registration
- Rank/branch checking → Context folding
- TalentType enum → Behavior flags in context

### Files to Reference

| File | Purpose | Key Info |
| ---- | ------- | -------- |
| [Talent.java](src/main/java/com/complextalents/talent/Talent.java) | Base talent class | Abstract methods: onUnlock, onRemove, onActivate, onTick |
| [BranchingActiveTalent.java](src/main/java/com/complextalents/talent/BranchingActiveTalent.java) | Current branching implementation | Shows existing branch structure |
| [ElementalUnleashTalent.java](src/main/java/com/complextalents/elemental/talents/mage/unleash/ElementalUnleashTalent.java) | Example complex branching talent | Lines 40-54: branch-specific arrays; Lines 155-290: branch logic |
| [TalentBranches.java](src/main/java/com/complextalents/talent/TalentBranches.java) | Branch selection storage | Static utility for path choices |
| [PlayerTalents.java](src/main/java/com/complextalents/capability/PlayerTalents.java) | Player capability interface | Resource and talent level tracking |
| [TalentRegistry.java](src/main/java/com/complextalents/talent/TalentRegistry.java) | Talent registration system | Registry pattern for all talents |

### Technical Decisions

1. **TalentContext as Universal State:**
   - Replaces all method parameters
   - Contains player, level, slot type, and all modifiable parameters
   - Tracks active/passive/hybrid behavior through flags
   - Includes lifecycle callbacks (onActivate, onTick, onDetonate)
   - Self-documenting with modifier list
   - **NEVER PERSISTED** - always rebuilt from tree + path

2. **Single Talent Class:**
   - No inheritance hierarchy for talent types (no ActiveTalent, PassiveTalent, HybridTalent)
   - Behavior classification through context flags:
     - `isPassive`: Has ongoing effects (tick behavior)
     - `isActive`: Can be activated by player
     - `hasToggle`: Active talent with persistent on/off state
     - `canDetonate`: Has special secondary action
   - All talents use same base class and interface
   - Type combinations are flexible (can be both passive AND active)
   - Modifiers can add new behavior types to base talent

3. **Tree-First Architecture:**
   - TalentTree defines structure and valid paths
   - Each tree has root node with base talent
   - Nodes contain modifiers for ranks 2-4
   - Path validation built into tree traversal

4. **Modifier Application:**
   - Base talent initializes context (Rank 1)
   - Modifiers apply in path order
   - Each modifier sees accumulated state
   - No modifier can "undo" previous modifications
   - Modifiers can add new behaviors through context callbacks

5. **Context Resolution:**
   - Build fresh context on each relevant event
   - Fold base + modifiers along player's unlocked path
   - Execute appropriate callback from final context
   - Context is ephemeral, not stored

6. **Persistence Strategy (Golden Rule):**
   - **Save only what player chose, not what talent does**
   - Persist: treeId → [nodeId list] mapping
   - Never persist: TalentContext, damage values, derived stats
   - Rebuild context deterministically from saved path on load
   - Network sync only sends paths, clients rebuild locally

## Implementation Plan

### Tasks

**TASK 1: Create TalentContext Class**
- **File**: New file `talent/TalentContext.java`
- **Action**:
  1. Create context class with player, level, slotType fields
  2. Add behavior classification flags:
     - `boolean isPassive` - Has ongoing tick effects
     - `boolean isActive` - Can be activated by player
     - `boolean hasToggle` - Active talent with toggle on/off mode
     - `boolean canDetonate` - Has special detonate action
     - `boolean toggleActive` - Current toggle state (runtime)
  3. Add lifecycle callbacks as Runnable/Consumer fields:
     - `Runnable onTick` - Called every tick for passive effects
     - `Runnable onActivate` - Called when talent is activated
     - `Runnable onDeactivate` - Called when toggled off
     - `Runnable onDetonate` - Special action (like ElementalUnleash detonate)
  4. Add all modifiable parameters (focusDrain, cooldown, damage multipliers, etc.)
  5. Add activeModifiers list for debugging/tooltips
  6. Add helper methods for common operations

**TASK 2: Create TalentModifier Interface**
- **File**: New file `talent/TalentModifier.java`
- **Action**:
  1. Create interface with single method: `void apply(TalentContext ctx)`
  2. Add getName() for debugging
  3. Document that modifiers should only augment, never replace

**TASK 3: Create New Base Talent Class**
- **File**: Replace `talent/Talent.java`
- **Action**:
  1. Single abstract class, no type hierarchy
  2. Methods: getId(), getName(), getDescription(), getMaxLevel()
  3. Core method: `void initializeContext(TalentContext ctx)`
  4. Remove onUnlock, onRemove, onActivate, onTick (all in context now)
  5. Keep resource helper methods

**TASK 4: Create TalentNode and TalentTree**
- **Files**: New files `talent/TalentNode.java`, `talent/TalentTree.java`
- **Action**:
  1. TalentNode: id, rank, modifier, children map (PathChoice → Node)
  2. TalentTree: root node, talent reference, tree traversal methods
  3. Method to get modifier path for a player's unlocked ranks
  4. Validation that paths are legal

**TASK 5: Replace TalentRegistry**
- **File**: Replace `talent/TalentRegistry.java`
- **Action**:
  1. Register TalentTree instances instead of Talent instances
  2. Trees contain the base talent + all modifiers
  3. Lookup returns tree, not talent
  4. Migration helper to load old save data

**TASK 6: Create All ElementalUnleash Modifiers**
- **Files**: New package `elemental/talents/mage/unleash/`
- **Action**:
  1. Create ElementalUnleashBase (Rank 1 talent)
  2. Create ChainDetonationModifier (Rank 2A)
  3. Create LingeringStacksModifier (Rank 2B)
  4. Create OverloadModifier (Rank 3A from 2A)
  5. Create AmplificationModifier (Rank 3B from 2B)
  6. Create ChainMasteryModifier (Rank 4A from 3A)
  7. Create ResonantCascadeModifier (Rank 4B from 3B)

**TASK 7: Build ElementalUnleash Tree**
- **File**: New file `elemental/talents/mage/unleash/ElementalUnleashTree.java`
- **Action**:
  1. Create tree with ElementalUnleashBase as root
  2. Add Rank 2 branches (A: Chain, B: Lingering)
  3. Add Rank 3 branches (A→A: Overload, B→B: Amplification)
  4. Add Rank 4 branches (A→A→A: ChainMastery, B→B→B: ResonantCascade)
  5. Register tree in new TalentRegistry

**TASK 8: Create TalentExecutor Service**
- **File**: New file `talent/TalentExecutor.java`
- **Action**:
  1. Service that builds and executes talent contexts
  2. Method: `executeActivation(player, talentId, level)`
     - Check `ctx.isActive` flag
     - If `ctx.hasToggle`, toggle state
     - Otherwise apply cooldown after activation
     - Execute `ctx.onActivate` callback
  3. Method: `executeTick(player, talentId, level)`
     - Check `ctx.isPassive` flag
     - Execute `ctx.onTick` callback if present
  4. Method: `executeDetonate(player, talentId, level)`
     - Check `ctx.canDetonate` flag
     - Execute `ctx.onDetonate` callback if present
  5. Builds context by folding base + modifiers from saved path
  6. Handles cooldowns and resource consumption based on context flags

**TASK 9: Create TalentData Capability**
- **Files**: New files `capability/ITalentData.java`, `capability/TalentData.java`
- **Action**:
  1. Create ITalentData interface with `Map<String, List<String>> getUnlockedTalentPaths()`
  2. Create TalentData implementation storing treeId → nodeId list
  3. Add NBT serialization (save only node paths, not context)
  4. Add capability provider and attach to players
  5. Migration code from old format: extract rank → build equivalent path

**TASK 10: Update PlayerTalents Implementation**
- **File**: `capability/PlayerTalentsImpl.java`
- **Action**:
  1. Use TalentData capability for persistence
  2. Use TalentExecutor for activation/tick
  3. Rebuild context on-demand from saved paths
  4. Never store TalentContext - always rebuild from tree + path

**TASK 11: Refactor ElementalMageDefinition**
- **Files**: `elemental/talents/mage/definition/`
- **Action**:
  1. Create ElementalMageDefinitionBase (passive, Focus resource management)
  2. No modifiers needed (Definition talents are rank 1 only)
  3. Context flags: `isPassive = true`
  4. Implement Focus decay in `onTick` callback
  5. Register as single-node tree (no branches)

**TASK 12: Refactor ElementalAttunement (Harmony Slot)**
- **Files**: New package `elemental/talents/mage/attunement/`
- **Action**:
  1. Create ElementalAttunementBase (passive Focus management)
  2. Create EnergyFeedbackModifier (Rank 2A - Focus on reaction)
  3. Create ManaSpringModifier (Rank 2B - passive regen)
  4. Create OverflowModifier (Rank 3A from 2A - overflow damage)
  5. Create DeepWellModifier (Rank 3B from 2B - max Focus increase)
  6. Create LoopCastingModifier (Rank 4A from 3A - spell acceleration)
  7. Create ResonantCascadeModifier (Rank 4B from 3B - cascade burst)

**TASK 13: Refactor ElementalWard (Resonance Slot)**
- **Files**: New package `elemental/talents/mage/ward/`
- **Action**:
  1. Create ElementalWardBase (active blocking ability)
  2. Create PerfectBlockModifier (Rank 2A - damage negation)
  3. Create ReflectiveModifier (Rank 2B - damage reflection)
  4. Create FortifyModifier (Rank 3A from 2A - shield generation)
  5. Create ReprisalModifier (Rank 3B from 2B - orb spawning)
  6. Create SanctuaryModifier (Rank 4A from 3A - zone creation)
  7. Create MirrorShieldModifier (Rank 4B from 3B - full reflection)

**TASK 14: Refactor ElementalConflux (Finale Slot)**
- **Files**: New package `elemental/talents/mage/conflux/`
- **Action**:
  1. Create ElementalConfluxBase (active ultimate ability)
  2. Create ConvergenceModifier (Rank 2A - pull effect)
  3. Create DivergenceModifier (Rank 2B - explosion)
  4. Create SingularityModifier (Rank 3A from 2A - enhanced pull)
  5. Create ChaosBloomModifier (Rank 3B from 2B - random reactions)
  6. Create BlackholeModifier (Rank 4A from 3A - execute threshold)
  7. Create PrimordialRiftModifier (Rank 4B from 3B - reality tear)

**TASK 15: Build All Talent Trees**
- **Files**: New files for each talent tree
- **Action**:
  1. ElementalMageDefinitionTree (single node, no branches)
  2. ElementalAttunementTree (full branching structure)
  3. ElementalUnleashTree (already defined in TASK 7)
  4. ElementalWardTree (full branching structure)
  5. ElementalConfluxTree (full branching structure)
  6. Register all trees in new TalentRegistry

**TASK 16: Consolidate and Update Command System**
- **File**: Replace `command/TalentCommand.java`
- **Action**:
  1. Consolidate all commands under single `/talent` command
  2. Subcommands:
     - `/talent list` - Show player's unlocked talent paths
     - `/talent info <tree>` - Show tree structure and player's path
     - `/talent grant <player> <tree> <path>` - Grant specific path (admin)
     - `/talent revoke <player> <tree>` - Remove talent tree (admin)
     - `/talent unlock <tree> <node>` - Unlock next node in path
     - `/talent respec [tree]` - Reset talent tree(s)
  3. Update command suggestions to use tree IDs instead of talent IDs
  4. Show full paths in list command: "elemental_unleash: Root → Chain Detonation → Overload"
  5. Remove old commands: GrantTalentCommand, RevokeTalentCommand, SelectBranchCommand, ListTalentsCommand
  6. Add tab completion for:
     - Tree IDs from registry
     - Valid next nodes based on player's current path
     - Player names for admin commands

**TASK 17: Remove Old Classes**
- **Files**: Delete old hierarchy
- **Action**:
  1. Delete ActiveTalent, PassiveTalent, HybridTalent
  2. Delete all Branching* variants
  3. Delete TalentType enum
  4. Delete BranchingTalentBase interface
  5. Delete TalentBranches utility (replaced by tree structure)
  6. Delete old talent implementations after migration
  7. Delete old command classes after consolidation

### Acceptance Criteria

**AC1: Base Talent Defines Core Behavior**
- GIVEN ElementalUnleash at Rank 1
- WHEN activated without any modifiers
- THEN only base behavior executes (drain focus, apply stacks)
- AND no branching logic is checked

**AC2: Modifiers Layer Without Replacement**
- GIVEN ElementalUnleash with Chain Detonation modifier (Rank 2A)
- WHEN activated
- THEN base behavior still executes
- AND chain parameters are added to context
- AND original behavior is augmented, not replaced

**AC3: Path Integrity Maintained**
- GIVEN a player selects Lingering Stacks at Rank 2
- WHEN they reach Rank 3
- THEN only Amplification is available (not Overload)
- AND the tree structure enforces this constraint

**AC4: Context Accumulates Changes**
- GIVEN multiple modifiers in a path
- WHEN context is built
- THEN each modifier sees previous modifications
- AND final context contains all adjustments

**AC5: No Type Hierarchy - Behavior Through Flags**
- GIVEN the new talent system
- WHEN reviewing the code
- THEN only one Talent base class exists
- AND no ActiveTalent, PassiveTalent, HybridTalent classes
- AND behavior determined by context flags (isPassive, isActive, hasToggle, canDetonate)
- AND a talent can have multiple behaviors (e.g., both passive AND active)
- AND modifiers can add new behavior types (e.g., make passive talent also active)

**AC6: No Rank Checking**
- GIVEN any talent implementation
- WHEN reviewing the code
- THEN no `if (rank >= X)` statements exist
- AND no `if (hasBranch(PathChoice.A))` checks exist
- AND all behavior comes from context fields

**AC7: Tree-Based Registration**
- GIVEN the new TalentRegistry
- WHEN registering a talent
- THEN a TalentTree is registered (not a Talent instance)
- AND the tree contains all modifiers for all ranks

**AC8: Old Save Migration**
- GIVEN a player with old save format
- WHEN loading their talent data
- THEN it migrates to new path-based format
- AND their unlocked talents still work

**AC9: Persistence Only Saves Paths**
- GIVEN a player with Elemental Unleash rank 3 path A
- WHEN their data is saved
- THEN only ["unleash_root", "chain_detonation", "overload"] is persisted
- AND no context values are saved
- AND on load, context is rebuilt from this path

**AC10: Unified Command System**
- GIVEN the new talent system
- WHEN using commands
- THEN all functionality is under `/talent` command
- AND commands work with tree IDs and node paths
- AND tab completion suggests valid trees and nodes
- AND list command shows human-readable paths

## Command System Refactor

### Consolidated `/talent` Command Structure

**Player Commands:**
```
/talent list
  Shows: "elemental_unleash: Root → Chain Detonation → Overload (Rank 3A)"

/talent info <tree_id>
  Shows tree structure with player's current path highlighted

/talent unlock <tree_id> <node_id>
  Unlocks next node if valid path and requirements met

/talent respec [tree_id]
  Resets specific tree or all trees
```

**Admin Commands:**
```
/talent grant <player> <tree_id> <path...>
  Example: /talent grant Steve elemental_unleash unleash_root chain_detonation overload

/talent revoke <player> <tree_id>
  Removes entire tree from player

/talent reset <player>
  Clears all talents for player
```

### Tab Completion Logic

1. **Tree IDs**: Pull from new TalentRegistry
2. **Node IDs**: Based on current path, suggest only valid next nodes
3. **Path Building**: For grant command, validate each node follows valid tree path
4. **Player Names**: Standard player selector for admin commands

### Command Migration

| Old Command | New Command | Changes |
| ----------- | ----------- | ------- |
| `/grant-talent <talent> <level>` | `/talent grant <player> <tree> <path...>` | Path-based instead of level |
| `/revoke-talent <talent>` | `/talent revoke <player> <tree>` | Tree-based removal |
| `/list-talents` | `/talent list` | Shows paths not levels |
| `/select-branch <talent> <rank> <choice>` | Removed - paths chosen on unlock | No separate branch selection |

## Full Talent Refactoring Map

### All Existing Talents to Migrate

| Current Talent | Type | Slot | Refactoring Strategy |
| -------------- | ---- | ---- | -------------------- |
| ElementalMageDefinition | PassiveTalent | DEFINITION | Single node tree, passive Focus management |
| ElementalAttunementTalent | BranchingPassiveTalent | HARMONY | Full tree with 6 modifiers (2A/2B → 3A/3B → 4A/4B) |
| ElementalUnleashTalent | BranchingHybridTalent | CRESCENDO | Full tree, hybrid with toggle + detonate |
| ElementalWardTalent | BranchingActiveTalent | RESONANCE | Full tree, active blocking with passive shield |
| ElementalConfluxTalent | BranchingActiveTalent | FINALE | Full tree, ultimate ability with zones |

### Behavior Flag Mapping

**ElementalMageDefinition:**
```java
ctx.isPassive = true;   // Focus decay management
ctx.isActive = false;   // No activation
```

**ElementalAttunementTalent:**
```java
ctx.isPassive = true;   // Focus generation
ctx.isActive = false;   // Changes to true with Loop Casting (Rank 4A)
```

**ElementalUnleashTalent:**
```java
ctx.isPassive = true;   // Focus drain while active
ctx.isActive = true;    // Toggle activation
ctx.hasToggle = true;   // Persistent state
ctx.canDetonate = true; // Super-reaction trigger
```

**ElementalWardTalent:**
```java
ctx.isPassive = true;   // Shield generation
ctx.isActive = true;    // Active blocking
ctx.hasToggle = false;  // One-shot block
```

**ElementalConfluxTalent:**
```java
ctx.isPassive = false;  // No ongoing effects
ctx.isActive = true;    // Ultimate activation
ctx.hasToggle = false;  // One-shot ability
```

## Additional Context

### Dependencies

**Minecraft/Forge:**
- Forge event system (unchanged)
- Capability system (unchanged)
- Network packets (structure unchanged)
- NBT serialization

**To Be Replaced:**
- Entire talent class hierarchy
- TalentBranches utility class
- Static talent registration
- TalentType enum

**To Be Created:**
- TalentContext system
- TalentModifier framework
- TalentTree structure
- TalentExecutor service

### Testing Strategy

**Phase 1: Core Infrastructure**
1. Create simple test talent with 2 modifiers
2. Verify context building and folding
3. Test modifier application order
4. Validate tree structure enforcement

**Phase 2: ElementalUnleash Migration**
1. Test base ElementalUnleash (Rank 1 only)
2. Test each branch path separately
3. Test full paths (1→2A→3A→4A and 1→2B→3B→4B)
4. Compare behavior with old system
5. Verify no regressions

**Phase 3: Full System Migration**
1. Migrate ElementalMageDefinition (simplest - no branches)
2. Migrate ElementalAttunement (passive branching)
3. Migrate ElementalWard (active branching)
4. Migrate ElementalConflux (ultimate branching)
5. Test all talent interactions together

**Phase 4: Integration Testing**
1. Test complete mage build (all 5 slots)
2. Test talent synergies
3. Test save/load with migration
4. Test network sync
5. Performance testing

**Key Validation:**
- Log context contents after each modifier
- Verify path choices are enforced
- Ensure resource consumption works
- Test cooldown management

### Notes

**Breaking Changes:**
- Complete replacement of talent system - no backward compatibility
- All existing talent implementations must be rewritten
- Save format will migrate automatically on first load
- Network packets keep same structure but different internal data

**Implementation Order:**
1. Build core infrastructure (Context, Modifier, Tree) - TASKS 1-5
2. Create ElementalUnleash as proof of concept - TASKS 6-7
3. Validate the pattern works with testing
4. Mass migrate remaining talents:
   - ElementalMageDefinition - TASK 11 (simplest, no branches)
   - ElementalAttunement - TASK 12 (passive with branches)
   - ElementalWard - TASK 13 (active with branches)
   - ElementalConflux - TASK 14 (ultimate ability)
5. Build all talent trees - TASK 15
6. Update command system - TASK 16
7. Remove all old code - TASK 17

**Benefits of Complete Refactor:**
- Cleaner codebase without legacy cruft
- Single consistent pattern throughout
- Easier to understand and maintain
- No confusion between old and new styles
- Better performance (no compatibility layers)

**Example Context Fields for ElementalUnleash:**
```java
// Base fields (Rank 1)
// Behavior classification
ctx.isActive = true;        // Can be activated
ctx.isPassive = true;       // Has tick effects while active
ctx.hasToggle = true;       // Toggle on/off mode
ctx.canDetonate = true;     // Has special detonate action

// Callbacks
ctx.onTick = () -> {
    if (ctx.toggleActive) {
        consumeFocus(ctx.player, ctx.focusDrainPerSecond);
    }
};
ctx.onActivate = () -> {
    ctx.toggleActive = !ctx.toggleActive;
};
ctx.onDetonate = () -> {
    triggerSuperReaction(ctx);
    ctx.toggleActive = false;
};

// Parameters
ctx.focusDrainPerSecond = 10;
ctx.maxElementalStacks = 6;

// After Chain Detonation (Rank 2A)
ctx.chainCount = 1;
ctx.chainDamageMultiplier = 1.0f;

// After Overload (Rank 3A)
ctx.chainCount = 4;  // Overrides previous
ctx.chainDamageMultiplier = 1.25f;
```

**Classification Examples:**
```java
// Pure Passive Talent
ctx.isPassive = true;
ctx.isActive = false;
// Only has onTick callback

// Pure Active Talent
ctx.isPassive = false;
ctx.isActive = true;
ctx.hasToggle = false;
// Only has onActivate callback, uses cooldown

// Hybrid Toggle Talent (like ElementalUnleash)
ctx.isPassive = true;
ctx.isActive = true;
ctx.hasToggle = true;
// Has both onTick and onActivate callbacks
```

**Persistence Example:**
```java
// What we SAVE (in NBT)
{
  "elemental_unleash": ["unleash_root", "chain_detonation", "overload"]
}

// What we REBUILD on load
TalentContext ctx = new TalentContext();
tree.getBaseTalent().initializeContext(ctx);  // Rank 1
chainDetonationMod.apply(ctx);                 // Rank 2A
overloadMod.apply(ctx);                        // Rank 3A
// ctx now has all the derived values

// What we NEVER save
// ❌ ctx.focusDrainPerSecond
// ❌ ctx.chainCount
// ❌ ctx.chainDamageMultiplier
// These are always recomputed from the path
```