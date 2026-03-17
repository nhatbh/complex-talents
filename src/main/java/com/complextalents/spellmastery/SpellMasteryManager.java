package com.complextalents.spellmastery;

import com.complextalents.spellmastery.capability.SpellMasteryDataProvider;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Manages Spell Mastery verification logic (The Gatekeeper).
 */
public class SpellMasteryManager {

    /**
     * Verifies if a player can cast a spell based on mastery and knowledge.
     * 
     * @param spell The spell being cast.
     * @param spellLevel The level of the spell.
     * @param castSource The source of the cast.
     * @param player The player attempting the cast.
     * @return A CastResult (Success or Failure with message).
     */
    public static Optional<CastResult> verifyCast(AbstractSpell spell, int spellLevel, CastSource castSource, Player player) {
        // Step 1: Identify Casting Source
        // Scroll casting bypasses mastery and knowledge checks.
        if (castSource == CastSource.SCROLL) {
            return Optional.empty(); // Proceed to standard checks
        }

        // Step 2: Learned Verification
        return player.getCapability(SpellMasteryDataProvider.MASTERY_DATA).map(mastery -> {
            ResourceLocation spellId = spell.getSpellResource();
            
            if (!mastery.isSpellLearned(spellId, spellLevel)) {
                return new CastResult(CastResult.Type.FAILURE, 
                    Component.literal("You have not learned this spell tier: " + spell.getDisplayName(player).getString())
                    .withStyle(ChatFormatting.RED));
            }

            // Step 3: Mastery Tier Verification
            SpellRarity spellRarity = spell.getRarity(spellLevel);
            ResourceLocation schoolId = spell.getSchoolType().getId();
            int playerMastery = mastery.getMasteryLevel(schoolId);
            int requiredMastery = spellRarity.getValue(); // Assuming SpellRarity.getValue() maps 0-4 or 1-5

            if (playerMastery < requiredMastery) {
                return new CastResult(CastResult.Type.FAILURE, 
                    Component.literal("Your " + spell.getSchoolType().getDisplayName().getString() + " Mastery is too low to cast " + spellRarity.getDisplayName().getString() + " spells!")
                    .withStyle(ChatFormatting.RED));
            }

            return new CastResult(CastResult.Type.SUCCESS);
        }).filter(result -> result.type == CastResult.Type.FAILURE);
    }

    public static int getSpellCost(SpellRarity rarity) {
        return switch (rarity) {
            case COMMON -> 2;
            case UNCOMMON -> 3;
            case RARE -> 5;
            case EPIC -> 10;
            case LEGENDARY -> 15;
            default -> 1;
        };
    }
}
