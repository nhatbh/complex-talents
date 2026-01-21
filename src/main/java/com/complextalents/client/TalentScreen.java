package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TalentScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "textures/gui/talent_overlay.png");

    private final Player player;
    private final List<TalentEntry> allTalents = new ArrayList<>();

    public TalentScreen(Player player) {
        super(Component.translatable("gui.complextalents.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        // Load talents (placeholder implementation)
        loadTalents();

        // Create close button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .pos(this.width / 2 - 50, this.height - 30)
                .size(100, 20)
                .build());

        TalentsMod.LOGGER.debug("TalentScreen initialized");
    }

    private void loadTalents() {
        // Placeholder implementation
        // In full implementation, would load from capability or registry
        for (int i = 0; i < 10; i++) {
            allTalents.add(new TalentEntry(
                    ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "example_talent_" + i),
                    Component.translatable("talent.complextalents.example_talent_" + i + ".name"),
                    Component.translatable("talent.complextalents.example_talent_" + i + ".description"),
                    0,
                    5,
                    false
            ));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        // Render title
        guiGraphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);

        // Render talent grid
        renderTalentGrid(guiGraphics, mouseX, mouseY);

        // Render buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTalentGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = 50;
        int startY = 50;
        int iconSize = 32;
        int gap = 8;
        int talentsPerRow = 6;

        for (int i = 0; i < allTalents.size(); i++) {
            int row = i / talentsPerRow;
            int col = i % talentsPerRow;

            int x = startX + col * (iconSize + gap);
            int y = startY + row * (iconSize + gap);

            TalentEntry entry = allTalents.get(i);
            renderTalentEntry(guiGraphics, x, y, iconSize, entry, mouseX, mouseY);
        }
    }

    private void renderTalentEntry(GuiGraphics guiGraphics, int x, int y, int size, TalentEntry entry, int mouseX, int mouseY) {
        // Render background
        int color = entry.unlocked() ? 0x404040 : 0x202020;
        guiGraphics.fill(x, y, x + size, y + size, color);

        // Render level indicator
        if (entry.unlocked()) {
            String levelText = "Lv" + entry.level() + "/" + entry.maxLevel();
            guiGraphics.drawString(font, levelText, x + 2, y + size - 10, 0xFFFFFF);
        }

        // Check for tooltip
        if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size) {
            renderTooltip(guiGraphics, entry, mouseX, mouseY);
        }
    }

    private void renderTooltip(GuiGraphics guiGraphics, TalentEntry entry, int mouseX, int mouseY) {
        // Render tooltip with talent info
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(entry.name());
        tooltip.add(Component.empty());
        tooltip.add(entry.description());
        if (entry.unlocked()) {
            tooltip.add(Component.literal("Level: " + entry.level() + "/" + entry.maxLevel()));
        } else {
            tooltip.add(Component.literal("Not unlocked").withStyle(style -> style.withColor(0xFF5555)));
        }

        guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private static class TalentEntry {
        private final ResourceLocation id;
        private final Component name;
        private final Component description;
        private final int level;
        private final int maxLevel;
        private final boolean unlocked;

        public TalentEntry(ResourceLocation id, Component name, Component description, int level, int maxLevel, boolean unlocked) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.level = level;
            this.maxLevel = maxLevel;
            this.unlocked = unlocked;
        }

        public ResourceLocation id() {
            return id;
        }

        public Component name() {
            return name;
        }

        public Component description() {
            return description;
        }

        public int level() {
            return level;
        }

        public int maxLevel() {
            return maxLevel;
        }

        public boolean unlocked() {
            return unlocked;
        }
    }
}
