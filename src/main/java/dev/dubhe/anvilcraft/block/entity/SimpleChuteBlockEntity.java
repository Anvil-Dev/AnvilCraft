package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.entity.IConvertableBlockEntity;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.SingleStackResourceHandler;
import dev.dubhe.anvilcraft.block.logistics.chute.SimpleChuteBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class SimpleChuteBlockEntity extends BlockEntity implements IItemResourceHandlerHolder, IConvertableBlockEntity<ChuteBlockEntity> {
    private final SingleStackResourceHandler itemHandler = new SingleStackResourceHandler() {
        @Override
        protected void onContentChanged(ItemStack stack) {
            SimpleChuteBlockEntity.this.setChanged();
            Level level = SimpleChuteBlockEntity.this.level;
            if (level == null) return;
            level.sendBlockUpdated(
                SimpleChuteBlockEntity.this.getBlockPos(),
                SimpleChuteBlockEntity.this.getBlockState(),
                SimpleChuteBlockEntity.this.getBlockState(),
                Block.UPDATE_ALL
            );
        }
    };
    @Setter
    private int cooldown = 0;
    private long tickedGameTime;

    public SimpleChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

    /// tick
    public void tick() {
        if (this.level == null) return;
        if (this.cooldown > 0) this.cooldown--;
        this.tickedGameTime = this.level.getGameTime();
        if (this.cooldown == 0 && !this.itemHandler.getResource(0).isEmpty()) {
            this.cooldown = AnvilCraft.CONFIG.chuteMaxCooldown + 1;
        }
        if (this.cooldown == 1) {
            BlockPos targetPos = this.getBlockPos().relative(this.getOutputDirection());
            BlockEntity targetBE = this.level.getBlockEntity(targetPos);
            boolean isTargetEmpty = false;
            if (targetBE != null) isTargetEmpty = this.isTargetEmpty(targetBE);
            // 尝试向朝向容器输出
            List<ResourceHandler<ItemResource>> targetList = ItemHandlerUtil.getTargetItemHandlerList(
                targetPos,
                this.getOutputDirection().getOpposite(),
                this.level
            );
            if (targetList != null && !targetList.isEmpty()) {
                for (ResourceHandler<ItemResource> target : targetList) {
                    boolean success = ItemHandlerUtil.exportToTarget(this.getItemHandler(), 64, (_, _) -> true, target);
                    if (!success) continue;
                    // 特判溜槽cd7gt
                    if (isTargetEmpty) this.setChuteCD(targetBE);
                    break;
                }
            } else {
                Vec3 center = this.getBlockPos().relative(this.getDirection()).getCenter();
                List<ItemEntity> itemEntities = Objects.requireNonNull(this.getLevel()).getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(this.getBlockPos().relative(this.getDirection())).expandTowards(0, -0.5, 0),
                    itemEntity -> !itemEntity.getItem().isEmpty()
                );
                AABB aabb = new AABB(
                    center.add(-0.125, -0.125, -0.125),
                    center.add(0.125, 0.125, 0.125)
                );
                if (!this.getLevel().noCollision(aabb)) return;
                ItemStack stack = this.itemHandler.getStack();
                if (stack.isEmpty()) return;
                int sameItemCount = 0;
                for (ItemEntity entity : itemEntities) {
                    if (entity.getItem().getItem() == stack.getItem()) {
                        sameItemCount += entity.getItem().getCount();
                    }
                }
                if (sameItemCount >= stack.getMaxStackSize()) return;
                int dropping = Math.min(stack.getCount(), stack.getMaxStackSize() - sameItemCount);
                ItemStack remaining = stack.copyWithCount(dropping);
                if (remaining.getCount() == 0) remaining = ItemStack.EMPTY;
                ItemEntity itemEntity = new ItemEntity(
                    this.getLevel(),
                    center.x,
                    center.y,
                    center.z,
                    remaining,
                    0,
                    0,
                    0
                );
                itemEntity.setDefaultPickUpDelay();
                this.getLevel().addFreshEntity(itemEntity);
                this.itemHandler.setStack(stack.copyWithCount(stack.getCount() - dropping));
            }
        }
        this.level.updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
    }

    public boolean isTargetEmpty(BlockEntity blockEntity) {
        if (blockEntity instanceof SimpleChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        if (blockEntity instanceof BaseChuteBlockEntity chute) {
            return chute.isEmpty();
        }
        return false;
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
    }

    private Direction getDirection() {
        if (this.getLevel() == null) return Direction.DOWN;
        BlockState state = this.getLevel().getBlockState(this.getBlockPos());
        if (state.getBlock() instanceof SimpleChuteBlock) {
            return state.getValue(SimpleChuteBlock.FACING);
        }
        return Direction.DOWN;
    }

    public int getRedstoneSignal() {
        return this.itemHandler.getStack().isEmpty() ? 0 : 1;
    }

    protected Direction getOutputDirection() {
        return this.getDirection();
    }

    public boolean isEmpty() {
        return this.itemHandler.getStack().isEmpty();
    }

    @Override
    public Holder<BlockEntityType<?>> targetTypeHolder() {
        return ModBlockEntities.CHUTE;
    }

    @Override
    public void convertTo(ChuteBlockEntity newBe) {
        SingleStackResourceHandler handler = this.getItemHandler();
        ItemStack stack = handler.getStack();
        if (stack.isEmpty()) {
            return;
        }

        int count = stack.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            stack.setCount(count - newBe.getItemHandler().insert(ItemResource.of(stack), count, transaction));
            transaction.commit();
        }

        if (stack.isEmpty()) {
            return;
        }

        AnvilUtil.dropItems(Collections.singletonList(stack), this.level, newBe.getBlockPos().getCenter());
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Vec3 center = pos.getCenter();
        Containers.dropItemStack(this.level, center.x, center.y, center.z, this.itemHandler.getStack());
    }
}
