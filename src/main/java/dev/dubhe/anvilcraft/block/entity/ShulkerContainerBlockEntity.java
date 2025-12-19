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
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ShulkerContainerBlockEntity extends BlockEntity implements IDiskCloneable, MenuProvider {
    private UUID storageId;
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            ShulkerContainerBlockEntity.playSound(level, pos, state, SoundEvents.CHEST_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            ShulkerContainerBlockEntity.playSound(level, pos, state, SoundEvents.CHEST_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
            ShulkerContainerBlockEntity.this.signalOpenCount(level, pos, state, count, openCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            if (!(player.containerMenu instanceof ShulkerContainerMenu menu)) return false;
            return menu.blockEntity == ShulkerContainerBlockEntity.this;
        }
    };

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
        UUID id = this.getStorageId();
        if (id == null) return;
        tag.putUUID("storageId", id);
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
        if (this.level != null && !this.level.isClientSide && !this.remove) {
            this.openersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }

        return new ShulkerContainerMenu(ModMenuTypes.SHULKER_CONTAINER.get(), containerId, inventory, this);
    }

    public void someoneClosedMenu(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public void setStorageId(UUID storageId) {
        this.storageId = storageId;
        if (!(this.level instanceof ServerLevel serverLevel)) return;
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

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            ShulkerContainerBlock.updateState(
                this.getBlockState().getBlock(),
                this.level,
                this.getBlockPos(),
                ShulkerContainerBlock.OPENED,
                type > 0,
                Block.UPDATE_ALL
            );
            return true;
        }
        return super.triggerEvent(id, type);
    }

    protected void signalOpenCount(Level level, BlockPos pos, BlockState state, int eventId, int eventParam) {
        level.blockEvent(pos, state.getBlock(), eventId, eventParam);
    }

    static void playSound(Level level, BlockPos pos, BlockState state, SoundEvent sound) {
        level.playSound(
            null,
            state.getValue(ShulkerContainerBlock.HALF).toMain(pos),
            sound,
            SoundSource.BLOCKS,
            0.5F,
            level.random.nextFloat() * 0.1F + 0.9F
        );
    }
}
