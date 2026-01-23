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
 * Nature Mastery talent - grants Nature-specific mastery
 * Each level grants +10 Nature Mastery
 * Affects Nature-based reactions: Burning, Bloom, Hyperbloom, Withering Seed
 */
public class NatureMasteryTalent extends PassiveTalent {
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes("NatureMasteryTalent".getBytes());
    private static final double MASTERY_PER_LEVEL = 10.0;

    public NatureMasteryTalent() {
        super(
                ElementalTalents.NATURE_MASTERY,
                Component.translatable("talent.complextalents.nature_mastery.name"),
                Component.translatable("talent.complextalents.nature_mastery.description"),
                10,  // Max level 10 = 100 Nature Mastery
                com.complextalents.talent.TalentSlotType.HARMONY  // Enhances playstyle mechanics
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        TalentsMod.LOGGER.debug("Player {} unlocked Nature Mastery at level {}",
                               player.getName().getString(), level);

        player.getAttribute(MasteryAttributes.NATURE_MASTERY.get())
              .removeModifier(MODIFIER_UUID);

        double masteryAmount = MASTERY_PER_LEVEL * level;
        AttributeModifier modifier = new AttributeModifier(
            MODIFIER_UUID,
            "Nature Mastery Talent Bonus",
            masteryAmount,
            AttributeModifier.Operation.ADDITION
        );

        player.getAttribute(MasteryAttributes.NATURE_MASTERY.get())
              .addPermanentModifier(modifier);

        TalentsMod.LOGGER.debug("Granted {} nature mastery to {}",
                               masteryAmount, player.getName().getString());
    }

    @Override
    public void onRemove(ServerPlayer player) {
        TalentsMod.LOGGER.debug("Player {} lost Nature Mastery", player.getName().getString());
        player.getAttribute(MasteryAttributes.NATURE_MASTERY.get())
              .removeModifier(MODIFIER_UUID);
    }
}
