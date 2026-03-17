package com.complextalents.impl.assassin.origin;

import com.complextalents.TalentsMod;
import com.complextalents.impl.assassin.client.AssassinRenderer;
import com.complextalents.stats.ClassCostMatrix;
import com.complextalents.stats.StatType;
import com.complextalents.origin.OriginBuilder;
import com.complextalents.origin.OriginManager;
import com.complextalents.origin.Origin;
import com.complextalents.origin.OriginRegistry;
import com.complextalents.util.UUIDHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/**
 * Assassin Origin - Stealth-based burst damage dealer.
 * <p>
 * Focuses on backstabbing enemies to apply team-wide damage amplification
 * and gain personal buffs to escape combat.
 * </p>
 */
public class AssassinOrigin {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "assassin");
    public static final UUID STEALTH_SPEED_UUID = UUIDHelper.generateAttributeModifierUUID("origin",
            "assassin_stealth_speed");

    public static void register() {
        OriginBuilder.create("complextalents", "assassin")
                .displayName("Assassin")
                .description(net.minecraft.network.chat.Component.literal("Stealth-based burst damage dealer. Melee attacks from behind apply 'Exposed', amplifying damage by up to 80% and granting move speed on disengagement."))
                .maxLevel(5)
                .renderer(new AssassinRenderer())
                // Passive: Expose Weakness
                .scaledStat("exposeDamageAmp", new double[] { 0.30, 0.40, 0.50, 0.60, 0.80 })
                .scaledStat("exposeDuration", new double[] { 8.0, 10.0, 12.0, 14.0, 16.0 })
                .scaledStat("exposeCooldown", new double[] { 45.0, 40.0, 35.0, 30.0, 25.0 })

                // Passive: The Disengage
                .scaledStat("disengageMoveSpeed", new double[] { 0.30, 0.45, 0.60, 0.75, 1.00 })
                .scaledStat("disengageDuration", new double[] { 1.5, 1.5, 2.0, 2.0, 2.5 })
                .register();

        ClassCostMatrix.defineCosts(ID)
                .cost(StatType.FLAT_AD, 1)
                .cost(StatType.PERCENT_AD, 1)
                .cost(StatType.AP, 4)
                .cost(StatType.ARMOR_PEN, 1)
                .cost(StatType.LUCK_CRIT, 1)
                .cost(StatType.MAX_HP, 3)
                .cost(StatType.MAX_MANA, 4)
                .cost(StatType.MOBILITY, 1)
                .cost(StatType.CDR, 2);

        TalentsMod.LOGGER.info("Assassin origin registered");
    }

    public static double getExposeAmp(int level) {
        Origin origin = OriginRegistry.getInstance().getOrigin(ID);
        if (origin == null)
            return 0.15;
        return origin.getScaledStat("exposeDamageAmp", level);
    }

    public static boolean isAssassin(ServerPlayer player) {
        return ID.equals(OriginManager.getOriginId(player));
    }
}
