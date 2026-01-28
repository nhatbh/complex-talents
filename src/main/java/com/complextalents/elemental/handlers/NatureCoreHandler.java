package com.complextalents.elemental.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.entity.NatureCoreEntity;
import com.complextalents.elemental.events.ElementStackPreAppliedEvent;
import com.complextalents.elemental.registry.ReactionRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles element application to Nature Core entities.
 * When Fire or Lightning is applied to a Nature Core, it explodes.
 *
 * Uses ElementStackPreAppliedEvent to cancel the application before it occurs,
 * preventing Nature Cores from being targets for elemental reactions.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class NatureCoreHandler {

    /**
     * Listens for element stack application to Nature Core entities.
     * Triggers explosion if Fire or Lightning is applied.
     * Cancels the event to prevent the element stack from being applied.
     */
    @SubscribeEvent
    public static void beforeElementApplied(ElementStackPreAppliedEvent event) {
        if (!ElementalReactionConfig.enableElementalSystem.get()) return;

        LivingEntity target = event.getTarget();
        LivingEntity source = event.getSource();
        ElementType element = event.getElement();

        // Server-side only
        if (target.level().isClientSide) return;

        // Check if target is a Nature Core
        if (!(target instanceof NatureCoreEntity natureCore)) {
            return;
        }

        // Check if element is Fire or Lightning
        if (element != ElementType.FIRE && element != ElementType.LIGHTNING) {
            return;
        }

        // Get elemental mastery from source if it's a player
        float mastery = 1.0f;
        if (source instanceof ServerPlayer player) {
            mastery = ReactionRegistry.getInstance().calculateElementalMastery(player);
        }

        natureCore.triggerExplosion(mastery);
        event.setCanceled(true);
    }
}
