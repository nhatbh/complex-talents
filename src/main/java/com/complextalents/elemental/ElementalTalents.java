package com.complextalents.elemental;

import com.complextalents.TalentsMod;
import com.complextalents.config.TalentConfig;
import com.complextalents.elemental.talents.*;
import com.complextalents.talent.TalentRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for elemental talents - updated for 6-element system
 * Talents now grant attribute points instead of percentage bonuses
 */
public class ElementalTalents {
    // General mastery talent
    public static final ResourceLocation ELEMENTAL_MASTERY =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_mastery");

    // Element-specific mastery talents (replaces old specialist talents)
    public static final ResourceLocation FIRE_MASTERY =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "fire_mastery");
    public static final ResourceLocation AQUA_MASTERY =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "aqua_mastery");
    public static final ResourceLocation ICE_MASTERY =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "ice_mastery");
    public static final ResourceLocation LIGHTNING_MASTERY =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "lightning_mastery");
    public static final ResourceLocation NATURE_MASTERY =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "nature_mastery");
    public static final ResourceLocation ENDER_MASTERY =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "ender_mastery");

    // Talent instances for registration
    private static final ElementalMasteryTalent ELEMENTAL_MASTERY_INSTANCE = new ElementalMasteryTalent();
    private static final FireMasteryTalent FIRE_MASTERY_INSTANCE = new FireMasteryTalent();
    private static final AquaMasteryTalent AQUA_MASTERY_INSTANCE = new AquaMasteryTalent();
    private static final IceMasteryTalent ICE_MASTERY_INSTANCE = new IceMasteryTalent();
    private static final LightningMasteryTalent LIGHTNING_MASTERY_INSTANCE = new LightningMasteryTalent();
    private static final NatureMasteryTalent NATURE_MASTERY_INSTANCE = new NatureMasteryTalent();
    private static final EnderMasteryTalent ENDER_MASTERY_INSTANCE = new EnderMasteryTalent();

    public static void register(IEventBus modEventBus) {
        // Register all elemental talents with the TalentRegistry
        // Note: Config is checked when talents are granted/used, not during registration
        TalentRegistry.register("elemental_mastery", () -> ELEMENTAL_MASTERY_INSTANCE);
        TalentRegistry.register("fire_mastery", () -> FIRE_MASTERY_INSTANCE);
        TalentRegistry.register("aqua_mastery", () -> AQUA_MASTERY_INSTANCE);
        TalentRegistry.register("ice_mastery", () -> ICE_MASTERY_INSTANCE);
        TalentRegistry.register("lightning_mastery", () -> LIGHTNING_MASTERY_INSTANCE);
        TalentRegistry.register("nature_mastery", () -> NATURE_MASTERY_INSTANCE);
        TalentRegistry.register("ender_mastery", () -> ENDER_MASTERY_INSTANCE);

        TalentsMod.LOGGER.info("Elemental mastery talents registered (6-element system)");
    }

    public static ElementalMasteryTalent getElementalMastery() {
        return ELEMENTAL_MASTERY_INSTANCE;
    }

    public static FireMasteryTalent getFireMastery() {
        return FIRE_MASTERY_INSTANCE;
    }

    public static AquaMasteryTalent getAquaMastery() {
        return AQUA_MASTERY_INSTANCE;
    }

    public static IceMasteryTalent getIceMastery() {
        return ICE_MASTERY_INSTANCE;
    }

    public static LightningMasteryTalent getLightningMastery() {
        return LIGHTNING_MASTERY_INSTANCE;
    }

    public static NatureMasteryTalent getNatureMastery() {
        return NATURE_MASTERY_INSTANCE;
    }

    public static EnderMasteryTalent getEnderMastery() {
        return ENDER_MASTERY_INSTANCE;
    }
}
