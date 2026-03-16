package com.complextalents.dev;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A simple UI factory for opening UIs directly via commands or other non-standard means.
 */
public class SimpleUIFactory extends UIFactory<SimpleUIFactory.SimpleHolder> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "simple");
    public static final SimpleUIFactory INSTANCE = new SimpleUIFactory();
    
    private static final Map<ResourceLocation, BiFunction<Player, SimpleHolder, ModularUI>> REGISTRY = new HashMap<>();

    static {
        UIFactory.register(INSTANCE);
    }

    public SimpleUIFactory() {
        super(ID);
    }

    public static void register(ResourceLocation id, BiFunction<Player, SimpleHolder, ModularUI> creator) {
        REGISTRY.put(id, creator);
    }

    public boolean open(ServerPlayer player, ResourceLocation uiId) {
        return openUI(new SimpleHolder(uiId), player);
    }

    @Override
    protected ModularUI createUITemplate(SimpleHolder holder, Player entityPlayer) {
        BiFunction<Player, SimpleHolder, ModularUI> creator = REGISTRY.get(holder.uiId);
        return creator != null ? creator.apply(entityPlayer, holder) : null;
    }

    @Override
    protected SimpleHolder readHolderFromSyncData(FriendlyByteBuf syncData) {
        return new SimpleHolder(syncData.readResourceLocation());
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, SimpleHolder holder) {
        syncData.writeResourceLocation(holder.uiId);
    }

    public static class SimpleHolder implements IUIHolder {
        public final ResourceLocation uiId;

        public SimpleHolder(ResourceLocation uiId) {
            this.uiId = uiId;
        }

        @Override
        public ModularUI createUI(Player entityPlayer) {
            return INSTANCE.createUITemplate(this, entityPlayer);
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public void markAsDirty() {
        }
    }
}
