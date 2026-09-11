package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.item.CustomRenderItemClientExtension;
import dev.dubhe.anvilcraft.client.renderer.item.StorageFluidPortItemRenderer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 仓储流体端口物品：tooltip 与物品内液面渲染沿用流体储罐那一套。
 *
 * <p>端口的 {@code Tank} NBT 与储罐同为「{@code Fluid} 复合标签」，故可直接交给
 * {@link FluidTankItemTooltip}；容量固定为 {@link StorageFluidPortBlockEntity#CAPACITY_MB}，
 * 且没有增强 / 无限形态。</p>
 */
public class StorageFluidPortBlockItem extends BlockItem {
    public StorageFluidPortBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        // 无增强形态，基础容量与增强容量取同一值
        return FluidTankItemTooltip.singleFluidTooltipImage(
            stack,
            StorageFluidPortBlockEntity.CAPACITY_MB,
            StorageFluidPortBlockEntity.CAPACITY_MB
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(CustomRenderItemClientExtension.of(StorageFluidPortItemRenderer.getInstance()));
    }
}
