package com.complextalents.elemental.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.elemental.effects.UnstableBioEnergyEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handler for Overgrowth death explosion.
 * Listens for death events and triggers the bio-energy explosion
 * when an entity with Unstable Bio-energy effect dies.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class OvergrowthDeathHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Check if target has Unstable Bio-energy effect
        if (entity.level().isClientSide) return;

        if (entity.hasEffect(ElementalEffects.UNSTABLE_BIO_ENERGY.get())) {
            TalentsMod.LOGGER.info("Entity {} died with Unstable Bio-energy, triggering explosion",
                entity.getName().getString());

            // Trigger the explosion
            UnstableBioEnergyEffect.triggerExplosion(entity);
        }
    }
}
