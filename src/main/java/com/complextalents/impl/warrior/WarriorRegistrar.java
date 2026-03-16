package com.complextalents.impl.warrior;
 
import com.complextalents.TalentsMod;
import com.complextalents.impl.warrior.skills.ChallengersRetribution;
 
/**
 * Central registration point for Warrior origin and skills.
 */
public class WarriorRegistrar {
 
    public static void register() {
        // Register origin
        WarriorOrigin.register();
 
        // Register skill
        ChallengersRetribution.register();
 
        TalentsMod.LOGGER.info("Warrior origin and skills registered");
    }
}
