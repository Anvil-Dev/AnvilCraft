package dev.dubhe.anvilcraft.api;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.block.state.LightColor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class BonemealManager {
    private static int cooldown = 0;
    private static final Set<Tuple<BlockPos, Level>> lightBlocks = Collections.synchronizedSet(new HashSet<>());

    private static boolean isLighting(@NotNull BlockState state) {
        return state.getValue(InductionLightBlock.POWERED)
            || state.getValue(InductionLightBlock.OVERLOAD);
    }

    private static boolean canCropGrow(@NotNull BlockState state) {
        return state.getValue(InductionLightBlock.COLOR).equals(LightColor.PINK);
    }

    private static HashSet<Tuple<BlockPos, Level>> doRipen(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull HashSet<Tuple<BlockPos, Level>> set) {
        int rangeSize = AnvilCraft.config.inductionLightBlockRipeningRange;
        HashSet<Tuple<BlockPos, Level>> ripened = new HashSet<>();
        for (int i = -rangeSize / 2; i <= rangeSize / 2; i++) {
            for (int j = -rangeSize / 2; j <= rangeSize / 2; j++) {
                for (int k = -rangeSize / 2; k <= rangeSize / 2; k++) {
                    BlockPos pos1 = pos.offset(i, j, k);
                    BlockState state = level.getBlockState(pos1);
                    if (state.getBlock() instanceof BonemealableBlock growable
                        && !growable.getClass().equals(GrassBlock.class)
                        && !growable.getClass().equals(NyliumBlock.class)
                        && !set.contains(new Tuple<>(pos1, level))
                    ) {
                        growable.performBonemeal(
                            (ServerLevel) level,
                            level.getRandom(), pos1, state);
                        ripened.add(new Tuple<>(pos1, level));
                    }
                }
            }
        }
        return ripened;
    }

    /**
     *
     */
    public static void tick() {
        cooldown--;
        if (cooldown <= 0 && !lightBlocks.isEmpty()) {
            cooldown = AnvilCraft.config.inductionLightBlockRipeningCooldown;
            HashSet<Tuple<BlockPos, Level>> ripenedBlocks = new HashSet<>();
            for (Tuple<BlockPos, Level> lightBlock : lightBlocks) {
                BlockState lightBlockState = lightBlock.getB().getBlockState(lightBlock.getA());
                if (lightBlockState.getBlock() instanceof InductionLightBlock) {
                    if (isLighting(lightBlockState) && canCropGrow(lightBlockState)) {
                        HashSet<Tuple<BlockPos, Level>> newRipened =
                            doRipen(lightBlock.getB(), lightBlock.getA(), ripenedBlocks);
                        ripenedBlocks.addAll(newRipened);
                    } else {
                        lightBlocks.remove(lightBlock);
                    }
                } else {
                    lightBlocks.remove(lightBlock);
                }
            }
            System.out.println(lightBlocks.size());
            System.out.println(ripenedBlocks.size());
        }
    }

    /**
     *
     */
    public static void addLightBlock(BlockPos pos, Level level) {
        if (!lightBlocks.contains(new Tuple<>(pos, level))) {
            lightBlocks.add(new Tuple<>(pos, level));
            System.out.println(pos + " at " + level + " size:" + lightBlocks.size());
        }
    }

    public static void removeLightBlock(BlockPos pos, Level level) {
        lightBlocks.remove(new Tuple<>(pos, level));
    }
}
