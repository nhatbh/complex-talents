package com.complextalents.impl.yygm.entity;

import com.complextalents.TalentsMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.EntityType;

/**
 * Registry for custom entity types used by the Yin Yang Grandmaster origin.
 */
public class YygmEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TalentsMod.MODID);

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        TalentsMod.LOGGER.info("Registered YYGM entity types");
    }
}
