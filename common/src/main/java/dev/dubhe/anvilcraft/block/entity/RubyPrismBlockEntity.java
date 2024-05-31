package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.RubyPrismBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class RubyPrismBlockEntity extends BaseLaserBlockEntity {
    private boolean isOverload = false;

    private RubyPrismBlockEntity(BlockEntityType<?> type,
        BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public RubyPrismBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.OVERSEER.get(), pos, blockState);
    }

    public static @NotNull RubyPrismBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new RubyPrismBlockEntity(type, pos, blockState);
    }

    public void tick(@NotNull Level level) {
        if (isOverload) emitLaser(getDirection());
        super.tick(level);
    }

    @Override
    protected int getBasePower() {
        return 0;
    }

    @Override
    public boolean isSwitch() {
        return isOverload;
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity baseLaserBlockEntity) {
        isOverload = false;
        super.onCancelingIrradiation(baseLaserBlockEntity);
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
        isOverload = true;
        super.onIrradiated(baseLaserBlockEntity);
    }

    @Override
    public Direction getDirection() {
        return getBlockState().getValue(RubyPrismBlock.FACING);
    }
}
