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
 * Aqua Mastery talent - grants Aqua-specific mastery
 * Each level grants +10 Aqua Mastery
 * Affects Aqua-based reactions: Vaporize, Frozen, Electro-Charged, Bloom, Decrepit Grasp
 */
public class AquaMasteryTalent extends PassiveTalent {
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes("AquaMasteryTalent".getBytes());
    private static final double MASTERY_PER_LEVEL = 10.0;

    public AquaMasteryTalent() {
        super(
                ElementalTalents.AQUA_MASTERY,
                Component.translatable("talent.complextalents.aqua_mastery.name"),
                Component.translatable("talent.complextalents.aqua_mastery.description"),
                10,  // Max level 10 = 100 Aqua Mastery
                com.complextalents.talent.TalentSlotType.HARMONY  // Enhances playstyle mechanics
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        TalentsMod.LOGGER.debug("Player {} unlocked Aqua Mastery at level {}",
                               player.getName().getString(), level);

        player.getAttribute(MasteryAttributes.AQUA_MASTERY.get())
              .removeModifier(MODIFIER_UUID);

        double masteryAmount = MASTERY_PER_LEVEL * level;
        AttributeModifier modifier = new AttributeModifier(
            MODIFIER_UUID,
            "Aqua Mastery Talent Bonus",
            masteryAmount,
            AttributeModifier.Operation.ADDITION
        );

        player.getAttribute(MasteryAttributes.AQUA_MASTERY.get())
              .addPermanentModifier(modifier);

        TalentsMod.LOGGER.debug("Granted {} aqua mastery to {}",
                               masteryAmount, player.getName().getString());
    }

    @Override
    public void onRemove(ServerPlayer player) {
        TalentsMod.LOGGER.debug("Player {} lost Aqua Mastery", player.getName().getString());
        player.getAttribute(MasteryAttributes.AQUA_MASTERY.get())
              .removeModifier(MODIFIER_UUID);
    }
}
