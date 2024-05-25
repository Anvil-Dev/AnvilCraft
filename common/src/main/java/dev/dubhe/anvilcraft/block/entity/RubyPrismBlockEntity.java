package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RubyPrismBlockEntity extends BaseLaserBlockEntity {

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
}
