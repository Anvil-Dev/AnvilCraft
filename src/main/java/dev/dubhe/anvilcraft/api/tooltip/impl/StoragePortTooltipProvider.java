package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.rpc.StoragePortServerStub;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
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
        if (!(entity instanceof StoragePortBlockEntity port)) {
            return List.of();
        }
        Component title = Component.translatable("tooltip.anvilcraft.storage_port.item").withStyle(ChatFormatting.BLUE);
        ItemStack marked = port.getMarkedItem();
        if (marked.isEmpty()) {
            this.resetClientState();
            return List.of(title, Component.translatable("tooltip.anvilcraft.storage_port.unmarked")
                .withStyle(ChatFormatting.GRAY));
        }
        this.refresh(port, marked);
        var player = Minecraft.getInstance().player;
        boolean original = player != null && player.isShiftKeyDown();
        Component amount = this.count < 0 ? Component.translatable("tooltip.anvilcraft.waiting")
            : Component.literal(original ? Long.toString(this.count) : FormattingUtil.toAbbrNum(this.count));
        return List.of(title, Component.literal("  ").append(marked.getHoverName()).append(" × ").append(amount)
            .withStyle(ChatFormatting.GRAY));
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

    @Override
    public void resetClientState() {
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
