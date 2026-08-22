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
 * 冲压台方块实体，用于存储原料与产物，并在铁砧砸落时执行冲压配方。
 */
@Getter
public class StampingPlatformBlockEntity extends BlockEntity implements IItemHandlerHolder, IItemHandlerCache {
    public static final int INPUT_SLOTS = 1;
    public static final int OUTPUT_SLOTS = 8;

    private final ItemStackHandler input = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            StampingPlatformBlockEntity.this.setChanged();
            StampingPlatformBlockEntity.this.sendUpdate();
        }
    };

    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            StampingPlatformBlockEntity.this.setChanged();
            StampingPlatformBlockEntity.this.sendUpdate();
        }
    };

    /**
     * 统一视图：前段为输出产物，后段为输入原料；自动化只能向原料槽插入物品。
     */
    private final ItemStackHandler proxy = new ItemStackHandler(INPUT_SLOTS + OUTPUT_SLOTS) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < OUTPUT_SLOTS
                   ? StampingPlatformBlockEntity.this.output.getStackInSlot(slot)
                   : StampingPlatformBlockEntity.this.input.getStackInSlot(slot - OUTPUT_SLOTS);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < OUTPUT_SLOTS
                   ? StampingPlatformBlockEntity.this.output.getSlotLimit(slot)
                   : StampingPlatformBlockEntity.this.input.getSlotLimit(slot - OUTPUT_SLOTS);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= OUTPUT_SLOTS && StampingPlatformBlockEntity.this.input.isItemValid(slot - OUTPUT_SLOTS, stack);
        }

        @Override
        public void setSize(int size) {
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot < OUTPUT_SLOTS) {
                StampingPlatformBlockEntity.this.output.setStackInSlot(slot, stack);
            } else {
                StampingPlatformBlockEntity.this.input.setStackInSlot(slot - OUTPUT_SLOTS, stack);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < OUTPUT_SLOTS) return stack;
            return StampingPlatformBlockEntity.this.input.insertItem(slot - OUTPUT_SLOTS, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < OUTPUT_SLOTS
                   ? StampingPlatformBlockEntity.this.output.extractItem(slot, amount, simulate)
                   : StampingPlatformBlockEntity.this.input.extractItem(slot - OUTPUT_SLOTS, amount, simulate);
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
        return this.output;
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
        tag.put("Outputs", this.output.serializeNBT(registries));
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
        for (ItemStackHandler handler : List.of(this.output, this.input)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack;
                while (!(stack = handler.extractItem(slot, Integer.MAX_VALUE, false)).isEmpty()) {
                    stacks.add(stack);
                }
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
        tag.put("Outputs", this.output.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.input.deserializeNBT(registries, tag.getCompound("Inputs"));
        this.output.deserializeNBT(registries, tag.getCompound("Outputs"));
    }
}
