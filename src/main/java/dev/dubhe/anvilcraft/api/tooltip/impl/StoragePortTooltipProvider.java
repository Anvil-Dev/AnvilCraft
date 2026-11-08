package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.client.gui.tooltip.StoragePortTooltipComponent;
import dev.dubhe.anvilcraft.rpc.StoragePortServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

/** 显示仓储端口标记物品的可用总量，并在标记或查看目标改变时重新获取计数。 */
public final class StoragePortTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    private static final int REFRESH_INTERVAL = 10;
    private WeakReference<@Nullable StoragePortBlockEntity> target = new WeakReference<>(null);
    private ItemStack marker = ItemStack.EMPTY;
    private @Nullable CompletableFuture<OptionalLong> pending;
    private long nextRefresh;
    private long count = -1;

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof StoragePortBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity entity) {
        return List.of(entity.getBlockState().getBlock().getName());
    }

    public List<ClientTooltipComponent> components(BlockEntity entity) {
        if (!(entity instanceof StoragePortBlockEntity port)) {
            return List.of();
        }
        ItemStack marked = port.getMarkedItem();
        if (marked.isEmpty()) {
            this.resetClientState();
        } else {
            this.refresh(port, marked);
        }
        var player = Minecraft.getInstance().player;
        boolean exact = player != null && player.isShiftKeyDown();
        List<StoragePortTooltipComponent.Entry> contents = new ArrayList<>();
        for (int slot = 0; slot < StoragePortBlockEntity.BUFFER_SLOTS; slot++) {
            ItemStack stack = port.getBuffer().getResource(slot).toStack(port.getBuffer().getAmountAsInt(slot));
            if (stack.isEmpty()) {
                continue;
            }
            int matching = -1;
            for (int index = 0; index < contents.size(); index++) {
                if (ItemStack.isSameItemSameComponents(contents.get(index).stack(), stack)) {
                    matching = index;
                    break;
                }
            }
            if (matching < 0) {
                contents.add(new StoragePortTooltipComponent.Entry(stack.copyWithCount(1), stack.getCount()));
            } else {
                var previous = contents.get(matching);
                contents.set(matching, new StoragePortTooltipComponent.Entry(previous.stack(), previous.count() + stack.getCount()));
            }
        }
        List<ClientTooltipComponent> result = new ArrayList<>();
        result.add(ClientTooltipComponent.create(port.getBlockState().getBlock().getName().getVisualOrderText()));
        result.add(new StoragePortTooltipComponent(Component.translatable("tooltip.anvilcraft.storage_port.cache"), contents, exact));
        if (!marked.isEmpty()) {
            result.add(new StoragePortTooltipComponent(Component.translatable("tooltip.anvilcraft.storage_port.storage"),
                List.of(new StoragePortTooltipComponent.Entry(marked.copyWithCount(1), this.count)), exact));
        }
        int width = result.stream().mapToInt(component -> component.getWidth(Minecraft.getInstance().font)).max().orElse(0);
        for (ClientTooltipComponent component : result) {
            if (component instanceof StoragePortTooltipComponent section) {
                section.setMinimumWidth(width);
            }
        }
        return result;
    }

    private void refresh(StoragePortBlockEntity port, ItemStack marked) {
        Level level = port.getLevel();
        if (level == null) {
            return;
        }
        if (this.target.get() != port || !ItemStack.isSameItemSameComponents(this.marker, marked)) {
            this.resetClientState();
            this.target = new WeakReference<>(port);
            this.marker = marked.copy();
            this.nextRefresh = level.getGameTime();
        }
        if (this.pending != null && this.pending.isDone()) {
            this.count = this.pending.join().orElse(-1);
            this.pending = null;
        }
        if (this.pending != null || level.getGameTime() < this.nextRefresh) {
            return;
        }
        this.nextRefresh = level.getGameTime() + REFRESH_INTERVAL;
        this.pending = RPC.invoke(RpcTarget.server(), StoragePortServerStub::getStoredCount, port.getBlockPos())
            .thenApply(OptionalLong::of).exceptionally(exception -> OptionalLong.empty());
    }

    private void resetClientState() {
        if (this.pending != null) {
            this.pending.cancel(false);
        }
        this.target.clear();
        this.marker = ItemStack.EMPTY;
        this.pending = null;
        this.nextRefresh = 0;
        this.count = -1;
    }

    @Override
    public int priority() {
        return 0;
    }
}
