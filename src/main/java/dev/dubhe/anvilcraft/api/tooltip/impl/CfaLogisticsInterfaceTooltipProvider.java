package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity.TooltipData;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity.TooltipSyncResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CfaLogisticsInterfaceTooltipProvider
    extends CfaInterfaceTooltipProvider<CelestialForgingAnvilLogisticsInterfaceBlockEntity> {
    private static final int DATA_REFRESH_INTERVAL = 10;
    private @Nullable CelestialForgingAnvilLogisticsInterfaceBlockEntity target;
    private @Nullable CompletableFuture<Optional<TooltipSyncResult>> pendingData;
    private long nextDataRefresh;
    private long dataVersion = 0;
    private @Nullable TooltipData data;

    public CfaLogisticsInterfaceTooltipProvider() {
        super(CelestialForgingAnvilLogisticsInterfaceBlockEntity.class);
    }

    @Override
    protected List<Component> buildTooltip(CelestialForgingAnvilLogisticsInterfaceBlockEntity logistics) {
        this.refreshData(logistics);
        List<Component> lines = new ArrayList<>();
        TooltipData data = this.data;
        if (data == null) {
            lines.add(Component.translatable("tooltip.anvilcraft.waiting").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        // 显示尚未完成的神庙供奉需求。
        ItemStack demandItem = data.templeDemandItem();
        if (!demandItem.isEmpty()) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.temple_demand")
                .withStyle(ChatFormatting.GOLD));
            int progress = data.templeDemandProgress();
            if (progress > 0) {
                lines.add(Component.literal(" · ")
                    .append(demandItem.getHoverName())
                    .append(Component.literal(" " + progress + "/" + data.templeDemandCount()))
                    .withStyle(ChatFormatting.YELLOW));
            } else {
                lines.add(Component.literal(" · ")
                    .append(demandItem.getHoverName())
                    .append(Component.literal(" ×" + data.templeDemandCount()))
                    .withStyle(ChatFormatting.YELLOW));
            }
            lines.add(Component.literal(""));
        }

        // 显示服务端快照中的对撞机状态。
        List<ItemStack> colliderTargets = data.colliderTargetItems();
        if (!colliderTargets.isEmpty() || data.colliderProcessing() || data.colliderStarMissing()) {
            // 缺少恒星时优先显示警告，不显示目标物品。
            if (data.colliderStarMissing()) {
                lines.add(Component.translatable("screen.anvilcraft.cfa.collider_star_missing")
                    .withStyle(ChatFormatting.RED));
                lines.add(Component.literal(""));
            } else if (data.colliderProcessing()) {
                var level = logistics.getLevel();
                int dots = level != null ? (int) ((level.getGameTime() / 10) % 3) : 0;
                String base = Component.translatable("screen.anvilcraft.cfa.collider_processing").getString();
                lines.add(Component.literal(base + ".".repeat(dots + 1) + "◇")
                    .withStyle(ChatFormatting.AQUA));
            } else {
                lines.add(Component.translatable("screen.anvilcraft.cfa.collider_targets")
                    .withStyle(ChatFormatting.AQUA));
            }
            // 仅在空闲且恒星存在时显示目标物品。
            if (!data.colliderStarMissing() && !data.colliderProcessing()) {
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

        // 显示接口内已存储的物品。
        for (ItemStack stack : data.storedItems()) {
            lines.add(Component.literal(" · ")
                .append(stack.getHoverName())
                .append(Component.literal(" ×" + stack.getCount()))
                .withStyle(ChatFormatting.GRAY));
        }
        if (data.storedItems().isEmpty()) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.interface.empty")
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private void refreshData(CelestialForgingAnvilLogisticsInterfaceBlockEntity logistics) {
        Level level = logistics.getLevel();
        if (level == null) return;
        if (this.target != logistics) {
            this.target = logistics;
            this.pendingData = null;
            this.nextDataRefresh = level.getGameTime();
            this.dataVersion = 0;
            this.data = null;
        }
        if (this.pendingData != null && this.pendingData.isDone()) {
            this.pendingData.join().ifPresent(result -> {
                this.dataVersion = result.version();
                TooltipData updatedData = result.data();
                if (updatedData != null) {
                    this.data = updatedData;
                }
            });
            this.pendingData = null;
        }
        if (this.pendingData != null || level.getGameTime() < this.nextDataRefresh) return;
        this.nextDataRefresh = level.getGameTime() + CfaLogisticsInterfaceTooltipProvider.DATA_REFRESH_INTERVAL;
        this.pendingData = RPC.invoke(
            RpcTarget.server(),
            CelestialForgingAnvilLogisticsInterfaceBlockEntity::syncTooltipData,
            logistics.getBlockPos(),
            this.dataVersion
        ).thenApply(Optional::of).exceptionally(_ -> Optional.empty());
    }

    @Override
    public ItemStack icon(BlockEntity value) {
        return ItemStack.EMPTY;
    }

}
