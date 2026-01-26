package com.complextalents.util;

import net.minecraft.world.entity.LivingEntity;

public class TeamHelper {
    public static boolean isAlly(LivingEntity entity1, LivingEntity entity2) {
        if (entity1 == null || entity2 == null) {
            return false;
        }
        // Use Minecraft's built-in team checking
        return entity1.isAlliedTo(entity2);
    }
}
