package dev.dubhe.anvilcraft.event.giantanvil;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.GiantAnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GiantAnvilLandingEventListener {
    private static final List<ShockBehaviorDefinition> behaviorDefs = new ArrayList<>();
    public static final Direction[] HORIZONTAL_DIRECTIONS =
            new Direction[]{Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH};

    static {
        behaviorDefs.add(new ShockBehaviorDefinition.SimpleTag(
                BlockTags.WOOL,
                (blockPosList, level) -> {
                    for (BlockPos pos : blockPosList) {
                        BlockState state = level.getBlockState(pos);
                        if (state.is(BlockTags.LEAVES)
                                || state.is(BlockTags.FLOWERS)
                                || state.is(Blocks.RED_MUSHROOM)
                                || state.canBeReplaced()
                                || state.is(Blocks.BROWN_MUSHROOM)
                                || state.is(BlockTags.SNOW)
                                || state.is(BlockTags.ICE)
                        ) {
                            level.destroyBlock(pos, true);
                        }
                    }
                }
        ));
        behaviorDefs.add(new ShockBehaviorDefinition.MatchAll((blockPosList, level) -> {
            for (BlockPos pos : blockPosList) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.LEAVES)
                        || state.is(BlockTags.FLOWERS)
                        || state.is(Blocks.RED_MUSHROOM)
                        || state.canBeReplaced()
                        || state.is(Blocks.BROWN_MUSHROOM)
                        || state.is(BlockTags.SNOW)
                        || state.is(BlockTags.ICE)
                ) {
                    level.destroyBlock(pos, true);
                }
            }
        }));
    }


    /**
     * 撼地
     */
    @SubscribeEvent
    public void onLand(@NotNull GiantAnvilFallOnLandEvent event) {
        BlockPos groundPos = event.getPos().below(2);
        if (isValidShockBaseBlock(groundPos, event.getLevel())) {
            Optional<ShockBehaviorDefinition> definitionOpt = behaviorDefs.stream()
                    .filter(it -> it.cornerMatches(groundPos, event.getLevel()))
                    .min((a, b) -> b.priority() - a.priority());
            if (definitionOpt.isEmpty())
                return;
            final ShockBehaviorDefinition def = definitionOpt.get();
            int radius = (int) Math.ceil(event.getFallDistance());
            BlockPos ground = groundPos.above();
            AABB aabb = AABB.ofSize(Vec3.atCenterOf(ground), radius * 2 + 1, 1, radius * 2 + 1);
            List<LivingEntity> e = event.getLevel().getEntitiesOfClass(LivingEntity.class, aabb);
            for (LivingEntity l : e) {
                if (l.getItemBySlot(EquipmentSlot.FEET).is(Items.AIR)) {
                    l.hurt(event.getLevel().damageSources().fall(), event.getFallDistance() * 2);
                }
            }
            ArrayList<BlockPos> posList = new ArrayList<>();
            System.out.println("radius = " + radius);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = ground.relative(Direction.Axis.X, dx)
                            .relative(Direction.Axis.Z, dz);
                    System.out.println("pos = " + pos);
                    posList.add(pos);
                }
            }
            def.acceptRanges(posList, event.getLevel());
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
