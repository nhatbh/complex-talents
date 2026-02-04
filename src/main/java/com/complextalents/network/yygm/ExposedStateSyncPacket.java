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
 * Network packet for syncing Exposed (Eight Formation Battle Array) state from server to client.
 * <p>
 * Contains:
 * - Entity ID: Which entity has the Exposed effect
 * - Player UUID: Which YYGM player's Exposed state to sync
 * - Gate pattern: 8-bit bitmap of gate types (1=Yang, 0=Yin per direction)
 * - Completed gates: 8-bit bitmap of completed gates
 * - Next required: Which gate type player needs to hit next (0=Yang, 1=Yin)
 * - Expiration tick: When the effect expires (0 = remove)
 * </p>
 */
public class ExposedStateSyncPacket {
    private final int entityId;
    private final UUID playerUuid;
    private final int gatePattern;
    private final int completedGates;
    private final int nextRequired;
    private final long expirationTick;

    public ExposedStateSyncPacket(int entityId, UUID playerUuid, int gatePattern, int completedGates, int nextRequired, long expirationTick) {
        this.entityId = entityId;
        this.playerUuid = playerUuid;
        this.gatePattern = gatePattern;
        this.completedGates = completedGates;
        this.nextRequired = nextRequired;
        this.expirationTick = expirationTick;
    }

    // Decode constructor
    public ExposedStateSyncPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.playerUuid = buffer.readUUID();
        this.gatePattern = buffer.readInt();
        this.completedGates = buffer.readInt();
        this.nextRequired = buffer.readInt();
        this.expirationTick = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeUUID(playerUuid);
        buffer.writeInt(gatePattern);
        buffer.writeInt(completedGates);
        buffer.writeInt(nextRequired);
        buffer.writeLong(expirationTick);
    }

    public static ExposedStateSyncPacket decode(FriendlyByteBuf buffer) {
        return new ExposedStateSyncPacket(buffer);
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

        TalentsMod.LOGGER.debug("Received ExposedStateSyncPacket for entity {}, pattern: 0x{}, completed: 0x{}, nextRequired: {}, expires: {}",
                entityId, Integer.toHexString(gatePattern), Integer.toHexString(completedGates), nextRequired, expirationTick);

        // Special case: expirationTick = 0 means remove this player's Exposed data
        if (expirationTick == 0 && gatePattern == 0) {
            ClientExposedData.removePlayerExposedData(entityId, playerUuid);
            TalentsMod.LOGGER.debug("Removed Exposed data for player {} from entity {}", playerUuid, entityId);
            return;
        }

        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            // Store Exposed data for client-side rendering
            ClientExposedData.updateExposedData(entityId, playerUuid, gatePattern, completedGates, nextRequired, expirationTick);

            // Clear any Harmonized data for this player/entity to prevent stacking
            // This ensures that when Exposed is applied to a Harmonized target, only Exposed gates render
            YinYangGateStateSyncPacket.ClientGateData.removePlayerGateData(entityId, playerUuid);
        }
    }

    // Getters
    public int getEntityId() {
        return entityId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getGatePattern() {
        return gatePattern;
    }

    public int getCompletedGates() {
        return completedGates;
    }

    public int getNextRequired() {
        return nextRequired;
    }

    public long getExpirationTick() {
        return expirationTick;
    }

    /**
     * Client-side storage for Exposed state data.
     * Used by YinYangBaguaRenderer to display Exposed gates.
     */
    public static class ClientExposedData {
        private static final java.util.Map<Integer, java.util.Map<UUID, ExposedData>> ENTITY_EXPOSED = new java.util.HashMap<>();

        public static void updateExposedData(int entityId, UUID playerUuid, int gatePattern,
                                             int completedGates, int nextRequired, long expirationTick) {
            ENTITY_EXPOSED.computeIfAbsent(entityId, k -> new java.util.HashMap<>())
                .put(playerUuid, new ExposedData(gatePattern, completedGates, nextRequired, expirationTick));
        }

        public static ExposedData getExposedData(int entityId, UUID playerUuid) {
            java.util.Map<UUID, ExposedData> entityExposed = ENTITY_EXPOSED.get(entityId);
            if (entityExposed == null) {
                return null;
            }
            return entityExposed.get(playerUuid);
        }

        public static void removeEntity(int entityId) {
            ENTITY_EXPOSED.remove(entityId);
        }

        public static void removePlayerExposedData(int entityId, UUID playerUuid) {
            java.util.Map<UUID, ExposedData> entityExposed = ENTITY_EXPOSED.get(entityId);
            if (entityExposed != null) {
                entityExposed.remove(playerUuid);
                if (entityExposed.isEmpty()) {
                    ENTITY_EXPOSED.remove(entityId);
                }
            }
        }

        public static java.util.Set<UUID> getPlayersForEntity(int entityId) {
            java.util.Map<UUID, ExposedData> entityExposed = ENTITY_EXPOSED.get(entityId);
            return entityExposed != null ? entityExposed.keySet() : java.util.Collections.emptySet();
        }

        public static boolean hasExposed(int entityId) {
            return ENTITY_EXPOSED.containsKey(entityId) && !ENTITY_EXPOSED.get(entityId).isEmpty();
        }
    }

    /**
     * Data class for Exposed state.
     */
    public static class ExposedData {
        private final int gatePattern;
        private final int completedGates;
        private final int nextRequired;
        private final long expirationTick;

        public ExposedData(int gatePattern, int completedGates, int nextRequired, long expirationTick) {
            this.gatePattern = gatePattern;
            this.completedGates = completedGates;
            this.nextRequired = nextRequired;
            this.expirationTick = expirationTick;
        }

        public int getGatePattern() {
            return gatePattern;
        }

        public int getCompletedGates() {
            return completedGates;
        }

        public int getNextRequired() {
            return nextRequired;
        }

        public long getExpirationTick() {
            return expirationTick;
        }

        /**
         * Get the gate type at a specific direction.
         * @return 0 for Yang (bit set), 1 for Yin (bit not set)
         */
        public int getGateTypeAtDirection(int direction) {
            return (gatePattern & (1 << direction)) != 0 ? 0 : 1; // 0 = GATE_YANG, 1 = GATE_YIN
        }

        /**
         * Check if a gate at the given direction is completed.
         */
        public boolean isGateCompleted(int direction) {
            return (completedGates & (1 << direction)) != 0;
        }
    }
}
