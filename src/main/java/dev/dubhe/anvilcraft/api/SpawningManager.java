package dev.dubhe.anvilcraft.api;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.block.entity.InductionLightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class SpawningManager {
    private static final Map<Level, SpawningManager> INSTANCES = new HashMap<>();

    private final Set<BlockPos> lightBlocks = Collections.synchronizedSet(new HashSet<>());

    /**
     * 获取当前维度实体生成实例
     */
    public static SpawningManager getInstance(Level level) {
        if (!INSTANCES.containsKey(level)) {
            INSTANCES.put(level, new SpawningManager());
        }
        return INSTANCES.get(level);
    }

    public SpawningManager() {
    }

    /**
     *
     */
    public static void addLightBlock(BlockPos pos, Level level) {
        SpawningManager inst = SpawningManager.getInstance(level);
        inst.lightBlocks.add(pos);
    }

    @SubscribeEvent
    private static void blockEntitySummon(@NotNull MobSpawnEvent.PositionCheck event) {
        if (!event.getSpawnType().equals(MobSpawnType.NATURAL)) return;
        if (!event.getSpawnType().equals(MobSpawnType.CHUNK_GENERATION)) return;
        if (!event.getSpawnType().equals(MobSpawnType.PATROL)) return;
        Entity entity = event.getEntity();
        Level level = entity.level();
        SpawningManager instance = getInstance(level);

        Iterator<BlockPos> it = instance.lightBlocks.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            BlockState lightBlockState = level.getBlockState(pos);
            if (
                lightBlockState.getBlock() instanceof InductionLightBlock
                && InductionLightBlock.isLit(lightBlockState)
                && (InductionLightBlock.canBlockMobSummoning(lightBlockState)
                    || InductionLightBlock.canBlockAnimalSummoning(lightBlockState))
            ) {
                if (level.getBlockEntity(pos) instanceof InductionLightBlockEntity blockEntity
                    && blockEntity.blockingArea.get().contains(entity.position())
                    && ((InductionLightBlock.canBlockMobSummoning(lightBlockState) && entity instanceof Monster)
                        || (InductionLightBlock.canBlockAnimalSummoning(lightBlockState) && entity instanceof Animal))
                ) {
                    event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                }
            } else {
                it.remove();
            }
        }
    }
}
