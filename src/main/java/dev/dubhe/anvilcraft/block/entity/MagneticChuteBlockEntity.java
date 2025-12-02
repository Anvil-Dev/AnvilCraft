package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.MagneticChuteBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.MagneticChuteMenu;
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
import org.jetbrains.annotations.Nullable;

public class MagneticChuteBlockEntity extends BaseChuteBlockEntity {
    public MagneticChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
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
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.magnetic_chute");
    }

    @Override
    protected void applySpeed(ItemEntity itemEntity, Direction direction) {
        itemEntity.setDeltaMovement(getOutputSpeed(direction));
    }

    public static Vec3 getOutputSpeed(Direction direction) {
        return new Vec3(
            direction.getStepX(),
            direction.getStepY(),
            direction.getStepZ()
        ).multiply(0.25, 0.25, 0.25);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new MagneticChuteMenu(ModMenuTypes.MAGNETIC_CHUTE.get(), i, inventory, this);
    }
}
