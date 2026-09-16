package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

record BuildingBlockMaterial(ItemStack stack, CompoundTag config) {
    @SuppressWarnings("deprecation")
    static BuildingBlockMaterial extract(BlockState state, @Nullable CompoundTag nbt, HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        CompoundTag config = nbt == null ? new CompoundTag() : nbt.copy();
        BlockEntity entity = nbt == null ? null : BlockEntity.loadStatic(BlockPos.ZERO, state, nbt, registries);
        if (entity == null || stack.isEmpty()) return new BuildingBlockMaterial(stack, config);
        if (entity instanceof IFluidHandlerHolder || entity instanceof IFluidHandler) {
            // 流体容器的数据由实际消耗的物品提供，不能再从蓝图恢复或另扣流体。
            entity.saveToItem(stack, registries);
            return new BuildingBlockMaterial(stack, new CompoundTag());
        }
        stack.applyComponents(entity.collectComponents());
        if (!stack.getPrototype().has(DataComponents.CONTAINER)
            && stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).equals(ItemContainerContents.EMPTY)) {
            stack.remove(DataComponents.CONTAINER);
        }
        entity.removeComponentsFromTag(config);
        config.remove("components");
        return new BuildingBlockMaterial(stack, config);
    }
}
