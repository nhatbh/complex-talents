package com.complextalents.spellmastery.command;

import com.complextalents.spellmastery.capability.SpellMasteryDataProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Commands for managing player spell mastery and knowledge.
 * 
 * /mastery mastery <schoolId> <level> [targets]
 * /mastery learn <spellId> [targets]
 * /mastery forget <spellId> [targets]
 * /mastery info [target]
 */
public class SpellMasteryCommand {

    public static final int OP_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mastery")
                .requires(src -> src.hasPermission(OP_LEVEL))
                .then(MasterySubCommand.register())
                .then(LearnSubCommand.register())
                .then(ForgetSubCommand.register())
                .then(InfoSubCommand.register())
                .then(Commands.literal("gui")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            com.complextalents.dev.SimpleUIFactory.INSTANCE.open(player, com.complextalents.spellmastery.client.SpellMasteryUI.UI_ID);
                            return 1;
                        }))
        );
    }

    private static class MasterySubCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("mastery")
                    .then(Commands.argument("school", ResourceLocationArgument.id())
                            .then(Commands.argument("level", IntegerArgumentType.integer(0, 5))
                                    .executes(ctx -> setMastery(ctx, Collections.singleton(ctx.getSource().getPlayerOrException()), ResourceLocationArgument.getId(ctx, "school"), IntegerArgumentType.getInteger(ctx, "level")))
                                    .then(Commands.argument("target", EntityArgument.players())
                                            .executes(ctx -> setMastery(ctx, EntityArgument.getPlayers(ctx, "target"), ResourceLocationArgument.getId(ctx, "school"), IntegerArgumentType.getInteger(ctx, "level")))
                                    )
                            )
                    );
        }

        private static int setMastery(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, ResourceLocation schoolId, int level) {
            for (ServerPlayer player : targets) {
                player.getCapability(SpellMasteryDataProvider.MASTERY_DATA).ifPresent(data -> {
                    data.setMasteryLevel(schoolId, level);
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aSet " + schoolId + " mastery to " + level + " for " + player.getName().getString()), true);
                });
            }
            return targets.size();
        }
    }

    private static class LearnSubCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("learn")
                    .then(Commands.argument("spellId", ResourceLocationArgument.id())
                            .executes(ctx -> learnSpell(ctx, Collections.singleton(ctx.getSource().getPlayerOrException()), ResourceLocationArgument.getId(ctx, "spellId"), 1))
                            .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                                    .executes(ctx -> learnSpell(ctx, Collections.singleton(ctx.getSource().getPlayerOrException()), ResourceLocationArgument.getId(ctx, "spellId"), IntegerArgumentType.getInteger(ctx, "level")))
                                    .then(Commands.argument("target", EntityArgument.players())
                                            .executes(ctx -> learnSpell(ctx, EntityArgument.getPlayers(ctx, "target"), ResourceLocationArgument.getId(ctx, "spellId"), IntegerArgumentType.getInteger(ctx, "level")))
                                    )
                            )
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(ctx -> learnSpell(ctx, EntityArgument.getPlayers(ctx, "target"), ResourceLocationArgument.getId(ctx, "spellId"), 1))
                            )
                    );
        }

        private static int learnSpell(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, ResourceLocation spellId, int level) {
            for (ServerPlayer player : targets) {
                player.getCapability(SpellMasteryDataProvider.MASTERY_DATA).ifPresent(data -> {
                    data.learnSpell(spellId, level);
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aLearned spell " + spellId + " at lvl " + level + " for " + player.getName().getString()), true);
                });
            }
            return targets.size();
        }
    }

    private static class ForgetSubCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("forget")
                    .then(Commands.argument("spellId", ResourceLocationArgument.id())
                            .executes(ctx -> forgetSpell(ctx, Collections.singleton(ctx.getSource().getPlayerOrException()), ResourceLocationArgument.getId(ctx, "spellId")))
                            .then(Commands.argument("target", EntityArgument.players())
                                    .executes(ctx -> forgetSpell(ctx, EntityArgument.getPlayers(ctx, "target"), ResourceLocationArgument.getId(ctx, "spellId")))
                            )
                    );
        }

        private static int forgetSpell(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, ResourceLocation spellId) {
            for (ServerPlayer player : targets) {
                player.getCapability(SpellMasteryDataProvider.MASTERY_DATA).ifPresent(data -> {
                    data.forgetSpell(spellId);
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A76Forgot spell " + spellId + " for " + player.getName().getString()), true);
                });
            }
            return targets.size();
        }
    }

    private static class InfoSubCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("info")
                    .executes(ctx -> info(ctx, ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "target")))
                    );
        }

        private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
            player.getCapability(SpellMasteryDataProvider.MASTERY_DATA).ifPresent(data -> {
                ctx.getSource().sendSuccess(() -> Component.literal("\u00A7eSpell Mastery Info for " + player.getName().getString() + ":"), false);
                
                var masteryLevels = data.getAllMasteryLevels();
                if (!masteryLevels.isEmpty()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A76Mastery Levels:"), false);
                    masteryLevels.forEach((schoolId, level) -> {
                        ctx.getSource().sendSuccess(() -> Component.literal("  - " + schoolId + ": \u00A7b" + level), false);
                    });
                }

                Set<ResourceLocation> learned = data.getLearnedSpells();
                if (!learned.isEmpty()) {
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A76Learned Spells:"), false);
                    learned.forEach(spellId -> {
                        ctx.getSource().sendSuccess(() -> Component.literal("  - " + spellId), false);
                    });
                } else {
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A77No spells learned."), false);
                }
            });
            return 1;
        }
    }
}
