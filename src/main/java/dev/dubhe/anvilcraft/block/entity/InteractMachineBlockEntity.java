package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.inventory.InteractMachineMenu;
import dev.dubhe.anvilcraft.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class InteractMachineBlockEntity extends BaseMachineBlockEntity {
    public InteractMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INTERACT_MACHINE, pos, blockState, 11);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("block.anvilcraft.interact_machine");
    }

    public static void tick(Level level, BlockPos pos, BlockEntity e) {
        if (!(e instanceof InteractMachineBlockEntity entity)) return;
        BaseMachineBlockEntity.tick(level, pos, entity);
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new InteractMachineMenu(containerId, inventory, this);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index != 0 && index != this.getContainerSize() - 1) return true;
        return index == 0 && stack.is(ModItemTags.PROTOCOL);
    }
}
