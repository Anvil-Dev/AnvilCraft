package dev.dubhe.anvilcraft.api;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.block.entity.InductionLightBlockEntity;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 电感灯生物生成管理器
 *
 * <p>
 * 该类负责管理电感灯方块对生物生成的控制功能，采用单例模式为每个世界维护一个实例。
 * 通过监听生物生成事件，检查生物是否在电感灯的生效区域内，并根据电感灯的设置阻止相应类型的生物生成。
 * </p>
 *
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>维护动物和非动物生物生成控制的电感灯方块集合</li>
 *   <li>处理生物生成事件，阻止在电感灯生效区域内的生物生成</li>
 *   <li>自动清理失效的电感灯方块引用</li>
 * </ul>
 * </p>
 *
 * @see InductionLightBlock
 * @see InductionLightBlockEntity
 * @see MobSpawnEvent.PositionCheck
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class SpawningManager {
    private static final Map<Level, SpawningManager> INSTANCES = new HashMap<>();
    private final Set<BlockPos> nonAnimalLightBlockSet = Collections.synchronizedSet(new ObjectOpenHashSet<>());
    private final Set<BlockPos> animalLightBlockSet = Collections.synchronizedSet(new ObjectOpenHashSet<>());

    @Getter
    private final Level level;

    /**
     * 获取与指定世界关联的 SpawningManager 实例
     *
     * <p>
     * 使用单例模式，为每个 Level 创建并缓存一个 SpawningManager 实例。<br/>
     * 如果指定的 Level 尚未创建实例，则通过 {@link SpawningManager#SpawningManager(Level)} 构造函数创建新实例。
     * </p>
     *
     * @param level 用于获取 SpawningManager 实例的世界对象
     * @return 与指定世界关联的 SpawningManager 实例
     * @see Map#computeIfAbsent(Object, java.util.function.Function)
     */
    public static SpawningManager getInstance(Level level) {
        return INSTANCES.computeIfAbsent(level, SpawningManager::new);
    }

    private SpawningManager(Level level) {
        this.level = level;
    }

    /**
     * 添加电感灯方块到对应的生物生成控制集合中
     *
     * <p>
     * 根据 isAnimal 参数决定将指定位置的电感灯添加到动物生成控制集合或非动物生物生成控制集合。<br/>
     * 该方法会获取对应世界的 SpawningManager 实例，并将方块位置添加到相应的集合中。
     * </p>
     *
     * @param pos       电感灯方块的位置
     * @param level     方块所在的世界
     * @param isAnimal  true表示该电感灯用于控制动物生成，false表示用于控制非动物生物生成
     * @see SpawningManager#getInstance(Level)
     * @see #animalLightBlockSet
     * @see #nonAnimalLightBlockSet
     */
    public static void addLightBlock(BlockPos pos, Level level, boolean isAnimal) {
        SpawningManager spawningManager = SpawningManager.getInstance(level);
        if (isAnimal) {
            spawningManager.animalLightBlockSet.add(pos);
        } else {
            spawningManager.nonAnimalLightBlockSet.add(pos);
        }
    }

    /**
     * 检查并阻止特定类型的生物在电感灯方块的生效区域内生成
     *
     * <p>
     * 遍历指定的电感灯方块位置集合，检查每个方块是否仍有效并且处于点亮状态。<br/>
     * 如果生物位于某个有效电感灯方块的阻挡区域内，则根据生物类型和电感灯设置阻止其生成。<br/>
     * 同时清理已失效的电感灯方块位置。
     * </p>
     *
     * @param level       生物生成所在的世界
     * @param event       生物生成位置检查事件
     * @param lightPosSet 电感灯方块位置集合
     * @param isAnimal    true表示检查动物生成，false表示检查非动物生物生成
     * @return 是否成功阻止生成实体
     * @see InductionLightBlock#isLit(BlockState)
     * @see InductionLightBlock#canBlockMobSummoning(BlockState)
     * @see InductionLightBlock#canBlockAnimalSummoning(BlockState)
     * @see InductionLightBlockEntity#isInRange(Vec3)
     * @see MobSpawnEvent.PositionCheck#setResult(MobSpawnEvent.PositionCheck.Result)
     */
    private static boolean ignoreSummonMob(Level level, MobSpawnEvent.PositionCheck event, Set<BlockPos> lightPosSet, boolean isAnimal) {
        Mob entity = event.getEntity();
        Iterator<BlockPos> iterator = lightPosSet.iterator();
        while (iterator.hasNext()) {
            BlockPos lightPos = iterator.next();
            if (!level.isLoaded(lightPos)) continue;
            BlockState state = level.getBlockState(lightPos);

            // 电感灯是否开启
            if (!(state.getBlock() instanceof InductionLightBlock) || InductionLightBlock.isLit(state)) {
                iterator.remove();
                continue;
            }

            // 电感灯模式是否正确
            if (isAnimal ? InductionLightBlock.canBlockAnimalSummoning(state) : InductionLightBlock.canBlockMobSummoning(state)) {
                iterator.remove();
                continue;
            }

            // 实体是否位于该电感灯影响范围内
            boolean isInRange = Util.castSafely(level.getBlockEntity(lightPos), InductionLightBlockEntity.class)
                .map(be -> be.isInRange(entity.position()))
                .orElse(false);
            if (!isInRange) continue;

            // 电感灯模式与实体类型是否匹配
            if (isAnimal != entity instanceof Animal) continue;

            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return true;
        }
        return false;
    }

    /**
     * 处理生物生成位置检查事件
     *
     * <p>
     * 监听 Minecraft 的生物生成事件，只处理自然生成、区块生成和巡逻生成类型的生物。<br/>
     * 根据生物类型（动物或非动物）检查其生成位置是否在电感灯方块的阻挡区域内，<br/>
     * 如果在阻挡区域内则阻止生物生成。
     * </p>
     *
     * @param event 生物生成位置检查事件
     * @see MobSpawnEvent.PositionCheck
     * @see MobSpawnType
     * @see #ignoreSummonMob(Level, MobSpawnEvent.PositionCheck, Set, boolean)
     */
    @SubscribeEvent
    private static void blockEntitySummon(MobSpawnEvent.PositionCheck event) {
        MobSpawnType spawnType = event.getSpawnType();
        if (
            !spawnType.equals(MobSpawnType.NATURAL)
            && !spawnType.equals(MobSpawnType.CHUNK_GENERATION)
            && !spawnType.equals(MobSpawnType.PATROL)
        ) {
            return;
        }
        Entity entity = event.getEntity();
        Level level = entity.level();
        SpawningManager spawningManager = getInstance(level);

        if (!ignoreSummonMob(level, event, spawningManager.animalLightBlockSet, true)) {
            ignoreSummonMob(level, event, spawningManager.nonAnimalLightBlockSet, false);
        }
    }
}
