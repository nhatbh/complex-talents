package com.complextalents.elemental.effects;

import com.complextalents.TalentsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom status effects used by the Elemental Reaction System
 * Note: Effects are now registered directly in strategy implementations when needed
 */
public class ElementalEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TalentsMod.MODID);

    public static final RegistryObject<MobEffect> MARKED_FOR_DEATH = EFFECTS.register("marked_for_death", () -> new MarkedForDeathEffect(MobEffectCategory.HARMFUL, 0x4B0082));
    public static final RegistryObject<MobEffect> BURNING = EFFECTS.register("burning", () -> new BurningEffect(MobEffectCategory.HARMFUL, 0xFF4500));

    // Ice reaction effects
    public static final RegistryObject<MobEffect> FREEZE = EFFECTS.register("freeze", () -> new FreezeEffect(MobEffectCategory.HARMFUL, 0xADD8E6));
    public static final RegistryObject<MobEffect> SUPERCONDUCT = EFFECTS.register("superconduct", () -> new SuperconductEffect(MobEffectCategory.HARMFUL, 0x4169E1));
    public static final RegistryObject<MobEffect> PERMAFROST = EFFECTS.register("permafrost", () -> new PermafrostEffect(MobEffectCategory.HARMFUL, 0x98FB98));
    public static final RegistryObject<MobEffect> FRACTURE = EFFECTS.register("fracture", () -> new FractureEffect(MobEffectCategory.HARMFUL, 0x8A2BE2));

    // Aqua reaction effects
    public static final RegistryObject<MobEffect> ELECTRO_CHARGED = EFFECTS.register("electro_charged", () -> new ElectroChargedEffect(MobEffectCategory.HARMFUL, 0x00CED1));

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        TalentsMod.LOGGER.info("Registered custom mob effects for Elemental System");
    }
}