# Complex Talents - Component Inventory

## Overview

This document catalogs all major components (classes, interfaces, enums) in the Complex Talents mod, organized by package and functionality.

**Total Components**: 94 Java files
**Last Updated**: 2026-01-23

---

## Core Components

### Main Entry Point

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| TalentsMod | Class | (root) | Main @Mod class, mod initialization |

---

## Talent System Components

### Base Talent Classes

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| Talent | Abstract Class | talent | Base class for all talents |
| PassiveTalent | Abstract Class | talent | Always-active talents |
| ActiveTalent | Abstract Class | talent | Cooldown-based activated talents |
| HybridTalent | Abstract Class | talent | Both passive + active effects |

### Branching Talent System

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| BranchingTalentBase | Interface | talent | Branching talent behavior contract |
| BranchingPassiveTalent | Abstract Class | talent | Passive with branch paths |
| BranchingActiveTalent | Abstract Class | talent | Active with branch paths |
| BranchingHybridTalent | Abstract Class | talent | Hybrid with branch paths |
| TalentBranches | Class (Static) | talent | Global branch selection storage |

### Talent Management

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| TalentRegistry | Class (Static) | talent | Talent registration and lookup |
| TalentManager | Class (Static) | talent | Registry wrapper with event handling |
| TalentType | Enum | talent | PASSIVE, ACTIVE, HYBRID |
| TalentSlotType | Enum | talent | 5 slot types (DEFINITION, HARMONY, etc.) |

### Resource Bar System

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| ResourceBarConfig | Class | talent | Resource bar configuration |
| ResourceBarRenderer | Interface | talent | Custom resource bar rendering contract |
| ResourceBarType | Enum | talent | BAR, ORBS, NUMERIC, CUSTOM |

---

## Capability System Components

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| PlayerTalents | Interface | capability | Capability contract for player talent data |
| PlayerTalentsImpl | Class | capability | Implementation with storage and sync logic |
| PlayerTalentsProvider | Class (Static) | capability | Capability registration and Forge event handlers |
| TalentsCapabilities | Class (Static) | capability | Capability token holder |

---

## Elemental System Components

### Core Elemental

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| ElementType | Enum | elemental | 6 element types + reaction matrix |
| ElementStack | Class | elemental | Single element stack on entity |
| ElementalStackManager | Class (Static) | elemental | Global stack management |
| ElementalReaction | Enum | elemental | 16 reaction types |
| ElementalReactionHandler | Class (Static) | elemental | Reaction triggering and damage calc |
| ElementalTalents | Class (Static) | elemental | Elemental talent registration |
| ParticleHelper | Class (Static) | elemental | Particle spawning utilities |
| DamageOverTimeManager | Class (Static) | elemental | DoT effect tracking |
| ResistanceModifier | Class (Static) | elemental | Resistance reduction system |

### Elemental Mastery Talents (7 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| ElementalMasteryTalent | Class | elemental.talents | General mastery (all reactions) |
| FireMasteryTalent | Class | elemental.talents | Fire mastery |
| AquaMasteryTalent | Class | elemental.talents | Aqua mastery |
| IceMasteryTalent | Class | elemental.talents | Ice mastery |
| LightningMasteryTalent | Class | elemental.talents | Lightning mastery |
| NatureMasteryTalent | Class | elemental.talents | Nature mastery |
| EnderMasteryTalent | Class | elemental.talents | Ender mastery |

### Elemental Mage Talents (5 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| ElementalMageDefinition | Class | elemental.talents.mage | DEFINITION - Focus resource system |
| ElementalAttunementTalent | Class | elemental.talents.mage.attunement | HARMONY - Focus generation boost |
| ElementalWardTalent | Class | elemental.talents.mage.ward | RESONANCE - Defensive Focus use |
| ElementalConfluxTalent | Class | elemental.talents.mage.conflux | CRESCENDO - Multi-element trigger |
| ElementalUnleashTalent | Class | elemental.talents.mage.unleash | FINALE - Massive Focus dump |

---

## Origin & Skill Implementations

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| AssassinOrigin | Class | impl.assassin.origin | Assassin origin registration and stats |
| ShadowWalkSkill | Class | impl.assassin.skill | Assassin active stealth skill |
| DarkMageOrigin | Class | impl.darkmage.origin | Dark Mage origin and soul mechanics |
| BloodPactSkill | Class | impl.darkmage.skill | Dark Mage HP-for-power toggle skill |
| ElementalMageOrigin | Class | impl.elementalmage.origin | Elemental Mage resonance and regen |
| HarmonicConvergenceSkill | Class | impl.elementalmage.skill | Elemental Mage mana/crit active skill |
| HighPriestOrigin | Class | impl.highpriest.origin | High Priest Grace and Command system |
| SeraphicEchoSkill | Class | impl.highpriest.skills.seraphsedge | High Priest beacon management skill |
| WarriorOrigin | Class | impl.warrior | Warrior origin and Style mechanics |
| ChallengersRetribution | Class | impl.warrior.skills | Warrior charge/reflect skill |

### Custom Mob Effects (10 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| ModEffects | Class (Static) | elemental.effects | Effect registry |
| ElementalEffectHandler | Class | elemental.effects | Effect application logic |
| FrostbiteEffect | Class | elemental.effects | Armor reduction (from Melt) |
| ConductiveEffect | Class | elemental.effects | Crit setup (from Electro-Charged) |
| BrittleEffect | Class | elemental.effects | Shatter bonus (from Frozen) |
| PanicEffect | Class | elemental.effects | Movement disruption (from Burning) |
| VulnerableEffect | Class | elemental.effects | Damage amplification (from Hyperbloom) |
| StaggerEffect | Class | elemental.effects | Movement interrupt |
| FractureEffect | Class | elemental.effects | Variable damage modifier |
| WitheringEffect | Class | elemental.effects | Life siphon to caster |
| DecrepitudeEffect | Class | elemental.effects | Attack/heal reduction |
| SpatialInstabilityEffect | Class | elemental.effects | Teleport risk |

### Reaction Entities (4 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| SteamCloudEntity | Class | elemental.entity | AoE obscuration zone (Vaporize) |
| BloomCoreEntity | Class | elemental.entity | Bloom core (Bloom reaction) |
| HyperbloomProjectile | Class | elemental.entity | Tracking projectile (Lightning on Bloom) |
| SmolderingGloomEntity | Class | elemental.entity | DoT zone (Fire on Bloom) |

### Super-Reaction System (9 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| SuperReaction | Interface | elemental.superreaction | Super-reaction contract |
| SuperReactionHandler | Class (Static) | elemental.superreaction | Detection and execution |
| SuperReactionTier | Enum | elemental.superreaction | 4 tiers (3-6 elements) |
| FireSuperReaction | Class | elemental.superreaction.reactions | Massive AoE explosion |
| IceSuperReaction | Class | elemental.superreaction.reactions | Freeze + shatter |
| AquaSuperReaction | Class | elemental.superreaction.reactions | Healing/cleansing |
| LightningSuperReaction | Class | elemental.superreaction.reactions | Chain lightning |
| NatureSuperReaction | Class | elemental.superreaction.reactions | Bloom garden |
| EnderSuperReaction | Class | elemental.superreaction.reactions | Teleport/rift |

### Attributes System

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| MasteryAttributes | Class (Static) | elemental.attributes | Attribute registry |
| MasteryAttributeHandler | Class | elemental.attributes | Attribute scaling logic |

### Integration System

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| ModIntegrationHandler | Class (Static) | elemental.integration | Mod detection |
| IronSpellbooksIntegration | Class | elemental.integration | Spell damage event listener |
| SpellElementMapper | Class | elemental.integration | School → Element mapping |

---

## Network Components (5 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| PacketHandler | Class (Static) | network | Network channel registration |
| SyncTalentsPacket | Class | network | Server → Client: Full talent sync |
| TalentActivationPacket | Class | network | Client → Server: Activation request |
| SpawnParticlesPacket | Class | network | Server → Client: Particles |
| SpawnReactionTextPacket | Class | network | Server → Client: Floating text |
| SyncBranchSelectionPacket | Class | network | Server → Client: Branch sync |

---

## Client Components (5 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| ClientHandler | Class (Static) | client | Client initialization |
| KeyBindings | Class (Static) | client | Keybinding registration |
| TalentScreen | Class | client | Full talent management UI |
| TalentOverlay | Class | client | HUD overlay (resource bar, cooldowns) |
| DefaultResourceBarRenderer | Class | client | Default resource rendering |

---

## Command Components (5 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| TalentCommand | Class | command | Base /talent command |
| GrantTalentCommand | Class | command | /grantalent <player> <talent> [level] |
| RevokeTalentCommand | Class | command | /revoketalent <player> <talent> |
| ListTalentsCommand | Class | command | /listtalents [player] |
| SelectBranchCommand | Class | command | /selectbranch <talent> <rank> <path> |

---

## Configuration Components (3 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| TalentConfig | Class | config | COMMON config - Core settings |
| ElementalReactionConfig | Class | config | COMMON config - Reaction parameters |
| TalentClientConfig | Class | config | CLIENT config - Visual settings |

---

## API Components (3 total)

| Component | Type | Package | Description |
|-----------|------|---------|-------------|
| TalentAPI | Class (Static) | api | Public API for other mods |
| TalentEvent | Class | api.events | Base talent event |
| ElementalReactionEvent | Class | api.events | Reaction event for mod hooks |

---

## Component Summary by Category

| Category | Count | Description |
|----------|-------|-------------|
| Talent System | 13 | Core talent framework |
| Capability System | 4 | Player data persistence |
| Elemental Core | 9 | Element stacks and reactions |
| Elemental Talents | 12 | Mastery + Elemental Mage |
| Elemental Effects | 11 | Custom mob effects |
| Elemental Entities | 4 | Reaction entities |
| Origin Implementations | 10 | Core classes for 5 origins |
| Super-Reactions | 9 | Advanced reaction system |
| Attributes | 2 | Mastery attributes |
| Integration | 3 | Mod integrations |
| Network | 5 | Client-server communication |
| Client | 5 | UI and rendering |
| Commands | 5 | Server commands |
| Configuration | 3 | Config system |
| API | 3 | Public API |
| **Total** | **94** | |

---

## Reusable Components

### For Other Developers

**Extend These Classes**:
- `PassiveTalent` - Create always-active talents
- `ActiveTalent` - Create cooldown-based abilities
- `HybridTalent` - Create dual-effect talents
- `BranchingPassiveTalent/ActiveTalent/HybridTalent` - Add branching progression
- `ResourceBarRenderer` - Custom resource bar visuals

**Use These APIs**:
- `TalentAPI` - Talent management from other mods
- `ElementalStackManager` - Apply elements programmatically
- `TalentRegistry` - Register custom talents
- `TalentEvent` / `ElementalReactionEvent` - Hook into talent/reaction events

**Implement These Interfaces**:
- `BranchingTalentBase` - Add branching to custom talents
- `SuperReaction` - Create custom super-reactions
- `ResourceBarRenderer` - Custom resource visualization

---

**Generated**: 2026-01-23
**Total Components**: 94
**Documentation Type**: Complete component catalog
