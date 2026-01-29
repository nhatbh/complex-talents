package com.complextalents.skill.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side cache of player skill data.
 * Used for input handling and UI display.
 */
@OnlyIn(Dist.CLIENT)
public class ClientSkillData {

    private static final ResourceLocation[] skillSlots = new ResourceLocation[4];

    /**
     * Sync skill slots from the server.
     *
     * @param slots The slot assignments from server
     */
    public static void syncFromServer(ResourceLocation[] slots) {
        if (slots != null && slots.length == 4) {
            System.arraycopy(slots, 0, skillSlots, 0, 4);
        }
    }

    /**
     * Get the skill assigned to a slot.
     *
     * @param slotIndex The slot index (0-3)
     * @return The skill ID, or null if empty
     */
    public static ResourceLocation getSkillInSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 4) {
            return null;
        }
        return skillSlots[slotIndex];
    }

    /**
     * Get all skill slots.
     *
     * @return Copy of the skill slots array
     */
    public static ResourceLocation[] getAllSlots() {
        return java.util.Arrays.copyOf(skillSlots, 4);
    }

    /**
     * Clear all skill slots.
     */
    public static void clear() {
        java.util.Arrays.fill(skillSlots, null);
    }
}
