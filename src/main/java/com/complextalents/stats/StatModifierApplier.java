package com.complextalents.stats;

import com.complextalents.registry.ModAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Logic for applying stat rank effects to player attributes.
 */
public class StatModifierApplier {

    /**
     * Re-applies all stat modifiers to the player based on their ranks.
     */
    public static void applyAll(Player player, java.util.Map<StatType, Integer> ranks) {
        for (StatType type : StatType.values()) {
            int rank = ranks.getOrDefault(type, 0);
            applyStatModifier(player, type, rank);
        }
    }

    private static void applyStatModifier(Player player, StatType type, int rank) {
        double totalValue = type.getYieldPerRank() * rank;
        UUID uuid = type.getModifierUuid();

        switch (type) {
            case FLAT_AD:
                updateModifier(player, Attributes.ATTACK_DAMAGE, totalValue, AttributeModifier.Operation.ADDITION,
                        uuid);
                break;
            case PERCENT_AD:
                updateModifier(player, Attributes.ATTACK_DAMAGE, totalValue,
                        AttributeModifier.Operation.MULTIPLY_TOTAL,
                        uuid);
                break;
            case AP:
                updateModAttribute(player, "irons_spellbooks", "spell_power", totalValue,
                        AttributeModifier.Operation.ADDITION, uuid);
                break;
            case ARMOR_PEN:
                updateModAttribute(player, "attributeslib", "armor_pierce", totalValue,
                        AttributeModifier.Operation.ADDITION, uuid);
                break;
            case LUCK_CRIT:
                // +1% Crit Chance, +0.2 Luck, +1% Spell Crit Chance per rank
                updateModifier(player, Attributes.LUCK, rank * 0.2, AttributeModifier.Operation.ADDITION, uuid);
                updateModAttribute(player, "attributeslib", "crit_chance", rank * 0.01,
                        AttributeModifier.Operation.ADDITION, uuid);
                updateModifier(player, ModAttributes.SPELL_CRIT_CHANCE.get(), rank * 0.01,
                        AttributeModifier.Operation.ADDITION, uuid);
                break;
            case MAX_HP:
                updateModifier(player, Attributes.MAX_HEALTH, totalValue, AttributeModifier.Operation.ADDITION, uuid);
                break;
            case MAX_MANA:
                updateModAttribute(player, "irons_spellbooks", "max_mana", totalValue,
                        AttributeModifier.Operation.ADDITION, uuid);
                break;
            case MOBILITY:
                // +1.5% Movement Speed, +1% Jump Height per rank
                updateModifier(player, Attributes.MOVEMENT_SPEED, rank * 0.015,
                        AttributeModifier.Operation.MULTIPLY_BASE, uuid);
                updateModifier(player, Attributes.JUMP_STRENGTH, rank * 0.01, AttributeModifier.Operation.MULTIPLY_BASE,
                        uuid);
                break;
            case CDR:
                double cdrPercentage = totalValue / (100.0 + totalValue);
                updateModAttribute(player, "irons_spellbooks", "cooldown_reduction", cdrPercentage,
                        AttributeModifier.Operation.MULTIPLY_TOTAL, uuid);
                break;
        }
    }

    private static void updateModifier(Player player, Attribute attribute, double amount,
            AttributeModifier.Operation operation, UUID uuid) {
        if (attribute == null)
            return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null)
            return;

        instance.removeModifier(uuid);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(uuid, "General Stat Modifier", amount, operation));
        }
    }

    private static void updateModAttribute(Player player, String modId, String attrName, double amount,
            AttributeModifier.Operation operation, UUID uuid) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES
                .getValue(ResourceLocation.fromNamespaceAndPath(modId, attrName));
        if (attribute != null) {
            updateModifier(player, attribute, amount, operation, uuid);
        }
    }
}
