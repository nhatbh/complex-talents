package com.complextalents.impl.darkmage;

import com.complextalents.TalentsMod;
import com.complextalents.impl.darkmage.origin.DarkMageOrigin;
import com.complextalents.impl.darkmage.skill.BloodPactSkill;

/**
 * Central registration point for Dark Mage origin and skills.
 * Call {@link #register()} during mod initialization.
 */
public class DarkMageRegistrar {

    /**
     * Register all Dark Mage components.
     * This includes:
     * - Dark Mage origin
     * - Blood Pact skill
     * <p>
     * Event handlers (SoulSiphonHandler, BloodPactTickHandler, PhylacteryHandler)
     * are automatically registered via @Mod.EventBusSubscriber.
     */
    public static void register() {
        // Register origin
        DarkMageOrigin.register();

        // Register skill
        BloodPactSkill.register();

        TalentsMod.LOGGER.info("Dark Mage origin and skills registered");
    }
}
