package com.complextalents.elemental.attributes;

import com.complextalents.TalentsMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles attaching mastery attributes to players
 * Uses EntityAttributeModificationEvent to add attributes to existing entity types
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MasteryAttributeHandler {

    /**
     * Called to modify entity attributes
     * This allows us to add our custom attributes to the Player entity type
     */
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        // Add mastery attributes to players
        event.add(EntityType.PLAYER, MasteryAttributes.ELEMENTAL_MASTERY.get());
        event.add(EntityType.PLAYER, MasteryAttributes.FIRE_MASTERY.get());
        event.add(EntityType.PLAYER, MasteryAttributes.AQUA_MASTERY.get());
        event.add(EntityType.PLAYER, MasteryAttributes.LIGHTNING_MASTERY.get());
        event.add(EntityType.PLAYER, MasteryAttributes.ICE_MASTERY.get());
        event.add(EntityType.PLAYER, MasteryAttributes.NATURE_MASTERY.get());
        event.add(EntityType.PLAYER, MasteryAttributes.ENDER_MASTERY.get());

        TalentsMod.LOGGER.info("Mastery attributes added to Player entity type");
    }
}
