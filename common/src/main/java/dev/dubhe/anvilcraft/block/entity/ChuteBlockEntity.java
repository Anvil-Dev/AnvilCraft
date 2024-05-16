package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IDiskCloneable;
import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepositoryHelper;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class ChuteBlockEntity extends BaseMachineBlockEntity implements IFilterBlockEntity, IDiskCloneable {
    private int cooldown = 0;
    private final FilteredItemDepository depository = new FilteredItemDepository(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };

    protected ChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.is(ModBlocks.CHUTE.get())) return state.getValue(ChuteBlock.FACING);
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {
        if (direction == Direction.UP) return;
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.CHUTE.get())) return;
        level.setBlockAndUpdate(pos, state.setValue(ChuteBlock.FACING, direction));
    }

    @ExpectPlatform
    public static ChuteBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void onBlockEntityRegister(BlockEntityType<ChuteBlockEntity> type) {
        throw new AssertionError();
    }

    @Override
    public FilteredItemDepository getFilteredItemDepository() {
        return depository;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.anvilcraft.chute");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new ChuteMenu(ModMenuTypes.CHUTE.get(), i, inventory, this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cooldown);
        tag.put("Inventory", depository.serializeNbt());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        cooldown = tag.getInt("Cooldown");
        depository.deserializeNbt(tag.getCompound("Inventory"));
    }

    /**
     * 溜槽 tick
     */
    @SuppressWarnings("UnreachableCode")
    public void tick() {
        if (cooldown <= 0) {
            if (getBlockState().getValue(ChuteBlock.ENABLED)) {
                IItemDepository depository = ItemDepositoryHelper.getItemDepository(
                        getLevel(),
                        getBlockPos().relative(Direction.UP),
                        Direction.UP.getOpposite()
                );
                if (depository != null) {
                    // 尝试从上方容器输入
                    if (ItemDepositoryHelper.importToTarget(this.depository, 64, stack -> true, depository)) {
                        cooldown = AnvilCraft.config.chuteMaxCooldown;
                    }
                } else {
                    List<ItemEntity> itemEntities = getLevel().getEntitiesOfClass(
                            ItemEntity.class, new AABB(getBlockPos().relative(Direction.UP)),
                            itemEntity -> !itemEntity.getItem().isEmpty());
                    int prevSize = itemEntities.size();
                    for (ItemEntity itemEntity : itemEntities) {
                        ItemStack remaining = ItemDepositoryHelper.insertItem(
                                this.depository, itemEntity.getItem(), true
                        );
                        if (!remaining.isEmpty()) continue;
                        ItemDepositoryHelper.insertItem(this.depository, itemEntity.getItem(), false);
                        itemEntity.kill();
                        break;
                    }
                    if (prevSize > itemEntities.size()) {
                        cooldown = AnvilCraft.config.chuteMaxCooldown;
                    }
                }
                depository = ItemDepositoryHelper.getItemDepository(
                        getLevel(),
                        getBlockPos().relative(this.getDirection()),
                        this.getDirection().getOpposite()
                );
                if (depository != null) {
                    // 尝试向朝向容器输出
                    if (!this.depository.isEmpty()) {
                        if (ItemDepositoryHelper.exportToTarget(this.depository, 64, stack -> true, depository)) {
                            cooldown = AnvilCraft.config.chuteMaxCooldown;
                        }
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
                                if (sameItemCount < stack.getItem().getMaxStackSize()) {
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
        level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
    }

    /**
     * 获取红石信号强度
     *
     * @return 红石信号强度
     */
    public int getRedstoneSignal() {
        int strength = 0;
        for (int index = 0; index < depository.getSlots(); index++) {
            ItemStack itemStack = depository.getStack(index);
            // 槽位为未设置过滤的已禁用槽位
            if (depository.isSlotDisabled(index) && depository.getFilter(index).isEmpty()) {
                strength++;
                continue;
            }
            // 槽位上没有物品
            if (itemStack.isEmpty()) {
                continue;
            }
            strength++;
        }
        return strength;
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.put("Filtering", depository.serializeFiltering());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        depository.deserializeFiltering(data.getCompound("Filtering"));
    }
}

