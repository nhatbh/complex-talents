package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.effects.OPEffects;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.effect.MobEffects;
import java.util.List;

public class LightningOPStrategy implements IOPStrategy {
    @Override
    public void execute(OPContext context, int tier) {
        float damage = context.getRawDamage();
        LivingEntity target = context.getTarget();
        ServerLevel level = context.getLevel();
        LivingEntity attacker = context.getAttacker();

        switch (tier) {
            case 5:
                applyT5(attacker, target, level, damage);
            case 4:
                applyT4(attacker, target, level, damage);
            case 3:
                applyT3(attacker, target, level, damage);
            case 2:
                applyT2(attacker, target, level, damage);
            case 1:
                applyT1(attacker, target, level, damage);
                break;
        }
    }

    private void applyT1(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
        float arcDamage = 15f * scale;
        
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0));
        int count = 0;
        for (LivingEntity entity : nearby) {
            if (entity != target && !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                entity.hurt(level.damageSources().lightningBolt(), arcDamage);
                
                // Visuals: Iron's lightning particles
                net.minecraft.core.particles.ParticleOptions lightning = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("lightning");
                if (lightning != null) {
                    level.sendParticles(lightning, entity.getX(), entity.getY() + 1, entity.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                }
                
                count++;
                if (count >= 2) break;
            }
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        // Chain Extension: Handled by increasing targets or range
        float arcDamage = 20f * scale;
        
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(6.0));
        int count = 0;
        for (LivingEntity entity : nearby) {
            if (entity != target && !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                entity.hurt(level.damageSources().lightningBolt(), arcDamage);
                
                net.minecraft.core.particles.ParticleOptions electricity = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("electricity");
                if (electricity != null) {
                    level.sendParticles(electricity, entity.getX(), entity.getY() + 1, entity.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                }
                
                count++;
                if (count >= 4) break; // T2 extends to 4 targets total
            }
        }
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        int duration = (int) (160 * Math.sqrt(scale));
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1)); // Speed II
        
        net.minecraft.core.particles.ParticleOptions electricity = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("electricity");
        if (electricity != null) {
            level.sendParticles(electricity, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 15, 0.4, 0.6, 0.4, 0.05);
        }
    }

    private void applyT4(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
        int duration = (int) (100 * Math.sqrt(scale));
        // Magnetize logic moved to OPTickHandler or handled here for pull
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(8.0));
        for (LivingEntity entity : nearby) {
            if (entity != target && !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                Vec3 pull = target.position().subtract(entity.position()).normalize().scale(0.4);
                entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
                entity.hurtMarked = true;
                // Use duration for a brief slow to help the grouping
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration / 2, 1));
            }
        }
        
        net.minecraft.core.particles.ParticleOptions shockwave = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("shockwave");
        if (shockwave != null) {
            level.sendParticles(shockwave, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        }
    }

    private void applyT5(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
        target.addEffect(new MobEffectInstance(OPEffects.THUNDERGODS_WRATH.get(), 200, (int)(scale * 10))); // 10s
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }
        
        net.minecraft.core.particles.ParticleOptions lightning = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("lightning");
        if (lightning != null) {
            level.sendParticles(lightning, target.getX(), target.getY() + 3, target.getZ(), 20, 0.5, 2.0, 0.5, 0);
        }
    }

    @Override
    public java.util.List<String> getEffectBreakdown(int tier, float damage) {
        java.util.List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier >= 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("+ %.1f Chain Lightning Damage (T1 Arcing Bolt)", 15f * scale));
        }
        if (tier >= 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("Chain Extension: +2 Targets (T2 Chain Extension)"));
        }
        if (tier >= 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format("Energize: Speed II for %.1fs (T3 Energize)", 160f * Math.sqrt(scale) / 20f));
        }
        if (tier >= 4) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
            breakdown.add(String.format("Magnetize: Grouping enemies for %.1fs (T4 Magnetize)", 100f * Math.sqrt(scale) / 20f));
        }
        if (tier >= 5) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
            breakdown.add(String.format("Thundergod's Wrath: %.1f Pulse Damage (T5 Thundergod)", 450f * scale));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.LIGHTNING;
    }
}
