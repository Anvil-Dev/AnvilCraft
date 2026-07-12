package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.InfinityItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class CreativeCrateBlockEntity extends BlockEntity implements IItemResourceHandlerHolder {
    private final InfinityItemStackHandler itemHandler = new InfinityItemStackHandler();

    public CreativeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.itemHandler.isEmpty()) {
            output.store("item", ItemStack.CODEC, this.itemHandler.getStack());
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.itemHandler.setStack(
            input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY)
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (!this.itemHandler.isEmpty()) {
            tag.store("item", ItemStack.CODEC, ops, this.itemHandler.getStack());
        }
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.itemHandler;
    }

    private void sendUpdate() {
        if (this.level == null) return;
        this.level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    /**
     * 获取当前显示的物品（用于渲染器）
     */
    public ItemStack getDisplayStack() {
        return this.itemHandler.getStack();
    }

    public boolean onPlayerUse(Player player) {
        ItemStack held = player.getMainHandItem();
        if (this.itemHandler.isEmpty()) {
            if (held.isEmpty()) return false;
            if (!player.level().isClientSide()) {
                this.itemHandler.setStack(held.copyWithCount(1));
                setChanged();
                this.sendUpdate();
            }
            return true;
        }
        if (!player.isCreative()) return true;
        if (!held.isEmpty()) return true;
        if (!player.level().isClientSide()) {
            player.getInventory().placeItemBackInInventory(this.itemHandler.getStack());
            this.itemHandler.setStack(ItemStack.EMPTY);
            setChanged();
            this.sendUpdate();
        }
        return true;
    }

    public boolean onPlayerAttack(Player player) {
        if (this.itemHandler.isEmpty()) return false;
        if (player.level().isClientSide()) return true;

        ItemStack stored = this.itemHandler.getStack();
        int count = player.isShiftKeyDown() && !player.isCreative() ? stored.getMaxStackSize() : 1;
        ItemStack extracted = stored.copyWithCount(count);
        if (!player.addItem(extracted)) {
            Block.popResource(player.level(), BlockPos.containing(player.position()), extracted);
        }
        if (player.isCreative()) {
            this.itemHandler.setStack(ItemStack.EMPTY);
            this.setChanged();
            this.sendUpdate();
        }
        return true;
    }
}
