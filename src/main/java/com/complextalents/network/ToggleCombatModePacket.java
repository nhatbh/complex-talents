package com.complextalents.network;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleCombatModePacket {

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
                player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                    boolean newState = talents.toggleCombatMode();

                    // Notify player
                    if (newState) {
                        player.sendSystemMessage(Component.literal("§6§lCombat Mode: §a§lENABLED"));
                        player.sendSystemMessage(Component.literal("§7Hotbar keys 1-4 now activate talents"));
                    } else {
                        player.sendSystemMessage(Component.literal("§6§lCombat Mode: §c§lDISABLED"));
                        player.sendSystemMessage(Component.literal("§7Hotbar keys 1-4 restored to normal"));
                    }

                    TalentsMod.LOGGER.debug("Player {} toggled Combat Mode to {}",
                        player.getName().getString(), newState);
                });
            }
        });
        context.setPacketHandled(true);
    }
}
