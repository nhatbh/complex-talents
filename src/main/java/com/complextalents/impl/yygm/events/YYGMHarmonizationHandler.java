package com.complextalents.impl.yygm.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.effect.ExposedEffect;
import com.complextalents.impl.yygm.effect.HarmonizedEffect;
import com.complextalents.impl.yygm.origin.YinYangGrandmasterOrigin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles smart AoE harmonization target selection via tick delegation.
 * Processes cached damage to find and harmonize the closest enemy.
 * Only harmonizes if the player has no current harmonized target.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class YYGMHarmonizationHandler {

    /**
     * Process smart AoE harmonization every tick.
     * Finds the closest target from cached damage and applies harmonization
     * only if the player has no current harmonized target.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (!YinYangGrandmasterOrigin.isYinYangGrandmaster(player)) {
                    continue;
                }

                if (!HarmonizedEffect.hasCachedDamage(player.getUUID())) {
                    continue;
                }

                // Check if player has an active Exposed target - cannot apply Harmonized during Exposed
                if (ExposedEffect.hasExposedTarget(player.getUUID())) {
                    continue;
                }

                // Get best target from cache
                Integer bestTargetId = HarmonizedEffect.processAndSelectBestTarget(player.getUUID());
                if (bestTargetId == null) {
                    continue;
                }

                LivingEntity target = findEntityById(level, bestTargetId);
                if (target == null || !target.isAlive()) {
                    continue;
                }

                // Check if player already has a harmonized target
                Integer currentId = HarmonizedEffect.getHarmonizedEntityId(player.getUUID());

                // Don't switch targets - only apply if no current target
                if (currentId == null) {
                    boolean isNew = HarmonizedEffect.applyToTarget(target, player.getUUID());
                    if (isNew) {
                        HarmonizedEffect.syncGateState(target, player.getUUID());
                        TalentsMod.LOGGER.debug("YYGM {} harmonized target {} (from cached damage)",
                            player.getName().getString(), target.getName().getString());
                    }
                }
            }
        }
    }

    private static LivingEntity findEntityById(ServerLevel level, int entityId) {
        net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
