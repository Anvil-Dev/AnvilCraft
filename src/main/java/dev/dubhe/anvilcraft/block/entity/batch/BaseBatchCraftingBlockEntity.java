package dev.dubhe.anvilcraft.block.entity.batch;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.PollableFilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.BaseMachineBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.block.power.batch.BaseBatchCraftingBlock;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public abstract class BaseBatchCraftingBlockEntity extends BaseMachineBlockEntity
    implements IFilterBlockEntity, IPowerConsumer, IDiskCloneable, IHasDisplayItem {

    @Getter
    protected final int inputPower = 4;
    @Getter
    @Setter
    @Nullable
    protected PowerGrid grid;

    protected final PollableFilteredItemStackHandler handler = this.constructHandler();

    @Getter
    protected @Nullable ItemStack displayingStack;

    protected boolean poweredBefore = false;
    protected int cooldown = 0;

    @Getter
    protected final int id;

    public BaseBatchCraftingBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState, int id) {
        super(type, pos, blockState);
        this.id = id;
    }
    
    protected abstract PollableFilteredItemStackHandler constructHandler();

    public void tick(Level level, BlockPos pos) {
        this.flushState(level, pos);
        BlockState state = level.getBlockState(pos);
        level.updateNeighbourForOutputSignal(pos, state.getBlock());
        boolean powered = state.getValue(BaseBatchCraftingBlock.POWERED);
        if (level.isClientSide()) return;
        this.cooldown = Math.max(0, this.cooldown - 1);
        if (powered && !this.poweredBefore && !level.isClientSide() && this.cooldown == 0) {
            if (this.craft(Util.cast(level))) this.cooldown = this.getCooldownDuration();
        }
        this.poweredBefore = powered;
    }

    protected abstract int getCooldownDuration();

    protected boolean cantCraft() {
        if (this.grid == null || !this.grid.isWorking()) return true;
        if (!this.handler.isFilterEnabled()) return false;
        for (int i = 0; i < this.handler.size(); i++) {
            if (this.handler.getResource(i).isEmpty() && !this.handler.getFilter(i).isEmpty()) return true;
        }
        return false;
    }

    public abstract boolean craft(ServerLevel level);

    protected boolean ejectItems(ItemStack result, List<ItemStack> craftRemaining, Direction direction) {
        ResourceHandler<ItemResource> cap = Objects.requireNonNull(this.getLevel()).getCapability(
            Capabilities.Item.BLOCK,
            this.getBlockPos().relative(direction),
            direction.getOpposite()
        );
        if (cap != null) {
            // 尝试向容器插入物品
            ItemStack remained = ItemHandlerUtil.insertItem(cap, result, true);
            if (!remained.isEmpty()) return true;
            remained = ItemHandlerUtil.insertItem(cap, result, false);
            this.ejectItem(remained);
            for (ItemStack stack : craftRemaining) {
                remained = ItemHandlerUtil.insertItem(cap, stack, false);
                this.ejectItem(remained);
            }
        } else {
            // 尝试向世界喷出物品
            Vec3 center = this.getBlockPos().relative(this.getDirection()).getCenter();
            AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
            if (!this.getLevel().noCollision(aabb)) return true;

            this.ejectItem(result);
            for (ItemStack stack : craftRemaining) {
                this.ejectItem(stack);
            }
        }
        return false;
    }

    private void ejectItem(ItemStack stack) {
        int maxStackSize = stack.getMaxStackSize();
        int stackSize = stack.getCount();
        for (; stackSize > maxStackSize; stackSize -= maxStackSize) {
            this.ejectItemEntity(stack.copyWithCount(maxStackSize));
        }
        if (stackSize != 0) {
            this.ejectItemEntity(stack.copyWithCount(stackSize));
        }
    }

    private void ejectItemEntity(ItemStack stack) {
        Vec3 center = this.getBlockPos().relative(this.getDirection()).getCenter();
        Vector3f step = this.getDirection().step();
        Level level = this.getLevel();
        if (level == null) return;
        ItemEntity itemEntity = new ItemEntity(
            level,
            center.x,
            center.y,
            center.z,
            stack,
            0.25 * step.x,
            0.25 * step.y,
            0.25 * step.z
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        ItemHandlerUtil.dropAllToPos(this.getItemHandler(), this.level, pos.getCenter());
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.handler.serialize(output.child("Inventory"));
        output.putBoolean("PoweredBefore", this.poweredBefore);
        output.putInt("Cooldown", this.cooldown);
        boolean displaying = this.displayingStack != null && !this.displayingStack.isEmpty();
        output.putBoolean("HasDisplayItemStack", displaying);
        if (displaying) output.store("ResultItemStack", ItemStack.OPTIONAL_CODEC, this.displayingStack);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.handler.deserialize(input.childOrEmpty("Inventory"));
        this.poweredBefore = input.getBooleanOr("PoweredBefore", false);
        this.cooldown = input.getIntOr("Cooldown", 0);
        if (!input.getBooleanOr("HasDisplayItemStack", false)) return;
        this.displayingStack = input.read("ResultItemStack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.getBlock() instanceof BaseBatchCraftingBlock) return state.getValue(BaseBatchCraftingBlock.FACING);
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseBatchCraftingBlock)) return;
        level.setBlockAndUpdate(pos, state.setValue(BaseBatchCraftingBlock.FACING, direction));
    }

    /**
     * 获取红石信号强度
     *
     * @return 红石信号强度
     */
    public int getRedstoneSignal() {
        int strength = 0;
        List<Integer> itemIdxList = new IntArrayList();
        for (int index = 0; index < this.handler.size(); index++) {
            if (this.handler.isSlotDisabled(index) && this.handler.getFilter(index).isEmpty()) { // 槽位为未设置过滤的已禁用槽位
                strength++;
            } else if (!this.handler.getResource(index).isEmpty()) { // 槽位上有物品
                strength++;
                itemIdxList.add(index);
            }
        }
        if (strength < this.handler.size()) return strength;

        // 找到数量最少的序号
        int minIdx = itemIdxList.stream()
            .min(Comparator.comparingInt(this.handler::getAmountAsInt))
            .orElse(-1);
        // 不存在说明全是锁住的格子 -> 15
        if (minIdx == -1) return 15;

        // 考虑这个物品的堆叠上限，计算满堆比例
        ItemResource resource = this.handler.getResource(minIdx);
        int count = this.handler.getAmountAsInt(minIdx);
        int maxSize = this.handler.getCapacityAsInt(minIdx, resource);
        if (maxSize <= 1) {
            return 15;
        } else if (maxSize == 2) {
            return count == 1 ? 9 : 15;
        }

        int range = 6;
        return count == 1 ? 9 : 9 + ((count - 2) * (range - 1) + (maxSize - 2)) / (maxSize - 2);
    }

    @Override
    public Level getCurrentLevel() {
        return Objects.requireNonNull(this.getLevel());
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return this.handler;
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.handler;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void storeDiskData(ValueOutput output) {
        this.handler.serializeFiltering(output.child("Filtering"));
    }

    @Override
    public void applyDiskData(ValueInput input) {
        this.handler.deserializeFiltering(input.childOrEmpty("Filtering"));
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void updateDisplayItem(ItemStack stack) {
        this.displayingStack = stack;
    }
}
