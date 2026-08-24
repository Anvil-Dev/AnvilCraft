package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.recipe.cache.IItemHandlerCache;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 冲压平台方块实体，用于存储原料，并在铁砧砸落时执行冲压配方。
 */
@Getter
public class StampingPlatformBlockEntity extends BlockEntity implements IItemHandlerHolder, IItemHandlerCache {
    public static final int INPUT_SLOTS = 8;

    private final ItemStackHandler input = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int sameItemCount = 0;
            for (int index = 0; index < INPUT_SLOTS; index++) {
                ItemStack existing = this.getStackInSlot(index);
                if (!existing.isEmpty() && ItemStack.isSameItem(existing, stack)) {
                    sameItemCount += existing.getCount();
                }
            }
            int acceptableCount = Math.min(
                stack.getMaxStackSize() - sameItemCount,
                stack.getCount()
            );
            if (acceptableCount <= 0) return stack;

            ItemStack acceptable = stack.copyWithCount(acceptableCount);
            ItemStack remaining = super.insertItem(slot, acceptable, simulate);
            if (simulate) {
                int rejectedCount = remaining.getCount() + stack.getCount() - acceptable.getCount();
                return rejectedCount == 0 ? ItemStack.EMPTY : stack.copyWithCount(rejectedCount);
            }
            if (remaining.isEmpty()) {
                return stack.copyWithCount(stack.getCount() - acceptable.getCount());
            }
            remaining.grow(stack.getCount() - acceptable.getCount());
            return remaining;
        }

        @Override
        protected void onContentsChanged(int slot) {
            StampingPlatformBlockEntity.this.setChanged();
            StampingPlatformBlockEntity.this.sendUpdate();
        }
    };

    /**
     * 物品处理器统一视图：仅暴露原料槽；自动化只能向原料槽插入物品。
     */
    private final ItemStackHandler proxy = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            return StampingPlatformBlockEntity.this.input.getStackInSlot(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return StampingPlatformBlockEntity.this.input.getSlotLimit(slot);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return StampingPlatformBlockEntity.this.input.isItemValid(slot, stack);
        }

        @Override
        public void setSize(int size) {
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            StampingPlatformBlockEntity.this.input.setStackInSlot(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return StampingPlatformBlockEntity.this.input.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return StampingPlatformBlockEntity.this.input.extractItem(slot, amount, simulate);
        }
    };

    public StampingPlatformBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.proxy;
    }

    @Override
    public IItemHandler getInput() {
        return this.input;
    }

    @Override
    public IItemHandler getOutput() {
        return EMPTY_OUTPUT;
    }

    private void sendUpdate() {
        if (this.level == null || this.level.isClientSide()) return;
        this.level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Inputs", this.input.serializeNBT(registries));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 手持物品时将手中物品插入原料槽，空手时取出全部内容物。
     *
     * @return 是否完成了交互
     */
    public boolean tryInteractItems(Player player, InteractionHand hand) {
        if (this.level == null || hand != InteractionHand.MAIN_HAND) return false;
        ItemStack inHand = player.getItemInHand(hand);
        if (inHand.isEmpty()) {
            List<ItemStack> stacks = new ArrayList<>();
            this.extractAllItems(stacks);
            if (stacks.isEmpty()) return false;
            if (this.level.isClientSide()) return true;
            Inventory inventory = player.getInventory();
            for (ItemStack stack : stacks) {
                inventory.placeItemBackInInventory(stack);
            }
            return true;
        }
        if (this.level.isClientSide()) return true;
        ItemStack remaining = ItemHandlerUtil.insertItem(this.input, inHand.copy(), false);
        int count = inHand.getCount();
        inHand.setCount(remaining.getCount());
        return remaining.getCount() != count;
    }

    private void extractAllItems(List<ItemStack> stacks) {
        for (int slot = 0; slot < this.input.getSlots(); slot++) {
            ItemStack stack;
            while (!(stack = this.input.extractItem(slot, Integer.MAX_VALUE, false)).isEmpty()) {
                stacks.add(stack);
            }
        }
    }

    public void dropAllContent(Level level, BlockPos pos) {
        List<ItemStack> stacks = new ArrayList<>();
        this.extractAllItems(stacks);
        for (ItemStack stack : stacks) {
            Block.popResource(level, pos, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inputs", this.input.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.input.deserializeNBT(registries, tag.getCompound("Inputs"));
        if (this.input.getSlots() != INPUT_SLOTS) {
            List<ItemStack> items = ItemHandlerUtil.getNonEmptyItemsFromHandler(this.input);
            this.input.setSize(INPUT_SLOTS);
            for (ItemStack item : items) this.input.insertItem(0, item, false);
        }
    }

    private static final ItemStackHandler EMPTY_OUTPUT = new ItemStackHandler(0);
}