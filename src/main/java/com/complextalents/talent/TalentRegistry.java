package com.complextalents.talent;

import com.complextalents.TalentsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class TalentRegistry {
    private static final java.util.Map<ResourceLocation, Talent> TALENTS = new java.util.HashMap<>();

    public static <T extends Talent> void register(String name, Supplier<T> talent) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, name);
        T talentInstance = talent.get();
        TALENTS.put(id, talentInstance);
        TalentsMod.LOGGER.debug("Registered talent: {} ({})", id, talentInstance.getClass().getSimpleName());
    }

    public static Talent getTalent(ResourceLocation id) {
        return TALENTS.get(id);
    }

    public static boolean hasTalent(ResourceLocation id) {
        return TALENTS.containsKey(id);
    }
}
