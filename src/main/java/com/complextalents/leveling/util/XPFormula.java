package com.complextalents.leveling.util;

/**
 * Utility class for calculating XP rewards based on the design spec.
 */
public class XPFormula {

    /**
     * Primary XP: $XP = 5 \times (MaxHP)^{0.6}$
     */
    public static double calculatePrimaryXP(double maxHP) {
        return 5 * Math.pow(maxHP, 0.6);
    }

    /**
     * Assassin Ambush: $XP = 2.5 \times (Total Ambush Damage)^{0.55}$
     */
    public static double calculateAssassinAmbushXP(double totalDamage) {
        return 2.5 * Math.pow(totalDamage, 0.55);
    }

    /**
     * Assassin Ghost: $XP = 3.0 \times (Backstab Damage)^{0.5} \times (Remaining Stealth / Max Stealth)$
     */
    public static double calculateAssassinGhostXP(double backstabDamage, double remainingStealth, double maxStealth) {
        double stealthRatio = maxStealth > 0 ? remainingStealth / maxStealth : 0;
        return 3.0 * Math.pow(backstabDamage, 0.5) * stealthRatio;
    }

    /**
     * Dark Mage Edge of Death: $XP = 4.0 \times (Killing Blow Damage)^{0.5} \times (1.0 - Current HP \%)$
     */
    public static double calculateDarkMageEdgeOfDeathXP(double killingBlowDamage, double currentHPPercentage) {
        return 4.0 * Math.pow(killingBlowDamage, 0.5) * (1.0 - currentHPPercentage);
    }

    /**
     * Dark Mage Soul Hoarder: $XP = 10.0 \times (Souls Harvested)^{0.5}$
     */
    public static double calculateDarkMageSoulHoarderXP(double soulsHarvested) {
        return 10.0 * Math.pow(soulsHarvested, 0.5);
    }

    /**
     * Elemental Mage Master of Elements: $XP = 2.0 \times (Reaction Damage)^{0.55}$
     */
    public static double calculateElementalMageMasterOfElementsXP(double reactionDamage) {
        return 2.0 * Math.pow(reactionDamage, 0.55);
    }

    /**
     * High Priest Clutch Savior: $XP = 5.0 \times (Effective Heal)^{0.5} \times (Ally Max HP / Ally Current HP)$
     * Note: HP ratio multiplier is hard-capped at 5x to prevent abuse.
     */
    public static double calculateHighPriestClutchSaviorXP(double effectiveHeal, double allyMaxHP, double allyCurrentHP) {
        double hpRatio = allyCurrentHP > 0 ? allyMaxHP / allyCurrentHP : 5.0;
        hpRatio = Math.min(hpRatio, 5.0); // Hard cap at 5x
        return 5.0 * Math.pow(effectiveHeal, 0.5) * hpRatio;
    }

    /**
     * High Priest Crowd Control: $XP = 1.5 \times Number of Pulled Mobs \times (Player Level)^{0.7}$
     */
    public static double calculateHighPriestCrowdControlXP(int numMobs, int playerLevel) {
        return 1.5 * numMobs * Math.pow(playerLevel, 0.7);
    }

    /**
     * Warrior Unstoppable Momentum: $XP = 1.5 \times (Total SSS Damage)^{0.55}$
     */
    public static double calculateWarriorUnstoppableMomentumXP(double totalSSSDamage) {
        return 1.5 * Math.pow(totalSSSDamage, 0.55);
    }

    /**
     * Warrior Perfect Parry: $XP = 3.0 \times (Absorbed Damage)^{0.55}$
     */
    public static double calculateWarriorPerfectParryXP(double absorbedDamage) {
        return 3.0 * Math.pow(absorbedDamage, 0.55);
    }
}
