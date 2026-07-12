package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.SingleStackResourceHandler;
import dev.dubhe.anvilcraft.block.logistics.chute.SimpleMagneticChuteBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Objects;

@Getter
public class SimpleMagneticChuteBlockEntity extends BlockEntity implements IItemResourceHandlerHolder {
    private final SingleStackResourceHandler itemHandler = new SingleStackResourceHandler() {
        @Override
        protected void onContentChanged(ItemStack stack) {
            SimpleMagneticChuteBlockEntity.this.setChanged();
        }
    };
    @Setter
    private int cooldown = 0;
    private long tickedGameTime;

    public SimpleMagneticChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Cooldown", this.cooldown);
        this.itemHandler.serialize(output.child("Inventory"));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.cooldown = input.getIntOr("Cooldown", 0);
        this.itemHandler.deserialize(input.childOrEmpty("Inventory"));
    }

    protected Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.getBlock() instanceof SimpleMagneticChuteBlock) {
            return state.getValue(SimpleMagneticChuteBlock.FACING);
        }
        return Direction.UP;
    }

    /// tick
    public void tick() {
        if (this.level == null) return;
        if (this.cooldown > 0) this.cooldown--;
        this.tickedGameTime = this.level.getGameTime();
        Direction facing = this.getDirection();
        boolean resetCD = false;
        if (this.cooldown <= 0) {
            // 面向方向输出物品
            BlockPos targetPos = this.getBlockPos().relative(facing);
            List<ResourceHandler<ItemResource>> targetList = ItemHandlerUtil.getTargetItemHandlerList(
                targetPos,
                facing.getOpposite(),
                this.level
            );
            if (targetList != null && !targetList.isEmpty()) {
                for (ResourceHandler<ItemResource> target : targetList) {
                    BlockEntity targetBE = this.level.getBlockEntity(targetPos);
                    boolean setChuteCD = targetBE != null && this.isTargetEmpty(targetBE);
                    boolean success = ItemHandlerUtil.exportToTarget(this.getItemHandler(), 64, (_, _) -> true, target);
                    if (success) {
                        if (setChuteCD) this.setChuteCD(targetBE);
                        resetCD = true;
                        break;
                    }
                }
            } else {
                Vec3 center = this.getBlockPos().relative(facing).getCenter();
                AABB aabb = new AABB(
                    center.add(-0.125, -0.125, -0.125),
                    center.add(0.125, 0.125, 0.125)
                );
                if (Objects.requireNonNull(this.getLevel()).noCollision(aabb)) {
                    ItemStack stack = this.itemHandler.getStack();
                    if (!stack.isEmpty()) {
                        List<ItemEntity> itemEntities = this.getLevel()
                            .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(this.getBlockPos().relative(facing)),
                                itemEntity -> !itemEntity.getItem().isEmpty()
                            );
                        int sameItemCount = 0;
                        for (ItemEntity entity : itemEntities) {
                            if (entity.getItem().getItem() == stack.getItem()) {
                                sameItemCount += entity.getItem().getCount();
                            }
                        }
                        if (sameItemCount < stack.getMaxStackSize()) {
                            int droppedItemCount =
                                Math.min(stack.getCount(), stack.getMaxStackSize() - sameItemCount);
                            ItemStack droppedItemStack = stack.copyWithCount(droppedItemCount);
                            ItemEntity itemEntity = new ItemEntity(
                                this.getLevel(), center.x, center.y, center.z, droppedItemStack, 0, 0, 0
                            );
                            itemEntity.setDeltaMovement(MagneticChuteBlockEntity.getOutputSpeed(facing));
                            itemEntity.setDefaultPickUpDelay();
                            this.getLevel().addFreshEntity(itemEntity);
                            this.itemHandler.setStack(stack.copyWithCount(stack.getCount() - droppedItemCount));
                            resetCD = true;
                        }
                    }
                }
            }
        }
        this.level.updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
        if (resetCD) this.cooldown = AnvilCraft.CONFIG.chuteMaxCooldown;
    }

    private boolean isTargetEmpty(BlockEntity blockEntity) {
        return switch (blockEntity) {
            case SimpleChuteBlockEntity chute -> chute.isEmpty();
            case BaseChuteBlockEntity chute -> chute.isEmpty();
            case SimpleMagneticChuteBlockEntity chute -> chute.isEmpty();
            default -> false;
        };
    }

    private void setChuteCD(BlockEntity targetBE) {
        if (targetBE instanceof BaseChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.CONFIG.chuteMaxCooldown - k);
        }
        if (targetBE instanceof SimpleChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.CONFIG.chuteMaxCooldown - k);
        }
        if (targetBE instanceof SimpleMagneticChuteBlockEntity chute) {
            int k = 0;
            if (chute.getTickedGameTime() >= this.tickedGameTime) k++;
            chute.setCooldown(AnvilCraft.CONFIG.chuteMaxCooldown - k);
        }
    }

    public int getRedstoneSignal() {
        return this.itemHandler.getStack().isEmpty() ? 0 : 1;
    }

    public boolean isEmpty() {
        return this.itemHandler.getStack().isEmpty();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Vec3 center = pos.getCenter();
        Containers.dropItemStack(this.level, center.x, center.y, center.z, this.itemHandler.getStack());
    }
}
