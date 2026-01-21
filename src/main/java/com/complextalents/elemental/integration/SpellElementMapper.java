package com.complextalents.elemental.integration;

import com.complextalents.elemental.ElementType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps spell names and schools to ElementType
 * Supports both Iron's Spellbooks and T.O's Spellbooks
 */
public class SpellElementMapper {
    private static final Map<String, ElementType> SPELL_TO_ELEMENT = new ConcurrentHashMap<>();
    private static final Map<String, ElementType> SCHOOL_TO_ELEMENT = new ConcurrentHashMap<>();

    static {
        // Map spell schools to elements (primary detection method)
        SCHOOL_TO_ELEMENT.put("fire_spell_power", ElementType.FIRE);
        SCHOOL_TO_ELEMENT.put("traveloptics:aqua_spell_power", ElementType.AQUA);
        SCHOOL_TO_ELEMENT.put("lightning_spell_power", ElementType.LIGHTNING);
        SCHOOL_TO_ELEMENT.put("ice_spell_power", ElementType.ICE);
        SCHOOL_TO_ELEMENT.put("nature_spell_power", ElementType.NATURE);
        SCHOOL_TO_ELEMENT.put("ender_spell_power", ElementType.ENDER);

        // Iron's Spellbooks - Fire spells
        SPELL_TO_ELEMENT.put("fireball", ElementType.FIRE);
        SPELL_TO_ELEMENT.put("fire_breath", ElementType.FIRE);
        SPELL_TO_ELEMENT.put("firebolt", ElementType.FIRE);
        SPELL_TO_ELEMENT.put("flame_ray", ElementType.FIRE);
        SPELL_TO_ELEMENT.put("inferno", ElementType.FIRE);
        SPELL_TO_ELEMENT.put("burning_dash", ElementType.FIRE);
        SPELL_TO_ELEMENT.put("blaze_storm", ElementType.FIRE);

        // Iron's Spellbooks - Water/Aqua spells
        SPELL_TO_ELEMENT.put("splash", ElementType.AQUA);
        SPELL_TO_ELEMENT.put("geyser", ElementType.AQUA);
        SPELL_TO_ELEMENT.put("aquatic", ElementType.AQUA);
        SPELL_TO_ELEMENT.put("water_grave", ElementType.AQUA);

        // Iron's Spellbooks - Ice spells
        SPELL_TO_ELEMENT.put("frost_wave", ElementType.ICE);
        SPELL_TO_ELEMENT.put("ice_lance", ElementType.ICE);
        SPELL_TO_ELEMENT.put("blizzard", ElementType.ICE);
        SPELL_TO_ELEMENT.put("freeze", ElementType.ICE);
        SPELL_TO_ELEMENT.put("cone_of_cold", ElementType.ICE);
        SPELL_TO_ELEMENT.put("icicle", ElementType.ICE);

        // Iron's Spellbooks - Lightning spells
        SPELL_TO_ELEMENT.put("lightning", ElementType.LIGHTNING);
        SPELL_TO_ELEMENT.put("chain_lightning", ElementType.LIGHTNING);
        SPELL_TO_ELEMENT.put("lightning_lance", ElementType.LIGHTNING);
        SPELL_TO_ELEMENT.put("lightning_bolt", ElementType.LIGHTNING);
        SPELL_TO_ELEMENT.put("shockwave", ElementType.LIGHTNING);
        SPELL_TO_ELEMENT.put("electrocute", ElementType.LIGHTNING);
        SPELL_TO_ELEMENT.put("thunderstorm", ElementType.LIGHTNING);

        // Iron's Spellbooks - Nature spells
        SPELL_TO_ELEMENT.put("nature", ElementType.NATURE);
        SPELL_TO_ELEMENT.put("root", ElementType.NATURE);
        SPELL_TO_ELEMENT.put("thorn", ElementType.NATURE);
        SPELL_TO_ELEMENT.put("poison_breath", ElementType.NATURE);
        SPELL_TO_ELEMENT.put("acid_orb", ElementType.NATURE);

        // Iron's Spellbooks - Ender spells
        SPELL_TO_ELEMENT.put("teleport", ElementType.ENDER);
        SPELL_TO_ELEMENT.put("portal", ElementType.ENDER);
        SPELL_TO_ELEMENT.put("dimension", ElementType.ENDER);
        SPELL_TO_ELEMENT.put("void", ElementType.ENDER);
        SPELL_TO_ELEMENT.put("black_hole", ElementType.ENDER);

        // T.O's Spellbooks mappings
        SPELL_TO_ELEMENT.put("aqua_blast", ElementType.AQUA);
        SPELL_TO_ELEMENT.put("aqua_jet", ElementType.AQUA);
        SPELL_TO_ELEMENT.put("water_spear", ElementType.AQUA);
    }

    /**
     * Gets element type from spell school attribute
     * This is the primary detection method
     */
    public static ElementType getElementTypeFromSchool(String schoolAttribute) {
        if (schoolAttribute == null) return null;
        return SCHOOL_TO_ELEMENT.get(schoolAttribute.toLowerCase());
    }

    /**
     * Gets element type from spell name
     * Used as fallback when school detection fails
     */
    public static ElementType getElementTypeFromSpell(String spellName) {
        if (spellName == null) return null;

        String lowerName = spellName.toLowerCase();

        // Direct mapping
        ElementType element = SPELL_TO_ELEMENT.get(lowerName);
        if (element != null) return element;

        // Fuzzy matching based on keywords
        if (lowerName.contains("fire") || lowerName.contains("pyro") ||
            lowerName.contains("flame") || lowerName.contains("burn") ||
            lowerName.contains("blaze") || lowerName.contains("ignite")) {
            return ElementType.FIRE;
        }

        if (lowerName.contains("water") || lowerName.contains("hydro") ||
            lowerName.contains("splash") || lowerName.contains("aqua") ||
            lowerName.contains("flood") || lowerName.contains("wave") ||
            lowerName.contains("geyser")) {
            return ElementType.AQUA;
        }

        if (lowerName.contains("ice") || lowerName.contains("cryo") ||
            lowerName.contains("frost") || lowerName.contains("freeze") ||
            lowerName.contains("cold") || lowerName.contains("snow") ||
            lowerName.contains("chill") || lowerName.contains("icicle")) {
            return ElementType.ICE;
        }

        if (lowerName.contains("lightning") || lowerName.contains("electro") ||
            lowerName.contains("shock") || lowerName.contains("spark") ||
            lowerName.contains("thunder") || lowerName.contains("volt") ||
            lowerName.contains("charge")) {
            return ElementType.LIGHTNING;
        }

        if (lowerName.contains("nature") || lowerName.contains("dendro") ||
            lowerName.contains("plant") || lowerName.contains("root") ||
            lowerName.contains("thorn") || lowerName.contains("forest") ||
            lowerName.contains("poison") || lowerName.contains("acid") ||
            lowerName.contains("vine")) {
            return ElementType.NATURE;
        }

        if (lowerName.contains("ender") || lowerName.contains("void") ||
            lowerName.contains("teleport") || lowerName.contains("portal") ||
            lowerName.contains("dimension") || lowerName.contains("warp") ||
            lowerName.contains("black_hole") || lowerName.contains("singularity")) {
            return ElementType.ENDER;
        }

        return null;
    }

    /**
     * Adds a custom spell to element mapping
     */
    public static void addCustomMapping(String spellName, ElementType element) {
        SPELL_TO_ELEMENT.put(spellName.toLowerCase(), element);
    }

    /**
     * Adds a custom school to element mapping
     */
    public static void addCustomSchoolMapping(String schoolAttribute, ElementType element) {
        SCHOOL_TO_ELEMENT.put(schoolAttribute.toLowerCase(), element);
    }
}
