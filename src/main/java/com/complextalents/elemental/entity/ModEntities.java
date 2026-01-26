package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Registry for custom entity types used by the Elemental System
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TalentsMod.MODID);

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        TalentsMod.LOGGER.info("Registered {} custom entity types for Elemental System", ENTITY_TYPES.getEntries().size());
    }
}