package com.complextalents.impl.assassin.effect;

import com.complextalents.TalentsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Registry for Assassin effects.
 */
public class AssassinEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TalentsMod.MODID);

    public static final RegistryObject<MobEffect> EXPOSE_WEAKNESS = EFFECTS.register("expose_weakness",
        () -> new ExposeWeaknessEffect());

    public static final RegistryObject<MobEffect> SHADOW_WALK = EFFECTS.register("shadow_walk",
        () -> new ShadowWalkEffect());

    public static final RegistryObject<MobEffect> AMBUSH = EFFECTS.register("ambush",
        () -> new AmbushEffect());

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        TalentsMod.LOGGER.info("Registered Assassin mob effects");
    }
}
