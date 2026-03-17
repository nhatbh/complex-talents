package com.complextalents.impl.warrior;

import com.complextalents.origin.OriginBuilder;
import com.complextalents.origin.ResourceType;
import com.complextalents.stats.ClassCostMatrix;
import com.complextalents.stats.StatType;
import net.minecraft.resources.ResourceLocation;

/**
 * Warrior Origin implementation.
 */
public class WarriorOrigin {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "warrior");
    public static final ResourceLocation STYLE_RESOURCE_ID = ResourceLocation.fromNamespaceAndPath("complextalents", "style");

    public static void register() {
        // Register Style resource type
        ResourceType styleType = ResourceType.register(STYLE_RESOURCE_ID, "Style", 0, 1000, 0xFFFFD700); // Gold color

        // Build and register Warrior origin
        OriginBuilder.create(ID)
                .displayName("Warrior")
                .description("A master of combat who thrives in battle by building Style. Style ranks (D to SSS) provide massive damage multipliers (up to 1.5x at level 5). High Style also resets shield breaks.")
                .resourceType(styleType)
                .maxLevel(5)
                // Passive Skill: Vanguard's Momentum - Damage Scaling
                .scaledStat("momentumDamage_D", new double[] { 0.7, 0.7, 0.7, 0.7, 0.7 })
                .scaledStat("momentumDamage_C", new double[] { 0.85, 0.85, 0.85, 0.85, 0.85 })
                .scaledStat("momentumDamage_B", new double[] { 1.0, 1.0, 1.0, 1.0, 1.0 })
                .scaledStat("momentumDamage_A", new double[] { 1.05, 1.08, 1.1, 1.12, 1.15 })
                .scaledStat("momentumDamage_S", new double[] { 1.08, 1.12, 1.15, 1.25, 1.3 })
                .scaledStat("momentumDamage_SS", new double[] { 1.09, 1.15, 1.25, 1.35, 1.4 })
                .scaledStat("momentumDamage_SSS", new double[] { 1.1, 1.2, 1.3, 1.4, 1.5 })
                // Vanguard's Momentum - Shield Break Reset (Style Points)
                .scaledStat("shieldBreakReset", new double[] { 250, 450, 700, 850, 900 })
                
                .passiveStack("sss_shield", com.complextalents.passive.PassiveStackDef.create("SSS Shield")
                        .maxStacks(1)
                        .displayName("SSS Shield")
                        .build())
                .renderer(new com.complextalents.impl.warrior.client.WarriorRenderer())
                .register();

        ClassCostMatrix.defineCosts(ID)
                .cost(StatType.FLAT_AD, 2)
                .cost(StatType.PERCENT_AD, 1)
                .cost(StatType.AP, 4)
                .cost(StatType.ARMOR_PEN, 2)
                .cost(StatType.LUCK_CRIT, 3)
                .cost(StatType.MAX_HP, 1)
                .cost(StatType.MAX_MANA, 4)
                .cost(StatType.MOBILITY, 2)
                .cost(StatType.CDR, 3);
    }
}
