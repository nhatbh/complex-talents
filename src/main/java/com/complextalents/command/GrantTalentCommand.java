package com.complextalents.command;

import com.complextalents.TalentsMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class GrantTalentCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("granttalent")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("target", EntityArgument.players())
                        .then(Commands.argument("talent", ResourceLocationArgument.id())
                                .executes(context -> grantTalent(context.getSource(),
                                        EntityArgument.getPlayer(context, "target"),
                                        context.getArgument("talent", ResourceLocation.class),
                                        1))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> grantTalent(context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                context.getArgument("talent", ResourceLocation.class),
                                                IntegerArgumentType.getInteger(context, "level")))))));
    }

    private static int grantTalent(CommandSourceStack source, ServerPlayer target, ResourceLocation talentId, int level) {
        // If the namespace is "minecraft", assume user meant "complextalents"
        ResourceLocation finalTalentId;
        if (talentId.getNamespace().equals("minecraft")) {
            finalTalentId = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, talentId.getPath());
            ResourceLocation correctedId = finalTalentId;
            source.sendSuccess(() -> Component.literal("§e[Note] Assuming namespace 'complextalents'. Full ID: " + correctedId), false);
        } else {
            finalTalentId = talentId;
        }

        target.getCapability(com.complextalents.capability.TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            talents.unlockTalent(finalTalentId, level);
            source.sendSuccess(() -> Component.literal("Granted talent " + finalTalentId + " at level " + level + " to " + target.getName().getString()), true);
            target.sendSystemMessage(Component.literal("§a[Talent Granted] §e" + finalTalentId + " §7at level §f" + level));
            TalentsMod.LOGGER.info("{} granted talent {} at level {} to {}", source.getTextName(), finalTalentId, level, target.getName().getString());

            // Debug: Show talents after granting
            target.sendSystemMessage(Component.literal("§e[Debug] Total talents after grant: " + talents.getUnlockedTalents().size()));
        });
        return 1;
    }
}
