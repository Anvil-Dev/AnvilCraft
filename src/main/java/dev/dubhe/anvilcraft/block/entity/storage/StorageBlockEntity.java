package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

@Getter
public class StorageBlockEntity extends BlockEntity {
    private final StorageType storageType;
    private UUID id;

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

    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative() && this.getId() != null) {
            ItemStack itemStack = new ItemStack(state.getBlock());
            itemStack.applyComponents(this.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemStack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }
}
