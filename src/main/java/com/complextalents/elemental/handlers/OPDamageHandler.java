package com.complextalents.elemental.handlers;

import com.complextalents.elemental.effects.OPEffects;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class OPDamageHandler {

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target == null) return;

        // Melt (Fire T3)
        if (target.hasEffect(OPEffects.MELT.get())) {
            MobEffectInstance effect = target.getEffect(OPEffects.MELT.get());
            if (effect != null) {
                float scale = (effect.getAmplifier() + 1) / 10f;
                if (scale < 1.0f) scale = 1.0f;
                event.setAmount(event.getAmount() * (1.0f + 0.1f * scale));
            }
        }

        // Drenched (Aqua T3)
        if (target.hasEffect(OPEffects.DRENCHED.get())) {
            event.setAmount(event.getAmount() * 1.15f);
        }

        // Absolute Zero (Ice T5)
        if (target.hasEffect(OPEffects.ABSOLUTE_ZERO.get())) {
            event.setAmount(event.getAmount() * 2.0f);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Handle non-spell damage amplification if needed
        LivingEntity target = event.getEntity();
        if (target == null) return;
        
        if (target.hasEffect(OPEffects.ABSOLUTE_ZERO.get())) {
            event.setAmount(event.getAmount() * 2.0f);
        }
    }
}
