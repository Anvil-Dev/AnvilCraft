package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.MagneticChuteBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.MagneticChuteMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MagneticChuteBlockEntity extends BaseChuteBlockEntity {

    public MagneticChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected boolean shouldSkipDirection(@NotNull Direction direction) {
        return false;
    }

    @Override
    protected boolean validateBlockState(@NotNull BlockState state) {
        return state.is(ModBlocks.MAGNETIC_CHUTE.get());
    }

    @Override
    protected boolean isEnabled() {
        return getBlockState().getValue(MagneticChuteBlock.ENABLED);
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

    @Override
    protected void applySpeed(ItemEntity itemEntity, Direction direction) {
        Vec3 delta = new Vec3(
            direction.getStepX(),
            direction.getStepY(),
            direction.getStepZ()
        );
        itemEntity.setDeltaMovement(
            delta.multiply(0.25, 0.25, 0.25)
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        if (player.isSpectator()) return null;
        return new MagneticChuteMenu(ModMenuTypes.MAGNETIC_CHUTE.get(), i, inventory, this);
    }
}
