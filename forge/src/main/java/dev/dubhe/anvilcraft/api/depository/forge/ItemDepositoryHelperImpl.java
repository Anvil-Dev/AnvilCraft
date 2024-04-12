package dev.dubhe.anvilcraft.api.depository.forge;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
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
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemDepositoryHelperImpl {
    private ItemDepositoryHelperImpl() {
    }

    /**
     * 获取物品容器
     *
     * @param level     世界
     * @param pos       位置
     * @param direction 方向
     * @return 物品容器
     */
    @Nullable
    public static IItemDepository getItemDepository(@NotNull Level level, BlockPos pos, Direction direction) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            LazyOptional<IItemHandler> capability =
                be.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            if (capability.isPresent() && capability.resolve().isPresent()) {
                return toItemDepository(capability.resolve().get());
            }
        }
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(pos));
        for (Entity entity : entities) {
            LazyOptional<IItemHandler> capability =
                entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            if (capability.isPresent() && capability.resolve().isPresent()) {
                return toItemDepository(capability.resolve().get());
            }
        }
        return null;
    }

    /**
     * 将 {@link IItemHandler} 转换为 {@link IItemDepository}
     *
     * @param handler 要转换的 ItemHandler
     * @return 转换为的 ItemDepository
     */
    public static @NotNull IItemDepository toItemDepository(IItemHandler handler) {
        return new IItemDepository() {
            @Override
            public int getSlots() {
                return handler.getSlots();
            }

            @Override
            public ItemStack getStack(int slot) {
                return handler.getStackInSlot(slot);
            }

            @Override
            public void setStack(int slot, ItemStack stack) {
                ((IItemHandlerModifiable) handler).setStackInSlot(slot, stack);
            }

            @Override
            public ItemStack insert(
                int slot, ItemStack stack, boolean simulate, boolean notifyChanges, boolean isServer
            ) {
                return handler.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extract(int slot, int amount, boolean simulate, boolean notifyChanges) {
                return handler.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return handler.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return handler.isItemValid(slot, stack);
            }
        };
    }

    /**
     * @param depository 物品容器
     * @return 物品容器
     */
    public static IItemHandler toItemHandler(IItemDepository depository) {
        return new IItemHandler() {

            @Override
            public int getSlots() {
                return depository.getSlots();
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int i) {
                return depository.getStack(i);
            }

            @Override
            public @NotNull ItemStack insertItem(int i, @NotNull ItemStack arg, boolean bl) {
                return depository.insert(i, arg, bl);
            }

            @Override
            public @NotNull ItemStack extractItem(int i, int j, boolean bl) {
                return depository.extract(i, j, bl);
            }

            @Override
            public int getSlotLimit(int i) {
                return depository.getSlotLimit(i);
            }

            @Override
            public boolean isItemValid(int i, @NotNull ItemStack arg) {
                return depository.isItemValid(i, arg);
            }
        };
    }

}
