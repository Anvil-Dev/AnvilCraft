package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

public class HeavyIronPlateBlock extends Block implements IHammerRemovable {

    public static final VoxelShape AABB = Block.box(0, 12, 0, 16, 16, 16);

    public HeavyIronPlateBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull VoxelShape getShape(
        @Nonnull BlockState blockState,
        @Nonnull BlockGetter blockGetter,
        @Nonnull BlockPos blockPos,
        @Nonnull CollisionContext collisionContext
    ) {
        return AABB;
    }
}
