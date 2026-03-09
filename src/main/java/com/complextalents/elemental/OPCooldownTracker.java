package com.complextalents.elemental;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Tracks per-player, per-element cooldowns for Overwhelming Power using NBT.
 * Cooldown: 60s (1200 ticks). 
 */
public class OPCooldownTracker {
    public static final String NBT_KEY_ROOT = "OverwhelmingPowerCD";
    public static final int COOLDOWN_TICKS = 100; // 5s for testing

    public static boolean canTrigger(Player player, OPElementType element) {
        CompoundTag nbt = player.getPersistentData().getCompound(NBT_KEY_ROOT);
        return !nbt.contains(element.name()) || nbt.getInt(element.name()) <= 0;
    }

    public static void startCooldown(Player player, OPElementType element) {
        CompoundTag nbt = player.getPersistentData().getCompound(NBT_KEY_ROOT);
        nbt.putInt(element.name(), COOLDOWN_TICKS);
        player.getPersistentData().put(NBT_KEY_ROOT, nbt);
    }

    public static void tickCooldowns(Player player) {
        if (!player.getPersistentData().contains(NBT_KEY_ROOT)) return;
        
        CompoundTag nbt = player.getPersistentData().getCompound(NBT_KEY_ROOT);
        boolean changed = false;
        
        for (String key : nbt.getAllKeys()) {
            int current = nbt.getInt(key);
            if (current > 0) {
                nbt.putInt(key, Math.max(0, current - 10)); // Decelerate by 10 since we tick every 10
                changed = true;
            }
        }
        
        if (changed) {
            player.getPersistentData().put(NBT_KEY_ROOT, nbt);
        }
    }
}
