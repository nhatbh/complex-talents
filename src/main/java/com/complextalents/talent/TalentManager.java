package com.complextalents.talent;

import com.complextalents.TalentsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TalentManager {
    private static final Map<ResourceLocation, RegistryObject<Talent>> REGISTERED_TALENTS = new HashMap<>();

    public static <T extends Talent> void registerTalent(String name, Supplier<T> talentSupplier) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, name);
        // Note: Actual registration happens through registry events
        REGISTERED_TALENTS.put(id, null); // Placeholder
    }

    public static Talent getTalent(ResourceLocation id) {
        return TalentRegistry.getTalent(id);
    }

    public static boolean isTalentRegistered(ResourceLocation id) {
        return TalentRegistry.getTalent(id) != null;
    }
}
