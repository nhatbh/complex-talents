---
title: 'Code-Driven Modular Talent System'
slug: 'code-driven-modular-talent-system'
created: '2026-01-24'
status: 'phase-4-complete'
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19]
tech_stack:
  - 'Java 17'
  - 'Minecraft Forge 47.4.10'
  - 'Minecraft 1.20.1'
  - 'NBT for data serialization'
  - 'Forge Capabilities API'
files_to_modify:
  - 'src/main/java/com/complextalents/capability/TalentsCapabilities.java'
  - 'src/main/java/com/complextalents/capability/PlayerTalentsProvider.java'
  - 'src/main/java/com/complextalents/network/SyncTalentsPacket.java'
  - 'src/main/java/com/complextalents/TalentsMod.java'
files_to_create:
  - 'src/main/java/com/complextalents/talent/data/ITalentData.java'
  - 'src/main/java/com/complextalents/talent/data/TalentDataImpl.java'
  - 'src/main/java/com/complextalents/talent/core/TalentNode.java'
  - 'src/main/java/com/complextalents/talent/core/TalentTree.java'
  - 'src/main/java/com/complextalents/talent/core/TalentContext.java'
  - 'src/main/java/com/complextalents/talent/core/TalentEventType.java'
  - 'src/main/java/com/complextalents/talent/core/TalentRegistry.java'
  - 'src/main/java/com/complextalents/talent/core/TalentSlot.java'
  - 'src/main/java/com/complextalents/talent/trees/ElementalUnleashTree.java'
code_patterns:
  - 'Capability pattern for player data persistence'
  - 'Network packet system for client-server sync'
  - 'Event-driven architecture with Forge EventBus'
  - 'NBT serialization for data storage'
  - 'LazyOptional for capability access'
  - 'Hierarchical package structure (talent.core, talent.data, talent.trees)'
test_patterns:
  - 'Manual in-game testing'
  - 'No existing unit tests in codebase'
---

# Tech-Spec: Code-Driven Modular Talent System

**Created:** 2026-01-24

## Overview

### Problem Statement

The current talent system needs a complete rewrite to support true branching paths, dynamic stat modification without hardcoding, and a clean Add/Remove/Replace effect system. The existing system mixes concerns and doesn't properly support the skill tree progression model with ranks and mutually exclusive branches.

### Solution

Implement a new modular talent system built on three pillars:
1. **ITalentData Capability**: Persistent data attached to players storing unlocked nodes, cooldowns, toggle states, and dynamic stats
2. **TalentTree & Nodes**: Java classes defining the logic with modular "hooks" that modify behavior or stats
3. **TalentContext**: A mutable object passed during events allowing base logic and upgrades to communicate

The system uses hierarchical node IDs, branch-exclusive progression, and event-driven architecture for maximum flexibility.

### Scope

**In Scope:**
- New ITalentData capability replacing PlayerTalents
- TalentTree and TalentNode architecture with hierarchical IDs
- TalentContext for mutable event data and cancellation
- Event-driven talent system (separate from Forge for now)
- Elemental Unleash implementation as proof of concept
- Branch-exclusive progression (choosing rank 2A locks out entire B path)
- Persistent toggle states in capability
- Dynamic stat calculation system with per-tick caching
- Disconnect handling for active toggles
- Registration safety to prevent duplicate nodes
- Talent respec system for changing branch choices

**Out of Scope:**
- Resource bar UI (TODO for later implementation)
- Full resource management system (basic Focus tracking only)
- Migration from old talent system
- Integration with Forge events (TODO for later)
- GUI for talent selection/progression
- Other talents beyond Elemental Unleash
- Network syncing (use existing infrastructure)

## Context for Development

### Codebase Patterns

**Current Architecture to Replace:**
- `PlayerTalents` interface and `PlayerTalentsImpl` capability
- `Talent` abstract class hierarchy (ActiveTalent, PassiveTalent, HybridTalent)
- `BranchingTalentBase` and its subclasses
- Static state management in talent classes (e.g., ElementalUnleashTalent uses static Map for states)

**Observed Patterns to Follow:**
- Capability registration via `RegisterCapabilitiesEvent`
- Capability attachment via `AttachCapabilitiesEvent<Entity>`
- NBT serialization with CompoundTag for persistence
- Network sync on login/respawn/dimension change events
- Tick handlers using `TickEvent.PlayerTickEvent`
- LazyOptional pattern for capability access
- Batch network updates with dirty flag pattern
- Cooldown ticking every 10 game ticks for performance

**New Design Principles:**
- **Stateless Logic**: All talent logic is stateless, state lives in capability
- **Dynamic Stats**: No hardcoded values, everything computed from active nodes
- **Event-Driven**: Nodes register handlers for specific event types
- **Hierarchical IDs**: Use dot notation for node relationships
- **Branch Exclusivity**: Choosing a branch at any rank locks out the alternate path entirely
- **Performance First**: Stats cached per tick to avoid recalculation
- **Simplified Nodes**: Single TalentNode class with functional configuration
- **Phased Calculation**: Stats computed in deterministic phases to avoid circular dependencies

### Files to Reference

| File | Purpose |
| ---- | ------- |
| src/main/java/com/complextalents/capability/PlayerTalents.java | Current capability interface to replace |
| src/main/java/com/complextalents/capability/PlayerTalentsImpl.java | Current implementation with NBT serialization patterns |
| src/main/java/com/complextalents/capability/PlayerTalentsProvider.java | Shows capability attachment, sync on login/respawn/dimension change |
| src/main/java/com/complextalents/elemental/talents/mage/unleash/ElementalUnleashTalent.java | Current Elemental Unleash implementation to rewrite |
| src/main/java/com/complextalents/network/SyncTalentsPacket.java | Network sync pattern to follow |
| src/main/java/com/complextalents/api/events/TalentEvent.java | Existing event structure (minimal, needs expansion) |
| src/main/java/com/complextalents/elemental/ElementalStackManager.java | Elemental system integration point |
| src/main/java/com/complextalents/elemental/ElementalReactionHandler.java | Reaction handling for Elemental Unleash |

### Technical Decisions

1. **Capability Architecture**: Complete replacement of PlayerTalents with ITalentData
2. **Node Identification**: Hierarchical IDs like "elemental_unleash.rank2.a.chain_detonation"
3. **Event System**: Custom TalentEventType enum, separate from Forge events initially
4. **State Persistence**: Toggle states and cooldowns persist in capability
5. **Branch Progression**: Selecting rank 2A means ranks 3A and 4A, never accessing B branch
6. **Resource System**: Stub implementation with TODOs for future expansion
7. **Context Pattern**: Mutable context object for event communication and cancellation
8. **Performance Optimization**: Per-tick stat caching to avoid recalculation overhead
9. **Node Simplification**: Single TalentNode class using functional interfaces instead of inheritance
10. **Disconnect Safety**: Automatic toggle cleanup when players disconnect
11. **Respec Support**: Allow branch changes with proper validation and cleanup

## Implementation Plan

### Tasks

#### Phase 1: Core Infrastructure

- [x] **Task 1**: Create ITalentData capability interface
  - File: `src/main/java/com/complextalents/talent/data/ITalentData.java`
  - Action: Define interface with node tracking, toggle states, cooldowns, resources, and respec methods
  - Notes: Include branch validation logic in interface contract

- [x] **Task 2**: Implement TalentDataImpl capability
  - File: `src/main/java/com/complextalents/talent/data/TalentDataImpl.java`
  - Action: Implement ITalentData with NBT serialization, branch exclusivity validation, and dirty flag pattern
  - Notes: Follow existing PlayerTalentsImpl pattern for NBT handling

- [x] **Task 3**: Create TalentSlot enum
  - File: `src/main/java/com/complextalents/talent/core/TalentSlot.java`
  - Action: Define SKILL_1 through SKILL_4 slots matching the four skill trees
  - Notes: Replace TalentSlotType references throughout codebase

- [x] **Task 4**: Define TalentEventType enum
  - File: `src/main/java/com/complextalents/talent/core/TalentEventType.java`
  - Action: Create event types: ACTIVATE, TICK, PASSIVE_TICK, TOGGLE_ON, TOGGLE_OFF, DETONATE, PRE_TOGGLE, POST_TOGGLE
  - Notes: Extensible for future event types

#### Phase 2: Talent System Core

- [x] **Task 5**: Implement TalentContext
  - File: `src/main/java/com/complextalents/talent/core/TalentContext.java`
  - Action: Create mutable context with player, data, cancellation flag, and storage map
  - Notes: Include helper methods for type-safe data access

- [x] **Task 6**: Create simplified TalentNode
  - File: `src/main/java/com/complextalents/talent/core/TalentNode.java`
  - Action: Implement single node class with functional handlers and stat modifiers
  - Notes: Use builder pattern for configuration, include priority field

- [x] **Task 7**: Implement TalentTree base class
  - File: `src/main/java/com/complextalents/talent/core/TalentTree.java`
  - Action: Create abstract tree with node management, stat calculation with caching, and event dispatch
  - Notes: Include ToggleTalent marker interface, per-tick stat cache

- [x] **Task 8**: Create TalentRegistry
  - File: `src/main/java/com/complextalents/talent/core/TalentRegistry.java`
  - Action: Singleton registry for talent trees with initialization safety flag
  - Notes: Prevent duplicate registration, map TalentSlot to TalentTree

#### Phase 3: Capability Integration

- [x] **Task 9**: Update TalentsCapabilities
  - File: `src/main/java/com/complextalents/capability/TalentsCapabilities.java`
  - Action: Replace PLAYER_TALENTS with TALENT_DATA capability reference
  - Notes: Update all capability access points

- [x] **Task 10**: Modify PlayerTalentsProvider
  - File: `src/main/java/com/complextalents/capability/PlayerTalentsProvider.java`
  - Action: Attach ITalentData instead of PlayerTalents, add disconnect handling, update tick handler
  - Notes: Add PlayerLoggedOutEvent handler for toggle cleanup

- [x] **Task 11**: Update SyncTalentsPacket
  - File: `src/main/java/com/complextalents/network/SyncTalentsPacket.java`
  - Action: Modify to sync ITalentData NBT instead of PlayerTalents
  - Notes: Maintain backward compatibility structure where possible

#### Phase 4: Elemental Unleash Implementation

- [x] **Task 12**: Create ElementalUnleashTree
  - File: `src/main/java/com/complextalents/talent/trees/ElementalUnleashTree.java`
  - Action: Implement full skill tree with all ranks and branches using new node system
  - Notes: Reference skill descriptions from requirements document

- [x] **Task 13**: Implement Rank 1 base nodes
  - File: `src/main/java/com/complextalents/talent/trees/ElementalUnleashTree.java`
  - Action: Create base unleash node with toggle, focus cost, drain, and detonation logic
  - Notes: Focus values: cost [40, 35, 30, 25], drain [12, 10, 8, 5], cooldown [30, 20, 15, 10]

- [x] **Task 14**: Implement Rank 2 branch nodes
  - File: `src/main/java/com/complextalents/talent/trees/ElementalUnleashTree.java`
  - Action: Create Chain Detonation (2A) and Lingering Stacks (2B) nodes with modifiers
  - Notes: Chain damage [60%, 75%, 90%, 110%], Lingering drain [8, 6, 4, 2]

- [x] **Task 15**: Implement Rank 3 branch nodes
  - File: `src/main/java/com/complextalents/talent/trees/ElementalUnleashTree.java`
  - Action: Create Overload (3A) and Amplification (3B) nodes
  - Notes: Overload jumps [2, 3, 4, 5], Amplification bonus [80%, 120%, 160%, 250%]

- [x] **Task 16**: Implement Rank 4 capstone nodes
  - File: `src/main/java/com/complextalents/talent/trees/ElementalUnleashTree.java`
  - Action: Create World Rune (4A) and Singularity (4B) with Replace logic using context.cancel()
  - Notes: World Rune chains on 5 jumps, Singularity creates black hole on full amplification

#### Phase 5: Integration and Cleanup

- [x] **Task 17**: Register ElementalUnleash in TalentRegistry
  - File: `src/main/java/com/complextalents/TalentsMod.java`
  - Action: Initialize TalentRegistry on mod load, register ElementalUnleashTree for SKILL_2 slot
  - Notes: Ensure registration happens once on startup

- [x] **Task 18**: Add tick event handler
  - File: `src/main/java/com/complextalents/capability/PlayerTalentsProvider.java`
  - Action: Update onPlayerTick to call TalentTree.tick() for each equipped talent
  - Notes: Maintain 10-tick cooldown processing optimization

- [x] **Task 19**: Implement respec system
  - File: `src/main/java/com/complextalents/talent/data/TalentDataImpl.java`
  - Action: Add respecBranch method with cascading node removal and refund calculation
  - Notes: TODO: Define refund cost/resource system

- [ ] **Task 20**: Remove old talent system files
  - Files: All old Talent classes, BranchingTalentBase hierarchy, old ElementalUnleashTalent
  - Action: Delete deprecated classes after verifying new system works
  - Notes: Keep for reference during development, delete in final cleanup

### Acceptance Criteria

#### Core System Functionality

- [ ] **AC1**: Given a player with no talents, when they unlock "elemental_unleash.rank1", then the node is stored in ITalentData and persists across logout/login

- [ ] **AC2**: Given a player with rank1 unlocked, when they unlock "elemental_unleash.rank2.a", then attempting to unlock "elemental_unleash.rank2.b" returns false

- [ ] **AC3**: Given a player with branch A selected at rank 2, when they unlock rank 3 nodes, then only rank3.a nodes are available (rank3.b locked)

- [ ] **AC4**: Given multiple nodes modifying "focus_cost" stat, when getStat() is called, then all modifiers are applied in priority order with result cached for the tick

- [ ] **AC5**: Given a toggle talent is active, when the player disconnects, then the toggle is automatically deactivated and resources stop draining

#### Elemental Unleash Behavior

- [ ] **AC6**: Given Elemental Unleash rank 1, when activated with 40 focus, then toggle turns on and focus drains at 12/second

- [ ] **AC7**: Given Unleash is active and focus reaches 0, when next tick occurs, then automatic detonation triggers and cooldown starts

- [ ] **AC8**: Given Chain Detonation (2A) is unlocked, when detonation kills a target, then chain reaction spawns dealing 75% damage

- [ ] **AC9**: Given Lingering Stacks (2B) is unlocked, when Unleash is active, then focus drains at 6/second instead of 12/second

- [ ] **AC10**: Given Singularity (4B) is unlocked with full amplification, when detonation occurs, then default explosion is cancelled and black hole spawns instead

#### Event System

- [ ] **AC11**: Given a node with DETONATE handler, when detonate event fires, then the handler executes with access to TalentContext data

- [ ] **AC12**: Given multiple nodes handling the same event, when the event fires, then nodes execute in priority order (lower number first)

- [ ] **AC13**: Given a node calls context.cancel(), when subsequent nodes check, then isCanceled() returns true and they can skip execution

#### Persistence and Sync

- [ ] **AC14**: Given a player with complex talent setup, when server saves and loads, then all node selections, toggles, cooldowns persist correctly

- [ ] **AC15**: Given a server-side talent change, when next sync occurs, then client receives update via SyncTalentsPacket

- [ ] **AC16**: Given toggle state changes, when dirty flag is set, then network sync happens within next tick

#### Respec System

- [ ] **AC17**: Given a player with full rank 2A-3A-4A progression, when respecBranch("elemental_unleash", 2) is called, then all A-branch nodes ranks 2-4 are removed

- [ ] **AC18**: Given nodes are removed via respec, when checking unlocked nodes, then removed nodes return false for isNodeUnlocked()

#### Performance

- [ ] **AC19**: Given 20 players with 10 nodes each, when stat calculation occurs, then each player's stats are calculated once per tick maximum

- [ ] **AC20**: Given stat cache exists, when same stat is requested multiple times in one tick, then cached value is returned without recalculation

## Additional Context

### Dependencies

**Minecraft/Forge Systems:**
- Forge Capability system for data persistence
- ServerPlayer for player interaction
- TickEvent.PlayerTickEvent for talent ticking
- PlayerEvent.PlayerLoggedOutEvent for disconnect handling
- NBT CompoundTag for data serialization
- FriendlyByteBuf for network packets
- LazyOptional for capability access

**Project Systems:**
- ElementalStackManager for elemental stack management
- ElementalReactionHandler for reaction triggering
- SuperReactionHandler for super-reaction logic
- Network PacketHandler for client-server communication
- TalentsMod.LOGGER for debug logging

**External Requirements:**
- None (pure Minecraft Forge mod)

### Testing Strategy

**Manual Testing Steps:**

**Phase 1: Core System**
1. `/talent grant @p elemental_unleash.rank1` - Verify node unlocks
2. Logout/login - Verify persistence
3. Attempt to unlock both rank2.a and rank2.b - Verify exclusivity
4. Use debug commands to verify stat calculations
5. Monitor server logs for performance metrics

**Phase 2: Elemental Unleash**
1. Activate Unleash with keybind - Verify toggle and focus drain
2. Let focus reach 0 - Verify auto-detonation
3. Test each branch path separately:
   - 2A path: Verify chain reactions on kill
   - 2B path: Verify reduced drain rate
4. Test rank 4 capstones:
   - 4A: Count chain jumps for World Rune trigger
   - 4B: Verify black hole spawns with full amplification

**Phase 3: Integration**
1. Multiple players testing simultaneously - Verify no state bleeding
2. Rapid login/logout - Verify cleanup and sync
3. Dimension changes - Verify capability persistence
4. Server restart - Verify full persistence
5. Performance profiling with 20+ players

**Debug Commands Needed:**
- `/talent grant <player> <nodeId>` - Grant specific node
- `/talent revoke <player> <nodeId>` - Remove node
- `/talent respec <player> <talentId> <rank>` - Test respec
- `/talent stats <player>` - Display calculated stats
- `/talent debug <player>` - Dump full talent data

**Known Testing Limitations:**
- No automated unit tests (manual only)
- No CI/CD pipeline
- Testing requires full Minecraft client/server

### Notes

**Implementation Risks:**
- **Performance Impact**: Dynamic stat calculation could cause lag with many players
  - Mitigation: Per-tick caching, lazy evaluation
- **State Synchronization**: Client-server desync possible with complex state
  - Mitigation: Dirty flag pattern, batch updates
- **Branch Lock Confusion**: Players might not understand permanent branch lock
  - Mitigation: Clear messaging, respec system
- **Memory Leaks**: Static caches could retain player data
  - Mitigation: Proper cleanup on player logout

**Known Limitations:**
- No GUI - commands only for now
- Single resource type (Focus) - others are TODO
- No visual feedback for talent effects
- No integration with other mod systems yet

**Future Enhancements:**
- Resource bar UI with custom renderers
- Full Forge event integration for mod compatibility
- Visual effects for talent activations
- Skill tree GUI with branch visualization
- Achievement/advancement integration
- Config file for balance tweaking
- API for other mods to add talents