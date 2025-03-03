package dev.dubhe.anvilcraft.api;

import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockPlaceAssist {

    private static List<Direction> orderDirectionByDistance(
        BlockPos pos, Vec3 hit, Predicate<Direction> includeDirection) {
        Vec3 centerToHit = hit.subtract(Vec3.atLowerCornerOf(pos).add(.5f, .5f, .5f));
        return Arrays.stream(Direction.values())
            .filter(includeDirection)
            .map(dir -> Pair.of(dir, Vec3.atLowerCornerOf(dir.getNormal()).distanceTo(centerToHit)))
            .sorted(Comparator.comparingDouble(Pair::getSecond))
            .map(Pair::getFirst)
            .collect(Collectors.toList());
    }

    /**
     *
     */
    public static InteractionResult tryPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit,
        Item blockItem,
        EnumProperty<Direction.Axis> propertyDef,
        BlockState newBlockState
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() || !player.mayBuild()) return InteractionResult.PASS;
        ItemStack itemInHand = player.getItemInHand(hand);
        if (itemInHand.is(blockItem)) {
            for (Direction direction : orderDirectionByDistance(
                pos,
                hit.getLocation(),
                dir -> dir.getAxis() == state.getValue(propertyDef)
            )) {
                int length = 0;
                BlockPos blockPos = pos.relative(direction);

                BlockState blockState = level.getBlockState(blockPos);
                while (!blockState.isAir()
                    && blockState.is(newBlockState.getBlock())
                    && blockState.getValue(propertyDef) == direction.getAxis()
                    && length <= 6) {
                    ++length;
                    blockPos = blockPos.relative(direction);
                    blockState = level.getBlockState(blockPos);
                }
                if (level.isOutsideBuildHeight(blockPos)) {
                    return InteractionResult.PASS;
                }
                if (blockState.canBeReplaced()) {
                    level.setBlockAndUpdate(blockPos, newBlockState.setValue(propertyDef, direction.getAxis()));
                    SoundType soundType = newBlockState.getSoundType();
                    level.playSound(
                        null,
                        blockPos,
                        soundType.getPlaceSound(),
                        SoundSource.BLOCKS,
                        (soundType.volume + 1) / 2.0f,
                        soundType.pitch * 0.8f
                    );
                    if (!player.getAbilities().instabuild) itemInHand.shrink(1);
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }
}
