# Complex Talents - Project Overview

## Project Information

**Name**: Complex Talents
**Version**: 1.0.0
**Type**: Minecraft Forge Mod
**Minecraft Version**: 1.20.1
**Forge Version**: 47.4.10
**Language**: Java 17
**License**: All Rights Reserved
**Authors**: Complex Talents Team

## Description

A comprehensive talent system mod with passive, active, and hybrid abilities. Features an elemental reaction system unlocked through talents, with support for mod integrations like Iron's Spellbooks and T.O's Spellbooks.

## Quick Reference

### Repository Structure
- **Type**: Monolith (single cohesive codebase)
- **Parts**: 1 (main mod)
- **Source Files**: 94 Java files
- **Build System**: Gradle

### Technology Stack Summary

| Component | Technology |
|-----------|-----------|
| Framework | Minecraft Forge 47.4.10 |
| Language | Java 17 |
| Build Tool | Gradle |
| Primary Integration | Iron's Spellbooks 3.4.0.11 |
| Runtime Dependencies | GeckoLib, Curios, Caelus, PlayerAnimator |

### Architecture Type
Event-driven Minecraft Forge mod architecture with capability-based persistence and network synchronization

## Core Features

### 1. Talent System
- **5-Slot System**: DEFINITION, HARMONY, CRESCENDO, RESONANCE, FINALE
- **3 Talent Types**: Passive, Active, Hybrid
- **Branching Progression**: Up to 4-way branch choices
- **Custom Resource Bars**: Configurable resource systems per Definition talent
- **12 Base Talents**: Including 7 elemental mastery talents and 5 Elemental Mage talents

### 2. Elemental System
- **6 Elements**: Fire, Aqua, Ice, Lightning, Nature, Ender
- **Element Stacks**: Up to 2 stacks per element, 10-second decay
- **16 Reactions**: From simple amplifying reactions to complex debuff combinations
- **Super-Reactions**: Triggered by 3-6 unique elements with tiered power
- **10 Custom Effects**: Frostbite, Conductive, Brittle, Panic, Vulnerable, and more

### 3. Integration
- **Iron's Spellbooks**: Automatic element application from spells
- **Travel Optics**: Fallback detection for Aqua element
- **Public API**: For other mod developers
- **Event System**: Extensible hooks for custom reactions

## Project Structure

### Main Packages
```
com.complextalents/
├── api/              # Public API and events
├── talent/           # Talent system core
├── capability/       # Player data storage
├── elemental/        # Elemental mechanics
├── network/          # Client-server communication
├── client/           # UI and rendering
├── command/          # Server commands
└── config/           # Configuration
```

### Key Directories
- `src/main/java/` - Java source code
- `src/main/resources/` - Assets, configs, mod metadata
- `src/main/resources/assets/complextalents/` - Textures, lang files
- `src/generated/resources/` - Data-generated resources
- `docs/` - Project documentation

## Getting Started

### For Players

**Commands**:
- `/grantalent <player> <talent> [level]` - Grant talent (OP)
- `/revoketalent <player> <talent>` - Remove talent (OP)
- `/listtalents [player]` - List unlocked talents
- `/selectbranch <talent> <rank> <path>` - Choose branch path

**Configuration Files**:
- `config/complextalents-common.toml` - Core settings
- `config/complextalents-reactions.toml` - Reaction parameters
- `config/complextalents-client.toml` - Visual settings

### For Developers

**Setup**:
1. Open in IntelliJ IDEA or Eclipse
2. Import Gradle project
3. Run `./gradlew genIntellijRuns` or `./gradlew genEclipseRuns`
4. Run `./gradlew build` to compile

**API Usage**:
```java
// Grant a talent
TalentAPI.grantTalent(serverPlayer, talentId, level);

// Apply element
TalentAPI.applyElement(target, ElementType.FIRE, source);

// Check talent
boolean has = TalentAPI.hasTalent(player, talentId);
```

## Documentation Files

### Generated Documentation
- [Architecture](./architecture.md) - Complete architectural overview
- [Source Tree Analysis](./source-tree-analysis.md) - Annotated file structure
- [Development Guide](./development-guide.md) - Setup and build instructions
- [Component Inventory](./component-inventory.md) - All classes and components
- [Data Models](./data-models.md) - Capability structure and NBT format

### Existing Documentation
- [README.txt](../README.txt) - Forge setup instructions
- [IMPLEMENTATION_VERIFICATION_REPORT.md](../IMPLEMENTATION_VERIFICATION_REPORT.md) - Recent verification
- [ELEMENTAL_MAGE_IMPLEMENTATION_CHECKLIST.md](../ELEMENTAL_MAGE_IMPLEMENTATION_CHECKLIST.md) - Elemental mage progress
- [TALENT_SLOT_SYSTEM.md](../TALENT_SLOT_SYSTEM.md) - Slot system documentation
- [ELEMENTAL_REACTION_IMPLEMENTATION_PLAN.md](../ELEMENTAL_REACTION_IMPLEMENTATION_PLAN.md) - Reaction system plan
- [IMPLEMENTATION_PROGRESS.md](../IMPLEMENTATION_PROGRESS.md) - Overall progress
- [IRON_SPELLBOOKS_INTEGRATION_CHANGES.md](../IRON_SPELLBOOKS_INTEGRATION_CHANGES.md) - Integration notes

## Development Status

### Completed Systems
✅ Core talent framework (Passive, Active, Hybrid)
✅ 5-slot talent system with dependencies
✅ Branching talent progression
✅ Resource bar system
✅ Capability-based player data
✅ Network synchronization
✅ 6-element system with stack mechanics
✅ 16 elemental reactions
✅ Super-reaction system (4 tiers)
✅ 10 custom mob effects
✅ 4 custom entities
✅ Mastery attribute system
✅ Iron's Spellbooks integration
✅ Command system
✅ Configuration system
✅ Elemental Mage talent set (5 talents)

### In Progress
⏳ Full talent screen GUI
⏳ Branch selection UI
⏳ Cooldown HUD overlays

### Planned
❌ Additional talent sets
❌ Keybinding implementation
❌ Localization (i18n)
❌ Tutorial/guide book
❌ Achievement system

## Key Statistics

- **Total Java Files**: 94
- **Total Lines of Code**: ~15,000+
- **Talents Registered**: 12 (7 mastery + 5 Elemental Mage)
- **Elemental Reactions**: 16
- **Super-Reactions**: 6 (one per element)
- **Custom Effects**: 10
- **Custom Entities**: 4
- **Network Packets**: 5
- **Commands**: 5
- **Config Options**: 50+

## Contributing

This is a proprietary mod. For issues or suggestions, contact the mod authors.

## License

All Rights Reserved

## Links

- **Mod Support**: Contact mod authors
- **Forge Documentation**: https://docs.minecraftforge.net/
- **Iron's Spellbooks**: https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks

---

**Last Updated**: 2026-01-23
**Documentation Version**: 1.0
**Generated By**: BMAD Document Project Workflow
