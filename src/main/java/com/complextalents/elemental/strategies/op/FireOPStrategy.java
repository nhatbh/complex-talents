package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.effects.OPEffects;
import com.complextalents.elemental.handlers.OPTickHandler;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class FireOPStrategy implements IOPStrategy {

    @Override
    public void execute(OPContext context, int tier) {
        float damage = context.getRawDamage();
        LivingEntity target = context.getTarget();
        ServerLevel level = context.getLevel();

        switch (tier) {
            case 5:
                applyT5(context.getAttacker(), target, level, damage);
            case 4:
                applyT4(context.getAttacker(), target, level, damage);
            case 3:
                applyT3(context.getAttacker(), target, damage);
            case 2:
                applyT2(context.getAttacker(), target, level, damage);
            case 1:
                applyT1(context.getAttacker(), target, damage);
                break;
        }
    }

    private void applyT1(LivingEntity attacker, LivingEntity target, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
        float bonusDamage = 5f * scale;
        target.hurt(target.level().damageSources().indirectMagic(attacker, target), bonusDamage); // True damage

        if (target.level() instanceof ServerLevel serverLevel) {
            net.minecraft.core.particles.ParticleOptions fire = OPTickHandler.getIronParticle("fire");
            if (fire != null) {
                serverLevel.sendParticles(fire, target.getX(), target.getY() + 1, target.getZ(), 15, 0.2, 0.2, 0.2,
                        0.1);
            }
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        float aoeDamage = 15f * scale;
        double radius = 3.0;

        // Visuals: Larger flame explosion
        net.minecraft.core.particles.ParticleOptions fire = OPTickHandler.getIronParticle("fire");
        net.minecraft.core.particles.ParticleOptions dragonFire = OPTickHandler.getIronParticle("dragon_fire");

        if (dragonFire != null)
            level.sendParticles(dragonFire, target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
        if (fire != null)
            level.sendParticles(fire, target.getX(), target.getY() + 1, target.getZ(), 30, 0.8, 0.8, 0.8, 0.1);

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(radius));
        for (LivingEntity entity : nearby) {
            if (entity != target && !com.complextalents.util.TeamHelper.isAlly(target, entity)) {
                entity.hurt(level.damageSources().indirectMagic(attacker, entity), aoeDamage); // True damage
                entity.setSecondsOnFire(3);
            }
        }
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        int duration = (int) (100 * Math.sqrt(scale));
        target.addEffect(new MobEffectInstance(OPEffects.MELT.get(), duration, 0));
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }

        if (target.level() instanceof ServerLevel level) {
            // Intense white-hot particles to show "Melt"
            net.minecraft.core.particles.ParticleOptions fire = OPTickHandler.getIronParticle("fire");
            if (fire != null) {
                level.sendParticles(fire, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    private void applyT4(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
        int durationTicks = (int) (160 * Math.sqrt(scale));
        target.setSecondsOnFire(durationTicks / 20);

        // Massive explosion visuals
        net.minecraft.core.particles.ParticleOptions dragonFire = OPTickHandler.getIronParticle("dragon_fire");
        if (dragonFire != null) {
            level.sendParticles(dragonFire, target.getX(), target.getY() + 1, target.getZ(), 60, 1.5, 1.5, 1.5, 0.2);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.5, target.getZ(), 5, 1.0, 1.0,
                1.0, 0.1);

        // Intense Scorched Zone (T4)
        OPTickHandler.spawnScorchedZone(level, target.position(), 4.0f, 100, 15f * scale, attacker);
    }

    private void applyT5(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
        // T5 is now the Flaming Geyser
        OPTickHandler.spawnFlamingGeyser(level, target.position(), scale, attacker);

        // Initial eruption force
        level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY(), target.getZ(), 40, 0.5, 0.5, 0.5, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
    }

    @Override
    public List<String> getEffectBreakdown(int tier, float damage) {
        List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier >= 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("+ %.1f Bonus True Damage (T1 Ignite)", 5f * scale));
        }
        if (tier >= 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("+ %.1f AoE Explosion Damage (T2 Flare)", 15f * scale));
        }
        if (tier >= 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format("Applied Melt: +10%% Vulnerability for %.1fs (T3 Melt)",
                    100f * Math.sqrt(scale) / 20f));
        }
        if (tier >= 4) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
            breakdown.add(String.format("+ %.1f DPS Intense Scorched Zone (T4 Scorch)", 15f * scale));
        }
        if (tier >= 5) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
            breakdown.add(String.format("+ %.1f DPS Flaming Geyser (T5 Geyser)", 60f * scale));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.FIRE;
    }
}
