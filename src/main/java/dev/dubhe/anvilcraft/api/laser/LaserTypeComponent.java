package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.LensBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

public record LaserTypeComponent(boolean gamma) implements ILaserComponent {
    @Override
    public void onEmitPre(ILaserComponentOwner owner) {
        if (gamma) owner.setLaserMaxLength(Math.min(16, owner.getLaserMaxLength()));
    }

    public boolean canPassThrough(Level level, Direction direction, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (gamma) return state.is(BlockTags.REPLACEABLE);
        if (state.is(ModBlockTags.LASER_CAN_PASS_THROUGH)
            || state.is(Tags.Blocks.GLASS_BLOCKS)
            || state.is(Tags.Blocks.GLASS_PANES)
            || state.is(BlockTags.REPLACEABLE)) {
            return true;
        }
        if (state.getBlock() instanceof LensBlock
            && state.getValue(LensBlock.TYPE) == LensType.NONE
            && direction.getAxis() == state.getValue(LensBlock.AXIS)) {
            return true;
        }
        if (!AnvilCraft.CONFIG.isLaserDoImpactChecking) return false;
        AABB bounds = switch (direction.getAxis()) {
            case X -> Block.box(0, 7, 7, 16, 9, 9).bounds();
            case Y -> Block.box(7, 0, 7, 9, 16, 9).bounds();
            case Z -> Block.box(7, 7, 0, 9, 9, 16).bounds();
        };
        return state.getCollisionShape(level, pos).toAabbs().stream().noneMatch(bounds::intersects);
    }

    public static boolean isGamma(ILaserComponentOwner owner) {
        LaserTypeComponent component = owner.getComponent(LaserComponentTypes.LASER_TYPE);
        return component != null && component.gamma;
    }
}
