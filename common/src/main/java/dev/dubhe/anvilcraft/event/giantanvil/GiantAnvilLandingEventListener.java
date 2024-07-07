package dev.dubhe.anvilcraft.event.giantanvil;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.GiantAnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GiantAnvilLandingEventListener {
    private static final List<ShockBehaviorDefinition> behaviorDefs = new ArrayList<>();
    public static final Direction[] HORIZONTAL_DIRECTIONS =
            new Direction[]{Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH};

    static {
        behaviorDefs.add(new ShockBehaviorDefinition.SimpleTag(
                BlockTags.WOOL,
                (blockPosList, level) -> {
                    for (BlockPos blockState : blockPosList) {

                    }
                }
        ));
        behaviorDefs.add(new ShockBehaviorDefinition.MatchAll((blockStates, level) -> {

        }));
    }


    /**
     * 撼地
     */
    @SubscribeEvent
    public void onLand(@NotNull GiantAnvilFallOnLandEvent event) {
        System.out.println("event.getPos() = " + event.getPos());
        BlockPos groundPos = event.getPos().below(2);
        if (isValidShockBaseBlock(groundPos, event.getLevel())) {
            behaviorDefs.stream()
                    .filter(it -> it.cornerMatches(groundPos, event.getLevel()))
                    .sorted((a, b) -> b.priority() - a.priority())
                    .forEach(it -> System.out.println("it.priority() = " + it.priority()));
        }
    }

    private boolean isValidShockBaseBlock(BlockPos centerPos, Level level) {
        BlockState blockState = level.getBlockState(centerPos);
        if (!blockState.is(ModBlocks.HEAVY_IRON_BLOCK.get())) {
            return false;
        }
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            if (!level.getBlockState(centerPos.relative(direction)).is(ModBlocks.HEAVY_IRON_BLOCK.get()))
                return false;
        }
        return true;
    }


}
