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
 * Lightning Mastery talent - grants Lightning-specific mastery
 * Each level grants +10 Lightning Mastery
 * Affects Lightning-based reactions: Overloaded, Electro-Charged, Superconduct, Hyperbloom, Rift Pull
 */
public class LightningMasteryTalent extends PassiveTalent {
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes("LightningMasteryTalent".getBytes());
    private static final double MASTERY_PER_LEVEL = 10.0;

    public LightningMasteryTalent() {
        super(
                ElementalTalents.LIGHTNING_MASTERY,
                Component.translatable("talent.complextalents.lightning_mastery.name"),
                Component.translatable("talent.complextalents.lightning_mastery.description"),
                10,  // Max level 10 = 100 Lightning Mastery
                com.complextalents.talent.TalentSlotType.HARMONY  // Enhances playstyle mechanics
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        TalentsMod.LOGGER.debug("Player {} unlocked Lightning Mastery at level {}",
                               player.getName().getString(), level);

        player.getAttribute(MasteryAttributes.LIGHTNING_MASTERY.get())
              .removeModifier(MODIFIER_UUID);

        double masteryAmount = MASTERY_PER_LEVEL * level;
        AttributeModifier modifier = new AttributeModifier(
            MODIFIER_UUID,
            "Lightning Mastery Talent Bonus",
            masteryAmount,
            AttributeModifier.Operation.ADDITION
        );

        player.getAttribute(MasteryAttributes.LIGHTNING_MASTERY.get())
              .addPermanentModifier(modifier);

        TalentsMod.LOGGER.debug("Granted {} lightning mastery to {}",
                               masteryAmount, player.getName().getString());
    }

    @Override
    public void onRemove(ServerPlayer player) {
        TalentsMod.LOGGER.debug("Player {} lost Lightning Mastery", player.getName().getString());
        player.getAttribute(MasteryAttributes.LIGHTNING_MASTERY.get())
              .removeModifier(MODIFIER_UUID);
    }
}
