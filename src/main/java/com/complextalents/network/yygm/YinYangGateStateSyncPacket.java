package com.complextalents.network.yygm;

import com.complextalents.TalentsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network packet for syncing Yin Yang Grandmaster gate state from server to client.
 * <p>
 * Contains:
 * - Entity ID: Which entity has the gates
 * - Player UUID: Which YYGM player's gates to sync
 * - Yang gate: Compass direction of Yang gate (-1 if none/respawning)
 * - Yin gate: Compass direction of Yin gate (-1 if none/respawning)
 * - Next required: 0 = Yang, 1 = Yin (which gate to strike next)
 * - Cooldown end: Tick when initial cooldown ends
 * - Yang respawn tick: When the Yang gate will respawn
 * - Yin respawn tick: When the Yin gate will respawn
 * - Used slots bitmap: Bitmap of which compass slots have been used (for debugging/visual feedback)
 * </p>
 * <p>
 * Note: Charges (yang/yin) are synced via PassiveStackSyncPacket, not this packet.
 * </p>
 */
public class YinYangGateStateSyncPacket {
    private final int entityId;
    private final UUID playerUuid;
    private final int yangGate;
    private final int yinGate;
    private final int nextRequired;
    private final long cooldownEnd;
    private final long yangRespawnTick;
    private final long yinRespawnTick;
    private final int usedSlotsBitmap;

    public YinYangGateStateSyncPacket(int entityId, UUID playerUuid, int yangGate, int yinGate,
                                       int nextRequired, long cooldownEnd,
                                       long yangRespawnTick, long yinRespawnTick) {
        this(entityId, playerUuid, yangGate, yinGate, nextRequired, cooldownEnd,
             yangRespawnTick, yinRespawnTick, 0);
    }

    public YinYangGateStateSyncPacket(int entityId, UUID playerUuid, int yangGate, int yinGate,
                                       int nextRequired, long cooldownEnd,
                                       long yangRespawnTick, long yinRespawnTick,
                                       int usedSlotsBitmap) {
        this.entityId = entityId;
        this.playerUuid = playerUuid;
        this.yangGate = yangGate;
        this.yinGate = yinGate;
        this.nextRequired = nextRequired;
        this.cooldownEnd = cooldownEnd;
        this.yangRespawnTick = yangRespawnTick;
        this.yinRespawnTick = yinRespawnTick;
        this.usedSlotsBitmap = usedSlotsBitmap;
    }

    // Decode constructor
    public YinYangGateStateSyncPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.playerUuid = buffer.readUUID();
        this.yangGate = buffer.readInt();
        this.yinGate = buffer.readInt();
        this.nextRequired = buffer.readInt();
        this.cooldownEnd = buffer.readLong();
        this.yangRespawnTick = buffer.readLong();
        this.yinRespawnTick = buffer.readLong();
        this.usedSlotsBitmap = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeUUID(playerUuid);
        buffer.writeInt(yangGate);
        buffer.writeInt(yinGate);
        buffer.writeInt(nextRequired);
        buffer.writeLong(cooldownEnd);
        buffer.writeLong(yangRespawnTick);
        buffer.writeLong(yinRespawnTick);
        buffer.writeInt(usedSlotsBitmap);
    }

    public static YinYangGateStateSyncPacket decode(FriendlyByteBuf buffer) {
        return new YinYangGateStateSyncPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient());
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level.getEntity(entityId);

        TalentsMod.LOGGER.debug("Received YinYangGateStateSyncPacket for entity {}, yang: {}, yin: {}, next: {}, usedSlots: {}",
                entityId, yangGate, yinGate, nextRequired, usedSlotsBitmap);

        // Special case: yangGate = -2 means remove this player's gate data
        if (yangGate == -2) {
            ClientGateData.removePlayerGateData(entityId, playerUuid);
            TalentsMod.LOGGER.debug("Removed YYGM gate data for player {} from entity {}", playerUuid, entityId);
            return;
        }

        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            // Store gate data for client-side rendering
            ClientGateData.updateGateData(entityId, playerUuid, yangGate, yinGate,
                    nextRequired, cooldownEnd, yangRespawnTick, yinRespawnTick, usedSlotsBitmap);
            // Also update the next required for HUD
            ClientGateData.setNextRequired(nextRequired);
        }
    }

    // Getters
    public int getEntityId() {
        return entityId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getYangGate() {
        return yangGate;
    }

    public int getYinGate() {
        return yinGate;
    }

    public int getNextRequired() {
        return nextRequired;
    }

    public long getCooldownEnd() {
        return cooldownEnd;
    }

    public long getYangRespawnTick() {
        return yangRespawnTick;
    }

    public long getYinRespawnTick() {
        return yinRespawnTick;
    }

    /**
     * Client-side storage for YYGM gate data.
     * Used by YinYangBaguaRenderer to display gates and YinYangRenderer for HUD.
     */
    public static class ClientGateData {
        private static final java.util.Map<Integer, java.util.Map<UUID, YYGateData>> ENTITY_GATES = new java.util.HashMap<>();
        private static Integer globalNextRequired = null; // For HUD display

        public static void updateGateData(int entityId, UUID playerUuid, int yangGate, int yinGate,
                                           int nextRequired, long cooldownEnd,
                                           long yangRespawnTick, long yinRespawnTick,
                                           int usedSlotsBitmap) {
            ENTITY_GATES.computeIfAbsent(entityId, k -> new java.util.HashMap<>())
                .put(playerUuid, new YYGateData(yangGate, yinGate, nextRequired,
                        cooldownEnd, yangRespawnTick, yinRespawnTick, usedSlotsBitmap));
        }

        // Legacy method for backward compatibility
        public static void updateGateData(int entityId, UUID playerUuid, int yangGate, int yinGate,
                                           int nextRequired, long cooldownEnd,
                                           long yangRespawnTick, long yinRespawnTick) {
            updateGateData(entityId, playerUuid, yangGate, yinGate, nextRequired,
                    cooldownEnd, yangRespawnTick, yinRespawnTick, 0);
        }

        public static YYGateData getGateData(int entityId, UUID playerUuid) {
            java.util.Map<UUID, YYGateData> entityGates = ENTITY_GATES.get(entityId);
            if (entityGates == null) {
                return null;
            }
            return entityGates.get(playerUuid);
        }

        public static void removeEntity(int entityId) {
            ENTITY_GATES.remove(entityId);
        }

        public static void removePlayerGateData(int entityId, UUID playerUuid) {
            java.util.Map<UUID, YYGateData> entityGates = ENTITY_GATES.get(entityId);
            if (entityGates != null) {
                entityGates.remove(playerUuid);
                if (entityGates.isEmpty()) {
                    ENTITY_GATES.remove(entityId);
                }
            }
        }

        public static java.util.Set<UUID> getPlayersForEntity(int entityId) {
            java.util.Map<UUID, YYGateData> entityGates = ENTITY_GATES.get(entityId);
            return entityGates != null ? entityGates.keySet() : java.util.Collections.emptySet();
        }

        public static boolean hasGates(int entityId) {
            return ENTITY_GATES.containsKey(entityId) && !ENTITY_GATES.get(entityId).isEmpty();
        }

        public static void setNextRequired(int nextRequired) {
            globalNextRequired = nextRequired;
        }

        public static Integer getNextRequired() {
            return globalNextRequired;
        }
    }

    /**
     * Data class for YYGM gate state.
     */
    public static class YYGateData {
        private final int yangGate;
        private final int yinGate;
        private final int nextRequired;
        private final long cooldownEnd;
        private final long yangRespawnTick;
        private final long yinRespawnTick;
        private final int usedSlotsBitmap;

        public YYGateData(int yangGate, int yinGate, int nextRequired, long cooldownEnd,
                          long yangRespawnTick, long yinRespawnTick, int usedSlotsBitmap) {
            this.yangGate = yangGate;
            this.yinGate = yinGate;
            this.nextRequired = nextRequired;
            this.cooldownEnd = cooldownEnd;
            this.yangRespawnTick = yangRespawnTick;
            this.yinRespawnTick = yinRespawnTick;
            this.usedSlotsBitmap = usedSlotsBitmap;
        }

        // Legacy constructor for backward compatibility
        public YYGateData(int yangGate, int yinGate, int nextRequired, long cooldownEnd,
                          long yangRespawnTick, long yinRespawnTick) {
            this(yangGate, yinGate, nextRequired, cooldownEnd, yangRespawnTick, yinRespawnTick, 0);
        }

        public int getYangGate() {
            return yangGate;
        }

        public int getYinGate() {
            return yinGate;
        }

        public int getNextRequired() {
            return nextRequired;
        }

        public long getCooldownEnd() {
            return cooldownEnd;
        }

        public long getYangRespawnTick() {
            return yangRespawnTick;
        }

        public long getYinRespawnTick() {
            return yinRespawnTick;
        }

        public int getUsedSlotsBitmap() {
            return usedSlotsBitmap;
        }

        public boolean isYangGateActive() {
            return yangGate >= 0 && yangGate < 8;
        }

        public boolean isYinGateActive() {
            return yinGate >= 0 && yinGate < 8;
        }
    }
}
