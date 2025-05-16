package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static dev.dubhe.anvilcraft.block.BlockPlacerBlock.ORIENTATION;

public class ItemHandlerUtil implements IItemHandler {
    @Override
    public int getSlots() {
        return 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return null;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return null;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return null;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    public static IItemHandler getSourceItemHandler(BlockPos inputBlockPos, Direction context, Level level) {
        if (level == null) return null;
        IItemHandler input = level.getCapability(
            Capabilities.ItemHandler.BLOCK,
            inputBlockPos,
            context
        );
        if (input != null) {
            return input;
        }
        AABB aabb = new AABB(inputBlockPos);
        List<ContainerEntity> entities =
            level.getEntitiesOfClass(
                    Entity.class,
                    aabb,
                    e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty()
                ).stream()
                .map(it -> (ContainerEntity) it)
                .toList();
        if (!entities.isEmpty()) {
            input = ((Entity) entities.getFirst()).getCapability(
                Capabilities.ItemHandler.ENTITY,
                null
            );
        }
        return input;
    }

    @Nullable
    public static IItemHandler getSourceItemHandlerRecursive(Block source, BlockPos inputBlockPos, Direction context, Level level) {
        int i = 0;
        do {
            if (level == null) return null;
            if (level.getBlockState(inputBlockPos).is(source)
                && level.getBlockState(inputBlockPos).getValue(ORIENTATION).getDirection() == context
            ) {
                i++;
                inputBlockPos = inputBlockPos.relative(context.getOpposite());
            } else {
                return getSourceItemHandler(inputBlockPos, context, level);
            }
        } while (i < AnvilCraft.config.blockPlacerRecursiveRetrievalDistanceMax);
        return null;
    }
}
