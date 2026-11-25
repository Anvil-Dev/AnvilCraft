package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.api.fluid.FluidItemRegistryAccess;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.item.CustomRenderItemClientExtension;
import dev.dubhe.anvilcraft.client.renderer.item.StorageFluidPortItemRenderer;
import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
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
        // 罐里还有流体时按普通储罐显示
        CompoundTag tankTag = StorageFluidPortBlockItem.readTankTag(stack);
        if (!tankTag.contains("Fluid")) {
            FluidStack marked = StorageFluidPortBlockItem.readMarkedFluid(stack);
            if (!marked.isEmpty()) {
                // 取空但仍有标记：把标记流体当作内容注入，数量固定显示 0
                HolderLookup.Provider registries = FluidItemRegistryAccess.get();
                if (registries != null) {
                    CompoundTag injected = new CompoundTag();
                    injected.put("Fluid", marked.save(registries, new CompoundTag()));
                    injected.putBoolean("Marked", true);
                    return Optional.of(new FluidTankTooltip(
                        injected, false, StorageFluidPortBlockEntity.CAPACITY_MB, false));
                }
            }
        }
        // 无增强形态，基础容量与增强容量取同一值
        return FluidTankItemTooltip.singleFluidTooltipImage(
            stack,
            StorageFluidPortBlockEntity.CAPACITY_MB,
            StorageFluidPortBlockEntity.CAPACITY_MB
        );
    }

    /// 读取物品上保存的储罐标签；没有时返回空标签。
    private static CompoundTag readTankTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTag().getCompound("Tank");
    }

    /// 读取物品上保存的标记流体；没有标记时返回空栈。
    private static FluidStack readMarkedFluid(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return FluidStack.EMPTY;
        }
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        return registries == null
            ? FluidStack.EMPTY : FluidStack.parseOptional(registries, data.copyTag().getCompound("marked_fluid"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(CustomRenderItemClientExtension.of(StorageFluidPortItemRenderer.getInstance()));
    }
}
