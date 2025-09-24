package dev.dubhe.anvilcraft.block.entity;

import com.mojang.datafixers.util.Either;
import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.api.container.ContainerStorages;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.block.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.item.property.component.ContainerStorageReference;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class ShulkerContainerBlockEntity extends BlockEntity implements IDiskCloneable, MenuProvider {
    private Either<UUID, BlockPos> stacks;

    public ShulkerContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        ShulkerContainerBlock crate = Util.cast(
            state.getBlock(),
            () -> new IllegalArgumentException("Unexpected non shulker crate block state")
        );
        OpenedCube3x3PartHalf half = state.getValue(crate.getPart());
        if (!half.isMain()) {
            this.stacks = Either.right(half.toMain(pos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (this.stacks != null && this.stacks.right().isPresent()) return;
        this.stacks = Either.left(tag.getUUID("id"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.stacks == null || this.stacks.left().isEmpty()) return;
        tag.putUUID("id", this.stacks.left().orElseThrow()); // 此处的orElseThrow几乎永远不会抛出
    }

    public UUID getUUID() {
        if (this.stacks == null) this.stacks = Either.left(ContainerStorages.get().create());
        return this.stacks.map(
            Function.identity(),
            pos -> Objects.requireNonNull(this.level).getBlockEntity(pos, ModBlockEntities.SHULKER_CONTAINER.get())
                .map(ShulkerContainerBlockEntity::getUUID)
                .orElseThrow()
        );
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        UUID uuid = Optional.ofNullable(componentInput.get(ModComponents.CONTAINER_STORAGE))
            .flatMap(ContainerStorageReference::id)
            .orElse(null);
        if (this.stacks != null && this.stacks.right().isPresent()) return;
        if (uuid == null) uuid = ContainerStorages.get().create();
        this.stacks = Either.left(uuid);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.stacks == null || this.stacks.left().isEmpty()) return;
        components.set(ModComponents.CONTAINER_STORAGE, new ContainerStorageReference(this.stacks.left()));
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.putUUID("CategorySource", this.getUUID());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        ContainerStorages storages = ContainerStorages.get();
        ContainerStorage source = storages.getOrCreateStorage(data.getUUID("CategorySource"));
        ContainerStorage target = storages.getOrCreateStorage(this.getUUID());
        target.applyCategory(source);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.shulker_container.title");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new ShulkerContainerMenu(ModMenuTypes.SHULKER_CONTAINER.get(), containerId, inventory, this);
    }
}
