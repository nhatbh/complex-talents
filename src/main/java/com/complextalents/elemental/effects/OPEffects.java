package com.complextalents.elemental.effects;

import com.complextalents.TalentsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for Overwhelming Power status effects.
 */
public class OPEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TalentsMod.MODID);

    public static final RegistryObject<MobEffect> MELT = MOB_EFFECTS.register("melt", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0xFF4500)); // Orange-Red
            
    public static final RegistryObject<MobEffect> DRENCHED = MOB_EFFECTS.register("drenched", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0x1E90FF)); // Dodger-Blue
            
    public static final RegistryObject<MobEffect> PARASITIC_SEED = MOB_EFFECTS.register("parasitic_seed", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0x228B22)); // Forest-Green
            
    public static final RegistryObject<MobEffect> BRITTLE = MOB_EFFECTS.register("brittle", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0x87CEEB)); // Sky-Blue
            
    public static final RegistryObject<MobEffect> ABSOLUTE_ZERO = MOB_EFFECTS.register("absolute_zero", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0x00FFFF)); // Cyan
            
    public static final RegistryObject<MobEffect> ENERGIZE = MOB_EFFECTS.register("energize", 
            () -> new OPMobEffect(MobEffectCategory.BENEFICIAL, 0xFFFF00)); // Yellow

    public static final RegistryObject<MobEffect> MINIATURE_SUN = MOB_EFFECTS.register("miniature_sun", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0xFFD700)); // Gold

    public static final RegistryObject<MobEffect> TSUNAMI = MOB_EFFECTS.register("tsunami", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0x00008B)); // Dark-Blue

    public static final RegistryObject<MobEffect> OVERGROWTH = MOB_EFFECTS.register("overgrowth", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0x006400)); // Dark-Green

    public static final RegistryObject<MobEffect> THUNDERGODS_WRATH = MOB_EFFECTS.register("thundergods_wrath", 
            () -> new OPMobEffect(MobEffectCategory.HARMFUL, 0x9370DB)); // Medium-Purple

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    private static class OPMobEffect extends MobEffect {
        protected OPMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
