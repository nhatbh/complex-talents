package com.complextalents.leveling.command;

import com.complextalents.leveling.data.PlayerLevelingData;
import com.complextalents.leveling.handlers.LevelingSyncHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;

/**
 * Commands for managing player leveling system (XP and SP).
 *
 * /level xp add <amount> [targets]
 * /level sp add <amount> [targets]
 */
public class LevelingCommand {

    public static final int OP_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("level")
                .requires(src -> src.hasPermission(OP_LEVEL))
                .then(XPCommand.register())
                .then(SPCommand.register())
        );
    }

    private static class XPCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("xp")
                    .then(Commands.literal("add")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                    .executes(ctx -> addXP(ctx, Collections.singleton(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
                                    .then(Commands.argument("target", EntityArgument.players())
                                            .executes(ctx -> addXP(ctx, EntityArgument.getPlayers(ctx, "target"), IntegerArgumentType.getInteger(ctx, "amount")))
                                    )
                            )
                    );
        }

        private static int addXP(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, int amount) {
            for (ServerPlayer player : targets) {
                PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
                data.addXP(player.getUUID(), amount);
                LevelingSyncHandler.syncPlayerLevelData(player);
                ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aAdded " + amount + " XP to " + player.getName().getString()), true);
            }
            return targets.size();
        }
    }

    private static class SPCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("sp")
                    .then(Commands.literal("add")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                    .executes(ctx -> addSP(ctx, Collections.singleton(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
                                    .then(Commands.argument("target", EntityArgument.players())
                                            .executes(ctx -> addSP(ctx, EntityArgument.getPlayers(ctx, "target"), IntegerArgumentType.getInteger(ctx, "amount")))
                                    )
                            )
                    );
        }

        private static int addSP(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, int amount) {
            for (ServerPlayer player : targets) {
                PlayerLevelingData data = PlayerLevelingData.get(player.serverLevel());
                data.addSkillPoints(player.getUUID(), amount);
                LevelingSyncHandler.syncPlayerLevelData(player);
                ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aAdded " + amount + " Skill Points to " + player.getName().getString()), true);
            }
            return targets.size();
        }
    }
}
