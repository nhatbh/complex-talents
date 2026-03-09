package com.complextalents.origin.integration;

import com.complextalents.TalentsMod;

/**
 * Handles mod integration for the origin module.
 * <p>
 * Checks for optional mods at runtime and initializes their respective
 * integration handlers if present.
 * </p>
 */
public class OriginModIntegrationHandler {
    private static boolean isIronSpellbooksLoaded = false;

    /**
     * Initializes all origin module integrations.
     * <p>
     * This checks for optional mods and enables their integration hooks
     * if the mods are present at runtime.
     * </p>
     */
    public static void init() {
        isIronSpellbooksLoaded = false;

        try {
            isIronSpellbooksLoaded = net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks");
        } catch (Exception e) {
            // Mod not loaded or API not available
        }

        if (isIronSpellbooksLoaded) {
            HolySpellbooksIntegration.init();
            SpellCritHandler.init();
            TalentsMod.LOGGER.info("Origin: Iron's Spellbooks integration enabled");
        }
    }

    /**
     * Checks if Iron's Spellbooks is loaded.
     *
     * @return true if Iron's Spellbooks is present
     */
    public static boolean isIronSpellbooksLoaded() {
        return isIronSpellbooksLoaded;
    }
}
