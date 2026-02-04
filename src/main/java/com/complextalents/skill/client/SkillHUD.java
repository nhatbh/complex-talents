package com.complextalents.skill.client;

import com.complextalents.TalentsMod;
import com.complextalents.client.KeyBindings;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders the skill HUD in the bottom-right corner.
 * Displays skill icons, cooldowns, and keybinds for slots 1-2.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SkillHUD {

    // Default icon when skill doesn't have one
    private static final ResourceLocation DEFAULT_ICON =
        ResourceLocation.fromNamespaceAndPath("complextalents", "textures/skill/default_icon.png");

    // Layout constants
    private static final int ICON_SIZE = 24;
    private static final int SLOT_SIZE = 26;
    private static final int SPACING = 2;
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_BOTTOM = 10;

    // Colors (ARGB format with alpha)
    private static final int BACKGROUND_COLOR = 0xDD000000;
    private static final int BORDER_COLOR = 0xDDFFFFFF;
    private static final int COOLDOWN_OVERLAY = 0xCC000000;
    private static final int KEYBIND_COLOR = 0xFFFFFFFF;
    private static final int COOLDOWN_TEXT_COLOR = 0xFFFFFFFF;
    private static final int EMPTY_SLOT_BG = 0x66000000;
    private static final int EMPTY_SLOT_BORDER = 0x66FFFFFF;
    private static final int EMPTY_KEYBIND_COLOR = 0x66FFFFFF;

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelow(
                VanillaGuiOverlay.HOTBAR.id(),
                "skill_hud",
                SkillHUD::render
        );
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        // Early exit if screen is open
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        // Calculate position (bottom-right) - now for 2 slots
        int totalWidth = (SLOT_SIZE * 2) + (SPACING * 1);
        int startX = width - MARGIN_RIGHT - totalWidth;
        int startY = height - MARGIN_BOTTOM - SLOT_SIZE;

        RenderSystem.enableBlend();

        for (int slot = 0; slot < 2; slot++) {
            int x = startX + slot * (SLOT_SIZE + SPACING);
            renderSlot(graphics, slot, x, startY);
        }

        RenderSystem.disableBlend();
    }

    private static void renderSlot(GuiGraphics graphics, int slot, int x, int y) {
        ResourceLocation skillId = ClientSkillData.getSkillInSlot(slot);
        if (skillId == null) {
            // Render empty slot
            renderEmptySlot(graphics, slot, x, y);
            return;
        }

        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill == null) {
            renderEmptySlot(graphics, slot, x, y);
            return;
        }

        // Background
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, BACKGROUND_COLOR);
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, BORDER_COLOR);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, BORDER_COLOR);
        graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, BORDER_COLOR);

        // Icon
        ResourceLocation icon = skill.getIcon();
        if (icon == null) {
            icon = DEFAULT_ICON;
        }
        graphics.blit(icon, x + 1, y + 1, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        // Keybind name - get from actual key binding
        String keybind = getKeybindName(slot);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, keybind, x + 2, y + 2, KEYBIND_COLOR);

        // Cooldown overlay
        double cooldown = ClientSkillData.getCooldownRemaining(skillId);
        if (cooldown > 0) {
            // Dark overlay
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, COOLDOWN_OVERLAY);

            // Cooldown text
            String cdText = cooldown >= 10 ? String.format("%.0f", cooldown) : String.format("%.1f", cooldown);
            int textX = x + (SLOT_SIZE - font.width(cdText)) / 2;
            int textY = y + (SLOT_SIZE - font.lineHeight) / 2 + 1;
            graphics.drawString(font, cdText, textX, textY, COOLDOWN_TEXT_COLOR);
        }
    }

    private static void renderEmptySlot(GuiGraphics graphics, int slot, int x, int y) {
        // Dim empty slot
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, EMPTY_SLOT_BG);
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, EMPTY_SLOT_BORDER);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, EMPTY_SLOT_BORDER);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, EMPTY_SLOT_BORDER);
        graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, EMPTY_SLOT_BORDER);

        // Dim keybind
        var font = Minecraft.getInstance().font;
        String keybind = getKeybindName(slot);
        graphics.drawString(font, keybind, x + 2, y + 2, EMPTY_KEYBIND_COLOR);
    }

    /**
     * Get the display name for the keybind of a slot.
     * This dynamically shows the actual key the user has bound.
     */
    private static String getKeybindName(int slot) {
        try {
            if (slot == 0) {
                return KeyBindings.SKILL_1.getTranslatedKeyMessage().getString();
            } else if (slot == 1) {
                return KeyBindings.SKILL_2.getTranslatedKeyMessage().getString();
            }
        } catch (Exception e) {
            // Fallback if key binding is not yet initialized
            return String.valueOf(slot + 1);
        }
        return String.valueOf(slot + 1);
    }
}
