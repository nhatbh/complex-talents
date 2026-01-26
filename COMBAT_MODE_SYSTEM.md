# Combat Mode System

## Overview

The Combat Mode system provides a clean, efficient way to toggle between normal Minecraft controls and talent ability activation. Using a clever key interception approach, it avoids the pitfalls of dynamic key rebinding while maintaining full vanilla compatibility.

## Architecture

### Core Principle
Instead of rebinding keys dynamically (which causes bugs), we:
1. **Intercept** hotbar key events when Combat Mode is ON
2. **Cancel** vanilla hotbar switching
3. **Trigger** talent abilities instead

This approach is bulletproof - no UI desync, no control conflicts, no Forge bugs.

### Client-Side State Management
```java
public class CombatModeClient {
    private static boolean combatMode = false;

    public static boolean isCombatMode() { return combatMode; }
    public static boolean toggle() { /* ... */ }
}
```
- Local state for zero-latency response
- Syncs with server for validation
- Resets on disconnect for safety

### Key Interception Flow
```
Player presses "1" key
    ↓
ClientInputHandler.onKeyInput()
    ↓
Check: Is Combat Mode ON?
    ├─ YES: event.setCanceled(true) → AbilityHandler.triggerAbility(1)
    └─ NO: Normal hotbar switching
```

## Features

### Toggle Key (R by default)
- **Action**: Toggles Combat Mode on/off
- **Feedback**: Chat message + HUD indicator
- **Cooldown**: 250ms to prevent spam

### Combat Mode ON
- **Keys 1-4**: Activate talents
  - 1 → Harmony slot
  - 2 → Crescendo slot
  - 3 → Resonance slot
  - 4 → Finale slot
- **Mouse Scroll**: Disabled (prevents hotbar switching)
- **Visual**: Green "[Combat Mode ON]" in HUD

### Combat Mode OFF
- **Keys 1-4**: Normal hotbar switching
- **Mouse Scroll**: Normal behavior
- **Visual**: Gray "[Combat Mode OFF]" in HUD

## Implementation Details

### Files Structure

#### New Client Files
- `CombatModeClient.java` - Client-side state manager
- `ClientInputHandler.java` - Key/mouse event interception
- `AbilityHandler.java` - Talent activation logic

#### Updated Files
- `KeyBindings.java` - Simplified to only toggle & screen keys
- `TalentOverlay.java` - Added combat mode indicator
- `SyncTalentsPacket.java` - Syncs combat mode state

### Key Interception (`ClientInputHandler.java`)
```java
@SubscribeEvent
public static void onKeyInput(InputEvent.Key event) {
    if (!CombatModeClient.isCombatMode()) return;

    KeyMapping[] hotbarKeys = mc.options.keyHotbarSlots;
    for (int i = 0; i < 4; i++) {
        if (hotbarKeys[i].matches(event.getKey(), event.getScanCode())) {
            event.setCanceled(true);  // Cancel vanilla
            AbilityHandler.triggerAbility(i + 1);  // Activate talent
            return;
        }
    }
}
```

### Ability Handler (`AbilityHandler.java`)
```java
public static void triggerAbility(int slot) {
    TalentSlotType slotType = switch (slot) {
        case 1 -> TalentSlotType.HARMONY;
        case 2 -> TalentSlotType.CRESCENDO;
        case 3 -> TalentSlotType.RESONANCE;
        case 4 -> TalentSlotType.FINALE;
    };

    // Send activation packet to server
    PacketHandler.sendToServer(new TalentActivationPacket(talentId));
}
```

### HUD Integration
The talent overlay dynamically shows/hides keybinds:
- **Combat Mode ON**: Shows "1", "2", "3", "4" on talent slots
- **Combat Mode OFF**: No keybind indicators
- Status text: "[Combat Mode ON/OFF]" next to "Talents" title

## Why This Approach?

### Problems with Dynamic Rebinding
- Forge's key system can desync
- UI shows wrong keybinds
- Conflicts with other mods
- Complex state management

### Benefits of Key Interception
- ✅ Zero UI desync
- ✅ No breaking vanilla controls
- ✅ Works with any hotbar mod
- ✅ Clean, simple code
- ✅ Instant toggle response
- ✅ No Forge bugs

## Usage

1. **Toggle**: Press R (configurable)
2. **Combat Mode ON**:
   - Keys 1-4 activate talents
   - Hotbar stays unchanged
   - Mouse scroll disabled
3. **Combat Mode OFF**:
   - Normal Minecraft controls
   - Keys 1-4 switch hotbar

## Server Synchronization

### Toggle Request
```
Client: Press R → CombatModeClient.toggle() → Send ToggleCombatModePacket
Server: Receive packet → Update capability → Broadcast to clients
```

### State Persistence
- Stored in `PlayerTalentsImpl.combatModeEnabled`
- Saved to NBT for persistence
- Synced via `SyncTalentsPacket`

## Performance

- **Zero overhead** when idle
- **Minimal processing** on key events
- **No per-tick checks**
- **Cached talent lookups**

## Configuration

- Toggle key rebindable in Controls menu
- State persists per-player on server
- Automatic reset on disconnect

## Testing Checklist

- [x] Toggle with R key shows feedback
- [x] Keys 1-4 activate talents when ON
- [x] Keys 1-4 switch hotbar when OFF
- [x] Mouse scroll blocked when ON
- [x] HUD shows current mode
- [x] State syncs on login
- [x] State persists across sessions

## Future Enhancements

Potential improvements:
- Custom keybind sets per talent build
- Visual effects on mode toggle
- Combo indicators for talent chains
- Scroll wheel for talent page cycling