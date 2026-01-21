package com.complextalents.elemental.attributes;

import com.complextalents.TalentsMod;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom attributes for the Elemental Mastery system
 * These attributes control reaction damage scaling
 */
public class MasteryAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, TalentsMod.MODID);

    // General Elemental Mastery - affects all reactions
    public static final RegistryObject<Attribute> ELEMENTAL_MASTERY = ATTRIBUTES.register("elemental_mastery",
        () -> new RangedAttribute("attribute.name.complex_talents.elemental_mastery", 0.0, 0.0, 1000.0)
            .setSyncable(true));

    // Element-Specific Mastery - stronger scaling for specific elements
    public static final RegistryObject<Attribute> FIRE_MASTERY = ATTRIBUTES.register("fire_mastery",
        () -> new RangedAttribute("attribute.name.complex_talents.fire_mastery", 0.0, 0.0, 500.0)
            .setSyncable(true));

    public static final RegistryObject<Attribute> AQUA_MASTERY = ATTRIBUTES.register("aqua_mastery",
        () -> new RangedAttribute("attribute.name.complex_talents.aqua_mastery", 0.0, 0.0, 500.0)
            .setSyncable(true));

    public static final RegistryObject<Attribute> LIGHTNING_MASTERY = ATTRIBUTES.register("lightning_mastery",
        () -> new RangedAttribute("attribute.name.complex_talents.lightning_mastery", 0.0, 0.0, 500.0)
            .setSyncable(true));

    public static final RegistryObject<Attribute> ICE_MASTERY = ATTRIBUTES.register("ice_mastery",
        () -> new RangedAttribute("attribute.name.complex_talents.ice_mastery", 0.0, 0.0, 500.0)
            .setSyncable(true));

    public static final RegistryObject<Attribute> NATURE_MASTERY = ATTRIBUTES.register("nature_mastery",
        () -> new RangedAttribute("attribute.name.complex_talents.nature_mastery", 0.0, 0.0, 500.0)
            .setSyncable(true));

    public static final RegistryObject<Attribute> ENDER_MASTERY = ATTRIBUTES.register("ender_mastery",
        () -> new RangedAttribute("attribute.name.complex_talents.ender_mastery", 0.0, 0.0, 500.0)
            .setSyncable(true));

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }

    /**
     * Calculates the damage bonus from general mastery
     * Uses diminishing returns formula: bonus = mastery / (mastery + scaling)
     */
    public static double calculateGeneralMasteryBonus(double masteryValue, double scalingConstant) {
        if (masteryValue <= 0) return 0.0;
        return masteryValue / (masteryValue + scalingConstant);
    }

    /**
     * Calculates the damage bonus from specific mastery
     * Uses more aggressive scaling with diminishing returns
     */
    public static double calculateSpecificMasteryBonus(double masteryValue, double scalingConstant) {
        if (masteryValue <= 0) return 0.0;
        return masteryValue / (masteryValue + scalingConstant);
    }

    /**
     * Gets the combined mastery bonus for a reaction
     * Formula: finalDamage = baseDamage * (1 + generalBonus) * (1 + specificBonus)
     */
    public static double getCombinedMasteryMultiplier(double generalMastery, double specificMastery,
                                                       double generalScaling, double specificScaling) {
        double generalBonus = calculateGeneralMasteryBonus(generalMastery, generalScaling);
        double specificBonus = calculateSpecificMasteryBonus(specificMastery, specificScaling);
        return (1.0 + generalBonus) * (1.0 + specificBonus);
    }
}
