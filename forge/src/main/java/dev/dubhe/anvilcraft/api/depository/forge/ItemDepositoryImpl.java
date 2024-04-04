package dev.dubhe.anvilcraft.api.depository.forge;

import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ItemDepositoryImpl extends ItemDepository {
    private IItemHandler container = null;

    @Override
    public boolean canInject(@NotNull ItemStack thing, long count) {
        if (null == container) return false;
        return this.inject(thing, count, true) == 0;
    }

    public long inject(@NotNull ItemStack thing, long count, boolean simulate) {
        if (count <= 0) return 0;
        if (null == container) return count;
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        for (int slot = 0; slot < container.getSlots(); slot++) {
            if (count <= 0) {
                count = 0;
                break;
            }
            ItemStack stackInSlot = container.getStackInSlot(slot);
            if (!stackInSlot.isEmpty() && !ItemStack.isSameItemSameTags(thingType, stackInSlot)) continue;
            int maxCount = container.getSlotLimit(slot);
            if (!stackInSlot.isEmpty()) maxCount -= stackInSlot.getCount();
            int insertCount = (int) Math.min(maxCount, count);
            count -= insertCount;
            ItemStack stack = thingType.copy();
            stack.setCount(insertCount);
            container.insertItem(slot, stack, simulate);
        }
        return count;
    }

    // TODO
    @Override
    public boolean canTake() {
        return true;
    }

    @Override
    public @NotNull Thing<ItemStack> take(@NotNull ItemStack thing, long count) {
        ItemStack thingType = thing.copy();
        thingType.setCount(1);
        long takeCount = 0;
        if (null == container) return new Thing<>(thingType.copy(), 0);
        for (int i = 0; i < container.getSlots(); i++) {
            if (count <= 0) break;
            ItemStack stackInSlot = container.getStackInSlot(i);
            if (stackInSlot.isEmpty()) continue;
            if (!ItemStack.isSameItemSameTags(stackInSlot, thingType)) continue;
            int canTakeCount = (int) Math.min(count, stackInSlot.getCount());
            count -= canTakeCount;
            takeCount += canTakeCount;
            container.extractItem(i, canTakeCount, false);
        }
        return new Thing<>(thingType.copy(), takeCount);
    }

    public static @Nullable ItemDepository getItemDepository(@NotNull Level level, BlockPos pos, Direction direction) {
        ItemDepositoryImpl depositoryImpl = new ItemDepositoryImpl();
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            LazyOptional<IItemHandler> capability = be.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            if (!capability.isPresent()) return null;
            Optional<IItemHandler> resolve = capability.resolve();
            if (resolve.isEmpty()) return null;
            depositoryImpl.container = resolve.get();
            return depositoryImpl;
        }
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(pos));
        for (Entity entity : entities) {
            LazyOptional<IItemHandler> capability = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            if (!capability.isPresent()) continue;
            Optional<IItemHandler> resolve = capability.resolve();
            if (resolve.isEmpty()) continue;
            depositoryImpl.container = resolve.get();
            return depositoryImpl;
        }
        return null;
    }
}
