package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.BlackHoleBlock;
import dev.dubhe.anvilcraft.block.WhiteHoleBlock;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableFallingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableLevitatingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "anvilcraft")
public class GravityManager {

    // 维度 -> 区块索引 -> 重力源列表
    private static final Map<ResourceKey<Level>, Map<Long, List<GravitySource>>> GRAVITY_CACHE = new ConcurrentHashMap<>();

    // 维度重力倍率表
    private static final Map<ResourceKey<Level>, Double> DIMENSION_GRAVITY_MAP = new HashMap<>();

    static {
        GravitySourceManager.registerSourceType(BlackHoleBlock.class, 7, 10);
        GravitySourceManager.registerSourceType(WhiteHoleBlock.class, 7, -10);
        // 在这里注册更多重力源，strength -> 距离该重力源 1 格处的重力是主世界重力的 strength 倍

        // registerDimensionGravity(Level.MUN, 0.1653061224489796);
        // 在这里注册更多维度重力，gravity -> 该维度重力是主世界重力的 gravity 倍
    }

    // 区块卸载事件，移除记录的重力源
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            Map<Long, List<GravitySource>> dimCache = GRAVITY_CACHE.get(level.dimension());
            if (dimCache != null) {
                dimCache.remove(event.getChunk().getPos().toLong());
            }
        }
    }

    // 世界卸载事件，清除所有缓存
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            GRAVITY_CACHE.remove(level.dimension());
        }
    }

    public static GravityType getGravityType(Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            var item = itemEntity.getItem();

            boolean isNegativeMatter = item.is(ModItems.NEGATIVE_MATTER_NUGGET.get()) || item.is(ModItems.NEGATIVE_MATTER.get()) || item.is(
                ModBlocks.NEGATIVE_MATTER_BLOCK.get().asItem());
            if (isNegativeMatter) return GravityType.ANTI_GRAVITY; // 负物质

            boolean isLevitationPowder = item.is(ModItems.LEVITATION_POWDER.get()) || item.is(ModBlocks.LEVITATION_POWDER_BLOCK.get()
                .asItem());
            if (isLevitationPowder) return GravityType.MICRO_ANTI_GRAVITY; // 漂浮粉

            // 在这里注册物品的重力类型
        }

        if (entity instanceof LevitatingBlockEntity) { // 漂浮粉块
            return GravityType.ANTI_GRAVITY;
        }

        if (entity instanceof StandableFallingBlockEntity) { // 下降的可控沙
            return GravityType.LOW_GRAVITY;
        }

        if (entity instanceof StandableLevitatingBlockEntity) { // 上升的可控沙
            return GravityType.ANTI_GRAVITY;
        }
        // 在这里注册下落方块的重力类型

        return GravityType.NORMAL; // 默认
    }

    public static GravityType getFallingBlockGravityType(Block block) {
        if (block.equals(ModBlocks.LEVITATION_POWDER_BLOCK.get())) {
            return GravityType.ANTI_GRAVITY; // 漂浮粉块
        }
        return GravityType.NORMAL; // 普通下落方块
    }

    // 得到重力向量
    public static Vec3 getGravityVector(Entity entity) {
        // 获取计算的引力
        Vec3 gravityVector = GravitySourceManager.calculateGravityVector(entity);

        // 如果没有受到引力，直接返回零
        if (gravityVector.equals(Vec3.ZERO)) {
            return Vec3.ZERO;
        }

        // 根据实体引力类型修正向量方向/大小
        return gravityVector.scale(GravityManager.getGravityType(entity).getScalar());
    }

    // 得到含维度的总体重力向量（仅用于下落方块方块）
    public static Vec3 getNetGravityVectorForFallingBlock(Level level, Vec3 pos, GravityType gravityType) {
        // 确定基础重力的大小和方向
        double baseGravity = 0.04 * getDimensionGravity(level);

        // 根据物质类型调整基础重力的方向
        double baseGravityY = (gravityType == GravityType.ANTI_GRAVITY) ? baseGravity : -baseGravity;
        Vec3 baseGravityVector = new Vec3(0, baseGravityY, 0);

        // 计算局部重力源的引力
        Vec3 localGravityVector = GravitySourceManager.calculateGravityVector(level, pos, Math.abs(baseGravity));

        // 根据物质类型调整重力向量方向
        if (gravityType == GravityType.ANTI_GRAVITY) {
            localGravityVector = localGravityVector.reverse();
        }

        // 返回向量用于下落方块计算
        return baseGravityVector.add(localGravityVector);
    }

    public static Vec3 getNetGravityVectorForFallingBlock(Entity entity) {
        return getNetGravityVectorForFallingBlock(entity.level(), entity.getBoundingBox().getCenter(), getGravityType(entity));
    }

    // 应用维度重力
    public static void registerDimensionGravity(ResourceKey<Level> dimension, double gravity) {
        DIMENSION_GRAVITY_MAP.put(dimension, gravity);
    }

    public static double getDimensionGravity(Level level) {
        return DIMENSION_GRAVITY_MAP.getOrDefault(level.dimension(), 1.0);
    }

    // 重力源定义与存储
    public static class GravitySourceType {
        final double strength;
        final int radius;
        final double radiusSqr;

        public GravitySourceType(double strength, int radius) {
            this.strength = strength;
            this.radius = radius;
            this.radiusSqr = radius * radius;
        }
    }

    public static class GravitySource {
        public final BlockPos pos;
        public final GravitySourceType type;

        public GravitySource(BlockPos pos, GravitySourceType type) {
            this.pos = pos;
            this.type = type;
        }
    }

    // 重力源管理器
    public static class GravitySourceManager {
        private static final Map<Class<? extends Block>, GravitySourceType> REGISTRY = new HashMap<>();

        public static void registerSourceType(Class<? extends Block> blockClass, int radius, double strength) {
            REGISTRY.put(blockClass, new GravitySourceType(strength, radius));
        }

        public static GravitySourceType getType(Block block) {
            return REGISTRY.get(block.getClass());
        }

        public static void addSource(Level level, BlockPos pos, GravitySourceType type) {
            long chunkKey = ChunkPos.asLong(pos);
            List<GravitySource> list = GRAVITY_CACHE.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> new ArrayList<>());

            // 检查列表中是否已经有该坐标的重力源
            boolean alreadyExists = false;
            for (GravitySource s : list) {
                if (s.pos.equals(pos)) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                list.add(new GravitySource(pos, type));
                // 唤醒周围的下落方块
                int r = type.radius;
                BlockPos.betweenClosedStream(pos.offset(-r, -r, -r), pos.offset(r, r, r)).forEach(p -> {
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof FallingBlock) {
                        level.scheduleTick(p, s.getBlock(), 2);
                    }
                });
            }
        }

        public static void removeSource(Level level, BlockPos pos) {
            long chunkKey = ChunkPos.asLong(pos);
            Map<Long, List<GravitySource>> dimCache = GRAVITY_CACHE.get(level.dimension());
            if (dimCache != null) {
                List<GravitySource> sources = dimCache.get(chunkKey);
                if (sources != null) {
                    // 查找并移除，同时获取被移除的源以得到半径
                    GravitySource removedSource = null;
                    for (int i = 0; i < sources.size(); i++) {
                        if (sources.get(i).pos.equals(pos)) {
                            removedSource = sources.remove(i);
                            break;
                        }
                    }
                    
                    if (sources.isEmpty()) {
                        dimCache.remove(chunkKey);
                    }
                    
                    if (removedSource != null) {
                        int r = removedSource.type.radius;
                        BlockPos.betweenClosedStream(pos.offset(-r, -r, -r), pos.offset(r, r, r)).forEach(p -> {
                            BlockState s = level.getBlockState(p);
                            if (s.getBlock() instanceof FallingBlock) {
                                level.scheduleTick(p, s.getBlock(), 2);
                            }
                        });
                    }
                }
            }
        }

        private static double getEntityG(Entity entity) {
            return switch (entity) {
                case LivingEntity livingEntity -> 0.08;
                case Projectile projectile -> 0.05;
                default -> 0.04;
            };
        }

        public static Vec3 calculateGravityVector(Level level, Vec3 p, double g) {
            var cache = GRAVITY_CACHE.get(level.dimension());
            if (cache == null) return Vec3.ZERO;

            double fx = 0;
            double fy = 0;
            double fz = 0;
            int cx = ((int) Math.floor(p.x)) >> 4;
            int cz = ((int) Math.floor(p.z)) >> 4;

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    var list = cache.get(ChunkPos.asLong(cx + x, cz + z));
                    if (list == null) continue;
                    for (var s : list) {
                        double dx = s.pos.getX() + 0.5 - p.x;
                        double dy = s.pos.getY() + 0.5 - p.y;
                        double dz = s.pos.getZ() + 0.5 - p.z;
                        double radiusSquare = dx * dx + dy * dy + dz * dz;

                        if (radiusSquare <= s.type.radiusSqr) {
                            // 使用传入的 g
                            double f = (g * s.type.strength) / (Math.max(radiusSquare, 1.0) * Math.sqrt(radiusSquare));
                            fx += dx * f;
                            fy += dy * f;
                            fz += dz * f;
                        }
                    }
                }
            }
            return new Vec3(fx, fy, fz);
        }

        public static Vec3 calculateGravityVector(Entity entity) {
            return calculateGravityVector(entity.level(), entity.getBoundingBox().getCenter(), getEntityG(entity));
        }
    }
}