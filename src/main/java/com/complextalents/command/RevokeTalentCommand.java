package com.complextalents.command;

import com.complextalents.TalentsMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class RevokeTalentCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("revoketalent")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("target", EntityArgument.players())
                        .then(Commands.argument("talent", ResourceLocationArgument.id())
                                .executes(context -> revokeTalent(context.getSource(),
                                        EntityArgument.getPlayer(context, "target"),
                                        context.getArgument("talent", ResourceLocation.class))))));
    }

    private static int revokeTalent(CommandSourceStack source, ServerPlayer target, ResourceLocation talentId) {
        target.getCapability(com.complextalents.capability.TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            if (talents.hasTalent(talentId)) {
                talents.removeTalent(talentId);
                source.sendSuccess(() -> Component.literal("Revoked talent " + talentId + " from " + target.getName().getString()), true);
                TalentsMod.LOGGER.info("{} revoked talent {} from {}", source.getTextName(), talentId, target.getName().getString());
            } else {
                source.sendFailure(Component.literal(target.getName().getString() + " does not have talent " + talentId));
            }
        });
        return 1;
    }
}
