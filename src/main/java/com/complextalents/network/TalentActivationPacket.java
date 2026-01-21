package com.complextalents.network;

import com.complextalents.TalentsMod;
import com.complextalents.capability.PlayerTalents;
import com.complextalents.talent.Talent;
import com.complextalents.talent.TalentRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TalentActivationPacket {
    private final ResourceLocation talentId;

    public TalentActivationPacket(ResourceLocation talentId) {
        this.talentId = talentId;
    }

    public TalentActivationPacket(FriendlyByteBuf buffer) {
        this.talentId = buffer.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(talentId);
    }

    public static TalentActivationPacket decode(FriendlyByteBuf buffer) {
        return new TalentActivationPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(com.complextalents.capability.TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                    if (talents.hasTalent(talentId)) {
                        Talent talent = TalentRegistry.getTalent(talentId);
                        if (talent != null) {
                            int level = talents.getTalentLevel(talentId);
                            int cooldown = talents.getTalentCooldown(talentId);

                            if (cooldown <= 0) {
                                talent.onActivate(player, level);

                                // Set cooldown
                                if (talent instanceof com.complextalents.talent.ActiveTalent activeTalent) {
                                    talents.setTalentCooldown(talentId, activeTalent.getCooldownTicks());
                                } else if (talent instanceof com.complextalents.talent.HybridTalent hybridTalent) {
                                    talents.setTalentCooldown(talentId, hybridTalent.getCooldownTicks());
                                }

                                TalentsMod.LOGGER.debug("Player {} activated talent {} at level {}", player.getName().getString(), talentId, level);
                            }
                        }
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
}
