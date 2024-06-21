package dev.dubhe.anvilcraft.api;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.block.state.LightColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import java.util.Iterator;
import java.util.Set;


public class BonemealManager {
    private static int cooldown = 0;
    private static final Set<Tuple<BlockPos, Level>> lightBlocks = Collections.synchronizedSet(new HashSet<>());

    private static boolean isLighting(@NotNull BlockState state) {
        return !(state.getValue(InductionLightBlock.POWERED)
            || state.getValue(InductionLightBlock.OVERLOAD));
    }

    private static boolean canCropGrow(@NotNull BlockState state) {
        return state.getValue(InductionLightBlock.COLOR).equals(LightColor.PINK);
    }

    private static boolean isInSet(HashSet<Tuple<BlockPos, Level>> set, Tuple<BlockPos, Level> item) {
        for (Tuple<BlockPos, Level> cur : set) {
            if (cur.getA().getX() == item.getA().getX()
                && cur.getA().getY() == item.getA().getY()
                && cur.getA().getZ() == item.getA().getZ()
                && cur.getB().dimension() == item.getB().dimension()) {
                return true;
            }
        }
        return false;
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
                        && !isInSet(set, new Tuple<>(pos1, level))
                        && growable.isValidBonemealTarget(level, pos1, state, false)
                    ) {
                        growable.performBonemeal(
                            (ServerLevel) level,
                            level.getRandom(), pos1, state);
                        level.addParticle(
                            ParticleTypes.HAPPY_VILLAGER,
                            pos1.getX() + 0.5,
                            pos1.getY() + 0.5,
                            pos1.getZ() + 0.5,
                            0.0, 0.0, 0.0);
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
            Iterator<Tuple<BlockPos, Level>> it = lightBlocks.iterator();
            while (it.hasNext()) {
                Tuple<BlockPos, Level> lightBlock = it.next();
                BlockState lightBlockState = lightBlock.getB().getBlockState(lightBlock.getA());
                if (lightBlockState.getBlock() instanceof InductionLightBlock) {
                    if (isLighting(lightBlockState) && canCropGrow(lightBlockState)) {
                        HashSet<Tuple<BlockPos, Level>> newRipened =
                            doRipen(lightBlock.getB(), lightBlock.getA(), ripenedBlocks);
                        ripenedBlocks.addAll(newRipened);
                    }
                } else {
                    it.remove();
                }
            }
        }

    }

    /**
     *
     */
    public static void addLightBlock(BlockPos pos, Level level) {
        lightBlocks.add(new Tuple<>(pos, level));
    }
}
