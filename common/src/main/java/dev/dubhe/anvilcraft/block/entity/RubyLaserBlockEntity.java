package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RubyLaserBlockEntity extends BaseLaserBlockEntity {

    private RubyLaserBlockEntity(BlockEntityType<?> type,
        BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public RubyLaserBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.OVERSEER.get(), pos, blockState);
    }

    public static @NotNull RubyLaserBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new RubyLaserBlockEntity(type, pos, blockState);
    }
}
