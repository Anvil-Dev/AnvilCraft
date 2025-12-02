package dev.dubhe.anvilcraft.api;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RipeningManager {
    private static final Map<Level, RipeningManager> INSTANCES = new HashMap<>();
    private final Level level;
    private final HashSet<BlockPos> ripened = new HashSet<>();
    private long lastTickRipen = -1;

    /**
     * 获取或新建一个当前维度催熟实例。
     */
    public static RipeningManager from(Level level) {
        return INSTANCES.computeIfAbsent(level, RipeningManager::new);
    }

    public RipeningManager(Level level) {
        this.level = level;
    }

    /**
     * 进行催熟
     *
     * @param pos     灯的位置
     * @param ripened 在本轮催熟中，已经被催熟过的位置
     */
    private void doRipen(BlockPos pos, HashSet<BlockPos> ripened) {
        int radius = AnvilCraft.CONFIG.inductionLightBlockRipeningRange / 2;
        for (BlockPos plantPos : BlockPos.betweenClosed(pos.offset(radius, radius, radius), pos.offset(-radius, -radius, -radius))) {
            if (ripened.contains(plantPos)) continue;
            BlockState state = level.getBlockState(plantPos);
            Block block = state.getBlock();

            if (
                block instanceof BonemealableBlock growable
                && !(growable instanceof GrassBlock)
                && !(growable instanceof NyliumBlock)
                && growable.isValidBonemealTarget(level, plantPos, state)
                && level.getBrightness(LightLayer.BLOCK, plantPos) >= 10
            ) {
                growable.performBonemeal((ServerLevel) level, level.getRandom(), plantPos, state);
                level.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    plantPos.getX() + 0.5,
                    plantPos.getY() + 0.5,
                    plantPos.getZ() + 0.5,
                    0.0,
                    0.0,
                    0.0
                );
                ripened.add(plantPos);
            }
            if (state.is(Blocks.SUGAR_CANE) && level.getBlockState(plantPos.above()).is(Blocks.AIR)) {
                level.setBlock(plantPos.above(), Blocks.SUGAR_CANE.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            } else if (state.is(Blocks.CACTUS) && level.getBlockState(plantPos.above()).is(Blocks.AIR)) {
                level.setBlock(plantPos.above(), Blocks.CACTUS.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            } else if (state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) != NetherWartBlock.MAX_AGE) {
                level.setBlock(
                    plantPos,
                    Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, state.getValue(NetherWartBlock.AGE) + 1),
                    Block.UPDATE_ALL_IMMEDIATE
                );
            }
        }
    }

    public void doRipen(BlockPos blockPos) {
        if (isRipenReady()) doRipen(blockPos, ripened);
    }

    /**
     * 如果当前时间距离上次催熟不小于催熟冷却则清空重复催熟过滤器 ripened 并重新计时，返回 true
     * 如果时间差在 (0, 冷却) 之间则返回 false 无事发生
     * 如果为 0 则返回 true 无事发生（因为意味着其他灯已经调用过这个函数了）
     * 如果为负数说明有时间旅行（time set xxx），重置上次催熟时间。
     *
     * @return if already cooldown for ripen
     */
    private boolean isRipenReady() {
        if (level.getServer() == null) return false;
        long curTime = level.getGameTime();
        long ticksBeforeLastRipen = curTime - lastTickRipen;
        if (ticksBeforeLastRipen == 0) return true; // another LightBlock is Ripened at this tick.
        if (ticksBeforeLastRipen < 0) { // time set xxx may change the gameTime.
            lastTickRipen = curTime - 1;
            return false;
        }
        if (ticksBeforeLastRipen >= AnvilCraft.CONFIG.inductionLightBlockRipeningCooldown) {
            lastTickRipen = curTime;
            ripened.clear();
            return true;
        }
        return false;
    }
}
