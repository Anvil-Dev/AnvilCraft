package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.block.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.item.property.component.SCStorageRef;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorage;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorages;
import dev.dubhe.anvilcraft.saved.sc.server.ServerSCStorage;
import dev.dubhe.anvilcraft.saved.sc.server.ServerSCStorages;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.sc.SCBESyncer;
import dev.dubhe.anvilcraft.util.sc.SCBESyncers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Optional;
import java.util.UUID;

public class ShulkerContainerBlockEntity extends BlockEntity implements MenuProvider, IDiskCloneable {
    private final SCBESyncer syncer;

    public ShulkerContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.syncer = SCBESyncers.get(this.level).register(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!this.getBlockState().getValue(ShulkerContainerBlock.HALF).isMain()) return;
        this.setStorageId(tag.getUUID("storageId"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.getBlockState().getValue(ShulkerContainerBlock.HALF).isMain()) return;
        UUID id = this.getStorageId();
        if (id == null) return;
        tag.putUUID("storageId", id);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        Optional.ofNullable(componentInput.get(ModComponents.SC_STORAGE))
            .flatMap(SCStorageRef::id)
            .ifPresentOrElse(
                this::setStorageId,
                () -> {
                    if (this.level != null && !this.level.isClientSide) this.setStorageId(ServerSCStorages.get().create());
                }
        );
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModComponents.SC_STORAGE, new SCStorageRef(Optional.ofNullable(this.getStorageId())));
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.putUUID("CategorySource", this.getStorageId());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        Util.ifAllPresent(
            ServerSCStorages.get().get(this.getStorageId()),
            () -> ServerSCStorages.get().get(data.getUUID("CategorySource")),
            ServerSCStorage::applyCategory
        );
    }

    @Override
    public Component getDisplayName() {
        if (this.level.isClientSide) {
            return ClientSCStorages.get(this.getStorageId())
                .map(ClientSCStorage::getName)
                .orElse(ModBlocks.SHULKER_CONTAINER.get().getName());
        } else {
            return ServerSCStorages.get().getOrCreate(this.getStorageId()).getName();
        }
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.getStorageId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.level.isClientSide) return null;
        if (player.isSpectator()) return null;
        if (this.remove) return null;
        this.someoneOpened(player);
        ServerSCStorages.get().sync2C(Util.cast(this.level), this.getBlockPos(), this.getStorageId());
        return new ShulkerContainerMenu(ModMenuTypes.SHULKER_CONTAINER.get(), containerId, inventory, this.getStorageId(), this);
    }

    public void someoneOpened(Player player) {
        if (!this.remove && !player.isSpectator()) this.syncer.someoneOpened(this.getLevel(), this.getBlockState());
    }

    public void someoneClosed(Player player) {
        if (!this.remove && !player.isSpectator()) this.syncer.someoneClosed(this.getLevel(), this.getBlockState());
    }

    /**
     * 获取存储ID。
     *
     * @return 存储ID。仅在客户端环境下可能为 {@code null}
     */
    public @UnknownNullability UUID getStorageId() {
        return this.syncer.getStorageId();
    }

    public void setStorageId(UUID storageId) {
        this.syncer.setStorageId(storageId);
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            new ChunkPos(this.getBlockPos()),
            new ShulkerContainerPackets.IdSync(this.getBlockPos(), this.syncer.getStorageId())
        );
    }

    @Override
    public void setRemoved() {
        SCBESyncers.get(this.level).remove(this);
        super.setRemoved();
    }
}
