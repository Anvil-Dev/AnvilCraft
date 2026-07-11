package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.util.PistonMoveGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(PistonBaseBlock.class)
abstract class PistonBaseBlockMixin implements IHammerChangeable {

    @WrapMethod(method = "moveBlocks")
    private boolean reserveMovingBlocks(
        Level level,
        BlockPos pistonPos,
        Direction direction,
        boolean extending,
        Operation<Boolean> original
    ) {
        if (level.isClientSide()) return original.call(level, pistonPos, direction, extending);
        try (PistonMoveGuard.Scope ignored = PistonMoveGuard.begin(level)) {
            return original.call(level, pistonPos, direction, extending);
        }
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState blockState = level.getBlockState(blockPos);
        // 活塞已伸出，不旋转
        if (blockState.getValue(PistonBaseBlock.EXTENDED)) {
            return false;
        }
        BlockState state = switch (blockState.getValue(DirectionalBlock.FACING)) {
            case UP -> blockState.setValue(DirectionalBlock.FACING, Direction.DOWN);
            case DOWN -> blockState.setValue(DirectionalBlock.FACING, Direction.NORTH);
            case NORTH -> blockState.setValue(DirectionalBlock.FACING, Direction.EAST);
            case EAST -> blockState.setValue(DirectionalBlock.FACING, Direction.SOUTH);
            case SOUTH -> blockState.setValue(DirectionalBlock.FACING, Direction.WEST);
            case WEST -> blockState.setValue(DirectionalBlock.FACING, Direction.UP);
        };
        level.setBlockAndUpdate(blockPos, state);
        return true;
    }

    @Override
    public boolean checkBlockState(BlockState blockState) {
        return !blockState.getValue(PistonBaseBlock.EXTENDED);
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return PistonBaseBlock.FACING;
    }
}
