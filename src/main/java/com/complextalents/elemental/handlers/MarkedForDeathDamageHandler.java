package com.complextalents.elemental.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.elemental.effects.MarkedForDeathEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handler for Marked for Death damage accumulation.
 * Listens for damage events and accumulates damage dealt to entities with the Marked for Death effect.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class MarkedForDeathDamageHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();

        // Check if target has Marked for Death effect
        if (target.level().isClientSide) return;

        MobEffectInstance markedEffect = target.getEffect(ElementalEffects.MARKED_FOR_DEATH.get());
        if (markedEffect == null) return;

        // Record the damage taken
        float damage = event.getAmount();
        MarkedForDeathEffect.recordDamage(target, damage);
    }
}
