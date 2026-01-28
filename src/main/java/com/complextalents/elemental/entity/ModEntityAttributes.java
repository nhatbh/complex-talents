package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(
            ModEntities.NATURE_CORE.get(),
            NatureCoreEntity.createAttributes().build()
        );
        event.put(
            ModEntities.SPRING_POTION.get(),
            SpringPotionEntity.createAttributes().build()
        );
        event.put(
            ModEntities.BLACK_HOLE.get(),
            BlackHoleEntity.createAttributes().build()
        );
    }
}
