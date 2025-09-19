package dev.dubhe.anvilcraft.block.entity;

import com.mojang.datafixers.util.Either;
import dev.dubhe.anvilcraft.api.crate.ShulkerCrateStorage;
import dev.dubhe.anvilcraft.block.ShulkerCrateBlock;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.CrateStorageReference;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

public class ShulkerCrateBlockEntity extends BlockEntity {
    @Getter
    private Either<UUID, BlockPos> stacks;

    public ShulkerCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        ShulkerCrateBlock crate = Util.cast(
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

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        UUID uuid = Optional.ofNullable(componentInput.get(ModComponents.CRATE_STORAGE))
            .flatMap(CrateStorageReference::id)
            .orElse(null);
        if (this.stacks != null && this.stacks.right().isPresent()) return;
        if (uuid == null) uuid = ShulkerCrateStorage.get().create();
        this.stacks = Either.left(uuid);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.stacks == null || this.stacks.left().isEmpty()) return;
        components.set(ModComponents.CRATE_STORAGE, new CrateStorageReference(this.stacks.left()));
    }
}
