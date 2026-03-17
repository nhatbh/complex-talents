package com.complextalents.spellmastery.network;

import com.complextalents.leveling.handlers.LevelingSyncHandler;
import com.complextalents.leveling.data.PlayerLevelingData;
import com.complextalents.spellmastery.capability.SpellMasteryDataProvider;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LearnSpellPacket {
    private final List<ResourceLocation> spellIds;
    private final List<Integer> levels;

    public LearnSpellPacket(List<ResourceLocation> spellIds, List<Integer> levels) {
        this.spellIds = spellIds;
        this.levels = levels;
    }

    public LearnSpellPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.spellIds = new ArrayList<>(size);
        this.levels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.spellIds.add(buf.readResourceLocation());
            this.levels.add(buf.readInt());
        }
    }

    public static void encode(LearnSpellPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.spellIds.size());
        for (int i = 0; i < msg.spellIds.size(); i++) {
            buf.writeResourceLocation(msg.spellIds.get(i));
            buf.writeInt(msg.levels.get(i));
        }
    }

    public static void handle(LearnSpellPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            PlayerLevelingData levelingData = PlayerLevelingData.get(player.serverLevel());
            
            player.getCapability(SpellMasteryDataProvider.MASTERY_DATA).ifPresent(mastery -> {
                for (int i = 0; i < msg.spellIds.size(); i++) {
                    ResourceLocation spellId = msg.spellIds.get(i);
                    int requestedLevel = msg.levels.get(i);
                    AbstractSpell spell = SpellRegistry.getSpell(spellId);
                    if (spell == null || spell == SpellRegistry.none()) continue;

                    if (mastery.isSpellLearned(spellId, requestedLevel)) continue;

                    SpellRarity rarity = spell.getRarity(requestedLevel);
                    int requiredMastery = rarity.getValue();
                    int playerMastery = mastery.getMasteryLevel(spell.getSchoolType().getId());

                    if (playerMastery < requiredMastery) continue;

                    int cost = com.complextalents.spellmastery.SpellMasteryManager.getSpellCost(rarity);
                    int currentSP = levelingData.getStats(player.getUUID()).getAvailableSkillPoints();
                    
                    if (currentSP >= cost) {
                        mastery.learnSpell(spellId, requestedLevel);
                        levelingData.setConsumedSkillPoints(player.getUUID(), levelingData.getStats(player.getUUID()).getConsumedSkillPoints() + cost);
                        
                        // Give Scroll
                        ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
                        CompoundTag tag = scroll.getOrCreateTag();
                        CompoundTag isbSpells = new CompoundTag();
                        isbSpells.putInt("maxSpells", 1);
                        isbSpells.putBoolean("mustEquip", false);
                        ListTag dataList = new ListTag();
                        CompoundTag spellEntry = new CompoundTag();
                        spellEntry.putInt("index", 0);
                        spellEntry.putString("id", spellId.toString());
                        spellEntry.putBoolean("locked", true);
                        spellEntry.putInt("level", requestedLevel);
                        dataList.add(spellEntry);
                        isbSpells.put("data", dataList);
                        isbSpells.putBoolean("spellWheel", false);
                        tag.put("ISB_Spells", isbSpells);
                        if (!player.getInventory().add(scroll)) {
                            player.drop(scroll, false);
                        }
                    }
                }
                LevelingSyncHandler.syncPlayerLevelData(player);
            });
        });
        ctx.setPacketHandled(true);
    }
}
