package com.complextalents.api.events;

import com.complextalents.TalentsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Base event for talent-related events.
 */
public abstract class TalentEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation talentId;

    public TalentEvent(ServerPlayer player, ResourceLocation talentId) {
        this.player = player;
        this.talentId = talentId;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getTalentId() {
        return talentId;
    }

    /**
     * Fired when a talent is unlocked or upgraded.
     * This event is not cancellable.
     */
    public static class TalentUnlocked extends TalentEvent {
        private final int level;

        public TalentUnlocked(ServerPlayer player, ResourceLocation talentId, int level) {
            super(player, talentId);
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * Fired when a talent is removed from a player.
     * This event is cancellable. If cancelled, the talent will not be removed.
     */
    public static class TalentRemoved extends TalentEvent {
        private boolean cancelled = false;

        public TalentRemoved(ServerPlayer player, ResourceLocation talentId) {
            super(player, talentId);
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
