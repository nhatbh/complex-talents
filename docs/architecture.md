# Complex Talents - Architecture Documentation

**Project**: Complex Talents
**Type**: Minecraft Forge Mod (Extension)
**Version**: 1.0.0
**Minecraft Version**: 1.20.1
**Forge Version**: 47.4.10
**Language**: Java 17
**Build System**: Gradle

## Executive Summary

Complex Talents is a sophisticated Minecraft Forge mod that implements a comprehensive talent system with deep integration into an elemental magic framework. The mod introduces a unique 5-slot talent system, 6-element interaction mechanics, elemental reactions (both standard and super-reactions), and seamless integration with Iron's Spellbooks mod.

## Architecture Pattern

**Event-Driven Minecraft Forge Architecture** with:
- Capability-based player data persistence
- Network packet synchronization (client-server)
- Event bus subscription system
- Modular subsystem design
- Static registry pattern for talents and effects

## Core Systems

### 1. Talent System

**5-Slot Architecture**:
- **DEFINITION**: Defines core playstyle, provides resource bar
- **HARMONY**: Enhances core mechanics
- **CRESCENDO**: Activates powerful effects
- **RESONANCE**: Defensive/utility applications
- **FINALE**: Ultimate/capstone ability

**Talent Types**:
- **Passive**: Always active when equipped
- **Active**: Cooldown-based activation
- **Hybrid**: Both passive and active effects
- **Branching**: Multi-path progression (2-4 branches)

**Resource Bar System**:
- Only Definition talents can provide resource bars
- Types: BAR, ORBS, NUMERIC, CUSTOM
- Custom renderer support via `ResourceBarRenderer` interface
- Automatic regeneration with configurable rate

### 2. Elemental System

**6 Elements**:
1. FIRE (Red) - High burst damage
2. AQUA (Blue) - Healing/sustain
3. ICE (Cyan) - Crowd control
4. LIGHTNING (Yellow) - Chain effects
5. NATURE (Green) - DoT/zones
6. ENDER (Purple) - Utility/teleport

**Element Stack Mechanics**:
- Each entity can have multiple element types simultaneously
- Stack count per element (default max: 2)
- Stacks decay after 10 seconds
- Applying stack triggers reaction check

**16 Elemental Reactions**:
- **Amplifying**: VAPORIZE, MELT, OVERLOADED, HYPERBLOOM, BURGEON
- **DoT**: ELECTRO_CHARGED, BURNING
- **Crowd Control**: FROZEN
- **Debuff**: SUPERCONDUCT, FRACTURE, WITHERING_SEED, DECREPIT_GRASP
- **Utility**: UNSTABLE_WARD, RIFT_PULL, SINGULARITY
- **Spawn**: BLOOM

**Super-Reactions**:
- Triggered when 3-6 unique elements on target
- 4 tiers with increasing damage multipliers (1.0x - 3.0x)
- Element-specific implementations (Fire, Ice, Aqua, Lightning, Nature, Ender)
- Consumes all element stacks

### 3. Capability System

**PlayerTalents Capability**:
- Attached to ServerPlayer entities
- Stores: unlocked talents, levels, cooldowns, active state, equipped slots, resource, branches
- NBT serialization for persistence
- Dirty-flag batched synchronization

**Lifecycle**:
- Created on player join
- Cloned on respawn/dimension change
- Synced to client on login and changes
- Saved with player data

### 4. Network Architecture

**5 Packet Types**:
1. **SyncTalentsPacket** (S→C): Full talent data sync
2. **TalentActivationPacket** (C→S): Activation request
3. **SpawnParticlesPacket** (S→C): Element/reaction particles
4. **SpawnReactionTextPacket** (S→C): Floating damage text
5. **SyncBranchSelectionPacket** (S→C): Branch choice sync

**Protocol**: SimpleChannel with versioning ("1")
**Optimization**: Batched sync with dirty flag

### 5. Integration System

**Iron's Spellbooks Integration**:
- Listens to `SpellDamageEvent`
- Maps spell schools to elements (fire, ice, lightning, etc.)
- Applies element stacks on spell damage
- Fallback particle system via reflection (no hard dependency)

**Travel Optics Support**:
- Detects "wet" effect for passive Aqua application

## Data Flow

### Talent Activation Flow
```
Client: Press keybind
  ↓
TalentActivationPacket → Server
  ↓
Validate (exists, unlocked, no cooldown)
  ↓
talent.onActivate(player, level)
  ↓
Set cooldown → Mark dirty
  ↓
Next tick: SyncTalentsPacket → Client
  ↓
Client: Display cooldown
```

### Elemental Reaction Flow
```
Iron's Spellbooks: Spell damage
  ↓
SpellDamageEvent
  ↓
Map school → ElementType
  ↓
ElementalStackManager.applyElementStack()
  ↓
BEFORE adding: checkAndTriggerReactions()
  ↓
For each existing element:
  if canReact && playerHasTalent:
    ElementalReactionHandler.triggerReaction()
  ↓
Add/refresh stack
  ↓
SpawnParticlesPacket → Clients
```

### Super-Reaction Detection
```
applyElementStack() → 3+ unique elements detected
  ↓
SuperReactionHandler.checkAndTrigger()
  ↓
Determine tier from element count
  ↓
Get primary element (highest stack)
  ↓
Execute element's super-reaction
  ↓
Apply tier multiplier
  ↓
Clear ALL stacks (consumed)
```

## Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Framework | Minecraft Forge | 47.4.10 |
| Language | Java | 17 |
| Build Tool | Gradle | 8.x |
| Minecraft | Minecraft | 1.20.1 |
| Main Dependency | Iron's Spellbooks | 3.4.0.11 |
| Runtime Deps | GeckoLib | 4.4.2 |
| | Curios | 5.4.7 |
| | Caelus | 3.2.0 |
| | PlayerAnimator | 1.0.2-rc1 |

## Module Structure

```
com.complextalents/
├── TalentsMod.java              # Main @Mod class
├── api/                         # Public API for other mods
├── talent/                      # Talent system (base classes, registry)
├── capability/                  # Player data storage
├── elemental/                   # Element stacks, reactions, super-reactions
│   ├── talents/                 # Elemental mastery talents
│   │   └── mage/               # Elemental Mage 5-slot talents
│   ├── effects/                # Custom mob effects
│   ├── entity/                 # Reaction entities
│   ├── superreaction/          # Super-reaction system
│   ├── attributes/             # Mastery attributes
│   └── integration/            # Mod integrations
├── network/                     # Packet handlers
├── client/                      # Client-side UI, rendering, keybindings
├── command/                     # Server commands
└── config/                      # Configuration system
```

## Performance Considerations

**Optimizations**:
- Batched network sync (dirty flag)
- Tick processing at 10/20 tick intervals (not every tick)
- Early returns on config checks
- LazyOptional for capabilities
- Reflection-based particle fallback

**Scaling**:
- Per-player capability instances (no shared state)
- Element stack cleanup on entity death
- Automatic stack expiration (10 seconds)
- Cooldown map pruning

## Configuration System

**3 Config Files**:
1. **TalentConfig** (COMMON): Core talent and elemental settings
2. **ElementalReactionConfig** (COMMON): Detailed reaction parameters
3. **TalentClientConfig** (CLIENT): UI and visual settings

**Key Configurables**:
- Enable/disable toggles for all systems
- Reaction damage multipliers (per-reaction)
- Stack decay time and max count
- Mastery scaling constants
- Effect durations
- Particle density

## Extension Points

**For Other Mods**:
- Public `TalentAPI` for talent management
- `TalentEvent` and `ElementalReactionEvent` for hooking
- Custom talent creation via base classes
- Element application API
- Custom resource bar renderers

**Creating Custom Talents**:
```java
public class MyTalent extends PassiveTalent {
    public MyTalent() {
        super(id, name, description, maxLevel, slotType);
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Grant logic
    }

    @Override
    public void onTick(ServerPlayer player, int level) {
        // Passive effect
    }
}

// Register
TalentRegistry.register("my_talent", () -> new MyTalent());
```

## Security & Validation

**Server Authority**:
- All talent/element logic validated server-side
- Client packets are requests (not commands)
- Cooldowns enforced on server
- Capability stored only on server

**Friendly Fire Protection**:
- Config option to prevent reactions on friendly entities
- Team/alliance checking

**Input Validation**:
- Talent existence checks
- Level bounds checking
- Slot type matching
- Definition requirement validation

## Known Limitations

1. Element application requires Iron's Spellbooks integration
2. Super-reactions rare in normal gameplay (3+ unique elements needed)
3. Branch selection via command (no GUI)
4. Single resource bar per player (from Definition talent)
5. No automatic Definition unequip validation

## Future Architecture Considerations

**Potential Enhancements**:
- Branch selection GUI
- Multiple resource bars support
- Additional mod integrations (other magic mods)
- Element application from non-spell sources
- Talent tree visualization
- Achievement/progression tracking
- Multiplayer balance features

---

**Generated**: 2026-01-23
**Scan Type**: Exhaustive (all 94 source files analyzed)
**Documentation Status**: Complete architectural overview
