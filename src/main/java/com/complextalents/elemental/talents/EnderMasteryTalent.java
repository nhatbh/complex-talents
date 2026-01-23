package com.complextalents.elemental.talents;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementalTalents;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.talent.PassiveTalent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * Ender Mastery talent - grants Ender-specific mastery
 * Each level grants +10 Ender Mastery
 * Affects Ender-based reactions: Unstable Ward, Rift Pull, Singularity, Fracture, Withering Seed, Decrepit Grasp
 */
public class EnderMasteryTalent extends PassiveTalent {
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes("EnderMasteryTalent".getBytes());
    private static final double MASTERY_PER_LEVEL = 10.0;

    public EnderMasteryTalent() {
        super(
                ElementalTalents.ENDER_MASTERY,
                Component.translatable("talent.complextalents.ender_mastery.name"),
                Component.translatable("talent.complextalents.ender_mastery.description"),
                10,  // Max level 10 = 100 Ender Mastery
                com.complextalents.talent.TalentSlotType.HARMONY  // Enhances playstyle mechanics
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        TalentsMod.LOGGER.debug("Player {} unlocked Ender Mastery at level {}",
                               player.getName().getString(), level);

        player.getAttribute(MasteryAttributes.ENDER_MASTERY.get())
              .removeModifier(MODIFIER_UUID);

        double masteryAmount = MASTERY_PER_LEVEL * level;
        AttributeModifier modifier = new AttributeModifier(
            MODIFIER_UUID,
            "Ender Mastery Talent Bonus",
            masteryAmount,
            AttributeModifier.Operation.ADDITION
        );

        player.getAttribute(MasteryAttributes.ENDER_MASTERY.get())
              .addPermanentModifier(modifier);

        TalentsMod.LOGGER.debug("Granted {} ender mastery to {}",
                               masteryAmount, player.getName().getString());
    }

    @Override
    public void onRemove(ServerPlayer player) {
        TalentsMod.LOGGER.debug("Player {} lost Ender Mastery", player.getName().getString());
        player.getAttribute(MasteryAttributes.ENDER_MASTERY.get())
              .removeModifier(MODIFIER_UUID);
    }
}
