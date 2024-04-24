package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepositoryHelper;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public class SimpleChuteBlockEntity extends BlockEntity {
    private int cooldown = 0;
    private final ItemDepository depository = new ItemDepository(1) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };

    protected SimpleChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @ExpectPlatform
    public static SimpleChuteBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void onBlockEntityRegister(BlockEntityType<SimpleChuteBlockEntity> type) {
        throw new AssertionError();
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
     * tick
     */
    @SuppressWarnings({"UnreachableCode", "DuplicatedCode"})
    public void tick() {
        if (cooldown <= 0) {
            if (getBlockState().getValue(SimpleChuteBlock.ENABLED)) {
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
                                ItemEntity itemEntity = new ItemEntity(getLevel(),
                                    center.x, center.y, center.z, stack, 0, 0, 0);
                                itemEntity.setDefaultPickUpDelay();
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
