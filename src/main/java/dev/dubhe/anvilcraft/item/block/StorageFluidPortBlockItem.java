package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.api.fluid.FluidItemRegistryAccess;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

public class StorageFluidPortBlockItem extends BlockItem {
    public StorageFluidPortBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        // 罐里还有流体时按普通储罐显示
        CompoundTag tank = StorageFluidPortBlockItem.readTankTag(stack);
        if (!tank.contains("Fluid")) {
            FluidStack marked = StorageFluidPortBlockItem.readMarkedFluid(stack);
            if (!marked.isEmpty()) {
                // 取空但仍有标记：把标记流体当作内容注入，数量固定显示 0
                CompoundTag injected = new CompoundTag();
                injected.put("Fluid", FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, marked).getOrThrow());
                injected.putBoolean("Marked", true);
                return Optional.of(new FluidTankTooltip(
                    injected, false, StorageFluidPortBlockEntity.CAPACITY_MB, false));
            }
        }
        return FluidTankItemTooltip.singleFluidTooltipImage(
            stack, StorageFluidPortBlockEntity.CAPACITY_MB, StorageFluidPortBlockEntity.CAPACITY_MB);
    }

    /// 读取物品上保存的储罐标签；没有时返回空标签。
    private static CompoundTag readTankTag(ItemStack stack) {
        TypedEntityData<?> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTagWithoutId().getCompoundOrEmpty("Tank");
    }

    /// 读取物品上保存的标记流体；没有标记时返回空栈。
    private static FluidStack readMarkedFluid(ItemStack stack) {
        HolderLookup.Provider registries = FluidItemRegistryAccess.get();
        TypedEntityData<?> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTagWithoutId();
        return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag)
            .childOrEmpty("MarkedFluid")
            .read("Fluid", FluidStack.OPTIONAL_CODEC)
            .orElse(FluidStack.EMPTY);
    }
}