package com.complextalents.dev;

import com.complextalents.origin.Origin;
import com.complextalents.origin.OriginManager;
import com.complextalents.origin.OriginRegistry;
import com.complextalents.origin.client.ClientOriginData;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.skill.client.ClientSkillData;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A custom UI for selecting Origins and Skills, primarily for development and
 * testing.
 */
public class OriginSkillSelectionUI {

    public static final ResourceLocation UI_ID = ResourceLocation.fromNamespaceAndPath("complextalents",
            "origin_skill_selection");

    public static void init() {
        SimpleUIFactory.register(UI_ID, (player, holder) -> new OriginSkillSelectionUI(player, holder).createUI());
    }

    private final Player player;
    private final IUIHolder holder;
    private Origin selectedOrigin;
    private Skill selectedSkill;
    private final AtomicInteger originLevel = new AtomicInteger(1);
    private final AtomicInteger skillLevel = new AtomicInteger(1);

    public OriginSkillSelectionUI(Player player, IUIHolder holder) {
        this.player = player;
        this.holder = holder;

        if (player.level().isClientSide) {
            initializeClient();
        } else if (player instanceof ServerPlayer serverPlayer) {
            initializeServer(serverPlayer);
        }

        if (this.originLevel.get() < 1)
            this.originLevel.set(1);
        if (this.skillLevel.get() < 1)
            this.skillLevel.set(1);
    }

    private void initializeClient() {
        ResourceLocation id = ClientOriginData.getOriginId();
        if (id != null) {
            this.selectedOrigin = OriginRegistry.getInstance().getOrigin(id);
            this.originLevel.set(ClientOriginData.getOriginLevel());
        }

        ResourceLocation skillId = ClientSkillData.getSkillInSlot(0);
        if (skillId != null) {
            this.selectedSkill = SkillRegistry.getInstance().getSkill(skillId);
            this.skillLevel.set(ClientSkillData.getSkillLevel(skillId));
        }
    }

    private void initializeServer(ServerPlayer serverPlayer) {
        this.selectedOrigin = OriginManager.getOrigin(serverPlayer);
        this.originLevel.set(OriginManager.getOriginLevel(serverPlayer));

        serverPlayer.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
            ResourceLocation skillId = data.getSkillInSlot(0);
            if (skillId != null) {
                this.selectedSkill = SkillRegistry.getInstance().getSkill(skillId);
                this.skillLevel.set(data.getSkillLevel(skillId));
            }
        });
    }

    public ModularUI createUI() {
        WidgetGroup root = new WidgetGroup();
        root.setSelfPosition(0, 0);
        root.setSize(300, 230);
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);

        // Titles
        LabelWidget title = new LabelWidget();
        title.setSelfPosition(10, 10);
        title.setText("§lOrigin & Skill Selection");
        root.addWidget(title);

        // Left Side: Origins
        LabelWidget originsTitle = new LabelWidget();
        originsTitle.setSelfPosition(10, 30);
        originsTitle.setText("§nOrigins");
        root.addWidget(originsTitle);

        WidgetGroup originList = new WidgetGroup();
        originList.setSelfPosition(10, 45);
        originList.setSize(135, 120);
        int yOffset = 0;
        for (Origin origin : OriginRegistry.getInstance().getAllOrigins()) {
            final Origin o = origin;
            ButtonWidget btn = new ButtonWidget();
            btn.setSelfPosition(0, yOffset);
            btn.setSize(130, 18);

            // Set dynamic texture for selection feedback
            btn.setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture(o.getDisplayName().getString()));

            btn.setOnPressCallback((clickData) -> {
                this.selectedOrigin = o;
            });

            // Add visual feedback via a client-side update check if possible,
            // but ButtonWidget doesn't have an easy way to change texture per tick.
            // For now, we rely on the Selection Info label.

            originList.addWidget(btn);
            yOffset += 20;
        }
        root.addWidget(originList);

        // Right Side: Skills
        LabelWidget skillsTitle = new LabelWidget();
        skillsTitle.setSelfPosition(155, 30);
        skillsTitle.setText("§nSkills (Slot 1)");
        root.addWidget(skillsTitle);

        WidgetGroup skillList = new WidgetGroup();
        skillList.setSelfPosition(155, 45);
        skillList.setSize(135, 120);
        yOffset = 0;
        for (Skill skill : SkillRegistry.getInstance().getAllSkills()) {
            final Skill s = skill;
            ButtonWidget btn = new ButtonWidget();
            btn.setSelfPosition(0, yOffset);
            btn.setSize(130, 18);
            btn.setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture(s.getDisplayName().getString()));
            btn.setOnPressCallback((clickData) -> {
                this.selectedSkill = s;
            });
            skillList.addWidget(btn);
            yOffset += 20;
        }
        root.addWidget(skillList);

        // Selection Info
        LabelWidget selectionInfo = new LabelWidget();
        selectionInfo.setSelfPosition(10, 165);
        selectionInfo.setTextProvider(() -> {
            String originName = selectedOrigin != null ? selectedOrigin.getDisplayName().getString() : "None";
            String skillName = selectedSkill != null ? selectedSkill.getDisplayName().getString() : "None";
            return String.format("Selection: §b%s§r | §e%s§r", originName, skillName);
        });
        root.addWidget(selectionInfo);

        // Origin Level Controls
        LabelWidget originLvlTitle = new LabelWidget();
        originLvlTitle.setSelfPosition(10, 185);
        originLvlTitle.setText("Origin Lvl:");
        root.addWidget(originLvlTitle);

        ButtonWidget minusOrigin = new ButtonWidget();
        minusOrigin.setSelfPosition(70, 183);
        minusOrigin.setSize(20, 18);
        minusOrigin.setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("-"));
        minusOrigin.setOnPressCallback((clickData) -> {
            if (originLevel.get() > 1)
                originLevel.decrementAndGet();
        });
        root.addWidget(minusOrigin);

        LabelWidget originLvlLabel = new LabelWidget();
        originLvlLabel.setSelfPosition(95, 185);
        originLvlLabel.setTextProvider(() -> {
            // Clamp levels if origin changes
            if (selectedOrigin != null && originLevel.get() > selectedOrigin.getMaxLevel()) {
                originLevel.set(selectedOrigin.getMaxLevel());
            }
            return String.valueOf(originLevel.get());
        });
        root.addWidget(originLvlLabel);

        ButtonWidget plusOrigin = new ButtonWidget();
        plusOrigin.setSelfPosition(115, 183);
        plusOrigin.setSize(20, 18);
        plusOrigin.setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("+"));
        plusOrigin.setOnPressCallback((clickData) -> {
            if (selectedOrigin != null && originLevel.get() < selectedOrigin.getMaxLevel()) {
                originLevel.incrementAndGet();
            } else if (selectedOrigin == null && originLevel.get() < 100) {
                originLevel.incrementAndGet();
            }
        });
        root.addWidget(plusOrigin);

        // Skill Level Controls
        LabelWidget skillLvlTitle = new LabelWidget();
        skillLvlTitle.setSelfPosition(155, 185);
        skillLvlTitle.setText("Skill Lvl:");
        root.addWidget(skillLvlTitle);

        ButtonWidget minusSkill = new ButtonWidget();
        minusSkill.setSelfPosition(210, 183);
        minusSkill.setSize(20, 18);
        minusSkill.setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("-"));
        minusSkill.setOnPressCallback((clickData) -> {
            if (skillLevel.get() > 1)
                skillLevel.decrementAndGet();
        });
        root.addWidget(minusSkill);

        LabelWidget skillLvlLabel = new LabelWidget();
        skillLvlLabel.setSelfPosition(235, 185);
        skillLvlLabel.setTextProvider(() -> {
            if (selectedSkill != null && skillLevel.get() > selectedSkill.getMaxLevel()) {
                skillLevel.set(selectedSkill.getMaxLevel());
            }
            return String.valueOf(skillLevel.get());
        });
        root.addWidget(skillLvlLabel);

        ButtonWidget plusSkill = new ButtonWidget();
        plusSkill.setSelfPosition(255, 183);
        plusSkill.setSize(20, 18);
        plusSkill.setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("+"));
        plusSkill.setOnPressCallback((clickData) -> {
            if (selectedSkill != null && skillLevel.get() < selectedSkill.getMaxLevel()) {
                skillLevel.incrementAndGet();
            } else if (selectedSkill == null && skillLevel.get() < 100) {
                skillLevel.incrementAndGet();
            }
        });
        root.addWidget(plusSkill);

        // Apply Button
        ButtonWidget applyBtn = new ButtonWidget();
        applyBtn.setSelfPosition(210, 205);
        applyBtn.setSize(80, 18);
        applyBtn.setButtonTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("Apply"));
        applyBtn.setOnPressCallback((clickData) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                applyChanges();
            }
            player.closeContainer();
        });
        root.addWidget(applyBtn);

        return new ModularUI(root, holder, player);
    }

    private void applyChanges() {
        if (player instanceof ServerPlayer serverPlayer) {
            if (selectedOrigin != null) {
                OriginManager.setOrigin(serverPlayer, selectedOrigin.getId(), originLevel.get());
            } else {
                OriginManager.clearOrigin(serverPlayer);
            }

            if (selectedSkill != null) {
                serverPlayer.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
                    data.setSkillInSlot(0, selectedSkill.getId());
                    data.setSkillLevel(selectedSkill.getId(), skillLevel.get());
                    data.sync();
                });
            }
        }
    }
}
