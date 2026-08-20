package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class HeatableBlockTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    private static final int DURATION_REFRESH_INTERVAL = 10;
    private @Nullable BlockEntity target;
    private @Nullable CompletableFuture<OptionalInt> pendingDuration;
    private long nextDurationRefresh;
    private int duration = -1;

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof HeatableBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity entity) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return null;
        List<Component> components = new ArrayList<>();
        Level level = entity.getLevel();
        if (level == null) return components;
        HeatRecorder.getTier(level, entity.getBlockPos(), entity.getBlockState())
            .ifPresent(tier -> components.add(ITooltipProvider.withIndentAndMerge(Component.translatable(
                "tooltip.anvilcraft.heat.tier",
                tier.getDisplayName()
            ).withStyle(ChatFormatting.GRAY))));
        this.refreshDuration(entity);
        Object duration = this.duration < 0
                          ? Component.translatable("tooltip.anvilcraft.waiting")
                          : FormattingUtil.toFormattedTime(Math.max(this.duration, 0), 1);
        components.add(ITooltipProvider.withIndentAndMerge(Component.translatable(
            "tooltip.anvilcraft.heat.duration",
            duration
        ).withStyle(ChatFormatting.GRAY)));
        components.addFirst(Component.translatable("tooltip.anvilcraft.heat.title").withStyle(ChatFormatting.BLUE));
        return components;
    }

    private void refreshDuration(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null) return;
        if (this.target != entity) {
            this.resetClientState();
            this.target = entity;
            this.nextDurationRefresh = level.getGameTime();
        }
        if (this.pendingDuration != null && this.pendingDuration.isDone()) {
            this.pendingDuration.join().ifPresent(value -> this.duration = value);
            this.pendingDuration = null;
        }
        if (this.pendingDuration != null || level.getGameTime() < this.nextDurationRefresh) return;
        this.nextDurationRefresh = level.getGameTime() + DURATION_REFRESH_INTERVAL;
        this.pendingDuration = RPC.invoke(RpcTarget.server(), HeatableBlockEntity::getDuration, entity.getBlockPos())
            .thenApply(OptionalInt::of)
            .exceptionally(ignored -> OptionalInt.empty());
    }

    @Override
    public void resetClientState() {
        if (this.pendingDuration != null) this.pendingDuration.cancel(false);
        this.target = null;
        this.pendingDuration = null;
        this.nextDurationRefresh = 0;
        this.duration = -1;
    }

    @Override
    public int priority() {
        return 0;
    }
}
