package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.InfinityItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class CreativeCrateBlockEntity extends BlockEntity implements IItemHandlerHolder {
    @Getter
    private final InfinityItemStackHandler itemStackHandler = new InfinityItemStackHandler() {
        @Override
        protected void onContentsChanged(int slot) {
            CreativeCrateBlockEntity.this.setChanged();
            if (CreativeCrateBlockEntity.this.level != null) {
                CreativeCrateBlockEntity.this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public CreativeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag compoundTag = this.itemStackHandler.serializeNBT(registries);
        tag.put("item", compoundTag);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.itemStackHandler.getStackInSlot(0).isEmpty()) {
            CompoundTag compoundTag = this.itemStackHandler.serializeNBT(registries);
            tag.put("item", compoundTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("item")) {
            CompoundTag item = tag.getCompound("item");
            this.itemStackHandler.deserializeNBT(registries, item);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = this.saveCustomOnly(registries);
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
        stack.applyComponents(this.collectComponents());
    }

    public boolean onPlayerAttack(Player player) {
        ItemStack stored = this.itemStackHandler.getStackInSlot(0);
        if (this.level == null || stored.isEmpty()) {
            return false;
        }
        if (!this.level.isClientSide()) {
            ItemStack extracted = stored.copyWithCount(player.isShiftKeyDown() ? stored.getMaxStackSize() : 1);
            if (!player.addItem(extracted)) {
                Block.popResource(this.level, BlockPos.containing(player.position()), extracted);
            }
        }
        return true;
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.itemStackHandler;
    }
}
