package dev.dubhe.anvilcraft.api;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.block.state.LightColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class RipeningManager {
    private static final Map<Level, RipeningManager> INSTANCES = new HashMap<>();

    private static int cooldown = 0;
    private final Level level;
    private final Set<BlockPos> lightBlocks = Collections.synchronizedSet(new HashSet<>());

    /**
     * 获取当前维度催熟实例
     */
    public static RipeningManager getInstance(Level level) {
        if (!INSTANCES.containsKey(level)) {
            INSTANCES.put(level, new RipeningManager(level));
        }
        return INSTANCES.get(level);
    }


    public RipeningManager(Level level) {
        this.level = level;
    }

    public static void tickAll() {
        INSTANCES.values().forEach(RipeningManager::tick);
    }

    private static boolean isLit(@NotNull BlockState state) {
        return !(state.getValue(InductionLightBlock.POWERED) || state.getValue(InductionLightBlock.OVERLOAD));
    }

    private static boolean canCropGrow(@NotNull BlockState state) {
        return state.getValue(InductionLightBlock.COLOR).equals(LightColor.PINK);
    }

    private void doRipen(
            @NotNull BlockPos pos,
            @NotNull HashSet<BlockPos> ripened
    ) {
        int rangeSize = AnvilCraft.config.inductionLightBlockRipeningRange;
        for (int dx = -rangeSize / 2; dx <= rangeSize / 2; dx++) {
            for (int dy = -rangeSize / 2; dy <= rangeSize / 2; dy++) {
                for (int dz = -rangeSize / 2; dz <= rangeSize / 2; dz++) {
                    BlockPos pos1 = pos.offset(dx, dy, dz);
                    if (ripened.contains(pos1)) continue;
                    BlockState state = level.getBlockState(pos1);
                    if (state.getBlock() instanceof BonemealableBlock growable
                            && !(growable instanceof GrassBlock)
                            && !(growable instanceof NyliumBlock)
                            && growable.isValidBonemealTarget(level, pos1, state, false)
                            && level.getBrightness(LightLayer.BLOCK, pos1) >= 10
                    ) {
                        growable.performBonemeal((ServerLevel) level, level.getRandom(), pos1, state);
                        level.addParticle(
                                ParticleTypes.HAPPY_VILLAGER,
                                pos1.getX() + 0.5,
                                pos1.getY() + 0.5,
                                pos1.getZ() + 0.5,
                                0.0, 0.0, 0.0);
                        ripened.add(pos1);
                    }
                }
            }
        }
    }

    /**
     *
     */
    private void tick() {
        cooldown--;

        if (cooldown <= 0 && !lightBlocks.isEmpty()) {
            MinecraftServer server = level.getServer();
            if (server == null) return;
            server.tell(new TickTask(server.getTickCount(), () -> {
                HashSet<BlockPos> ripenedBlocks = new HashSet<>();
                Iterator<BlockPos> it = lightBlocks.iterator();
                while (it.hasNext()) {
                    BlockPos pos = it.next();
                    BlockState lightBlockState = level.getBlockState(pos);
                    if (lightBlockState.getBlock() instanceof InductionLightBlock
                            && isLit(lightBlockState)
                            && canCropGrow(lightBlockState)
                    ) {
                        doRipen(pos, ripenedBlocks);
                    } else {
                        it.remove();
                    }
                }
            }));
            cooldown = AnvilCraft.config.inductionLightBlockRipeningCooldown;
        }
    }

    /**
     *
     */
    public static void addLightBlock(BlockPos pos, Level level) {
        RipeningManager inst = RipeningManager.getInstance(level);
        inst.lightBlocks.add(pos);
    }
}
