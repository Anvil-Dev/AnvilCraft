package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepositoryHelper;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class SimpleChuteBlockEntity extends BlockEntity {
    private int cooldown = 0;
    private final ItemDepository depository = new ItemDepository(1) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public SimpleChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Cooldown", cooldown);
        tag.put("Inventory", depository.serializeNbt(provider));
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cooldown = tag.getInt("Cooldown");
        depository.deserializeNbt(provider, tag.getCompound("Inventory"));
    }

    /**
     * tick
     */
    @SuppressWarnings({"UnreachableCode", "DuplicatedCode"})
    public void tick() {
        if (cooldown <= 0) {
            if (getBlockState().getValue(SimpleChuteBlock.ENABLED)) {
                IItemDepository depository = ItemDepositoryHelper.getItemDepository(
                        getLevel(),
                        getBlockPos().relative(getDirection()),
                        getDirection().getOpposite()
                );
                if (depository != null) {
                    // 尝试向朝向容器输出
                    if (!this.depository.isEmpty()) {
                        ItemDepositoryHelper.exportToTarget(this.depository, 64, stack -> true, depository);
                    }
                } else {
                    Vec3 center = getBlockPos().relative(getDirection()).getCenter();
                    List<ItemEntity> itemEntities = getLevel().getEntitiesOfClass(
                            ItemEntity.class, new AABB(getBlockPos().relative(getDirection())),
                            itemEntity -> !itemEntity.getItem().isEmpty());
                    AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
                    if (getLevel().noCollision(aabb)) {
                        for (int i = 0; i < this.depository.getSlots(); i++) {
                            ItemStack stack = this.depository.getStack(i);
                            if (!stack.isEmpty()) {
                                int sameItemCount = 0;
                                for (ItemEntity entity : itemEntities) {
                                    if (entity.getItem().getItem() == stack.getItem()) {
                                        sameItemCount += entity.getItem().getCount();
                                    }
                                }
                                if (sameItemCount < stack.getItem().getMaxStackSize(stack)) {
                                    ItemStack droppedItemStack = stack.copy();
                                    int droppedItemCount = Math.min(stack.getCount(),
                                            stack.getMaxStackSize() - sameItemCount);
                                    droppedItemStack.setCount(droppedItemCount);
                                    stack.setCount(stack.getCount() - droppedItemCount);
                                    if (stack.getCount() == 0) stack = ItemStack.EMPTY;
                                    ItemEntity itemEntity = new ItemEntity(
                                            getLevel(), center.x, center.y, center.z,
                                            droppedItemStack,
                                            0, 0, 0);
                                    itemEntity.setDefaultPickUpDelay();
                                    getLevel().addFreshEntity(itemEntity);
                                    this.depository.setStack(i, stack);
                                    cooldown = AnvilCraft.config.chuteMaxCooldown;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            cooldown--;
        }
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    private Direction getDirection() {
        if (getLevel() == null) return Direction.DOWN;
        BlockState state = getLevel().getBlockState(getBlockPos());
        if (state.getBlock() instanceof SimpleChuteBlock) return state.getValue(SimpleChuteBlock.FACING);
        return Direction.DOWN;
    }

    /**
     * @return 红石信号强度
     */
    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < depository.getSlots(); ++j) {
            ItemStack itemStack = depository.getStack(j);
            if (itemStack.isEmpty()) continue;
            ++i;
        }
        return i;
    }
}
