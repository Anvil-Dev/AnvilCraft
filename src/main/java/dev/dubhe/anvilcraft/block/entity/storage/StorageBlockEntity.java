package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.api.StorageComparatorManager;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.TerminalSourceManager;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
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
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
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
        if (id.equals(this.id)) {
            return;
        }
        this.updateStorageId(id);
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_ALL);
        }
    }

    public void clearId() {
        this.updateStorageId(null);
        this.setChanged();
    }

    private void updateStorageId(@Nullable UUID id) {
        final UUID previousId = this.id;
        TerminalSourceManager.unregisterIfApplicable(this);
        StorageComparatorManager.unregisterIfApplicable(this);
        this.id = id;
        if (id != null) {
            TerminalSourceManager.registerIfApplicable(this);
            StorageComparatorManager.registerIfApplicable(this);
        }
        if (!Objects.equals(previousId, id)) {
            this.invalidateStorageCapabilities();
            this.invalidateItemPorts(id);
        }
        this.invalidateItemPorts(previousId);
    }

    private void invalidateStorageCapabilities() {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block) {
            this.invalidatePartCapabilities(level, state, block);
        } else {
            this.invalidateCapabilities();
        }
    }

    private <P extends Enum<P>> void invalidatePartCapabilities(ServerLevel level, BlockState state, AbstractMultiPartBlock<P> block) {
        // 外部设备在所连接的部件坐标缓存库存，即使该部件自身没有方块实体。
        for (P part : block.getParts()) {
            level.invalidateCapabilities(this.getBlockPos().offset(block.offsetFrom(state, part)));
        }
    }

    private void invalidateItemPorts(@Nullable UUID storageId) {
        if (storageId != null && this.level != null && !this.level.isClientSide()) {
            StoragePortManager.invalidateItemPorts(storageId);
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
        input.read("storage_id", UUIDUtil.CODEC).ifPresent(this::updateStorageId);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.id == null && this.level instanceof ServerLevel) {
            this.setId(UUID.randomUUID());
            return;
        }
        this.invalidateStorageCapabilities();
        TerminalSourceManager.registerIfApplicable(this);
        StorageComparatorManager.registerIfApplicable(this);
        this.invalidateItemPorts(this.id);
    }

    @Override
    public void onChunkUnloaded() {
        TerminalSourceManager.unregisterIfApplicable(this);
        StorageComparatorManager.unregisterIfApplicable(this);
        super.onChunkUnloaded();
        this.invalidateStorageCapabilities();
        this.invalidateItemPorts(this.id);
    }

    @Override
    public void setRemoved() {
        TerminalSourceManager.unregisterIfApplicable(this);
        StorageComparatorManager.unregisterIfApplicable(this);
        super.setRemoved();
        this.invalidateStorageCapabilities();
        this.invalidateItemPorts(this.id);
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
        if (this.id == null || ref.id().isPresent()) {
            this.setId(ref.id().orElseGet(UUID::randomUUID));
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        components.set(ModComponents.STORAGE, new StorageRef(this.storageType, this.id));
    }

    public int getComparatorSignal() {
        if (this.level == null || this.level.isClientSide() || this.id == null) return 0;
        var storage = Storages.get().get(this.id).orElse(null);
        if (storage == null || !(storage.getItems() instanceof SpaceSizeItemStacksResourceHandler items)) {
            return 0;
        }
        double fullness = items.getFullness();
        return (int) (fullness * 14) + (fullness > 0 ? 1 : 0);
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

    public boolean isCraftingUnlocked() {
        return this.id != null && Storages.get().get(this.id).map(BaseStorage::isCraftingUnlocked).orElse(false);
    }

    public long getTotalCount() {
        if (this.id == null) return 0;
        return Storages.get().get(this.id).map(storage -> {
            long total = 0;
            var items = storage.getAvailableItems();
            for (int index = 0; index < items.size(); index++) total += items.getAmountAsLong(index);
            return total;
        }).orElse(0L);
    }

    public boolean isEmpty() {
        return this.getTotalCount() == 0;
    }

    public void dropContents(Level level, BlockPos pos) {
        if (level.isClientSide() || this.id == null) return;
        UUID storageId = this.id;
        Storages.get().get(storageId).ifPresent(storage -> {
            var items = storage.getItems();
            for (int index = 0; index < items.size(); index++) {
                long remaining = items.getAmountAsLong(index);
                while (remaining > 0) {
                    int count = (int) Math.min(64, remaining);
                    Block.popResource(level, pos, items.getResource(index).toStack(count));
                    remaining -= count;
                }
            }
            var crafting = storage.getCrafting();
            StorageBlockEntity.dropStack(level, pos, crafting.stonecutterInput().copy());
            for (ItemStack input : crafting.craftingInput()) {
                StorageBlockEntity.dropStack(level, pos, input.copy());
            }
            if (storage.getRecipeBases() != null) {
                for (ItemStack base : storage.getRecipeBases()) {
                    StorageBlockEntity.dropStack(level, pos, base.copy());
                }
            }
            Storages.get().remove(storageId);
        });
    }

    private static void dropStack(Level level, BlockPos pos, ItemStack stack) {
        while (!stack.isEmpty()) {
            Block.popResource(level, pos, stack.split(Math.min(64, stack.getCount())));
        }
    }

    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (this.isEmpty() && !this.isCraftingUnlocked()) {
            UUID storageId = this.id;
            this.clearId();
            if (storageId != null) Storages.get().remove(storageId);
        } else if (player.hasInfiniteMaterials()) {
            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.getBlockPos()))
                .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, this);
            for (ItemStack stack : this.getBlockState().getDrops(builder)) {
                Block.popResource(serverLevel, this.getBlockPos(), stack);
            }
        }
    }
}
