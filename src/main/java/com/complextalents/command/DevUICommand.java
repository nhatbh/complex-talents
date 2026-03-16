package com.complextalents.command;

import com.complextalents.dev.OriginSkillSelectionUI;
import com.complextalents.dev.SimpleUIFactory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to open the developer UI.
 */
public class DevUICommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("talents")
                .requires(source -> source.hasPermission(2))
                .executes(DevUICommand::openUI)
        );
    }

    private static int openUI(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            SimpleUIFactory.INSTANCE.open(player, OriginSkillSelectionUI.UI_ID);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Failed to open UI: " + e.getMessage()));
            return 0;
        }
    }
}
