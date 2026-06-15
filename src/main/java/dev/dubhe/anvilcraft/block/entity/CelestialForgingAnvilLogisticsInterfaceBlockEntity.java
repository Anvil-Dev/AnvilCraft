package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Logistics interface for the Celestial Forging Anvil.
 * Stores 16 item types, each up to 16 stacks (1024 items per type).
 * Items auto-route to their type's slot and don't overflow to other slots.
 */
public class CelestialForgingAnvilLogisticsInterfaceBlockEntity extends BlockEntity {
    private static final int TYPE_COUNT = 16;
    private static final int STACKS_PER_TYPE = 16;
    private static final int MAX_PER_SLOT = STACKS_PER_TYPE * 64;

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(TYPE_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            ItemStack current = getStackInSlot(slot);
            if (current.isEmpty()) {
                for (int i = 0; i < TYPE_COUNT; i++) {
                    if (i != slot && ItemStack.isSameItemSameComponents(getStackInSlot(i), stack)) {
                        return false;
                    }
                }
                return true;
            }
            return ItemStack.isSameItemSameComponents(current, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            CelestialForgingAnvilLogisticsInterfaceBlockEntity.this.setChanged();
        }
    };

    public CelestialForgingAnvilLogisticsInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        for (int i = 0; i < TYPE_COUNT; i++) {
            itemHandler.setSlotLimit(i, MAX_PER_SLOT);
            itemHandler.setSlotDisabled(i, false);
        }
    }

    /**
     * Sync block entity data to all tracking clients.
     */
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(serverLevel.getChunkAt(worldPosition).getPos(), false)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @SuppressWarnings("unused")
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    // === Temple demand display (pushed by CFA controller) ===
    @Getter @Setter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    @Getter @Setter
    private int templeDemandCount = 0;
    @Getter @Setter
    private boolean templeDemandSatisfied = false;

    // === Collider target items display (pushed by CFA controller) ===
    @Getter @Setter
    private List<ItemStack> colliderTargetItems = new ArrayList<>();
    @Getter @Setter
    private boolean colliderProcessing = false;
    @Getter @Setter
    private boolean colliderStarMissing = false;

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemandItem", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        if (!colliderTargetItems.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.add(stack.save(registries));
                }
            }
            tag.put("colliderTargetItems", list);
        }
        tag.putBoolean("colliderProcessing", colliderProcessing);
        tag.putBoolean("colliderStarMissing", colliderStarMissing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("templeDemandItem")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemandItem"))
                .orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        this.colliderTargetItems.clear();
        if (tag.contains("colliderTargetItems")) {
            ListTag list = tag.getList("colliderTargetItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(colliderTargetItems::add);
            }
        }
        this.colliderProcessing = tag.getBoolean("colliderProcessing");
        this.colliderStarMissing = tag.getBoolean("colliderStarMissing");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemandItem", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        if (!colliderTargetItems.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : colliderTargetItems) {
                if (!stack.isEmpty()) {
                    list.add(stack.save(registries));
                }
            }
            tag.put("colliderTargetItems", list);
        }
        tag.putBoolean("colliderProcessing", colliderProcessing);
        tag.putBoolean("colliderStarMissing", colliderStarMissing);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("templeDemandItem")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemandItem"))
                .orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        this.colliderTargetItems.clear();
        if (tag.contains("colliderTargetItems")) {
            ListTag list = tag.getList("colliderTargetItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(colliderTargetItems::add);
            }
        }
        this.colliderProcessing = tag.getBoolean("colliderProcessing");
        this.colliderStarMissing = tag.getBoolean("colliderStarMissing");
    }
}
