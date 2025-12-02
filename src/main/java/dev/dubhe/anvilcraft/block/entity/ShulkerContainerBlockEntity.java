package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.container.ContainerStorages;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.block.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.item.property.component.ContainerStorageReference;
import dev.dubhe.anvilcraft.network.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ShulkerContainerBlockEntity extends BlockEntity implements IDiskCloneable, MenuProvider {
    private UUID storageId;
    private int openingPlayers = 0;

    public ShulkerContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.setStorageId(tag.getUUID("storageId"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("storageId", this.getStorageId());
    }

    public @Nullable UUID getStorageIdRaw() {
        return this.storageId;
    }

    /**
     * 获取存储ID。
     *
     * @return 存储ID。仅在客户端环境下可能为 {@code null}
     */
    public @UnknownNullability UUID getStorageId() {
        if (this.storageId != null) return this.storageId;
        BlockState state = this.getBlockState();
        ShulkerContainerBlock container = Util.cast(
            state.getBlock(),
            () -> new IllegalArgumentException("Unexpected non shulker container block state")
        );
        var half = state.getValue(container.getPart());
        var storages = ContainerStorages.get();
        if (half.isMain()) {
            if (this.level == null) throw new IllegalStateException("Unexpected no level");
            for (OpenedCube3x3PartHalf value : OpenedCube3x3PartHalf.values()) {
                if (value.isMain()) continue;
                this.level.getBlockEntity(value.fromMain(this.getBlockPos()), ModBlockEntities.SHULKER_CONTAINER.get())
                    .map(ShulkerContainerBlockEntity::getStorageIdRaw)
                    .ifPresent(this::setStorageId);
                if (this.storageId != null) return this.storageId;
            }
            if (this.level instanceof ServerLevel serverLevel) {
                this.setStorageId(storages.create());
                storages.syncToClient(serverLevel, this.getBlockPos(), this.storageId);
            }
            return this.storageId;
        }
        if (this.level == null) throw new IllegalStateException("Unexpected no level");
        this.level.getBlockEntity(half.toMain(this.getBlockPos()), ModBlockEntities.SHULKER_CONTAINER.get())
            .ifPresent(entity -> this.setStorageId(entity.getStorageId()));
        if (this.level instanceof ServerLevel serverLevel) storages.syncToClient(serverLevel, this.getBlockPos(), this.storageId);
        return this.storageId;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        Optional.ofNullable(componentInput.get(ModComponents.CONTAINER_STORAGE))
            .flatMap(ContainerStorageReference::id)
            .ifPresentOrElse(
                this::setStorageId,
                () -> {
                    if (this.level != null && !this.level.isClientSide) this.setStorageId(ContainerStorages.get().create());
                }
        );
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModComponents.CONTAINER_STORAGE, new ContainerStorageReference(Optional.ofNullable(this.getStorageId())));
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.putUUID("CategorySource", this.getStorageId());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        ContainerStorages storages = ContainerStorages.get();
        Util.ifAllPresent(
            storages.getStorage(data.getUUID("CategorySource")),
            () -> storages.getStorage(this.getStorageId()),
            (source, target) -> target.applyCategory(source)
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.shulker_container.title");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        if (this.level != null && !this.level.isClientSide) this.openingPlayers++;
        return new ShulkerContainerMenu(ModMenuTypes.SHULKER_CONTAINER.get(), containerId, inventory, this);
    }

    public void someoneClosedMenu() {
        this.openingPlayers--;
        if (this.openingPlayers <= 0) {
            this.openingPlayers = 0;
            if (this.level == null) return;
            this.level.playSound(null, this.getBlockPos(), SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS);
            ShulkerContainerBlock.updateState(
                this.getBlockState().getBlock(),
                this.level,
                this.getBlockPos(),
                ShulkerContainerBlock.OPENED,
                false,
                Block.UPDATE_ALL
            );
        }
    }

    public void setStorageId(UUID storageId) {
        this.storageId = storageId;
        if (this.level == null || !(this.level instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            new ChunkPos(this.getBlockPos()),
            new ShulkerContainerPackets.IdSync(this.getBlockPos(), this.storageId)
        );
    }

    public void syncStorageId(UUID id) {
        this.storageId = id;
        ModBlocks.SHULKER_CONTAINER.get().forEachPart(
            Objects.requireNonNull(this.level),
            this.getBlockPos(),
            pos -> this.level.getBlockEntity(pos, ModBlockEntities.SHULKER_CONTAINER.get())
                .ifPresent(entity -> entity.storageId = id)
        );
    }
}
