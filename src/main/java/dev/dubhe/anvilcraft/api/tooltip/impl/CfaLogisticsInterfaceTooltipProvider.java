package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity.TooltipData;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity.TooltipSyncResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

/** Displays a server snapshot of the logistics interface without block-entity sync spam. */
public class CfaLogisticsInterfaceTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    private static final int DATA_REFRESH_INTERVAL = 10;
    private @Nullable CelestialForgingAnvilLogisticsInterfaceBlockEntity target;
    private @Nullable CompletableFuture<Optional<TooltipSyncResult>> pendingData;
    private long nextDataRefresh;
    private long dataVersion;
    private @Nullable TooltipData data;

    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (!(value instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logistics)) return List.of();
        this.refreshData(logistics);
        TooltipData snapshot = this.data;
        if (snapshot == null) {
            return List.of(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
        }

        List<Component> lines = new ArrayList<>();
        ItemStack demandItem = snapshot.templeDemandItem();
        if (!demandItem.isEmpty()) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.temple_demand")
                .withStyle(ChatFormatting.GOLD));
            int progress = snapshot.templeDemandProgress();
            if (progress > 0) {
                lines.add(Component.literal(" · ")
                    .append(demandItem.getHoverName())
                    .append(Component.literal(" " + progress + "/" + snapshot.templeDemandCount()))
                    .withStyle(ChatFormatting.YELLOW));
            } else {
                lines.add(Component.literal(" · ")
                    .append(demandItem.getHoverName())
                    .append(Component.literal(" ×" + snapshot.templeDemandCount()))
                    .withStyle(ChatFormatting.YELLOW));
            }
            lines.add(Component.literal(""));
        }

        List<ItemStack> colliderTargets = snapshot.colliderTargetItems();
        if (!colliderTargets.isEmpty() || snapshot.colliderProcessing() || snapshot.colliderStarMissing()) {
            if (snapshot.colliderStarMissing()) {
                lines.add(Component.translatable("screen.anvilcraft.cfa.collider_star_missing")
                    .withStyle(ChatFormatting.RED));
                lines.add(Component.literal(""));
            } else if (snapshot.colliderProcessing()) {
                Level level = logistics.getLevel();
                int dots = level != null ? (int) ((level.getGameTime() / 10) % 3) : 0;
                String base = Component.translatable("screen.anvilcraft.cfa.collider_processing").getString();
                lines.add(Component.literal(base + ".".repeat(dots + 1) + "◇")
                    .withStyle(ChatFormatting.AQUA));
            } else {
                lines.add(Component.translatable("screen.anvilcraft.cfa.collider_targets")
                    .withStyle(ChatFormatting.AQUA));
            }
            if (!snapshot.colliderStarMissing() && !snapshot.colliderProcessing()) {
                for (ItemStack target : colliderTargets) {
                    if (!target.isEmpty()) {
                        lines.add(Component.literal(" · ")
                            .append(target.getHoverName())
                            .withStyle(ChatFormatting.AQUA));
                    }
                }
            }
            lines.add(Component.literal(""));
        }

        for (ItemStack stack : snapshot.storedItems()) {
            lines.add(Component.literal(" · ")
                .append(stack.getHoverName())
                .append(Component.literal(" ×" + stack.getCount()))
                .withStyle(ChatFormatting.GRAY));
        }
        if (snapshot.storedItems().isEmpty()) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.interface.empty")
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private void refreshData(CelestialForgingAnvilLogisticsInterfaceBlockEntity logistics) {
        Level level = logistics.getLevel();
        if (level == null) return;
        if (this.target != logistics) {
            this.resetClientState();
            this.target = logistics;
            this.nextDataRefresh = level.getGameTime();
        }
        if (this.pendingData != null && this.pendingData.isDone()) {
            this.pendingData.join().ifPresent(result -> {
                this.dataVersion = result.version();
                if (result.data() != null) this.data = result.data();
            });
            this.pendingData = null;
        }
        if (this.pendingData != null || level.getGameTime() < this.nextDataRefresh) return;
        this.nextDataRefresh = level.getGameTime() + DATA_REFRESH_INTERVAL;
        this.pendingData = RPC.invoke(
            RpcTarget.server(),
            CelestialForgingAnvilLogisticsInterfaceBlockEntity::syncTooltipData,
            logistics.getBlockPos(),
            this.dataVersion
        ).thenApply(Optional::of).exceptionally(ignored -> Optional.empty());
    }

    @Override
    public void resetClientState() {
        if (this.pendingData != null) this.pendingData.cancel(false);
        this.target = null;
        this.pendingData = null;
        this.nextDataRefresh = 0;
        this.dataVersion = 0;
        this.data = null;
    }

    @Override
    public ItemStack icon(BlockEntity value) {
        return ItemStack.EMPTY;
    }

    @Override
    public int priority() {
        return -1;
    }
}
