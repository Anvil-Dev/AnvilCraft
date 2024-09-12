package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.MagneticChuteBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.MagneticChuteMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MagneticChuteBlockEntity extends BaseChuteBlockEntity {

    public MagneticChuteBlockEntity(
            BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return false;
    }

    @Override
    protected boolean validateBlockState(BlockState state) {
        return state.is(ModBlocks.MAGNETIC_CHUTE.get());
    }

    @Override
    protected boolean isEnabled() {
        return !getBlockState().getValue(MagneticChuteBlock.POWERED);
    }

    @Override
    protected DirectionProperty getFacingProperty() {
        return MagneticChuteBlock.FACING;
    }

    @Override
    protected Direction getOutputDirection() {
        return getDirection();
    }

    @Override
    protected Direction getInputDirection() {
        return getOutputDirection().getOpposite();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.anvilcraft.magnetic_chute");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(
            int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new MagneticChuteMenu(ModMenuTypes.MAGNETIC_CHUTE.get(), i, inventory, this);
    }
}
