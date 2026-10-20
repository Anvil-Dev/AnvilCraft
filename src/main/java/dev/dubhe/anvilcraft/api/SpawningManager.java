package dev.dubhe.anvilcraft.api;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import dev.dubhe.anvilcraft.block.entity.InductionLightBlockEntity;
import dev.dubhe.anvilcraft.util.AabbUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 *   <li>维护有效电感灯的不可变模式与范围快照</li>
 *   <li>处理生物生成事件，阻止在电感灯生效区域内的生物生成</li>
 *   <li>通过方块实体生命周期清理失效的电感灯快照</li>
 * </ul>
 * </p>
 *
 * @see InductionLightBlock
 * @see InductionLightBlockEntity
 * @see MobSpawnEvent.PositionCheck
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class SpawningManager {
    private static final Map<Level, SpawningManager> INSTANCES = new ConcurrentHashMap<>();
    private final Map<BlockPos, Light> lightBlocks = new ConcurrentHashMap<>();

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
     * 在服务端更新已点亮电感灯的模式与范围，供生成线程读取。
     */
    public static void addLightBlock(BlockPos pos, Level level, boolean isAnimal) {
        if (level.isClientSide) return;
        SpawningManager spawningManager = getInstance(level);
        spawningManager.lightBlocks.compute(pos.immutable(), (lightPos, light) ->
            light != null && light.isAnimal() == isAnimal
                ? light
                : new Light(AabbUtil.centerSectionTo3x3x3(lightPos), isAnimal)
        );
    }

    public static void removeLightBlock(BlockPos pos, Level level) {
        SpawningManager spawningManager = INSTANCES.get(level);
        if (spawningManager != null) spawningManager.lightBlocks.remove(pos);
    }

    public static void clear(Level level) {
        SpawningManager spawningManager = INSTANCES.remove(level);
        if (spawningManager != null) spawningManager.lightBlocks.clear();
    }

    private boolean ignoreSummonMob(Vec3 pos, boolean isAnimal) {
        for (Light light : this.lightBlocks.values()) {
            if (light.isAnimal() == isAnimal && light.area().contains(pos)) return true;
        }
        return false;
    }

    private record Light(AABB area, boolean isAnimal) {
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
     * @see #ignoreSummonMob(Vec3, boolean)
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
        SpawningManager spawningManager = INSTANCES.get(level);
        if (spawningManager == null) return;

        if (spawningManager.ignoreSummonMob(entity.position(), entity instanceof Animal)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
