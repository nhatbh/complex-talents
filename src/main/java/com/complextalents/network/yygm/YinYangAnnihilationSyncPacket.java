package com.complextalents.network.yygm;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network packet for syncing Yin Yang Annihilation state from server to client.
 * <p>
 * Contains:
 * - Entity ID: Which entity has the Annihilation effect
 * - Player UUID: Which YYGM player's Annihilation state to sync
 * - Start tick: When Annihilation started (for steady spin animation)
 * - Expiration tick: When the effect expires
 * </p>
 */
public class YinYangAnnihilationSyncPacket {
    private final int entityId;
    private final UUID playerUuid;
    private final long startTick;
    private final long expirationTick;

    public YinYangAnnihilationSyncPacket(int entityId, UUID playerUuid, long startTick, long expirationTick) {
        this.entityId = entityId;
        this.playerUuid = playerUuid;
        this.startTick = startTick;
        this.expirationTick = expirationTick;
    }

    // Decode constructor
    public YinYangAnnihilationSyncPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.playerUuid = buffer.readUUID();
        this.startTick = buffer.readLong();
        this.expirationTick = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeUUID(playerUuid);
        buffer.writeLong(startTick);
        buffer.writeLong(expirationTick);
    }

    public static YinYangAnnihilationSyncPacket decode(FriendlyByteBuf buffer) {
        return new YinYangAnnihilationSyncPacket(buffer);
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

        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            // Store Annihilation data for client-side rendering
            ClientAnnihilationData.updateAnnihilationData(entityId, playerUuid, startTick, expirationTick);

            // Clear any Exposed data for this player/entity to prevent stacking
            ExposedStateSyncPacket.ClientExposedData.removePlayerExposedData(entityId, playerUuid);
        }
    }

    // Getters
    public int getEntityId() {
        return entityId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public long getStartTick() {
        return startTick;
    }

    public long getExpirationTick() {
        return expirationTick;
    }

    /**
     * Client-side storage for Annihilation state data.
     * Used by YinYangBaguaRenderer to display spinning Yin Yang texture.
     */
    public static class ClientAnnihilationData {
        private static final java.util.Map<Integer, java.util.Map<UUID, AnnihilationData>> ENTITY_ANNIHILATION = new java.util.HashMap<>();

        public static void updateAnnihilationData(int entityId, UUID playerUuid, long startTick, long expirationTick) {
            ENTITY_ANNIHILATION.computeIfAbsent(entityId, k -> new java.util.HashMap<>())
                .put(playerUuid, new AnnihilationData(startTick, expirationTick));
        }

        public static AnnihilationData getAnnihilationData(int entityId, UUID playerUuid) {
            java.util.Map<UUID, AnnihilationData> entityAnnihilation = ENTITY_ANNIHILATION.get(entityId);
            if (entityAnnihilation == null) {
                return null;
            }
            return entityAnnihilation.get(playerUuid);
        }

        public static void removeEntity(int entityId) {
            ENTITY_ANNIHILATION.remove(entityId);
        }

        public static void removePlayerAnnihilationData(int entityId, UUID playerUuid) {
            java.util.Map<UUID, AnnihilationData> entityAnnihilation = ENTITY_ANNIHILATION.get(entityId);
            if (entityAnnihilation != null) {
                entityAnnihilation.remove(playerUuid);
                if (entityAnnihilation.isEmpty()) {
                    ENTITY_ANNIHILATION.remove(entityId);
                }
            }
        }

        public static java.util.Set<UUID> getPlayersForEntity(int entityId) {
            java.util.Map<UUID, AnnihilationData> entityAnnihilation = ENTITY_ANNIHILATION.get(entityId);
            return entityAnnihilation != null ? entityAnnihilation.keySet() : java.util.Collections.emptySet();
        }

        public static boolean hasAnnihilation(int entityId) {
            return ENTITY_ANNIHILATION.containsKey(entityId) && !ENTITY_ANNIHILATION.get(entityId).isEmpty();
        }

        /**
         * Clean up expired Annihilation data.
         * Should be called periodically or on render tick.
         */
        public static void cleanupExpired(long currentTime) {
            ENTITY_ANNIHILATION.entrySet().removeIf(entityEntry -> {
                entityEntry.getValue().entrySet().removeIf(playerEntry -> {
                    if (currentTime >= playerEntry.getValue().getExpirationTick()) {
                        return true;
                    }
                    return false;
                });
                return entityEntry.getValue().isEmpty();
            });
        }
    }

    /**
     * Data class for Annihilation state.
     */
    public static class AnnihilationData {
        private final long startTick;
        private final long expirationTick;

        public AnnihilationData(long startTick, long expirationTick) {
            this.startTick = startTick;
            this.expirationTick = expirationTick;
        }

        public long getStartTick() {
            return startTick;
        }

        public long getExpirationTick() {
            return expirationTick;
        }
    }
}
