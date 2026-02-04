package com.complextalents.skill.command;

import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.capability.IPlayerSkillData;
import com.complextalents.skill.capability.SkillDataProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command for managing skill assignments.
 *
 * Commands:
 * /skill assign <slot> <skillId> - Assign skill to slot
 * /skill clear <slot> - Clear a slot
 * /skill slots - View current assignments
 * /skill list - List all available skills
 * /skill toggle <slot> - Toggle a toggle skill
 * /skill resetcd [slot] - Reset cooldown for a slot or all slots
 */
public class SkillCommand {

    public static final int OP_LEVEL = 2; // OP level 2 (default for cheat/game rules)

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skill")
                .requires(src -> src.hasPermission(OP_LEVEL))
                .then(AssignCommand.register())
                .then(ClearCommand.register())
                .then(SlotsCommand.register())
                .then(ListCommand.register())
                .then(ToggleCommand.register())
                .then(ResetCooldownCommand.register())
        );
    }

    public static class AssignCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("assign")
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                            .then(Commands.argument("skillId", ResourceLocationArgument.id())
                                    .executes(ctx -> assignSkill(
                                            ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "slot") - 1,
                                            ResourceLocationArgument.getId(ctx, "skillId"),
                                            1 // Default level
                                    ))
                                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                            .executes(ctx -> assignSkill(
                                                    ctx.getSource(),
                                                    IntegerArgumentType.getInteger(ctx, "slot") - 1,
                                                    ResourceLocationArgument.getId(ctx, "skillId"),
                                                    IntegerArgumentType.getInteger(ctx, "level")
                                            ))
                                    )
                            )
                    );
        }

        private static int assignSkill(CommandSourceStack src, int slotIndex, ResourceLocation skillId, int level) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            Skill skill = SkillRegistry.getInstance().getSkill(skillId);
            if (skill == null) {
                src.sendFailure(Component.literal("Unknown skill: " + skillId));
                return 0;
            }

            if (level > skill.getMaxLevel()) {
                src.sendFailure(Component.literal("Level " + level + " exceeds max level " +
                    skill.getMaxLevel() + " for " + skill.getDisplayName().getString()));
                return 0;
            }

            player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                data.setSkillInSlot(slotIndex, skillId);
                data.setSkillLevel(skillId, level);
                String levelText = level > 1 ? " (level " + level + ")" : "";
                src.sendSuccess(() -> Component.literal("Assigned " + skill.getDisplayName().getString() +
                        levelText + " to slot " + (slotIndex + 1)), true);
            });

            return 1;
        }
    }

    public static class ClearCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("clear")
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                            .executes(ctx -> clearSlot(
                                    ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "slot") - 1
                            ))
                    );
        }

        private static int clearSlot(CommandSourceStack src, int slotIndex) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                data.clearSlot(slotIndex);
                src.sendSuccess(() -> Component.literal("Cleared slot " + (slotIndex + 1)), true);
            });

            return 1;
        }
    }

    public static class SlotsCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("slots")
                    .executes(ctx -> showSlots(ctx.getSource()));
        }

        private static int showSlots(CommandSourceStack src) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                src.sendSuccess(() -> Component.literal("\u00A7eSkill Assignments:"), false);
                for (int slot = 0; slot < IPlayerSkillData.SLOT_COUNT; slot++) {
                    ResourceLocation skillId = data.getSkillInSlot(slot);
                    if (skillId != null) {
                        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                        String name = skill != null ? skill.getDisplayName().getString() : skillId.toString();
                        String levelText = (skill != null && skill.getMaxLevel() > 1) ?
                                " \u00A7b[Lvl " + data.getSkillLevel(skillId) + "/" + skill.getMaxLevel() + "]" : "";
                        String toggleInfo = (skill != null && skill.isToggleable() &&
                                data.isToggleActive(skillId)) ? " \u00A7a[ON]" : "";
                        final int slotNum = slot;
                        src.sendSuccess(() -> Component.literal("  Slot " + (slotNum + 1) + ": \u00A7a" + name + levelText + toggleInfo), false);
                    } else {
                        final int slotNum = slot;
                        src.sendSuccess(() -> Component.literal("  Slot " + (slotNum + 1) + ": \u00A77(empty)"), false);
                    }
                }
            });

            return 1;
        }
    }

    public static class ListCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("list")
                    .executes(ctx -> listSkills(ctx.getSource()));
        }

        private static int listSkills(CommandSourceStack src) {
            var skills = SkillRegistry.getInstance().getAllSkills();
            src.sendSuccess(() -> Component.literal("\u00A7eAvailable Skills (" + skills.size() + "):"), false);

            for (Skill skill : skills) {
                String nature = skill.getNature().name();
                String toggle = skill.isToggleable() ? " \u00A7b[TOGGLE]" : "";
                String levelInfo = skill.getMaxLevel() > 1 ?
                        " \u00A7b[Max Lvl: " + skill.getMaxLevel() + "]" : "";
                String cooldown = skill.getActiveCooldown() > 0 ?
                        String.format(" \u00A77[%.1fs CD]", skill.getActiveCooldown()) : "";
                String cost = skill.getResourceCost() > 0 ?
                        String.format(" \u00A7b[%.0f %s]", skill.getResourceCost(),
                                skill.getResourceType() != null ? skill.getResourceType().getPath() : "resource") : "";
                src.sendSuccess(() -> Component.literal("  - " + skill.getId() + toggle + cooldown + cost + levelInfo +
                        " \u00A77(" + nature + ")"), false);
            }

            return 1;
        }
    }

    public static class ToggleCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("toggle")
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                            .executes(ctx -> toggleSkill(
                                    ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "slot") - 1
                            ))
                    );
        }

        private static int toggleSkill(CommandSourceStack src, int slotIndex) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                ResourceLocation skillId = data.getSkillInSlot(slotIndex);
                if (skillId == null) {
                    src.sendFailure(Component.literal("No skill in slot " + (slotIndex + 1)));
                    return;
                }

                Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                if (skill == null || !skill.isToggleable()) {
                    src.sendFailure(Component.literal("Skill in slot " + (slotIndex + 1) + " is not toggleable"));
                    return;
                }

                boolean newState = data.toggle(skillId);
                String status = newState ? "\u00A7aactivated" : "\u00A7cdeactivated";
                src.sendSuccess(() -> Component.literal("Toggle skill " + status), true);
            });

            return 1;
        }
    }

    public static class ResetCooldownCommand {
        static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("resetcd")
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                            .executes(ctx -> resetSlotCooldown(
                                    ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "slot") - 1
                            ))
                    )
                    .executes(ctx -> resetAllCooldowns(ctx.getSource()));
        }

        private static int resetSlotCooldown(CommandSourceStack src, int slotIndex) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                ResourceLocation skillId = data.getSkillInSlot(slotIndex);
                if (skillId == null) {
                    src.sendFailure(Component.literal("No skill in slot " + (slotIndex + 1)));
                    return;
                }

                data.clearCooldown(skillId);
                Skill skill = SkillRegistry.getInstance().getSkill(skillId);
                String name = skill != null ? skill.getDisplayName().getString() : skillId.toString();
                src.sendSuccess(() -> Component.literal("Reset cooldown for " + name + " (slot " + (slotIndex + 1) + ")"), true);
            });

            return 1;
        }

        private static int resetAllCooldowns(CommandSourceStack src) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("This command can only be used by players"));
                return 0;
            }

            player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                final int[] count = {0};
                for (int slot = 0; slot < IPlayerSkillData.SLOT_COUNT; slot++) {
                    ResourceLocation skillId = data.getSkillInSlot(slot);
                    if (skillId != null) {
                        data.clearCooldown(skillId);
                        count[0]++;
                    }
                }
                src.sendSuccess(() -> Component.literal("Reset cooldowns for " + count[0] + " skill(s)"), true);
            });

            return 1;
        }
    }
}
