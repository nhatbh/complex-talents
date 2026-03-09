package com.complextalents.elemental.api;

/**
 * Strategy interface for Overwhelming Power effects.
 */
public interface IOPStrategy {

    /**
     * Executes the tier-specific effect.
     * Note: Effects are cumulative, so calling this with tier 5 
     * should ideally handle tiers 1-5 logic if not already handled.
     * 
     * @param context The OP context
     * @param tier The tier reached (1-5)
     */
    void execute(OPContext context, int tier);

    /**
     * Gets a list of strings describing the additional damage/effects from this reaction.
     */
    default java.util.List<String> getEffectBreakdown(int tier, float damage) {
        return java.util.Collections.emptyList();
    }

    /**
     * Gets the element type this strategy handles.
     */
    com.complextalents.elemental.OPElementType getElementType();
}
