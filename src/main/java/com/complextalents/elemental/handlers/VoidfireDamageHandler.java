package com.complextalents.elemental.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.effects.ElementalEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for the Voidfire "Marked for Death" vulnerability effect
 * Increases damage taken by 30% when the target has the effect
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class VoidfireDamageHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();

        // Check if target has "Marked for Death" effect
        if (target.hasEffect(ElementalEffects.MARKED_FOR_DEATH.get())) {
            float originalDamage = event.getAmount();
            float amplifiedDamage = originalDamage * 1.3f; // +30% damage

            event.setAmount(amplifiedDamage);

            // Debug logging
            TalentsMod.LOGGER.debug("Marked for Death: {} takes amplified damage {} (was {})",
                target.getName().getString(), amplifiedDamage, originalDamage);
        }
    }
}