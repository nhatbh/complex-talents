package com.complextalents.command;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.talent.BranchingTalentBase;
import com.complextalents.talent.TalentBranches;
import com.complextalents.talent.Talent;
import com.complextalents.talent.TalentRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;

public class SelectBranchCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("talent")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("branch")
                    .then(Commands.literal("select")
                        .then(Commands.argument("player", EntityArgument.players())
                            .then(Commands.argument("talent", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    // Suggest all branching talents
                                    TalentRegistry.getAllTalents().stream()
                                        .filter(talent -> talent instanceof BranchingTalentBase)
                                        .map(talent -> talent.getId().toString())
                                        .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("rank", IntegerArgumentType.integer(1, 4))
                                    .then(Commands.argument("path", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest("a");
                                            builder.suggest("b");
                                            return builder.buildFuture();
                                        })
                                        .executes(SelectBranchCommand::selectBranch)
                                    )
                                )
                            )
                        )
                    )
                    .then(Commands.literal("view")
                        .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("talent", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    // Suggest all branching talents
                                    TalentRegistry.getAllTalents().stream()
                                        .filter(talent -> talent instanceof BranchingTalentBase)
                                        .map(talent -> talent.getId().toString())
                                        .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(SelectBranchCommand::viewBranches)
                            )
                        )
                    )
                )
        );
    }

    private static int selectBranch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        String talentIdStr = StringArgumentType.getString(context, "talent");
        int rank = IntegerArgumentType.getInteger(context, "rank");
        String pathStr = StringArgumentType.getString(context, "path").toLowerCase();

        ResourceLocation talentId = ResourceLocation.tryParse(talentIdStr);
        if (talentId == null) {
            context.getSource().sendFailure(Component.literal("Invalid talent ID: " + talentIdStr));
            return 0;
        }

        // Get the talent
        Talent talent = TalentRegistry.getTalent(talentId);
        if (talent == null) {
            context.getSource().sendFailure(Component.literal("Talent not found: " + talentId));
            return 0;
        }

        if (!(talent instanceof BranchingTalentBase branchingTalent)) {
            context.getSource().sendFailure(Component.literal("Talent does not support branching: " + talentId));
            return 0;
        }

        // Determine branch choice
        TalentBranches.BranchChoice choice;
        if (pathStr.equals("a")) {
            choice = TalentBranches.BranchChoice.PATH_A;
        } else if (pathStr.equals("b")) {
            choice = TalentBranches.BranchChoice.PATH_B;
        } else {
            context.getSource().sendFailure(Component.literal("Invalid path. Use 'a' or 'b'"));
            return 0;
        }

        // Apply to all specified players
        for (ServerPlayer player : players) {
            // Check if player has the talent at the required level
            player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                int level = talents.getTalentLevel(talentId);
                if (level < rank) {
                    context.getSource().sendFailure(Component.literal(
                        player.getName().getString() + " doesn't have " + talentId + " at rank " + rank
                    ));
                    return;
                }

                // Check if this rank has branching
                if (!branchingTalent.hasBranchingAtRank(rank)) {
                    context.getSource().sendFailure(Component.literal(
                        "Talent " + talentId + " doesn't have branching at rank " + rank
                    ));
                    return;
                }

                // Set the branch
                TalentBranches.setBranch(player, talentId, rank, choice);

                // Get branch name for feedback
                Component branchName = branchingTalent.getBranchName(rank, choice);

                context.getSource().sendSuccess(() -> Component.literal(
                    "Set " + player.getName().getString() + "'s " + talent.getName().getString() +
                    " rank " + rank + " to: " + branchName.getString()
                ), true);
            });
        }

        return players.size();
    }

    private static int viewBranches(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String talentIdStr = StringArgumentType.getString(context, "talent");

        ResourceLocation talentId = ResourceLocation.tryParse(talentIdStr);
        if (talentId == null) {
            context.getSource().sendFailure(Component.literal("Invalid talent ID: " + talentIdStr));
            return 0;
        }

        // Get the talent
        Talent talent = TalentRegistry.getTalent(talentId);
        if (talent == null) {
            context.getSource().sendFailure(Component.literal("Talent not found: " + talentId));
            return 0;
        }

        if (!(talent instanceof BranchingTalentBase branchingTalent)) {
            context.getSource().sendFailure(Component.literal("Talent does not support branching: " + talentId));
            return 0;
        }

        // Check talent level
        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS).map(talents -> {
            int level = talents.getTalentLevel(talentId);
            if (level == 0) {
                context.getSource().sendFailure(Component.literal(
                    player.getName().getString() + " doesn't have " + talent.getName().getString()
                ));
                return 0;
            }

            context.getSource().sendSuccess(() -> Component.literal(
                "=== " + player.getName().getString() + "'s " + talent.getName().getString() + " Branches ==="
            ), false);

            // Show each rank's branch selection
            for (int rank = 1; rank <= level; rank++) {
                if (branchingTalent.hasBranchingAtRank(rank)) {
                    TalentBranches.BranchChoice choice = TalentBranches.getBranch(player, talentId, rank);

                    Component status;
                    if (choice == TalentBranches.BranchChoice.NONE) {
                        status = Component.literal("Not selected").withStyle(style -> style.withColor(0xFFFF00));
                    } else {
                        Component branchName = branchingTalent.getBranchName(rank, choice);
                        status = Component.literal(branchName.getString()).withStyle(style -> style.withColor(0x00FF00));
                    }

                    final int finalRank = rank;
                    context.getSource().sendSuccess(() -> Component.literal(
                        "Rank " + finalRank + ": " + status.getString()
                    ), false);
                }
            }

            return 1;
        }).orElse(0);
    }
}