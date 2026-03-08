package com.complextalents.impl.assassin.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Utility methods for Assassin mechanics.
 */
public class AssassinUtils {

    public static boolean isBackstab(LivingEntity attacker, LivingEntity target) {
        Vec3 attackerLook = attacker.getLookAngle();
        Vec3 targetLook = target.getLookAngle();
        double dot = attackerLook.dot(targetLook);
        return dot > 0.6; // Both looking in roughly same direction
    }

    public static boolean isEntityOnCooldown(LivingEntity entity, long gameTime) {
        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        if (data.contains("AssassinMarkCooldown")) {
            return gameTime < data.getLong("AssassinMarkCooldown");
        }
        return false;
    }

    public static void setEntityCooldown(LivingEntity entity, long startTime, long expirationTime) {
        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        data.putLong("AssassinMarkStartTime", startTime);
        data.putLong("AssassinMarkCooldown", expirationTime);
    }
}
