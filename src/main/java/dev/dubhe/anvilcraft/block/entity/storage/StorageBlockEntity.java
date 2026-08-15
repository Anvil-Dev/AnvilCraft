package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;
import javax.annotation.Nullable;

@Getter
public class StorageBlockEntity extends BlockEntity {
    private final StorageType storageType;
    private @Nullable UUID id;

    public StorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, StorageType storageType) {
        super(type, pos, state);
        this.storageType = storageType;
    }

    public void setId(UUID id) {
        if (this.id != null) {
            return;
        }
        this.id = id;
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_ALL);
        }
    }

    public void clearId() {
        this.id = null;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.id != null) {
            tag.putUUID("storage_id", this.id);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("storage_id")) {
            this.id = tag.getUUID("storage_id");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (this.id != null) {
            tag.putUUID("storage_id", this.id);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        StorageRef ref = componentInput.get(ModComponents.STORAGE);
        if (ref == null || ref.type() != this.storageType) {
            return;
        }
        this.setId(ref.id().orElse(UUID.randomUUID()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(ModComponents.STORAGE, new StorageRef(this.storageType, this.id));
    }

    public long getTotalCount() {
        if (this.id == null) {
            return 0;
        }
        return Storages.get().get(this.id)
            .map(storage -> {
                UnlimitedItemStacksResourceHandler items = storage.getItems();
                long total = 0;
                for (int i = 0; i < items.size(); i++) {
                    total += items.getAmountAsLong(i);
                }
                return total;
            })
            .orElse(0L);
    }

    public void dropContents(Level level, BlockPos pos) {
        if (this.id != null) {
            Storages.get().get(this.id).ifPresent(storage -> {
                UnlimitedItemStacksResourceHandler items = storage.getItems();
                for (int i = 0; i < items.size(); i++) {
                    ItemStack stack = items.getUnlimitedStackInSlot(i).toStack();
                    if (stack.isEmpty()) continue;
                    while (!stack.isEmpty()) {
                        Block.popResource(level, pos, stack.split(Math.min(64, stack.getCount())));
                    }
                }
                Storages.get().remove(this.id);
            });
        }
    }
}
