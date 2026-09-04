package dev.dubhe.anvilcraft.api.tooltip.impl;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class ShulkerContainerTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    /** 初始空间大小。 */
    private static final int INITIAL_SPACE_SIZE = 65536;
    /** 升到最满（空间大小达到上限）所需的翻倍升级次数。 */
    private static final int UPGRADES_TO_MAX = 4;
    private static final int METADATA_REFRESH_INTERVAL = 10;
    private @Nullable BlockEntity target;
    private @Nullable CompletableFuture<OptionalInt> pendingUpgradeCount;
    private long nextMetadataRefresh;
    private int upgradeCount = -1;

    @Override
    public boolean accepts(BlockEntity value) {
        if (
            Minecraft.getInstance().player == null
            || !Minecraft.getInstance().player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)
               && !Minecraft.getInstance().player.getOffhandItem().is(ModItemTags.ANVIL_HAMMER)
        ) {
            return false;
        }
        return value instanceof ShulkerContainerBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (
            Minecraft.getInstance().player == null
            || !Minecraft.getInstance().player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)
               && !Minecraft.getInstance().player.getOffhandItem().is(ModItemTags.ANVIL_HAMMER)
        ) {
            return ImmutableList.of();
        }
        this.refreshUpgradeCount(value);
        Object upgradeCount = this.upgradeCount < 0
                              ? Component.translatable("tooltip.anvilcraft.shulker_container.6.waiting")
                              : this.upgradeCount;
        ImmutableList.Builder<Component> builder = ImmutableList.builder();
        if (this.upgradeCount < ShulkerContainerTooltipProvider.UPGRADES_TO_MAX) {
            builder.add(
                Component.translatable("tooltip.anvilcraft.shulker_container.0"),
                Component.translatable("tooltip.anvilcraft.shulker_container.1"),
                Component.translatable("tooltip.anvilcraft.shulker_container.2"),
                Component.translatable("tooltip.anvilcraft.shulker_container.3"),
                Component.translatable("tooltip.anvilcraft.shulker_container.4"),
                Component.translatable("tooltip.anvilcraft.shulker_container.5"),
                Component.translatable("tooltip.anvilcraft.shulker_container.6", upgradeCount)
            );
        } else {
            builder.add(
                Component.translatable("tooltip.anvilcraft.shulker_container.hyperdimension.0"),
                Component.translatable("tooltip.anvilcraft.shulker_container.hyperdimension.1"),
                Component.translatable("tooltip.anvilcraft.shulker_container.hyperdimension.2"),
                Component.translatable("tooltip.anvilcraft.shulker_container.hyperdimension.3")
            );
        }
        return builder.build();
    }

    private void refreshUpgradeCount(BlockEntity value) {
        Level level = value.getLevel();
        if (level == null) {
            return;
        }
        if (this.target != value) {
            this.target = value;
            this.pendingUpgradeCount = null;
            this.nextMetadataRefresh = level.getGameTime();
            this.upgradeCount = -1;
        }
        if (this.pendingUpgradeCount != null && this.pendingUpgradeCount.isDone()) {
            this.pendingUpgradeCount.join().ifPresent(count -> this.upgradeCount = count);
            this.pendingUpgradeCount = null;
        }
        if (this.pendingUpgradeCount != null || level.getGameTime() < this.nextMetadataRefresh) {
            return;
        }
        this.nextMetadataRefresh = level.getGameTime() + ShulkerContainerTooltipProvider.METADATA_REFRESH_INTERVAL;
        this.pendingUpgradeCount = StorageClientStub.loadMetadata(value.getBlockPos())
            .thenApply(metadata -> OptionalInt.of(calculateUpgradeCount(metadata.capacity().spaceSize())))
            .exceptionally(ignored -> OptionalInt.empty());
    }

    private static int calculateUpgradeCount(int spaceSize) {
        int upgradeCount = 0;
        while (spaceSize > ShulkerContainerTooltipProvider.INITIAL_SPACE_SIZE) {
            spaceSize /= 2;
            upgradeCount++;
        }
        return upgradeCount;
    }

    @Override
    public int priority() {
        return 0;
    }
}
