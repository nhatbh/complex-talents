package com.complextalents.impl.highpriest.effect;

import com.complextalents.TalentsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for High Priest effects.
 */
public class HighPriestEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TalentsMod.MODID);

    public static final RegistryObject<MobEffect> SERAPHIC_GRACE = EFFECTS.register("seraphic_grace",
        () -> new SeraphicGraceEffect());

    public static final RegistryObject<MobEffect> COVENANT_PROTECTION = EFFECTS.register("covenant_protection",
        () -> new CovenantProtectionEffect());

    public static final RegistryObject<MobEffect> DIVINE_EXALTATION = EFFECTS.register("divine_exaltation",
        () -> new DivineExaltationEffect());

    public static final RegistryObject<MobEffect> DIVINE_ASCENDANCE = EFFECTS.register("divine_ascendance",
        () -> new DivineAscendanceEffect());

    public static final RegistryObject<MobEffect> DIVINE_ASCENDANCE_FLIGHT = EFFECTS.register("divine_ascendance_flight",
        () -> new DivineAscendanceFlightEffect());

    public static void register(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        TalentsMod.LOGGER.info("Registered High Priest mob effects");
    }
}
