package com.complextalents.stats.command;

import com.complextalents.stats.StatType;
import com.complextalents.stats.capability.GeneralStatsDataProvider;
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
 * Command for managing player statistics.
 *
 * Commands:
 * /stats add_sp <amount> [player] - Add Skill Points
 * /stats set_rank <stat> <rank> [player] - Set stat rank
 * /stats reset [player] - Reset all stats
 * /stats info [player] - View stats info
 */
public class StatsCommand {

    public static final int OP_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stats")
                .requires(src -> src.hasPermission(OP_LEVEL))
                .then(AddSPCommand.register())
                .then(SetRankCommand.register())
                .then(ResetCommand.register())
                .then(InfoCommand.register())
        );
    }

    private static class AddSPCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("add_sp")
                    .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(ctx -> addSP(ctx, Collections.singleton(ctx.getSource().getPlayerOrException()), IntegerArgumentType.getInteger(ctx, "amount")))
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(ctx -> addSP(ctx, EntityArgument.getPlayers(ctx, "target"), IntegerArgumentType.getInteger(ctx, "amount")))
                            )
                    );
        }

        private static int addSP(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, int amount) {
            for (ServerPlayer player : targets) {
                player.getCapability(GeneralStatsDataProvider.STATS_DATA).ifPresent(data -> {
                    data.addSkillPoints(amount);
                    data.sync();
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aAdded " + amount + " SP to " + player.getName().getString()), true);
                });
            }
            return targets.size();
        }
    }

    private static class SetRankCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            var statArg = Commands.literal("set_rank");
            for (StatType type : StatType.values()) {
                statArg.then(Commands.literal(type.name().toLowerCase())
                        .then(Commands.argument("rank", IntegerArgumentType.integer(0, 50))
                                .executes(ctx -> setRank(ctx, Collections.singleton(ctx.getSource().getPlayerOrException()), type, IntegerArgumentType.getInteger(ctx, "rank")))
                                .then(Commands.argument("target", EntityArgument.players())
                                        .executes(ctx -> setRank(ctx, EntityArgument.getPlayers(ctx, "target"), type, IntegerArgumentType.getInteger(ctx, "rank")))
                                )
                        )
                );
            }
            return statArg;
        }

        private static int setRank(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, StatType type, int rank) {
            for (ServerPlayer player : targets) {
                player.getCapability(GeneralStatsDataProvider.STATS_DATA).ifPresent(data -> {
                    data.setStatRank(type, rank);
                    data.sync();
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aSet " + type.name() + " rank to " + rank + " for " + player.getName().getString()), true);
                });
            }
            return targets.size();
        }
    }

    private static class ResetCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("reset")
                    .executes(ctx -> reset(ctx, Collections.singleton(ctx.getSource().getPlayerOrException())))
                    .then(Commands.argument("target", EntityArgument.players())
                            .executes(ctx -> reset(ctx, EntityArgument.getPlayers(ctx, "target")))
                    );
        }

        private static int reset(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
            for (ServerPlayer player : targets) {
                player.getCapability(GeneralStatsDataProvider.STATS_DATA).ifPresent(data -> {
                    for (StatType type : StatType.values()) {
                        data.setStatRank(type, 0);
                    }
                    data.setSkillPoints(0);
                    data.sync();
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A76Reset stats and SP for " + player.getName().getString()), true);
                });
            }
            return targets.size();
        }
    }

    private static class InfoCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("info")
                    .executes(ctx -> info(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "target")))
                    );
        }

        private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
            player.getCapability(GeneralStatsDataProvider.STATS_DATA).ifPresent(data -> {
                ctx.getSource().sendSuccess(() -> Component.literal("\u00A7eStats for " + player.getName().getString() + ":"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("  Available SP: \u00A7b" + data.getSkillPoints()), false);
                for (StatType type : StatType.values()) {
                    int rank = data.getStatRank(type);
                    if (rank > 0) {
                        ctx.getSource().sendSuccess(() -> Component.literal("  - " + type.name() + ": \u00A7aRank " + rank), false);
                    }
                }
            });
            return 1;
        }
    }
}
