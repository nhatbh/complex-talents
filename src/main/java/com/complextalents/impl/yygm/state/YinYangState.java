package com.complextalents.impl.yygm.state;

/**
 * Enum representing the three states of the YYGM gate combat system.
 * The state cycle is: Harmonized (2 gates) → Exposed (8 gates) → Annihilated (no gates).
 */
public enum YinYangState {
    /**
     * Harmonized state - The default combat state.
     * Features 2 gates (1 Yang, 1 Yin) that respawn after being hit.
     * Player must alternate between Yin and Yang gates to gain Equilibrium.
     */
    HARMONIZED("harmonized", 2, true),

    /**
     * Exposed state - Activated by the Eight Formation Battle Array ultimate.
     * Features 8 gates (4 Yang, 4 Yin) that do not respawn.
     * Player must hit all 8 gates in alternating sequence to convert to Annihilation.
     */
    EXPOSED("exposed", 8, false),

    /**
     * Annihilation state - The reward for completing all Exposed gates.
     * No gates - all attacks deal amplified true damage from any angle.
     */
    ANNIHILATION("annihilation", 0, false);

    private final String name;
    private final int gateCount;
    private final boolean gatesRespawn;

    YinYangState(String name, int gateCount, boolean gatesRespawn) {
        this.name = name;
        this.gateCount = gateCount;
        this.gatesRespawn = gatesRespawn;
    }

    /**
     * Get the display name of this state.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the number of gates in this state.
     *
     * @return 2 for Harmonized, 8 for Exposed, 0 for Annihilation
     */
    public int getGateCount() {
        return gateCount;
    }

    /**
     * Check if gates respawn in this state.
     *
     * @return true for Harmonized (gates respawn), false otherwise
     */
    public boolean hasGatesRespawn() {
        return gatesRespawn;
    }

    /**
     * Check if this state has any gates.
     *
     * @return true for Harmonized and Exposed, false for Annihilation
     */
    public boolean hasGates() {
        return gateCount > 0;
    }

    /**
     * Check if a transition from this state to the target state is valid.
     * Valid transitions:
     * - Harmonized → Exposed
     * - Harmonized → Annihilation
     * - Exposed → Annihilation
     * - Any state → null (clearing)
     *
     * @param target The target state (null = clearing)
     * @return true if transition is valid
     */
    public boolean canTransitionTo(YinYangState target) {
        if (target == null) {
            return true; // Can always clear
        }
        return switch (this) {
            case HARMONIZED -> target == EXPOSED || target == ANNIHILATION;
            case EXPOSED -> target == ANNIHILATION;
            case ANNIHILATION -> false; // Must clear first
        };
    }

    /**
     * Check if this state allows direct damage from any angle.
     *
     * @return true only for Annihilation state
     */
    public boolean isAnyAngleDamage() {
        return this == ANNIHILATION;
    }
}
