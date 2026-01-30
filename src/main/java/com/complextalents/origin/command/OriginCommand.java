package com.complextalents.origin.command;

import com.complextalents.origin.Origin;
import com.complextalents.origin.OriginManager;
import com.complextalents.origin.OriginRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command for managing player origins.
 *
 * Commands:
 * /origin set <originId> [level] - Set active origin with optional level
 * /origin clear - Clear active origin
 * /origin get - Show current origin and resource
 * /origin list - List all available origins
 */
public class OriginCommand {

    public static final int OP_LEVEL = 2; // OP level 2 (default for cheat/game rules)

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("origin")
                .requires(src -> src.hasPermission(OP_LEVEL))
                .then(SetCommand.register())
                .then(ClearCommand.register())
                .then(GetCommand.register())
                .then(ListCommand.register())
        );
    }

    public static class SetCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("set")
                    .then(Commands.argument("originId", ResourceLocationArgument.id())
                            .executes(ctx -> setOrigin(
                                    ctx.getSource(),
                                    ResourceLocationArgument.getId(ctx, "originId"),
                                    1 // Default level
                            ))
                            .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                    .executes(ctx -> setOrigin(
                                            ctx.getSource(),
                                            ResourceLocationArgument.getId(ctx, "originId"),
                                            IntegerArgumentType.getInteger(ctx, "level")
                                    ))
                            )
                    );
        }

        private static int setOrigin(CommandSourceStack src, ResourceLocation originId, int level) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Origin origin = OriginRegistry.getInstance().getOrigin(originId);
            if (origin == null) {
                src.sendFailure(Component.literal("Unknown origin: " + originId));
                return 0;
            }

            if (level > origin.getMaxLevel()) {
                src.sendFailure(Component.literal("Level " + level + " exceeds max level " +
                    origin.getMaxLevel() + " for " + origin.getDisplayName().getString()));
                return 0;
            }

            OriginManager.setOrigin(player, originId, level);
            String levelText = level > 1 ? " at level " + level : "";
            src.sendSuccess(() -> Component.literal("Origin set to " + origin.getDisplayName().getString() +
                    levelText), true);

            return 1;
        }
    }

    public static class ClearCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("clear")
                    .executes(ctx -> clearOrigin(ctx.getSource()));
        }

        private static int clearOrigin(CommandSourceStack src) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Origin oldOrigin = OriginManager.getOrigin(player);
            if (oldOrigin == null) {
                src.sendFailure(Component.literal("You don't have an active origin"));
                return 0;
            }

            OriginManager.clearOrigin(player);
            src.sendSuccess(() -> Component.literal("Origin cleared"), true);

            return 1;
        }
    }

    public static class GetCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("get")
                    .executes(ctx -> showOrigin(ctx.getSource()));
        }

        private static int showOrigin(CommandSourceStack src) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Origin origin = OriginManager.getOrigin(player);
            if (origin == null) {
                src.sendSuccess(() -> Component.literal("\u00A77No active origin"), false);
                return 1;
            }

            int level = OriginManager.getOriginLevel(player);
            double resource = OriginManager.getResource(player);
            double maxResource = origin.getResourceType() != null ? origin.getResourceType().getMax() : 0;

            src.sendSuccess(() -> Component.literal("\u00A7eOrigin: \u00A7a" + origin.getDisplayName().getString()), false);
            src.sendSuccess(() -> Component.literal("  Level: \u00A7b" + level + "/" + origin.getMaxLevel()), false);
            if (origin.getResourceType() != null) {
                src.sendSuccess(() -> Component.literal("  Resource: \u00A7f" + origin.getResourceType().getName() +
                        ": \u00A7b" + String.format("%.0f", resource) + "/" + String.format("%.0f", maxResource)), false);
            }

            return 1;
        }
    }

    public static class ListCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("list")
                    .executes(ctx -> listOrigins(ctx.getSource()));
        }

        private static int listOrigins(CommandSourceStack src) {
            var origins = OriginRegistry.getInstance().getAllOrigins();
            src.sendSuccess(() -> Component.literal("\u00A7eAvailable Origins (" + origins.size() + "):"), false);

            for (Origin origin : origins) {
                String resourceInfo = origin.getResourceType() != null ?
                        " \u00A7b[" + origin.getResourceType().getName() + "]" : "";
                String levelInfo = origin.getMaxLevel() > 1 ?
                        " \u00A7b[Max Lvl: " + origin.getMaxLevel() + "]" : "";
                src.sendSuccess(() -> Component.literal("  - " + origin.getId() + resourceInfo + levelInfo), false);
            }

            return 1;
        }
    }
}
