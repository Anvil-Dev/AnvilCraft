package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin implements IHammerChangeable {

    @Override
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer) {
        BlockState blockState = level.getBlockState(blockPos);
        // 活塞已伸出，不旋转
        if (blockState.getValue(PistonBaseBlock.EXTENDED)) {
            return false;
        }
        BlockState state =
                switch (blockState.getValue(DirectionalBlock.FACING)) {
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
}
