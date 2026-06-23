package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.StorageMenu;
import dev.dubhe.anvilcraft.inventory.state.StorageMenuState;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Getter
public class StorageBlockEntity extends BlockEntity implements MenuProvider {
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
        // 信任加载的数据
        input.read("storage_id", UUIDUtil.CODEC).ifPresent(id -> this.id = id);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        // 信任服务端传来的数据
        input.read("storage_id", UUIDUtil.CODEC).ifPresent(id -> this.id = id);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.store("storage_id", UUIDUtil.CODEC, this.id);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        StorageRef ref = components.get(ModComponents.STORAGE);
        if (ref.type() != this.storageType) {
            return;
        }
        this.setId(ref.id().orElse(UUID.randomUUID()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        components.set(ModComponents.STORAGE, new StorageRef(this.storageType, this.id));
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new StorageMenu(ModMenuTypes.STORAGE.get(), containerId, inventory, this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.id != null) {
            StorageMenuState.clear(this.id);
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
