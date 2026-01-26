---
title: 'Branching Talent Tree with Modifier System Implementation'
slug: 'branching-talent-modifier-system'
created: '2026-01-24'
status: 'in-progress'
stepsCompleted: [1]
tech_stack:
  - 'Java 17'
  - 'Minecraft Forge 47.4.10'
  - 'Minecraft 1.20.1'
files_to_modify:
  - 'src/main/java/com/complextalents/talent/Talent.java'
  - 'src/main/java/com/complextalents/talent/TalentContext.java'
  - 'src/main/java/com/complextalents/capability/PlayerTalentsImpl.java'
files_to_create:
  - 'src/main/java/com/complextalents/talent/ITalent.java'
  - 'src/main/java/com/complextalents/talent/TalentPassive.java'
  - 'src/main/java/com/complextalents/talent/ITalentModifier.java'
  - 'src/main/java/com/complextalents/talent/PassiveTickModifier.java'
  - 'src/main/java/com/complextalents/talent/PassiveOverrideModifier.java'
  - 'src/main/java/com/complextalents/talent/TalentEventListener.java'
  - 'src/main/java/com/complextalents/talent/TalentContextBuilder.java'
  - 'src/main/java/com/complextalents/talent/ModifierState.java'
  - 'src/main/java/com/complextalents/talent/ModifierStateStore.java'
  - 'src/main/java/com/complextalents/talent/TalentTreeResolver.java'
  - 'src/main/java/com/complextalents/talent/TalentExecutor.java'
  - 'src/main/java/com/complextalents/api/events/TalentEvent.java'
code_patterns:
  - 'Interface-based design with stateless implementations'
  - 'Snapshot-only context pattern'
  - 'Modifier pipeline architecture'
  - 'Event-driven modifier hooks'
  - 'Priority-based override system'
test_patterns:
  - 'Manual in-game testing'
  - 'Modifier composition testing'
  - 'Passive override verification'
  - 'Event dispatch testing'
---

# Tech-Spec: Branching Talent Tree with Modifier System Implementation

**Created:** 2026-01-24

## Overview

### Problem Statement

The current talent system needs a complete architecture for:
- Branching talent trees with multiple paths
- Modifiers that can tweak numbers, add passives, inject active effects, or replace base passives
- Clean separation between static data, runtime state, and computed snapshots
- Scalable design that supports dozens of talents with complex interactions
- Minimal save/load footprint with no behavior persistence

### Solution

Implement a modifier-based talent system with:
- **Talent**: Interface defining base behavior and context building
- **TalentModifier**: Interface for augmenting talents with stat changes, passive behaviors, or overrides
- **TalentContext**: Snapshot-only computed state (rebuilt on demand, never cached)
- **TalentPassive**: Stateless passive behavior provider that can be overridden
- **ModifierState**: Persistent runtime state for modifiers that need memory
- **Execution Layer**: Tick dispatcher and event system for modifier hooks

The system uses a priority-based override system for passives and supports modifier composition through a pipeline architecture.

### Scope

**In Scope:**
- Core talent and modifier interfaces
- Context builder with modifier pipeline
- Passive override system with priorities
- Event-driven modifier hooks
- Modifier state persistence
- Talent tree resolver for branch selection
- Execution layer for ticks and events
- Example implementation (Elemental Unleash)

**Out of Scope:**
- GUI for talent trees
- Visual effects
- Talent unlock/progression UI
- Combat mode integration (separate concern)
- Network syncing (uses existing infrastructure)

## Context for Development

### Codebase Patterns

**Key Design Principles:**
- **Stateless Interfaces**: All behavior interfaces are stateless (no fields)
- **Snapshot Context**: TalentContext is read-only after building, never mutated
- **Clean Persistence**: Only save choices (branches) and modifier state, never behavior
- **Modifier Pipeline**: Modifiers apply in sequence, each seeing previous changes
- **Override Priority**: Higher priority passive overrides win

**Existing Infrastructure to Replace:**
- Current Talent class becomes an interface
- TalentContext becomes snapshot-only (remove mutable operations)
- Modifier system becomes primary extension mechanism

### Files to Reference

| File | Purpose | Key Changes |
| ---- | ------- | ----------- |
| [Talent.java](src/main/java/com/complextalents/talent/Talent.java) | Current base class | Convert to interface |
| [TalentContext.java](src/main/java/com/complextalents/talent/TalentContext.java) | Context class | Make snapshot-only |
| [TalentModifier.java](src/main/java/com/complextalents/talent/TalentModifier.java) | Existing modifier | Enhance with hooks |

### Technical Decisions

1. **Interface-Based Architecture:**
   - Talent is an interface, not a class
   - All behaviors defined through interfaces
   - Stateless implementations (often enums)

2. **Passive Override System:**
   - Modifiers can completely replace base passive
   - Priority determines which override wins
   - Non-overriding modifiers still execute

3. **Context as Pure Snapshot:**
   - Built fresh for each operation
   - Contains resolved stats and passive provider
   - No mutable state or timers

4. **Modifier State Separation:**
   - ModifierState holds persistent data
   - Stored separately from context
   - Only for modifiers that need memory

5. **Event-Driven Extensions:**
   - Modifiers can listen to talent events
   - Type-safe event dispatch
   - Decoupled from base talent logic

## Implementation Plan

### Tasks

**TASK 1: Create Core Talent Interface**
- **File**: New file `talent/ITalent.java`
- **Action**:
  ```java
  public interface ITalent {
      ResourceLocation id();

      /**
       * Build the base context for this talent.
       * Sets default passive, stats, and capabilities.
       */
      void buildContext(TalentContext ctx);

      // Optional activation hooks
      default void onActivate(ServerPlayer player, TalentContext ctx) {}
      default void onDeactivate(ServerPlayer player, TalentContext ctx) {}
      default void onDetonate(ServerPlayer player, TalentContext ctx) {}
  }
  ```

**TASK 2: Create TalentPassive Interface**
- **File**: New file `talent/TalentPassive.java`
- **Action**:
  ```java
  /**
   * Stateless passive behavior provider.
   * Often implemented as enums for singleton pattern.
   */
  public interface TalentPassive {
      void tick(ServerPlayer player, TalentContext ctx);
  }
  ```

**TASK 3: Create Modifier Interfaces**
- **Files**: New files in `talent/` package
- **Action**:
  1. Base modifier interface:
     ```java
     public interface ITalentModifier {
         ResourceLocation id();

         /**
          * Apply stat changes to the context during building
          */
         default void apply(TalentContext ctx) {}
     }
     ```
  2. Passive tick modifier:
     ```java
     public interface PassiveTickModifier extends ITalentModifier {
         void onTick(ServerPlayer player, TalentContext ctx, ModifierState state);
     }
     ```
  3. Passive override modifier:
     ```java
     public interface PassiveOverrideModifier extends ITalentModifier {
         TalentPassive overridePassive();
         int priority(); // higher wins
     }
     ```
  4. Event listener modifier:
     ```java
     public interface TalentEventListener<E extends TalentEvent> extends ITalentModifier {
         Class<E> eventType();
         void onEvent(ServerPlayer player, TalentContext ctx, E event, ModifierState state);
     }
     ```

**TASK 4: Update TalentContext to Snapshot-Only**
- **File**: `talent/TalentContext.java`
- **Action**:
  1. Remove all setter methods except during building
  2. Add fields:
     - `TalentPassive passive` - The resolved passive provider
     - `List<ResourceLocation> activeModifiers` - Applied modifier IDs
  3. Make all fields private final after building
  4. Add builder pattern or package-private setters for construction only
  5. Methods:
     ```java
     public final class TalentContext {
         private TalentPassive passive;
         private float focusDrainPerSecond;
         private int maxElementalStacks;
         // ... other stats

         private final List<ResourceLocation> activeModifiers = new ArrayList<>();

         // Package-private for builder only
         void setPassive(TalentPassive passive) {
             this.passive = passive;
         }

         // Public getters only
         public TalentPassive passive() {
             return passive;
         }

         public float getFocusDrainPerSecond() {
             return focusDrainPerSecond;
         }

         public List<ResourceLocation> getActiveModifiers() {
             return Collections.unmodifiableList(activeModifiers);
         }
     }
     ```

**TASK 5: Create ModifierState System**
- **Files**: New files `talent/ModifierState.java` and `talent/ModifierStateStore.java`
- **Action**:
  1. ModifierState class:
     ```java
     public class ModifierState {
         private final Map<String, Object> data = new HashMap<>();

         public int getInt(String key, int defaultValue) {
             return (Integer) data.getOrDefault(key, defaultValue);
         }

         public void setInt(String key, int value) {
             data.put(key, value);
         }

         // Similar for other types

         public CompoundTag save() {
             // NBT serialization
         }

         public void load(CompoundTag tag) {
             // NBT deserialization
         }
     }
     ```
  2. ModifierStateStore:
     ```java
     public class ModifierStateStore {
         // Stored in player capability
         private final Map<ResourceLocation, ModifierState> states = new HashMap<>();

         public static ModifierState get(ServerPlayer player, ResourceLocation modifierId) {
             // Get from player capability
             return states.computeIfAbsent(modifierId, k -> new ModifierState());
         }
     }
     ```

**TASK 6: Create TalentContextBuilder**
- **File**: New file `talent/TalentContextBuilder.java`
- **Action**:
  ```java
  public class TalentContextBuilder {

      public static TalentContext build(ServerPlayer player, ITalent talent) {
          TalentContext ctx = new TalentContext();

          // 1. Base talent defines defaults
          talent.buildContext(ctx);

          // 2. Get modifiers from tree based on player's path
          List<ITalentModifier> modifiers =
              TalentTreeResolver.resolveModifiers(player, talent);

          // 3. Resolve passive override (highest priority wins)
          PassiveOverrideModifier chosenOverride = modifiers.stream()
              .filter(m -> m instanceof PassiveOverrideModifier)
              .map(m -> (PassiveOverrideModifier) m)
              .max(Comparator.comparingInt(PassiveOverrideModifier::priority))
              .orElse(null);

          if (chosenOverride != null) {
              ctx.setPassive(chosenOverride.overridePassive());
          }

          // 4. Apply all modifier stat changes
          for (ITalentModifier mod : modifiers) {
              mod.apply(ctx);
              ctx.getActiveModifiers().add(mod.id());
          }

          return ctx; // Now immutable
      }
  }
  ```

**TASK 7: Create TalentTreeResolver**
- **File**: New file `talent/TalentTreeResolver.java`
- **Action**:
  ```java
  public class TalentTreeResolver {

      /**
       * Resolve which modifiers are active based on player's
       * learned talents and selected branches
       */
      public static List<ITalentModifier> resolveModifiers(
              ServerPlayer player, ITalent talent) {

          List<ITalentModifier> result = new ArrayList<>();

          // Get player's talent data
          PlayerTalents talents = getPlayerTalents(player);
          String branch = talents.getSelectedBranch(talent.id());
          int rank = talents.getTalentRank(talent.id());

          // Walk the tree and collect modifiers
          TalentNode node = TalentRegistry.getTree(talent.id());
          collectModifiers(node, branch, rank, result);

          return result;
      }

      private static void collectModifiers(
              TalentNode node, String branch, int rank,
              List<ITalentModifier> out) {
          // Tree traversal logic
      }
  }
  ```

**TASK 8: Create Execution Layer**
- **File**: New file `talent/TalentExecutor.java`
- **Action**:
  ```java
  public class TalentExecutor {

      /**
       * Execute one tick of a talent
       */
      public static void tickTalent(ServerPlayer player, ITalent talent) {
          // Build fresh context
          TalentContext ctx = TalentContextBuilder.build(player, talent);

          // Execute base or overridden passive
          if (ctx.passive() != null) {
              ctx.passive().tick(player, ctx);
          }

          // Execute modifier passives
          List<ITalentModifier> modifiers =
              TalentTreeResolver.resolveModifiers(player, talent);

          for (ITalentModifier mod : modifiers) {
              if (mod instanceof PassiveTickModifier ptm) {
                  ModifierState state = ModifierStateStore.get(player, mod.id());
                  ptm.onTick(player, ctx, state);
              }
          }
      }

      /**
       * Dispatch an event to all listening modifiers
       */
      public static <E extends TalentEvent> void dispatchEvent(
              ServerPlayer player, E event) {

          // For each equipped talent
          for (ITalent talent : getEquippedTalents(player)) {
              TalentContext ctx = TalentContextBuilder.build(player, talent);

              List<ITalentModifier> modifiers =
                  TalentTreeResolver.resolveModifiers(player, talent);

              for (ITalentModifier mod : modifiers) {
                  if (mod instanceof TalentEventListener<?> listener) {
                      if (listener.eventType().isInstance(event)) {
                          ModifierState state = ModifierStateStore.get(player, mod.id());
                          ((TalentEventListener<E>) listener)
                              .onEvent(player, ctx, event, state);
                      }
                  }
              }
          }
      }
  }
  ```

**TASK 9: Create TalentEvent Base**
- **File**: New file `api/events/TalentEvent.java`
- **Action**:
  ```java
  public abstract class TalentEvent extends Event {
      private final ServerPlayer player;

      protected TalentEvent(ServerPlayer player) {
          this.player = player;
      }

      public ServerPlayer getPlayer() {
          return player;
      }
  }

  // Example events
  public class ElementalReactionTriggeredEvent extends TalentEvent {
      private final ElementType element;
      private final Entity target;
      // ...
  }
  ```

**TASK 10: Implement Elemental Unleash Example**
- **Files**: New implementation classes
- **Action**:
  1. Base talent:
     ```java
     public class ElementalUnleashTalent implements ITalent {
         public static final ResourceLocation ID = new ResourceLocation("complextalents", "elemental_unleash");

         @Override
         public ResourceLocation id() {
             return ID;
         }

         @Override
         public void buildContext(TalentContext ctx) {
             ctx.setPassive(ElementalUnleashBasePassive.INSTANCE);
             ctx.setFocusDrainPerSecond(10f);
             ctx.setMaxElementalStacks(6);
         }

         @Override
         public void onActivate(ServerPlayer player, TalentContext ctx) {
             // Toggle logic
         }

         @Override
         public void onDetonate(ServerPlayer player, TalentContext ctx) {
             // Trigger super-reaction
         }
     }
     ```
  2. Base passive (enum singleton):
     ```java
     public enum ElementalUnleashBasePassive implements TalentPassive {
         INSTANCE;

         @Override
         public void tick(ServerPlayer player, TalentContext ctx) {
             // Apply elemental stack
             ElementalStackManager.addStack(player, getCurrentElement(player));

             // Drain focus
             PlayerTalents.consumeResource(player, ctx.getFocusDrainPerSecond() / 20f);
         }
     }
     ```
  3. Lingering override modifier:
     ```java
     public class LingeringPassiveModifier implements PassiveOverrideModifier {
         public static final ResourceLocation ID = new ResourceLocation("complextalents", "lingering_passive");

         @Override
         public ResourceLocation id() {
             return ID;
         }

         @Override
         public TalentPassive overridePassive() {
             return LingeringUnleashPassive.INSTANCE;
         }

         @Override
         public int priority() {
             return 10;
         }

         @Override
         public void apply(TalentContext ctx) {
             // Reduce focus drain
             ctx.setFocusDrainPerSecond(6f);
         }
     }
     ```
  4. Lingering passive:
     ```java
     public enum LingeringUnleashPassive implements TalentPassive {
         INSTANCE;

         @Override
         public void tick(ServerPlayer player, TalentContext ctx) {
             // Apply stack with decay
             ElementalStackManager.addStack(player, getCurrentElement(player));
             ElementalStackManager.decayStacks(player, 1);

             // Lower focus drain
             PlayerTalents.consumeResource(player, 6f / 20f);
         }
     }
     ```
  5. Amplification modifier (additional passive):
     ```java
     public class AmplificationModifier implements PassiveTickModifier {
         public static final ResourceLocation ID = new ResourceLocation("complextalents", "amplification");

         @Override
         public void onTick(ServerPlayer player, TalentContext ctx, ModifierState state) {
             int timer = state.getInt("timer", 0);
             float damageBonus = state.getFloat("damageBonus", 0f);

             timer++;
             if (timer >= 20) {
                 damageBonus += 0.1f;
                 timer = 0;

                 // Apply damage bonus somehow
                 player.getAttribute(Attributes.ATTACK_DAMAGE)
                     .addTransientModifier(...);
             }

             state.setInt("timer", timer);
             state.setFloat("damageBonus", damageBonus);
         }
     }
     ```

**TASK 11: Update Player Capability for Branches**
- **File**: `capability/PlayerTalentsImpl.java`
- **Action**:
  1. Add branch selection storage:
     ```java
     private final Map<ResourceLocation, String> selectedBranches = new HashMap<>();
     private final Map<ResourceLocation, ModifierState> modifierStates = new HashMap<>();
     ```
  2. Add methods:
     - `void selectBranch(ResourceLocation talentId, String branch)`
     - `String getSelectedBranch(ResourceLocation talentId)`
     - `ModifierState getModifierState(ResourceLocation modifierId)`
  3. Update NBT serialization:
     - Save selected branches as string map
     - Save modifier states with their serialization

**TASK 12: Create Tick Integration**
- **File**: New or update existing tick handler
- **Action**:
  ```java
  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase != Phase.END || event.player.level().isClientSide) return;

      ServerPlayer player = (ServerPlayer) event.player;
      PlayerTalents talents = getPlayerTalents(player);

      // Tick each equipped talent
      for (String talentId : talents.getEquippedTalents()) {
          if (talentId == null) continue;

          ITalent talent = TalentRegistry.get(new ResourceLocation(talentId));
          if (talent != null) {
              TalentExecutor.tickTalent(player, talent);
          }
      }
  }
  ```

### Acceptance Criteria

**AC1: Base Talent Defines Defaults**
- GIVEN ElementalUnleashTalent
- WHEN context is built without modifiers
- THEN base passive and stats are set
- AND context is immutable after building

**AC2: Passive Override Works**
- GIVEN LingeringPassiveModifier with priority 10
- WHEN context is built
- THEN LingeringUnleashPassive replaces base passive
- AND stats from modifier are applied

**AC3: Multiple Modifiers Compose**
- GIVEN Lingering and Amplification modifiers
- WHEN both are active
- THEN Lingering overrides passive
- AND Amplification adds its tick behavior
- AND both execute during tick

**AC4: Modifier State Persists**
- GIVEN AmplificationModifier with timer and bonus
- WHEN player logs out and back in
- THEN modifier state is preserved
- AND timer continues from saved value

**AC5: Context is Snapshot-Only**
- GIVEN a built TalentContext
- WHEN attempting to modify it
- THEN no public setters exist
- AND all fields are effectively final

**AC6: Event Dispatch Works**
- GIVEN a modifier implementing TalentEventListener
- WHEN the event type occurs
- THEN the modifier receives the event
- AND can modify its state

**AC7: Priority Resolution Correct**
- GIVEN two PassiveOverrideModifiers
- WHEN both are active
- THEN higher priority wins
- AND lower priority is ignored

**AC8: Clean Save Format**
- GIVEN a player with talents and modifiers
- WHEN data is saved
- THEN only branches and modifier states are persisted
- AND no context or behavior is saved

**AC9: Stateless Interfaces**
- GIVEN all talent interfaces
- WHEN reviewing implementations
- THEN no instance fields exist
- AND often implemented as enums

**AC10: Fresh Context Each Operation**
- GIVEN multiple ticks of a talent
- WHEN each tick executes
- THEN fresh context is built
- AND no context caching occurs

## Additional Context

### Dependencies

**Core Systems:**
- Forge capability system for persistence
- Event bus for tick handling
- NBT for state serialization
- Existing talent registry

**Design Patterns:**
- Interface-based architecture
- Singleton pattern (enum implementations)
- Builder pattern (context construction)
- Strategy pattern (passive overrides)
- Observer pattern (event listeners)

### Testing Strategy

**Phase 1: Core Infrastructure**
1. Test talent interface implementation
2. Test context builder
3. Test passive override priority
4. Test modifier state persistence

**Phase 2: Modifier Composition**
1. Test multiple modifiers on same talent
2. Test passive override + additional passives
3. Test event dispatch to modifiers
4. Test modifier state updates

**Phase 3: Example Implementation**
1. Test ElementalUnleash base behavior
2. Test with Lingering override
3. Test with Amplification modifier
4. Test full combination

**Phase 4: Integration**
1. Test save/load cycle
2. Test with multiple talents
3. Test branch switching
4. Performance testing

### Notes

**Key Principles:**
- **Stateless**: Interfaces have no fields
- **Immutable**: Context is snapshot-only
- **Composable**: Modifiers layer cleanly
- **Persistent**: Only state and choices saved
- **Extensible**: New modifiers don't break existing

**Implementation Order:**
1. Core interfaces (foundation)
2. Context and builder
3. Modifier state system
4. Tree resolver
5. Execution layer
6. Example implementation
7. Integration with existing

**Performance Considerations:**
- Context rebuilt each tick (intentional, ensures consistency)
- Modifier lists cached per tick
- State lookups use hash maps
- Event dispatch uses type checking (minimal overhead)