package dev.dubhe.anvilcraft.block.utility.redstone;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/// 红石元件的朝向保存在底座的局部坐标中，每个安装面对应四个面内方向。
public interface MountedRedstone extends IHammerChangeable {
    EnumProperty<Direction> ATTACHMENT = EnumProperty.create("attachment", Direction.class);

    static BlockState placementState(BlockState base, BlockPlaceContext context, boolean facesPlayer) {
        Direction look = context.getHorizontalDirection();
        return base.setValue(ATTACHMENT, Direction.DOWN)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, facesPlayer ? look.getOpposite() : look);
    }

    static OctahedralGroup mountRotation(Direction attachment) {
        return switch (attachment) {
            case DOWN -> OctahedralGroup.IDENTITY;
            case UP -> OctahedralGroup.BLOCK_ROT_X_180;
            default -> Quadrant.fromXYAngles(Quadrant.R270, Quadrant.parseJson((int) attachment.getOpposite().toYRot()));
        };
    }

    static OctahedralGroup modelRotation(Direction attachment, Direction facing) {
        return mountRotation(attachment).compose(Quadrant.parseJson((int) facing.toYRot()).rotationY);
    }

    static OctahedralGroup modelRotation(BlockState state) {
        return modelRotation(state.getValue(ATTACHMENT), state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    static Direction inputDirection(BlockState state) {
        return mountRotation(state.getValue(ATTACHMENT)).rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    static BlockState transform(BlockState state, OctahedralGroup transform) {
        Direction attachment = transform.rotate(state.getValue(ATTACHMENT));
        Direction input = transform.rotate(inputDirection(state));
        return state.setValue(ATTACHMENT, attachment)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, mountRotation(attachment).inverse().rotate(input));
    }

    @Override
    default boolean change(Player player, BlockPos pos, Level level, ItemStack hammer) {
        BlockState state = level.getBlockState(pos);
        return level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING,
            state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise()));
    }

    @Override
    default EnumProperty<Direction> getChangeableProperty(BlockState state) {
        return BlockStateProperties.HORIZONTAL_FACING;
    }
}
