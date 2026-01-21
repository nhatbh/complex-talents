package com.complextalents.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ListTalentsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("listtalents")
                .requires(source -> source.hasPermission(2))
                .executes(context -> listPlayerTalents(context.getSource())));
    }

    private static int listPlayerTalents(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            player.getCapability(com.complextalents.capability.TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                if (talents.getUnlockedTalents().isEmpty()) {
                    source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no talents unlocked."), true);
                } else {
                    source.sendSuccess(() -> Component.literal(player.getName().getString() + "'s talents:"), true);
                    for (var talentId : talents.getUnlockedTalents()) {
                        int level = talents.getTalentLevel(talentId);
                        source.sendSuccess(() -> Component.literal("  - " + talentId + " (Level " + level + ")"), true);
                    }
                }
            });
        }
        return 1;
    }
}
