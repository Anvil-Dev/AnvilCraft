package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
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
public class ChuteBlockEntity extends BaseMachineBlockEntity implements IFilterBlockEntity {
    private int cooldown = 0;
    private final FilteredItemDepository depository = new FilteredItemDepository(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public ChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
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
                if (ItemDepositoryHelper.getItemDepository(getLevel(),
                    getBlockPos().relative(Direction.UP), Direction.UP.getOpposite()) != null) {
                    // 尝试从上方容器输入
                    ItemDepositoryHelper.importToTarget(depository,
                        64, stack -> true, getLevel(),
                        getBlockPos().relative(Direction.UP), Direction.UP.getOpposite());
                } else {
                    List<ItemEntity> itemEntities = getLevel().getEntitiesOfClass(
                        ItemEntity.class, new AABB(getBlockPos().relative(Direction.UP)),
                        itemEntity -> !itemEntity.getItem().isEmpty());
                    for (ItemEntity itemEntity : itemEntities) {
                        ItemStack remaining = ItemDepositoryHelper.insertItem(depository, itemEntity.getItem(), true);
                        if (!remaining.isEmpty()) continue;
                        ItemDepositoryHelper.insertItem(depository, itemEntity.getItem(), false);
                        itemEntity.kill();
                        break;
                    }
                }
                if (ItemDepositoryHelper.getItemDepository(getLevel(),
                    getBlockPos().relative(getDirection()), getDirection().getOpposite()) != null) {
                    // 尝试向朝向容器输出
                    if (!depository.isEmpty()) {
                        ItemDepositoryHelper.exportToTarget(depository,
                            64, stack -> true, getLevel(),
                            getBlockPos().relative(getDirection()), getDirection().getOpposite());
                    }
                } else {
                    Vec3 center = getBlockPos().relative(getDirection()).getCenter();
                    AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
                    if (getLevel().noCollision(aabb)) {
                        for (int i = 0; i < depository.getSlots(); i++) {
                            ItemStack stack = depository.getStack(i);
                            if (!stack.isEmpty()) {
                                ItemEntity itemEntity = new ItemEntity(
                                    getLevel(), center.x, center.y, center.z, stack, 0, 0, 0);
                                getLevel().addFreshEntity(itemEntity);
                                depository.setStack(i, ItemStack.EMPTY);
                                break;
                            }
                        }
                    }
                }
            }
            cooldown = AnvilCraft.config.chuteMaxCooldown;
        } else {
            cooldown--;
        }
    }

    /**
     * 获取红石信号强度
     *
     * @return 红石信号强度
     */
    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < depository.getSlots(); ++j) {
            ItemStack itemStack = depository.getStack(j);
            if (itemStack.isEmpty() && !depository.isSlotDisabled(j)) continue;
            ++i;
        }
        return i;
    }
}

