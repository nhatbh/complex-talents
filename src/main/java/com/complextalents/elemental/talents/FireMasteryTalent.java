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
 * Fire Mastery talent - grants Fire-specific mastery
 * Each level grants +10 Fire Mastery
 * Affects Fire-based reactions: Vaporize, Melt, Overloaded, Burning, Burgeon, Singularity
 */
public class FireMasteryTalent extends PassiveTalent {
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes("FireMasteryTalent".getBytes());
    private static final double MASTERY_PER_LEVEL = 10.0;

    public FireMasteryTalent() {
        super(
                ElementalTalents.FIRE_MASTERY,
                Component.translatable("talent.complextalents.fire_mastery.name"),
                Component.translatable("talent.complextalents.fire_mastery.description"),
                10,  // Max level 10 = 100 Fire Mastery
                com.complextalents.talent.TalentSlotType.HARMONY  // Enhances playstyle mechanics
        );
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        TalentsMod.LOGGER.debug("Player {} unlocked Fire Mastery at level {}",
                               player.getName().getString(), level);

        // Remove old modifier if it exists
        player.getAttribute(MasteryAttributes.FIRE_MASTERY.get())
              .removeModifier(MODIFIER_UUID);

        // Add new modifier with current level
        double masteryAmount = MASTERY_PER_LEVEL * level;
        AttributeModifier modifier = new AttributeModifier(
            MODIFIER_UUID,
            "Fire Mastery Talent Bonus",
            masteryAmount,
            AttributeModifier.Operation.ADDITION
        );

        player.getAttribute(MasteryAttributes.FIRE_MASTERY.get())
              .addPermanentModifier(modifier);

        TalentsMod.LOGGER.debug("Granted {} fire mastery to {}",
                               masteryAmount, player.getName().getString());
    }

    @Override
    public void onRemove(ServerPlayer player) {
        TalentsMod.LOGGER.debug("Player {} lost Fire Mastery", player.getName().getString());

        // Remove the attribute modifier
        player.getAttribute(MasteryAttributes.FIRE_MASTERY.get())
              .removeModifier(MODIFIER_UUID);
    }
}
