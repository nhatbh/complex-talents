package com.complextalents.elemental.integration;

import com.complextalents.TalentsMod;
import net.minecraftforge.fml.ModList;

public class ModIntegrationHandler {
    private static boolean isIronSpellbooksLoaded = false;

    public static void init() {
        isIronSpellbooksLoaded = false;

        try {
            isIronSpellbooksLoaded = net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks");
        } catch (Exception e) {
            // Mod not loaded or API not available
        }

        if (isIronSpellbooksLoaded) {
            IronSpellbooksIntegration.init();
            TalentsMod.LOGGER.info("Iron's Spellbooks integration enabled");
        }
    }

    public static boolean isIronSpellbooksLoaded() {
        return isIronSpellbooksLoaded;
    }
}
