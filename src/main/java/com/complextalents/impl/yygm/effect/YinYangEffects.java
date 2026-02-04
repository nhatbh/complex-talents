package com.complextalents.impl.yygm.effect;

import com.complextalents.TalentsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for Yin Yang Grandmaster effects.
 */
public class YinYangEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TalentsMod.MODID);

    public static final RegistryObject<MobEffect> HARMONIZED = EFFECTS.register("harmonized",
        () -> new HarmonizedEffect());

    public static final RegistryObject<MobEffect> EXPOSED = EFFECTS.register("exposed",
        () -> new ExposedEffect());

    public static final RegistryObject<MobEffect> YIN_YANG_ANNIHILATION = EFFECTS.register("yin_yang_annihilation",
        () -> new YinYangAnnihilationEffect());

    public static void register(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        TalentsMod.LOGGER.info("Registered Yin Yang Grandmaster mob effects");
    }
}
