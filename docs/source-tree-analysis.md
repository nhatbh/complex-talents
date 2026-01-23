# Complex Talents - Source Tree Analysis

## Project Root Structure

```
d:\ModDevelopment\complex-talents/
├── .claude/                     # Claude Code configuration
├── .git/                        # Git repository
├── .gradle/                     # Gradle cache
├── .vscode/                     # VS Code settings
├── _bmad/                       # BMAD workflow system
├── _bmad-output/                # BMAD generated artifacts
├── bin/                         # Compiled binaries
├── build/                       # Build output
├── docs/                        # 📚 PROJECT DOCUMENTATION (this directory)
├── gradle/                      # Gradle wrapper
├── src/                         # 🔥 SOURCE CODE (main development)
│   ├── generated/               # Auto-generated resources
│   ├── main/                    # Main source code
│   │   ├── java/                # Java source files
│   │   └── resources/           # Assets, configs, metadata
│   └── test/                    # Test source code
├── build.gradle                 # Gradle build configuration
├── gradle.properties            # Project properties
├── gradlew                      # Gradle wrapper script
├── settings.gradle              # Gradle settings
├── README.txt                   # Forge setup instructions
├── LICENSE.txt                  # License file
├── CREDITS.txt                  # Credits
└── changelog.txt                # Mod changelog
```

## Source Code Structure (`src/main/java/com/complextalents/`)

### Entry Point
```
TalentsMod.java                  # 🎯 Main @Mod class - Entry point
```

### Package: api/ (Public API)
```
api/
├── TalentAPI.java               # Public API for other mods
└── events/
    ├── TalentEvent.java         # Base talent event
    └── ElementalReactionEvent.java  # Reaction event for mod hooks
```

### Package: talent/ (Core Talent System)
```
talent/
├── Talent.java                  # Abstract base class for all talents
├── PassiveTalent.java           # Always-active talents
├── ActiveTalent.java            # Cooldown-based activated talents
├── HybridTalent.java            # Both passive + active effects
├── BranchingTalentBase.java    # Interface for branching progression
├── BranchingPassiveTalent.java # Passive with branch paths
├── BranchingActiveTalent.java  # Active with branch paths
├── BranchingHybridTalent.java  # Hybrid with branch paths
├── TalentType.java              # Enum: PASSIVE, ACTIVE, HYBRID
├── TalentSlotType.java          # Enum: 5 slot types
├── TalentRegistry.java          # Static talent registration
├── TalentManager.java           # Registry wrapper
├── TalentBranches.java          # Global branch selection storage
├── ResourceBarConfig.java       # Resource bar configuration
├── ResourceBarRenderer.java    # Interface for custom rendering
└── ResourceBarType.java         # Enum: BAR, ORBS, NUMERIC, CUSTOM
```

### Package: capability/ (Player Data Storage)
```
capability/
├── PlayerTalents.java           # Interface - Capability contract
├── PlayerTalentsImpl.java       # Implementation - Data storage logic
├── PlayerTalentsProvider.java  # Provider - Capability registration + events
└── TalentsCapabilities.java    # Static capability token holder
```

### Package: elemental/ (Elemental Magic System)
```
elemental/
├── ElementType.java             # Enum: 6 elements + reaction matrix
├── ElementStack.java            # Single element stack on entity
├── ElementalStackManager.java  # Global stack management + world tick
├── ElementalReaction.java       # Enum: All 16 reaction types
├── ElementalReactionHandler.java  # Reaction triggering + damage calc
├── ElementalTalents.java        # Elemental talent registration
├── ParticleHelper.java          # Particle spawning utilities
├── DamageOverTimeManager.java  # DoT effect tracking
└── ResistanceModifier.java     # Resistance reduction system
```

### Package: elemental/talents/ (Elemental Mastery Talents)
```
elemental/talents/
├── ElementalMasteryTalent.java # General mastery (allows all reactions)
├── FireMasteryTalent.java      # Fire mastery
├── AquaMasteryTalent.java      # Aqua mastery
├── IceMasteryTalent.java       # Ice mastery
├── LightningMasteryTalent.java # Lightning mastery
├── NatureMasteryTalent.java    # Nature mastery
└── EnderMasteryTalent.java     # Ender mastery
```

### Package: elemental/talents/mage/ (Elemental Mage 5-Slot Set)
```
elemental/talents/mage/
├── ElementalMageDefinition.java  # DEFINITION slot - Focus resource
├── attunement/
│   └── ElementalAttunementTalent.java  # HARMONY - Focus generation boost
├── ward/
│   └── ElementalWardTalent.java        # RESONANCE - Defensive Focus use
├── conflux/
│   └── ElementalConfluxTalent.java     # CRESCENDO - Multi-element trigger
└── unleash/
    └── ElementalUnleashTalent.java     # FINALE - Massive Focus dump
```

### Package: elemental/effects/ (Custom Mob Effects)
```
elemental/effects/
├── ModEffects.java              # Effect registry
├── ElementalEffectHandler.java # Effect application logic
├── FrostbiteEffect.java         # Armor reduction (from Melt)
├── ConductiveEffect.java        # Crit setup (from Electro-Charged)
├── BrittleEffect.java           # Shatter bonus (from Frozen)
├── PanicEffect.java             # Movement disruption (from Burning)
├── VulnerableEffect.java        # Damage amplification (from Hyperbloom)
├── StaggerEffect.java           # Movement interrupt
├── FractureEffect.java          # Variable damage modifier
├── WitheringEffect.java         # Life siphon to caster
├── DecrepitudeEffect.java       # Attack/heal reduction
└── SpatialInstabilityEffect.java  # Teleport risk
```

### Package: elemental/entity/ (Reaction Entities)
```
elemental/entity/
├── SteamCloudEntity.java        # AoE obscuration zone (from Vaporize)
├── BloomCoreEntity.java         # Bloom core (from Bloom reaction)
├── HyperbloomProjectile.java   # Tracking projectile (triggered by Lightning on Bloom)
└── SmolderingGloomEntity.java  # DoT zone (triggered by Fire on Bloom)
```

### Package: elemental/superreaction/ (Super-Reaction System)
```
elemental/superreaction/
├── SuperReaction.java           # Interface for super-reactions
├── SuperReactionHandler.java   # Detection and execution logic
├── SuperReactionTier.java       # Enum: 4 tiers (3-6 elements)
└── reactions/
    ├── FireSuperReaction.java   # Massive AoE explosion
    ├── IceSuperReaction.java    # Freeze + shatter
    ├── AquaSuperReaction.java   # Healing/cleansing
    ├── LightningSuperReaction.java  # Chain lightning
    ├── NatureSuperReaction.java # Bloom garden
    └── EnderSuperReaction.java  # Teleport/rift
```

### Package: elemental/attributes/ (Mastery Attributes)
```
elemental/attributes/
├── MasteryAttributes.java       # Attribute registration
└── MasteryAttributeHandler.java # Attribute scaling logic
```

### Package: elemental/integration/ (Mod Integrations)
```
elemental/integration/
├── ModIntegrationHandler.java   # Mod detection
├── IronSpellbooksIntegration.java  # Spell damage event listener
└── SpellElementMapper.java      # School → Element mapping
```

### Package: network/ (Client-Server Communication)
```
network/
├── PacketHandler.java           # Network channel registration
├── SyncTalentsPacket.java       # Server → Client: Full talent sync
├── TalentActivationPacket.java # Client → Server: Activation request
├── SpawnParticlesPacket.java   # Server → Client: Element/reaction particles
├── SpawnReactionTextPacket.java # Server → Client: Floating damage text
└── SyncBranchSelectionPacket.java  # Server → Client: Branch choice sync
```

### Package: client/ (Client-Side Only)
```
client/
├── ClientHandler.java           # Client initialization
├── KeyBindings.java             # Keybinding registration
├── TalentScreen.java            # Full talent management UI
├── TalentOverlay.java           # HUD resource bar + cooldowns
└── DefaultResourceBarRenderer.java  # Default resource bar rendering
```

### Package: command/ (Server Commands)
```
command/
├── TalentCommand.java           # Base /talent command
├── GrantTalentCommand.java      # /grantalent <player> <talent> [level]
├── RevokeTalentCommand.java    # /revoketalent <player> <talent>
├── ListTalentsCommand.java      # /listtalents [player]
└── SelectBranchCommand.java     # /selectbranch <talent> <rank> <path>
```

### Package: config/ (Configuration System)
```
config/
├── TalentConfig.java            # COMMON config - Core settings
├── ElementalReactionConfig.java # COMMON config - Reaction parameters
└── TalentClientConfig.java      # CLIENT config - Visual settings
```

## Resources Structure (`src/main/resources/`)

```
resources/
├── META-INF/
│   └── mods.toml                # Mod metadata (id, version, dependencies)
├── assets/
│   └── complextalents/
│       ├── lang/                # Localization files
│       └── textures/
│           └── gui/             # UI textures
└── pack.mcmeta                  # Resource pack metadata
```

## Critical Directories Explained

### `/src/main/java/com/complextalents/`
**Purpose**: Main source code for the mod
**Entry Point**: `TalentsMod.java` (annotated with @Mod)
**Key Subdirectories**:
- `talent/` - Core talent framework (10+ classes)
- `elemental/` - Elemental magic system (50+ classes across subpackages)
- `capability/` - Player data persistence (4 classes)
- `network/` - Client-server sync (5 packet types)

### `/src/main/resources/`
**Purpose**: Non-code assets and configuration
**Key Files**:
- `META-INF/mods.toml` - Declares mod ID, version, dependencies
- `assets/complextalents/` - Textures, lang files, sounds

### `/docs/`
**Purpose**: Generated project documentation
**Created By**: BMAD Document Project workflow
**Contains**: Architecture, guides, component inventories, data models

### `/build/`
**Purpose**: Gradle build output (gitignored)
**Contains**: Compiled .class files, packaged JAR, deobfuscation maps

### `/_bmad/`
**Purpose**: BMAD workflow system configuration
**Contains**: Workflow definitions, agent configurations, templates

## File Counts by Package

| Package | Java Files | Purpose |
|---------|-----------|---------|
| talent/ | 13 | Core talent framework |
| capability/ | 4 | Player data storage |
| elemental/ (root) | 7 | Element stacks, reactions |
| elemental/talents/ | 7 | Elemental mastery talents |
| elemental/talents/mage/ | 5 | Elemental Mage talent set |
| elemental/effects/ | 11 | Custom mob effects |
| elemental/entity/ | 4 | Reaction entities |
| elemental/superreaction/ | 9 | Super-reaction system |
| elemental/attributes/ | 2 | Mastery attributes |
| elemental/integration/ | 3 | Mod integrations |
| network/ | 5 | Network packets |
| client/ | 5 | Client UI/rendering |
| command/ | 5 | Server commands |
| config/ | 3 | Configuration |
| api/ | 3 | Public API |
| **Total** | **94** | |

## Important File Locations

### Configuration Files (Runtime)
- `config/complextalents-common.toml` - Core settings (generated on first run)
- `config/complextalents-reactions.toml` - Reaction parameters
- `config/complextalents-client.toml` - Client visual settings

### Build Files
- `build.gradle` - Gradle build script
- `gradle.properties` - Project properties (mod version, Minecraft version, etc.)
- `settings.gradle` - Multi-project settings

### Documentation Files (Root)
- `README.txt` - Standard Forge setup instructions
- `ELEMENTAL_MAGE_IMPLEMENTATION_CHECKLIST.md` - Implementation progress
- `TALENT_SLOT_SYSTEM.md` - Slot system design doc
- `IMPLEMENTATION_VERIFICATION_REPORT.md` - Recent verification

## Entry Points and Critical Files

### Mod Initialization
1. **TalentsMod.java** - Main mod class, registers systems
2. **PlayerTalentsProvider.java** - Capability registration on Forge bus
3. **PacketHandler.java** - Network channel setup
4. **ModEffects.java** - Effect registration
5. **MasteryAttributes.java** - Attribute registration
6. **ElementalTalents.java** - Talent registration

### Game Lifecycle Hooks
- **onServerStarting**: Commands registered
- **onPlayerLoggedIn**: Talent sync to client
- **onPlayerTick**: Cooldowns, resource regen, sync batching
- **onWorldTick**: Element stack particles, expiration

### Client Entry Points
- **ClientHandler.java** - Client-side initialization
- **KeyBindings.java** - Keybind registration
- **TalentScreen.java** - UI entry point

## Multi-Part Structure

**Repository Type**: Monolith (single part)
**No Multi-Part Architecture**: All code in single cohesive codebase

---

**Generated**: 2026-01-23
**Total Files Analyzed**: 94 Java files
**Documentation Type**: Exhaustive source tree analysis
