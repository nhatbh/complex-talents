# Talent Slot System

The talent system has been updated to include 5 specific slot types. Each player can equip **one talent per slot type**, allowing for a structured and balanced playstyle customization.

## The 5 Talent Slot Types

### 1. DEFINITION
**Purpose**: Defines the core playstyle
**Example Use Cases**:
- Choose a combat style (melee, ranged, magic)
- Select a primary element affinity
- Define a character archetype (warrior, mage, rogue)

### 2. HARMONY
**Purpose**: Enhances the playstyle mechanics
**Example Use Cases**:
- Increase elemental mastery for chosen element
- Boost damage of specific weapon types
- Improve efficiency of core mechanic

**Current Examples**:
- All Mastery talents (FireMastery, AquaMastery, etc.) use HARMONY slot

### 3. CRESCENDO
**Purpose**: Activates powerful effects from core mechanics
**Example Use Cases**:
- Trigger bonus damage after successful combos
- Activate special effects when conditions are met
- Amplify reaction damage at critical moments

### 4. RESONANCE
**Purpose**: Uses mechanics defensively
**Example Use Cases**:
- Reduce damage when using specific elements
- Create shields or barriers based on playstyle
- Heal or regenerate based on core mechanic usage

### 5. FINALE
**Purpose**: A powerful ability maximizing core mechanics
**Example Use Cases**:
- Ultimate ability with long cooldown
- Powerful burst damage combining all mechanics
- Game-changing effect that defines peak performance

## Creating a New Talent

### Example 1: Definition Talent (No Requirements)
Definition talents define the core playstyle and never require another talent.

```java
public class FireAffinityTalent extends PassiveTalent {
    public FireAffinityTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath("complextalents", "fire_affinity"),
            Component.translatable("talent.complextalents.fire_affinity.name"),
            Component.translatable("talent.complextalents.fire_affinity.description"),
            1,  // Max level
            TalentSlotType.DEFINITION  // This talent defines your playstyle
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Grant fire-based bonuses
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Remove fire-based bonuses
    }
}
```

### Example 2: Harmony Talent (Requires Fire Affinity)
Other slot types can require a specific Definition talent to be equipped first.

```java
public class InfernoMasteryTalent extends PassiveTalent {
    public static final ResourceLocation FIRE_AFFINITY =
        ResourceLocation.fromNamespaceAndPath("complextalents", "fire_affinity");

    public InfernoMasteryTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath("complextalents", "inferno_mastery"),
            Component.translatable("talent.complextalents.inferno_mastery.name"),
            Component.translatable("talent.complextalents.inferno_mastery.description"),
            5,  // Max level
            TalentSlotType.HARMONY,  // Enhances playstyle mechanics
            FIRE_AFFINITY  // REQUIRES Fire Affinity to be equipped in DEFINITION slot
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Grant fire mastery bonuses
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Remove fire mastery bonuses
    }
}
```

### Example 3: Active Talent for FINALE Slot (With Requirement)
```java
public class InfernoFinale extends ActiveTalent {
    public static final ResourceLocation FIRE_AFFINITY =
        ResourceLocation.fromNamespaceAndPath("complextalents", "fire_affinity");

    public InfernoFinale() {
        super(
            ResourceLocation.fromNamespaceAndPath("complextalents", "inferno_finale"),
            Component.translatable("talent.complextalents.inferno_finale.name"),
            Component.translatable("talent.complextalents.inferno_finale.description"),
            3,  // Max level
            TalentSlotType.FINALE,  // Powerful finale ability
            FIRE_AFFINITY,  // REQUIRES Fire Affinity
            12000,  // 10 minute cooldown (12000 ticks)
            ChatFormatting.GOLD
        );
    }

    @Override
    public void onActivate(ServerPlayer player, int level) {
        // Powerful ability that combines all fire mechanics
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Setup when talent is unlocked
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Cleanup when talent is removed
    }
}
```

## Using the Slot System

### Equipping Talents
```java
// First, unlock the talent
playerTalents.unlockTalent(talentId, 1);

// Then equip it to the appropriate slot
boolean success = playerTalents.equipTalentToSlot(talentId, TalentSlotType.DEFINITION);
```

### Checking if a Talent Can Be Equipped
```java
// Check if all requirements are met before attempting to equip
if (playerTalents.canEquipTalent(talentId, TalentSlotType.HARMONY)) {
    playerTalents.equipTalentToSlot(talentId, TalentSlotType.HARMONY);
} else {
    // Notify player why they can't equip it
}
```

### Checking Equipped Talents
```java
// Get talent in specific slot
ResourceLocation talentId = playerTalents.getTalentInSlot(TalentSlotType.HARMONY);

// Check if slot is filled
boolean filled = playerTalents.isSlotFilled(TalentSlotType.CRESCENDO);

// Get all equipped talents
Map<TalentSlotType, ResourceLocation> equipped = playerTalents.getEquippedTalents();
```

### Checking Dependencies
```java
// Get all talents that depend on a specific Definition talent
ResourceLocation fireAffinity = ResourceLocation.fromNamespaceAndPath("complextalents", "fire_affinity");
List<ResourceLocation> dependents = playerTalents.getDependentTalents(fireAffinity);

// This is useful before attempting to unequip a Definition talent
if (!dependents.isEmpty()) {
    // Inform player they need to unequip dependent talents first
}
```

### Unequipping Talents
```java
// Unequip from specific slot
playerTalents.unequipTalentFromSlot(TalentSlotType.RESONANCE);

// Note: Talents are auto-unequipped when removed via removeTalent()
// Note: Cannot unequip a Definition talent if other talents depend on it
```

## Validation

The system automatically validates:
1. **Talent must be unlocked** before it can be equipped
2. **Talent slot type must match** the target slot
3. **Only one talent per slot** - equipping a new talent replaces the old one
4. **Auto-unequip on removal** - removing a talent auto-unequips it from its slot
5. **Required Definition must be equipped** - talents with definition requirements can only be equipped if the required Definition talent is currently equipped
6. **Cannot unequip required Definitions** - Definition talents cannot be unequipped if any equipped talent depends on them

## Migration Notes

### Legacy Support
All existing talent code continues to work through deprecated constructors that default to the DEFINITION slot. However, you should update talents to specify their intended slot type.

### Breaking Changes
- `Talent` class now requires `TalentSlotType` parameter (legacy constructors provided)
- `PassiveTalent`, `ActiveTalent`, `HybridTalent` now require `TalentSlotType` parameter
- NBT structure updated to include "EquippedTalents" compound tag

## Talent Dependencies

### How Requirements Work

1. **Only Definition talents can be required** - The system enforces that talents can only require Definition talents, not other slot types
2. **Requirements are optional** - Talents can be created without any requirements (pass `null` or omit the parameter)
3. **Automatic validation** - The system automatically checks requirements when equipping talents
4. **Protection for Definitions** - Definition talents with active dependents cannot be unequipped

### Creating Talent Trees

You can create coherent talent trees by having multiple talents require the same Definition:

**Fire Affinity Tree:**
- Definition: Fire Affinity (no requirement)
- Harmony: Inferno Mastery (requires Fire Affinity)
- Crescendo: Pyroclasm (requires Fire Affinity)
- Resonance: Fire Shield (requires Fire Affinity)
- Finale: Inferno Finale (requires Fire Affinity)

**Ice Affinity Tree:**
- Definition: Ice Affinity (no requirement)
- Harmony: Frost Mastery (requires Ice Affinity)
- Crescendo: Glacial Spike (requires Ice Affinity)
- Resonance: Ice Barrier (requires Ice Affinity)
- Finale: Frozen Domain (requires Ice Affinity)

This creates distinct playstyle paths where choosing a Definition talent determines which other talents become available.

## Design Guidelines

When creating talents, consider:

1. **DEFINITION** talents should be mutually exclusive playstyle choices that serve as the foundation for talent trees
2. **HARMONY** talents should synergize with the chosen definition and typically require a specific Definition
3. **CRESCENDO** talents should reward skillful play and timing, often tied to a specific playstyle
4. **RESONANCE** talents should provide survival and defensive options aligned with the playstyle
5. **FINALE** talents should be powerful, high-cooldown abilities that feel impactful and require their associated Definition

This structure encourages players to build coherent, synergistic talent loadouts rather than simply picking the "best" talents. The requirement system ensures that talent choices lead to meaningful, thematic builds.

## Resource Bar System

Definition talents can define a **Resource Bar** that provides a unified resource mechanic for the entire talent build. All talents in the build can access and manipulate this resource.

### Resource Bar Types

The system includes several pre-configured resource types:

- **MANA**: Regenerates over time, classic magic resource
- **RAGE**: Builds through combat, decays when out of combat
- **ENERGY**: Fast regeneration, used for quick abilities
- **HEAT**: Builds when using abilities, decays over time
- **CHI**: Balanced resource with moderate regeneration
- **CORRUPTION**: Builds when using dark powers, dangerous at high levels
- **COMBO**: Builds through attacks, spent on finishers, no natural regen
- **CHARGES**: Builds over time, can store multiple charges
- **CUSTOM**: Fully customizable resource type

### Creating a Definition Talent with a Resource Bar

```java
public class FireAffinityTalent extends PassiveTalent {
    public FireAffinityTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath("complextalents", "fire_affinity"),
            Component.translatable("talent.complextalents.fire_affinity.name"),
            Component.translatable("talent.complextalents.fire_affinity.description"),
            1,  // Max level
            TalentSlotType.DEFINITION,
            ResourceBarConfig.builder(
                ResourceBarType.HEAT,
                Component.translatable("resource.complextalents.heat")
            )
                .maxValue(100.0f)
                .startingValue(0.0f)
                .regenRate(-1.0f)  // Decays at 1 unit per second
                .color(0xFF8800)   // Orange color
                .build()
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Grant fire-based bonuses
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Remove fire-based bonuses
    }
}
```

### Using Resources in Other Talents

All talents can access the resource bar defined by the equipped Definition talent:

```java
public class FireballTalent extends ActiveTalent {
    private static final float FIREBALL_COST = 25.0f;

    public FireballTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath("complextalents", "fireball"),
            Component.translatable("talent.complextalents.fireball.name"),
            Component.translatable("talent.complextalents.fireball.description"),
            3,  // Max level
            TalentSlotType.FINALE,
            ResourceLocation.fromNamespaceAndPath("complextalents", "fire_affinity"),  // Requires Fire Affinity
            1200,  // 60 second cooldown
            ChatFormatting.GOLD
        );
    }

    @Override
    public void onActivate(ServerPlayer player, int level) {
        // Check if player has enough resource
        if (!hasResource(player, FIREBALL_COST)) {
            player.sendSystemMessage(Component.translatable("talent.complextalents.not_enough_heat"));
            return;
        }

        // Consume the resource
        if (consumeResource(player, FIREBALL_COST)) {
            // Cast fireball spell
            float damage = 10.0f * level;
            // ... fireball logic here
        }
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Setup when talent is unlocked
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Cleanup when talent is removed
    }
}
```

### Resource Gain Example

Talents can also generate resource:

```java
public class EmberStrikeTalent extends PassiveTalent {
    public EmberStrikeTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath("complextalents", "ember_strike"),
            Component.translatable("talent.complextalents.ember_strike.name"),
            Component.translatable("talent.complextalents.ember_strike.description"),
            3,  // Max level
            TalentSlotType.HARMONY,
            ResourceLocation.fromNamespaceAndPath("complextalents", "fire_affinity")  // Requires Fire Affinity
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // This talent generates heat when the player attacks
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Cleanup
    }

    // Called when player attacks (hooked elsewhere in your code)
    public void onPlayerAttack(ServerPlayer player, int level) {
        float heatGain = 5.0f * level;
        addResource(player, heatGain);
    }
}
```

### Resource Access Methods

All talents have access to these protected helper methods:

- `float getResource(ServerPlayer player)` - Get current resource value
- `void setResource(ServerPlayer player, float value)` - Set resource value (clamped)
- `float addResource(ServerPlayer player, float amount)` - Add to resource (returns actual amount added)
- `boolean hasResource(ServerPlayer player, float amount)` - Check if enough resource available
- `boolean consumeResource(ServerPlayer player, float amount)` - Consume resource (returns false if not enough)

### Resource Bar Configuration Options

```java
ResourceBarConfig.builder(ResourceBarType type, Component displayName)
    .maxValue(100.0f)           // Maximum resource value
    .startingValue(50.0f)       // Initial value when equipped
    .regenRate(2.0f)            // Regeneration per second (positive = regen, negative = decay)
    .color(0xFF0000)            // Display color (RGB hex)
    .showInUI(true)             // Whether to show in UI
    .build();
```

### Design Examples

**Mana-based Mage Build**:
- Definition: Arcane Affinity (MANA bar, regenerates slowly)
- Harmony: Arcane Mastery (increases mana regen)
- Crescendo: Arcane Surge (deal bonus damage at high mana)
- Resonance: Mana Shield (consume mana to absorb damage)
- Finale: Arcane Nova (huge spell consuming all mana)

**Rage-based Warrior Build**:
- Definition: Battle Fury (RAGE bar, builds in combat, decays outside)
- Harmony: Bloodlust (generate more rage from attacks)
- Crescendo: Enrage (bonus damage at high rage)
- Resonance: Berserker's Resilience (reduce damage at high rage)
- Finale: Rampage (massive AoE attack consuming all rage)

**Combo-based Rogue Build**:
- Definition: Shadow Arts (COMBO points, no regen)
- Harmony: Quick Strikes (generate combo points faster)
- Crescendo: Finisher (spend combo points for big damage)
- Resonance: Evasion (spend combo points to dodge)
- Finale: Assassinate (ultimate finisher requiring 5 combo points)

## Custom Resource Bar Rendering

Definition talents can provide their own custom HUD rendering function for their resource bars, allowing for unique visual representations.

### Creating a Custom Renderer

Implement the `ResourceBarRenderer` interface:

```java
@OnlyIn(Dist.CLIENT)
public class HeatBarRenderer implements ResourceBarRenderer {
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       float currentValue, float maxValue, ResourceBarConfig config, Component displayName) {
        // Custom rendering logic here
        // Example: Pulsating red bar that gets brighter with more heat
        
        float percentage = maxValue > 0 ? currentValue / maxValue : 0.0f;
        int fillWidth = (int) (width * percentage);

        // Background
        graphics.fill(x, y, x + width, y + height, 0xFF000000);

        // Filled portion - color intensity based on heat level
        if (fillWidth > 0) {
            int red = (int) (255 * percentage);
            int color = 0xFF000000 | (red << 16) | (red / 4 << 8);
            graphics.fill(x + 1, y + 1, x + fillWidth - 1, y + height - 1, color);

            // Pulsating effect (would need animation timer in real implementation)
            int pulseColor = 0x44FFFFFF;
            graphics.fill(x + 1, y + 1, x + fillWidth - 1, y + 2, pulseColor);
        }

        // Border
        graphics.fill(x, y, x + width, y + 1, 0xFF883300);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF883300);
        graphics.fill(x, y, x + 1, y + height, 0xFF883300);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF883300);
    }

    @Override
    public int getPreferredHeight() {
        return 12;
    }

    @Override
    public int getPreferredWidth() {
        return 120;
    }

    @Override
    public boolean shouldShowNumericValue() {
        return false; // Don't show numbers, just visual heat
    }

    @Override
    public boolean shouldShowResourceName() {
        return false;
    }
}
```

### Using a Custom Renderer in a Definition Talent

```java
public class FireAffinityTalent extends PassiveTalent {
    public FireAffinityTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath("complextalents", "fire_affinity"),
            Component.translatable("talent.complextalents.fire_affinity.name"),
            Component.translatable("talent.complextalents.fire_affinity.description"),
            1,
            TalentSlotType.DEFINITION,
            ResourceBarConfig.builder(
                ResourceBarType.HEAT,
                Component.translatable("resource.complextalents.heat")
            )
                .maxValue(100.0f)
                .startingValue(0.0f)
                .regenRate(-1.0f)
                .color(0xFF8800)
                .renderer(() -> new HeatBarRenderer())  // Custom renderer!
                .build()
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Grant fire-based bonuses
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Remove fire-based bonuses
    }
}
```

### Custom Renderer Examples

**Circular Resource Display:**
```java
@OnlyIn(Dist.CLIENT)
public class CircularManaRenderer implements ResourceBarRenderer {
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       float currentValue, float maxValue, ResourceBarConfig config, Component displayName) {
        float percentage = maxValue > 0 ? currentValue / maxValue : 0.0f;
        
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int radius = Math.min(width, height) / 2 - 2;

        // Draw circular arc for filled portion
        // (Simplified - actual implementation would need proper arc rendering)
        float angle = percentage * 360.0f;
        
        // Draw background circle
        // Draw filled arc based on percentage
        // Draw center text with current/max values
    }

    @Override
    public int getPreferredHeight() {
        return 40;
    }

    @Override
    public int getPreferredWidth() {
        return 40;
    }
}
```

**Segmented Resource Display (like combo points):**
```java
@OnlyIn(Dist.CLIENT)
public class ComboPointsRenderer implements ResourceBarRenderer {
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       float currentValue, float maxValue, ResourceBarConfig config, Component displayName) {
        int points = (int) maxValue;
        int filled = (int) currentValue;
        int pointWidth = (width - (points - 1) * 2) / points; // 2px spacing between points
        
        for (int i = 0; i < points; i++) {
            int pointX = x + i * (pointWidth + 2);
            
            if (i < filled) {
                // Filled point (bright gold)
                graphics.fill(pointX, y, pointX + pointWidth, y + height, 0xFFFFAA00);
            } else {
                // Empty point (dark gray)
                graphics.fill(pointX, y, pointX + pointWidth, y + height, 0xFF333333);
            }
            
            // Border
            graphics.fill(pointX, y, pointX + pointWidth, y + 1, 0xFF888888);
            graphics.fill(pointX, y + height - 1, pointX + pointWidth, y + height, 0xFF888888);
            graphics.fill(pointX, y, pointX + 1, y + height, 0xFF888888);
            graphics.fill(pointX + pointWidth - 1, y, pointX + pointWidth, y + height, 0xFF888888);
        }
    }

    @Override
    public int getPreferredHeight() {
        return 8;
    }

    @Override
    public int getPreferredWidth() {
        return 60; // 5 points * 10px + 4 * 2px spacing
    }

    @Override
    public boolean shouldShowNumericValue() {
        return false; // Visual representation is clear enough
    }
}
```

### Renderer API Methods

The `ResourceBarRenderer` interface provides these methods:

- `render(...)` - Main rendering method (required)
- `getPreferredHeight()` - Preferred height in pixels (default: 10)
- `getPreferredWidth()` - Preferred width in pixels (default: 100)
- `shouldShowNumericValue()` - Whether to show numeric values (default: true)
- `shouldShowResourceName()` - Whether to show resource name (default: true)

### Default Renderer

If no custom renderer is provided, the system uses `DefaultResourceBarRenderer` which displays:
- Horizontal bar with border
- Color from config
- Shine effect on filled portion
- Optional text showing resource name and current/max values

This gives each Definition talent complete control over how its resource is displayed, allowing for unique visual identities!
