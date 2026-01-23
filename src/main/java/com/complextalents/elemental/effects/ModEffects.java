package com.complextalents.elemental.effects;

import com.complextalents.TalentsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom status effects used by the Elemental Reaction System
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TalentsMod.MODID);

    // Amplifying Reaction Effects
    public static final RegistryObject<MobEffect> FROSTBITE = EFFECTS.register("frostbite",
        FrostbiteEffect::new);

    public static final RegistryObject<MobEffect> STAGGER = EFFECTS.register("stagger",
        StaggerEffect::new);

    // DoT Reaction Effects
    public static final RegistryObject<MobEffect> CONDUCTIVE = EFFECTS.register("conductive",
        ConductiveEffect::new);

    // Crowd Control Effects
    public static final RegistryObject<MobEffect> BRITTLE = EFFECTS.register("brittle",
        BrittleEffect::new);

    public static final RegistryObject<MobEffect> PANIC = EFFECTS.register("panic",
        PanicEffect::new);

    // Debuff Effects
    public static final RegistryObject<MobEffect> VULNERABLE = EFFECTS.register("vulnerable",
        VulnerableEffect::new);

    public static final RegistryObject<MobEffect> SPATIAL_INSTABILITY = EFFECTS.register("spatial_instability",
        SpatialInstabilityEffect::new);

    public static final RegistryObject<MobEffect> FRACTURE = EFFECTS.register("fracture",
        FractureEffect::new);

    public static final RegistryObject<MobEffect> DECREPITUDE = EFFECTS.register("decrepitude",
        DecrepitudeEffect::new);

    public static final RegistryObject<MobEffect> WITHERING = EFFECTS.register("withering",
        WitheringEffect::new);

    // Ice Super-Reaction Effects
    public static final RegistryObject<MobEffect> SHATTERING_PRISM = EFFECTS.register("shattering_prism",
        ShatteringPrismEffect::new);

    public static final RegistryObject<MobEffect> CRYO_SHATTER = EFFECTS.register("cryo_shatter",
        CryoShatterEffect::new);

    // Ender Super-Reaction Effects
    public static final RegistryObject<MobEffect> VOID_TOUCHED = EFFECTS.register("void_touched",
        VoidTouchedEffect::new);

    public static final RegistryObject<MobEffect> UNRAVELING = EFFECTS.register("unraveling",
        UnravelingEffect::new);

    // Fire Super-Reaction Effects
    public static final RegistryObject<MobEffect> CONFLAGRATION_BURN = EFFECTS.register("conflagration_burn",
        ConflagrationBurnEffect::new);

    public static final RegistryObject<MobEffect> SCORCHED_EARTH = EFFECTS.register("scorched_earth",
        ScorchedEarthEffect::new);

    public static final RegistryObject<MobEffect> IGNITION_FUSE = EFFECTS.register("ignition_fuse",
        IgnitionFuseEffect::new);

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        TalentsMod.LOGGER.info("Registered {} custom status effects for Elemental Reaction System", EFFECTS.getEntries().size());
    }
}
