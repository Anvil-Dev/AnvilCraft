package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.api.StorageComparatorManager;
import dev.dubhe.anvilcraft.api.TerminalSourceManager;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

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
        TerminalSourceManager.registerIfApplicable(this);
        StorageComparatorManager.registerIfApplicable(this);
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        if (this.id != null) {
            output.store("storage_id", UUIDUtil.CODEC, this.id);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        StorageComparatorManager.unregisterIfApplicable(this);
        // 信任加载的数据
        input.read("storage_id", UUIDUtil.CODEC).ifPresent(id -> this.id = id);
        TerminalSourceManager.registerIfApplicable(this);
        StorageComparatorManager.registerIfApplicable(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        TerminalSourceManager.registerIfApplicable(this);
        StorageComparatorManager.registerIfApplicable(this);
    }

    @Override
    public void onChunkUnloaded() {
        TerminalSourceManager.unregisterIfApplicable(this);
        StorageComparatorManager.unregisterIfApplicable(this);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        TerminalSourceManager.unregisterIfApplicable(this);
        StorageComparatorManager.unregisterIfApplicable(this);
        super.setRemoved();
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        // 信任服务端传来的数据
        input.read("storage_id", UUIDUtil.CODEC).ifPresent(id -> this.id = id);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (this.id != null) {
            tag.store("storage_id", UUIDUtil.CODEC, this.id);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        StorageRef ref = components.get(ModComponents.STORAGE);
        if (ref == null || ref.type() != this.storageType) {
            return;
        }
        this.setId(ref.id().orElse(UUID.randomUUID()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        components.set(ModComponents.STORAGE, new StorageRef(this.storageType, this.id));
    }

    public int getComparatorSignal() {
        if (this.level == null || this.level.isClientSide() || this.id == null) return 0;
        var storage = Storages.get().get(this.id).orElse(null);
        return storage != null && storage.getItems() instanceof SpaceSizeItemStacksResourceHandler items
            ? (int) Math.ceil(items.getFullness() * 15.0) : 0;
    }

    public void refreshComparatorSignal() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.updateNeighbourForOutputSignal(this.getBlockPos(), this.getBlockState().getBlock());
        }
    }

    public static void applyPickStorageId(
        ItemStack stack, LevelReader level, BlockPos pos, BlockState state, boolean includeData
    ) {
        if (!includeData) return;
        BlockPos mainPos = state.getBlock() instanceof AbstractMultiPartBlock<?> block
            ? block.getMainPartPos(pos, state) : pos;
        if (level.hasChunk(mainPos.getX() >> 4, mainPos.getZ() >> 4)
            && level.getBlockEntity(mainPos) instanceof StorageBlockEntity storage && storage.id != null) {
            stack.set(ModComponents.STORAGE, new StorageRef(storage.storageType, storage.id));
        }
    }

    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && this.getId() != null) {
            ItemStack itemStack = new ItemStack(state.getBlock());
            itemStack.applyComponents(this.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemStack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }
}
