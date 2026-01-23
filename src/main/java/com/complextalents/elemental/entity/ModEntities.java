package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom entity types used by the Elemental System
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TalentsMod.MODID);

    // Elemental Orb - Orbiting projectile spawned by Ward talent (Rank 3B - Reprisal)
    public static final RegistryObject<EntityType<ElementalOrbEntity>> ELEMENTAL_ORB =
        ENTITY_TYPES.register("elemental_orb", () -> EntityType.Builder.<ElementalOrbEntity>of(
            ElementalOrbEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build("elemental_orb"));

    // Existing entities from other files are registered separately

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        TalentsMod.LOGGER.info("Registered {} custom entity types for Elemental System", ENTITY_TYPES.getEntries().size());
    }
}
