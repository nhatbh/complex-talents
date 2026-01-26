package com.complextalents.network;

import com.complextalents.TalentsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleCombatModePacket {
    private static boolean combatModeEnabled = false;

    public ToggleCombatModePacket() {
        // Empty constructor - no data needed
    }

    public ToggleCombatModePacket(FriendlyByteBuf buffer) {
        // No data to read
    }

    public void encode(FriendlyByteBuf buffer) {
        // No data to write
    }

    public static ToggleCombatModePacket decode(FriendlyByteBuf buffer) {
        return new ToggleCombatModePacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Simple toggle without talent system
                combatModeEnabled = !combatModeEnabled;

                // Notify player
                if (combatModeEnabled) {
                    player.sendSystemMessage(Component.literal("§6§lCombat Mode: §a§lENABLED"));
                } else {
                    player.sendSystemMessage(Component.literal("§6§lCombat Mode: §c§lDISABLED"));
                }

                TalentsMod.LOGGER.debug("Player {} toggled Combat Mode to {}",
                    player.getName().getString(), combatModeEnabled);
            }
        });
        context.setPacketHandled(true);
    }
}