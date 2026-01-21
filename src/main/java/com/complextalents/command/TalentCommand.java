package com.complextalents.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TalentCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("talent")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("list")
                        .executes(context -> listTalents(context.getSource()))));

        dispatcher.register(Commands.literal("talent")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("info")
                        .then(Commands.argument("talent", ResourceLocationArgument.id())
                                .executes(context -> talentInfo(context.getSource(),
                                        context.getArgument("talent", ResourceLocation.class))))));
    }

    private static int listTalents(CommandSourceStack source) {
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            player.getCapability(com.complextalents.capability.TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                if (talents.getUnlockedTalents().isEmpty()) {
                    source.sendSuccess(() -> Component.literal("You have no talents unlocked."), true);
                } else {
                    source.sendSuccess(() -> Component.literal("Your talents:"), true);
                    for (ResourceLocation talentId : talents.getUnlockedTalents()) {
                        int level = talents.getTalentLevel(talentId);
                        source.sendSuccess(() -> Component.literal("  - " + talentId + " (Level " + level + ")"), true);
                    }
                }
            });
        }
        return 1;
    }

    private static int talentInfo(CommandSourceStack source, ResourceLocation talentId) {
        // Implementation to show talent info
        source.sendSuccess(() -> Component.literal("Talent: " + talentId), true);
        return 1;
    }
}
