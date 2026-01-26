package com.complextalents.elemental.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.elemental.effects.FractureEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handler for Fracture effect hit tracking.
 * Listens for damage events and records hits on entities with the Fracture effect.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class FractureDamageHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();

        // Check if target has Fracture effect
        if (target.level().isClientSide) return;

        MobEffectInstance fractureEffect = target.getEffect(ElementalEffects.FRACTURE.get());
        if (fractureEffect == null) return;

        // Record the hit
        boolean shouldRemove = FractureEffect.recordHit(target);

        // If max hits reached, remove the effect
        if (shouldRemove) {
            target.removeEffect(ElementalEffects.FRACTURE.get());
        }
    }
}
