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
 * General Elemental Mastery talent
 * Grants general elemental mastery attribute points (affects all reactions)
 * Each level grants +20 Elemental Mastery
 */
public class ElementalMasteryTalent extends PassiveTalent {
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes("ElementalMasteryTalent".getBytes());
    private static final double MASTERY_PER_LEVEL = 20.0;

    public ElementalMasteryTalent() {
        super(
                ElementalTalents.ELEMENTAL_MASTERY,
                Component.translatable("talent.complextalents.elemental_mastery.name"),
                Component.translatable("talent.complextalents.elemental_mastery.description"),
                5,  // Max level 5 = 100 Elemental Mastery
                com.complextalents.talent.TalentSlotType.HARMONY  // Enhances playstyle mechanics
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        TalentsMod.LOGGER.debug("Player {} unlocked Elemental Mastery at level {}",
                               player.getName().getString(), level);

        // Remove old modifier if it exists
        player.getAttribute(MasteryAttributes.ELEMENTAL_MASTERY.get())
              .removeModifier(MODIFIER_UUID);

        // Add new modifier with current level
        double masteryAmount = MASTERY_PER_LEVEL * level;
        AttributeModifier modifier = new AttributeModifier(
            MODIFIER_UUID,
            "Elemental Mastery Talent Bonus",
            masteryAmount,
            AttributeModifier.Operation.ADDITION
        );

        player.getAttribute(MasteryAttributes.ELEMENTAL_MASTERY.get())
              .addPermanentModifier(modifier);

        TalentsMod.LOGGER.debug("Granted {} elemental mastery to {}",
                               masteryAmount, player.getName().getString());
    }

    @Override
    public void onRemove(ServerPlayer player) {
        TalentsMod.LOGGER.debug("Player {} lost Elemental Mastery", player.getName().getString());

        // Remove the attribute modifier
        player.getAttribute(MasteryAttributes.ELEMENTAL_MASTERY.get())
              .removeModifier(MODIFIER_UUID);
    }
}
