package dev.dubhe.anvilcraft.block.entity.batch;

import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.PollableFilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.batch.BaseBatchCraftingBlock;
import dev.dubhe.anvilcraft.block.entity.BaseMachineBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.AccessLevel;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

@Getter
public abstract class BaseBatchCraftingBlockEntity extends BaseMachineBlockEntity
    implements IFilterBlockEntity, IPowerConsumer, IDiskCloneable, IHasDisplayItem {

    protected final int inputPower = 4;
    @Setter
    protected @javax.annotation.Nullable PowerGrid grid;

    @Getter(AccessLevel.NONE)
    protected final PollableFilteredItemStackHandler handler = this.constructHandler();

    protected @Nullable ItemStack displayingStack;

    @Getter(AccessLevel.NONE)
    protected boolean poweredBefore = false;
    @Getter(AccessLevel.NONE)
    protected int cooldown = 0;

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
        this.cooldown = Math.max(0, this.cooldown - 1);
        if (powered && !this.poweredBefore && !level.isClientSide && this.cooldown == 0) {
            if (this.craft(level)) this.cooldown = this.getCooldownDuration();
        }
        this.poweredBefore = powered;
    }

    protected abstract int getCooldownDuration();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean canCraft() {
        if (this.grid == null || !this.grid.isWorking()) return false;
        if (!this.handler.isFilterEnabled()) return true;
        for (int i = 0; i < this.handler.getSlots(); i++) {
            if (this.handler.getStackInSlot(i).isEmpty() && !this.handler.getFilter(i).isEmpty()) return false;
        }
        return true;
    }

    public abstract boolean craft(Level level);

    protected boolean ejectItems(ItemStack result, List<ItemStack> craftRemaining, Direction direction) {
        IItemHandler cap = Objects.requireNonNull(getLevel()).getCapability(
            Capabilities.ItemHandler.BLOCK,
            getBlockPos().relative(direction),
            direction.getOpposite()
        );
        if (cap != null) {
            // 尝试向容器插入物品
            ItemStack remained = ItemHandlerUtil.insertItem(cap, result, true);
            if (!remained.isEmpty()) return true;
            remained = ItemHandlerUtil.insertItem(cap, result, false);
            ejectItem(remained);
            for (ItemStack stack : craftRemaining) {
                remained = ItemHandlerUtil.insertItem(cap, stack, false);
                ejectItem(remained);
            }
        } else {
            // 尝试向世界喷出物品
            Vec3 center = getBlockPos().relative(getDirection()).getCenter();
            AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
            if (!getLevel().noCollision(aabb)) return true;

            ejectItem(result);
            for (ItemStack stack : craftRemaining) {
                ejectItem(stack);
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
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.handler.deserializeNBT(provider, tag.getCompound("Inventory"));
        this.poweredBefore = tag.getBoolean("PoweredBefore");
        this.cooldown = tag.getInt("Cooldown");
        if (!tag.getBoolean("HasDisplayItemStack") || !tag.contains("ResultItemStack")) return;
        CompoundTag rawResult = tag.getCompound("ResultItemStack");
        this.displayingStack = rawResult.contains("id")
                               ? ItemStack.parse(provider, rawResult).orElse(ItemStack.EMPTY)
                               : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", this.handler.serializeNBT(provider));
        tag.putBoolean("PoweredBefore", this.poweredBefore);
        tag.putInt("Cooldown", this.cooldown);
        boolean displaying = this.displayingStack != null && !this.displayingStack.isEmpty();
        tag.putBoolean("HasDisplayItemStack", displaying);
        if (displaying) {
            CompoundTag rawResult = (CompoundTag) this.displayingStack.save(provider);
            tag.put("ResultItemStack", rawResult);
        }
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
        for (int index = 0; index < this.handler.getSlots(); index++) {
            ItemStack itemStack = this.handler.getStackInSlot(index);
            if (this.handler.isSlotDisabled(index) && this.handler.getFilter(index).isEmpty()) { // 槽位为未设置过滤的已禁用槽位
                strength++;
            } else if (!itemStack.isEmpty()) { // 槽位上有物品
                strength++;
                itemIdxList.add(index);
            }
        }
        if (strength < this.handler.getSlots()) return strength;

        // 找到数量最少的序号
        int minIdx = itemIdxList.stream()
            .min(Comparator.comparingInt(idx -> this.handler.getStackInSlot(idx).getCount()))
            .orElse(-1);
        // 不存在说明全是锁住的格子 -> 15
        if (minIdx == -1) return 15;

        // 考虑这个物品的堆叠上限，计算满堆比例
        ItemStack stack = this.handler.getStackInSlot(minIdx);
        int maxStack = stack.getMaxStackSize();
        int count = stack.getCount();
        if (maxStack <= 1) {
            return 15;
        } else if (maxStack == 2) {
            return count == 1 ? 9 : 15;
        }

        int range = 6;
        return count == 1 ? 9 : 9 + ((count - 2) * (range - 1) + (maxStack - 2)) / (maxStack - 2);
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
    public IItemHandler getItemHandler() {
        return this.handler;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.put("Filtering", handler.serializeFiltering());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        handler.deserializeFiltering(data.getCompound("Filtering"));
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void updateDisplayItem(ItemStack stack) {
        this.displayingStack = stack;
    }
}
