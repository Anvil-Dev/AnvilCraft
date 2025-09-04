package dev.dubhe.anvilcraft.block.entity;

import com.mojang.datafixers.util.Either;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.block.ShulkerCrateBlock;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;
import java.util.function.Function;

public class ShulkerCrateBlockEntity extends BlockEntity implements IItemHandlerHolder {
    private final Either<ItemStackHandler, BlockPos> stacks;

    public ShulkerCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        ShulkerCrateBlock crate = Util.cast(
            state.getBlock(),
            () -> new IllegalArgumentException("Unexpected non shulker crate block state")
        );
        OpenedCube3x3PartHalf half = state.getValue(crate.getPart());
        if (!half.isMain()) {
            this.stacks = Either.right(half.toMain(pos));
        } else {
            this.stacks = Either.left(new ItemStackHandler(144));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (this.stacks.left().isEmpty()) return;
        this.stacks.orThrow().deserializeNBT(registries, tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.stacks.left().isEmpty()) return;
        tag.merge(this.stacks.orThrow().serializeNBT(registries));
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.stacks.map(
            Function.identity(),
            pos -> Optional.ofNullable(this.level)
                .flatMap(level -> level.getBlockEntity(pos, ModBlockEntities.SHULKER_CRATE.get()))
                .map(ShulkerCrateBlockEntity::getItemHandler)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected no shulker crate in " + pos + " or no handler"))
        );
    }
}
