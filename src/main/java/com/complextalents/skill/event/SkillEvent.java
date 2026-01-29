package com.complextalents.skill.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Base event for all skill-related events.
 * Provides common fields and methods for skill execution.
 */
public abstract class SkillEvent extends Event {

    protected final ServerPlayer player;
    protected final ResourceLocation skillId;

    /**
     * Create a new skill event.
     *
     * @param player The player involved in this event
     * @param skillId The skill ID for this event
     */
    protected SkillEvent(ServerPlayer player, ResourceLocation skillId) {
        this.player = player;
        this.skillId = skillId;
    }

    /**
     * @return The player involved in this event
     */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * @return The skill ID for this event
     */
    public ResourceLocation getSkillId() {
        return skillId;
    }
}
