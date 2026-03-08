package com.complextalents.impl.assassin;

import com.complextalents.TalentsMod;
import com.complextalents.impl.assassin.origin.AssassinOrigin;
import com.complextalents.impl.assassin.skill.ShadowWalkSkill;

/**
 * Central registration point for Assassin origin and skills.
 */
public class AssassinRegistrar {

    public static void register() {
        // Register origin
        AssassinOrigin.register();

        // Register skill
        ShadowWalkSkill.register();

        TalentsMod.LOGGER.info("Assassin origin and skills registered");
    }
}
