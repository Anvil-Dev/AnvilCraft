package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

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
        List<Component> components = new ArrayList<>();
        if (entity.getLevel() == null) return components;
        Optional<HeatTier> tierOp = HeatRecorder.getTier(
            entity.getLevel(),
            entity.getBlockPos(),
            entity.getBlockState()
        );
        tierOp.ifPresent(heatTier -> components.add(ITooltipProvider.withIndentAndMerge(Component.translatable(
            "tooltip.anvilcraft.heat.tier",
            heatTier.getDisplayName()
        ).withStyle(ChatFormatting.GRAY))));
        this.refreshDuration(entity);
        Object duration = this.duration < 0
                          ? Component.translatable("tooltip.anvilcraft.waiting")
                          : FormattingUtil.toFormattedTime(this.duration, 1);
        components.add(ITooltipProvider.withIndentAndMerge(Component.translatable(
            "tooltip.anvilcraft.heat.duration",
            duration
        ).withStyle(ChatFormatting.GRAY)));
        components.addFirst(Component.translatable("tooltip.anvilcraft.heat.title").withStyle(ChatFormatting.BLUE));
        return components;
    }

    private void refreshDuration(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null) {
            return;
        }
        if (this.target != entity) {
            this.target = entity;
            this.pendingDuration = null;
            this.nextDurationRefresh = level.getGameTime();
            this.duration = -1;
        }
        if (this.pendingDuration != null && this.pendingDuration.isDone()) {
            this.pendingDuration.join().ifPresent(duration -> this.duration = duration);
            this.pendingDuration = null;
        }
        if (this.pendingDuration != null || level.getGameTime() < this.nextDurationRefresh) {
            return;
        }
        this.nextDurationRefresh = level.getGameTime() + HeatableBlockTooltipProvider.DURATION_REFRESH_INTERVAL;
        this.pendingDuration = RPC.invoke(RpcTarget.server(), HeatableBlockEntity::getDuration, entity.getBlockPos())
            .thenApply(OptionalInt::of)
            .exceptionally(_ -> OptionalInt.empty());
    }

    @Override
    public int priority() {
        return 0;
    }
}
