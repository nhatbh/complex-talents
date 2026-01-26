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

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        TalentsMod.LOGGER.info("Registered custom mob effects for Elemental System");
    }
}