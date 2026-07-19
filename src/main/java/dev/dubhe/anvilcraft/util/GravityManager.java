package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.IAnvilCraftEntityExtension;
import dev.dubhe.anvilcraft.block.BlackHoleBlock;
import dev.dubhe.anvilcraft.block.WhiteHoleBlock;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableFallingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableLevitatingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.GravitySourcesSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class GravityManager {
    private static final double MIN_SWEPT_MOVEMENT_SQR = 0.98 * 0.98;
    private static final double VECTOR_EPSILON = 1.0e-12;
    private static final double BODY_COLLISION_MARGIN = 1.0e-4;
    private static final double MAX_SOURCE_STRENGTH = 1.0e6;
    private static final int MAX_SOURCE_RADIUS = 512;
    private static final SweptGravity ZERO_SWEPT_GRAVITY = new SweptGravity(Vec3.ZERO, Vec3.ZERO);

    // Level identity is significant: an integrated server and its client use the same dimension key.
    private static final Map<Level, GravityFieldIndex> GRAVITY_FIELDS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Double> DIMENSION_GRAVITY_MAP = new HashMap<>();

    static {
        GravitySourceManager.registerSourceType(BlackHoleBlock.class, 7, 10);
        GravitySourceManager.registerSourceType(WhiteHoleBlock.class, 7, -10);
    }

    private GravityManager() {
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        GravityFieldIndex index = GRAVITY_FIELDS.get(level);
        if (index == null) return;
        for (BlockPos id : index.idsOwnedByChunk(event.getChunk().getPos())) {
            GravitySourceManager.removeSource(level, id);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            GRAVITY_FIELDS.remove(level);
        }
    }

    public static void syncTo(ServerPlayer player) {
        GravityFieldIndex index = GRAVITY_FIELDS.get(player.level());
        List<GravitySourcesSyncPacket.SourceData> sources = index == null
            ? List.of()
            : index.allSources().stream().map(GravitySourcesSyncPacket.SourceData::from).toList();
        PacketDistributor.sendToPlayer(player, new GravitySourcesSyncPacket(true, sources, List.of()));
    }

    public static void applyNetworkSync(
        Level level,
        boolean replace,
        List<GravitySourcesSyncPacket.SourceData> sources,
        List<BlockPos> removed
    ) {
        GravityFieldIndex index = GRAVITY_FIELDS.computeIfAbsent(level, ignored -> new GravityFieldIndex());
        if (replace) index.clear();
        for (BlockPos id : removed) {
            index.remove(id);
        }
        for (GravitySourcesSyncPacket.SourceData source : sources) {
            GravitySource decoded = source.toSource();
            if (decoded != null) index.upsert(decoded);
        }
    }

    public static GravityType getGravityType(Entity entity) {
        if (entity instanceof IAnvilCraftEntityExtension extension) {
            GravityType supplied = extension.anvilcraft$getGravityType();
            if (supplied != null) return supplied;
        }
        if (entity instanceof ItemEntity itemEntity) {
            var item = itemEntity.getItem();
            if (item.is(ModItems.NEGATIVE_MATTER_NUGGET.get())
                || item.is(ModItems.NEGATIVE_MATTER.get())
                || item.is(ModBlocks.NEGATIVE_MATTER_BLOCK.get().asItem())) {
                return GravityType.ANTI_GRAVITY;
            }
            if (item.is(ModItems.LEVITATION_POWDER.get())
                || item.is(ModBlocks.LEVITATION_POWDER_BLOCK.get().asItem())) {
                return GravityType.MICRO_ANTI_GRAVITY;
            }
        }
        if (entity instanceof StandableLevitatingBlockEntity || entity instanceof LevitatingBlockEntity) {
            return GravityType.ANTI_GRAVITY;
        }
        if (entity instanceof StandableFallingBlockEntity) {
            return GravityType.LOW_GRAVITY;
        }
        return GravityType.NORMAL;
    }

    public static GravityType getFallingBlockGravityType(Block block) {
        return block.equals(ModBlocks.LEVITATION_POWDER_BLOCK.get())
            ? GravityType.ANTI_GRAVITY
            : GravityType.NORMAL;
    }

    public static Vec3 getGravityVector(Entity entity) {
        return getGravityVector(entity, GravitySourceManager.getEntityG(entity));
    }

    public static Vec3 getGravityVector(Entity entity, double baseGravity) {
        Vec3 gravity = GravitySourceManager.calculateGravityVector(
            entity.level(),
            entity.getBoundingBox().getCenter(),
            Math.abs(baseGravity)
        );
        if (entity instanceof IAnvilCraftEntityExtension extension) {
            Vec3 additional = extension.anvilcraft$getAdditionalGravity(Math.abs(baseGravity));
            if (isFinite(additional)) gravity = gravity.add(additional);
        }
        return gravity.scale(getGravityType(entity).getScalar());
    }

    /**
     * Prevents fast entities from tunneling through visual celestial bodies and integrates fields that are crossed
     * completely between two ordinary point samples.
     */
    public static Vec3 applySweptGravity(Entity entity, Vec3 movement) {
        boolean flyingPlayer = entity instanceof Player player && player.getAbilities().flying;
        Vec3 clippedMovement = entity.noPhysics || entity.isSpectator() || flyingPlayer
            ? movement
            : GravitySourceManager.clipMovementToBodies(entity, movement);
        if (clippedMovement.lengthSqr() <= MIN_SWEPT_MOVEMENT_SQR
            || entity.isNoGravity()
            || AccelerateManager.isControlledByRing(entity)
            || flyingPlayer) {
            return clippedMovement;
        }

        SweptGravity sweptGravity = GravitySourceManager.calculateSweptGravity(entity, clippedMovement);
        double gravityScalar = getGravityType(entity).getScalar();
        Vec3 movementImpulse = sweptGravity.movementImpulse().scale(gravityScalar);
        Vec3 velocityImpulse = sweptGravity.velocityImpulse().scale(gravityScalar);
        if (!isFinite(movementImpulse) || !isFinite(velocityImpulse)) return clippedMovement;
        if (movementImpulse.lengthSqr() <= VECTOR_EPSILON && velocityImpulse.lengthSqr() <= VECTOR_EPSILON) {
            return clippedMovement;
        }

        entity.setDeltaMovement(entity.getDeltaMovement().add(velocityImpulse));
        return clippedMovement.add(movementImpulse);
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private record SweptGravity(Vec3 movementImpulse, Vec3 velocityImpulse) {
    }

    public static Vec3 getNetGravityVectorForFallingBlock(Level level, Vec3 pos, GravityType gravityType) {
        double scalar = gravityType.getScalar();
        double baseGravity = 0.04 * getDimensionGravity(level);
        Vec3 baseGravityVector = new Vec3(0, -baseGravity * scalar, 0);
        Vec3 localGravityVector = GravitySourceManager.calculateGravityVector(level, pos, 0.04).scale(scalar);
        return baseGravityVector.add(localGravityVector);
    }

    public static Vec3 getNetGravityVectorForFallingBlock(Entity entity) {
        GravityType gravityType = getGravityType(entity);
        double scalar = gravityType.getScalar();
        double baseGravity = 0.04 * getDimensionGravity(entity.level());
        Vec3 baseGravityVector = new Vec3(0, -baseGravity * scalar, 0);
        return baseGravityVector.add(getGravityVector(entity, 0.04));
    }

    public static void registerDimensionGravity(ResourceKey<Level> dimension, double gravity) {
        DIMENSION_GRAVITY_MAP.put(dimension, gravity);
    }

    public static double getDimensionGravity(Level level) {
        return DIMENSION_GRAVITY_MAP.getOrDefault(level.dimension(), 1.0);
    }

    public record GravitySourceType(double strength, int radius, double bodyRadius) {
        public GravitySourceType(double strength, int radius) {
            this(strength, radius, 0);
        }

        public boolean isValid() {
            return Double.isFinite(strength)
                && Math.abs(strength) <= MAX_SOURCE_STRENGTH
                && radius > 0
                && radius <= MAX_SOURCE_RADIUS
                && Double.isFinite(bodyRadius)
                && bodyRadius >= 0
                && bodyRadius <= radius;
        }

        double radiusSqr() {
            return (double) radius * radius;
        }

        double bodyRadiusCubed() {
            return bodyRadius * bodyRadius * bodyRadius;
        }
    }

    public record GravitySource(BlockPos id, Vec3 center, GravitySourceType type) {
    }

    public static final class GravitySourceManager {
        private static final Map<Class<? extends Block>, GravitySourceType> REGISTRY = new HashMap<>();

        private GravitySourceManager() {
        }

        public static void registerSourceType(Class<? extends Block> blockClass, int radius, double strength) {
            REGISTRY.put(blockClass, new GravitySourceType(strength, radius));
        }

        public static @Nullable GravitySourceType getType(Block block) {
            return REGISTRY.get(block.getClass());
        }

        public static void addSource(Level level, BlockPos pos, GravitySourceType type) {
            upsertSource(level, pos, pos.getCenter(), type);
        }

        public static void upsertSource(Level level, BlockPos id, Vec3 center, GravitySourceType type) {
            if (!type.isValid() || !isFinite(center)) return;
            GravityFieldIndex index = GRAVITY_FIELDS.computeIfAbsent(level, ignored -> new GravityFieldIndex());
            GravitySource source = new GravitySource(id.immutable(), center, type);
            GravitySource old = index.upsert(source);
            if (source.equals(old)) return;

            wakeAffectedFallingBlocks(level, old, source);
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersInDimension(
                    serverLevel,
                    new GravitySourcesSyncPacket(
                        false,
                        List.of(GravitySourcesSyncPacket.SourceData.from(source)),
                        List.of()
                    )
                );
            }
        }

        public static void removeSource(Level level, BlockPos id) {
            GravityFieldIndex index = GRAVITY_FIELDS.get(level);
            if (index == null) return;
            GravitySource removed = index.remove(id);
            if (removed == null) return;

            wakeAffectedFallingBlocks(level, removed, null);
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersInDimension(
                    serverLevel,
                    new GravitySourcesSyncPacket(false, List.of(), List.of(id))
                );
            }
        }

        private static double getEntityG(Entity entity) {
            return switch (entity) {
                case LivingEntity ignored -> 0.08;
                case Projectile ignored -> 0.05;
                default -> 0.04;
            };
        }

        public static Vec3 calculateGravityVector(Level level, Vec3 position, double g) {
            GravityFieldIndex index = GRAVITY_FIELDS.get(level);
            if (index == null) return Vec3.ZERO;

            double fx = 0;
            double fy = 0;
            double fz = 0;
            for (GravitySource source : index.sourcesAt(position)) {
                Vec3 force = calculateGravityVector(source, position, g);
                fx += force.x;
                fy += force.y;
                fz += force.z;
            }
            return new Vec3(fx, fy, fz);
        }

        private static Vec3 calculateGravityVector(GravitySource source, Vec3 position, double g) {
            Vec3 offset = source.center().subtract(position);
            double radiusSquare = offset.lengthSqr();
            if (radiusSquare > source.type().radiusSqr() || radiusSquare <= VECTOR_EPSILON) return Vec3.ZERO;

            double dist = Math.sqrt(radiusSquare);
            double factor;
            if (source.type().bodyRadius() > 0 && dist < source.type().bodyRadius()) {
                factor = (g * source.type().strength()) / source.type().bodyRadiusCubed();
            } else {
                factor = (g * source.type().strength()) / (Math.max(radiusSquare, 1.0) * dist);
            }
            return offset.scale(factor);
        }

        private static Vec3 clipMovementToBodies(Entity entity, Vec3 movement) {
            if (movement.lengthSqr() <= VECTOR_EPSILON) return movement;
            GravityFieldIndex index = GRAVITY_FIELDS.get(entity.level());
            if (index == null) return movement;

            Vec3 start = entity.getBoundingBox().getCenter();
            double entityRadius = Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.5;
            double firstHit = 1.0;
            for (GravitySource source : index.sourcesAlong(start, start.add(movement))) {
                if (source.type().bodyRadius() <= 0) continue;
                double expandedRadius = source.type().bodyRadius() + entityRadius;
                Vec3 relativeStart = start.subtract(source.center());
                double c = relativeStart.lengthSqr() - expandedRadius * expandedRadius;
                if (c <= 0) continue;
                double b = 2.0 * relativeStart.dot(movement);
                double a = movement.lengthSqr();
                double discriminant = b * b - 4.0 * a * c;
                if (discriminant < 0) continue;
                double enter = (-b - Math.sqrt(discriminant)) / (2.0 * a);
                if (enter >= 0 && enter < firstHit) firstHit = enter;
            }
            if (firstHit >= 1.0) return movement;

            double clippedScale = Math.min(1.0, firstHit + BODY_COLLISION_MARGIN / movement.length());
            entity.setDeltaMovement(entity.getDeltaMovement().scale(clippedScale));
            return movement.scale(clippedScale);
        }

        private static SweptGravity calculateSweptGravity(Entity entity, Vec3 movement) {
            GravityFieldIndex index = GRAVITY_FIELDS.get(entity.level());
            if (index == null) return ZERO_SWEPT_GRAVITY;

            Vec3 start = entity.getBoundingBox().getCenter();
            Vec3 end = start.add(movement);
            double movementSqr = movement.lengthSqr();
            double g = getEntityG(entity);
            Vec3 movementImpulse = Vec3.ZERO;
            Vec3 velocityImpulse = Vec3.ZERO;

            for (GravitySource source : index.sourcesAlong(start, end)) {
                if (start.distanceToSqr(source.center()) <= source.type().radiusSqr()
                    || end.distanceToSqr(source.center()) <= source.type().radiusSqr()) {
                    continue;
                }

                Vec3 relativeStart = start.subtract(source.center());
                double projection = relativeStart.dot(movement);
                double discriminant = projection * projection
                    - movementSqr * (relativeStart.lengthSqr() - source.type().radiusSqr());
                if (discriminant <= 0) continue;

                double root = Math.sqrt(discriminant);
                double enter = Math.max(0.0, (-projection - root) / movementSqr);
                double exit = Math.min(1.0, (-projection + root) / movementSqr);
                if (enter >= exit) continue;

                double interval = exit - enter;
                for (int sample = 0; sample < 3; sample++) {
                    double t = enter + interval * ((sample + 0.5) / 3.0);
                    Vec3 samplePosition = start.add(movement.scale(t));
                    Vec3 impulse = calculateGravityVector(source, samplePosition, g).scale(interval / 3.0);
                    velocityImpulse = velocityImpulse.add(impulse);
                    movementImpulse = movementImpulse.add(impulse.scale(1.0 - t));
                }
            }
            return new SweptGravity(movementImpulse, velocityImpulse);
        }

        private static void wakeAffectedFallingBlocks(
            Level level,
            @Nullable GravitySource oldSource,
            @Nullable GravitySource newSource
        ) {
            if (!(level instanceof ServerLevel serverLevel)) return;
            Bounds bounds = Bounds.union(oldSource, newSource);
            if (bounds == null) return;

            int minChunkX = bounds.minX >> 4;
            int maxChunkX = bounds.maxX >> 4;
            int minChunkZ = bounds.minZ >> 4;
            int maxChunkZ = bounds.maxZ >> 4;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) continue;
                    LevelChunkSection[] sections = chunk.getSections();
                    for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                        LevelChunkSection section = sections[sectionIndex];
                        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
                        if (sectionY > bounds.maxY || sectionY + 15 < bounds.minY) continue;
                        if (!section.maybeHas(state -> state.getBlock() instanceof FallingBlock)) continue;

                        int minX = Math.max(bounds.minX, chunkX << 4);
                        int maxX = Math.min(bounds.maxX, (chunkX << 4) + 15);
                        int minY = Math.max(bounds.minY, sectionY);
                        int maxY = Math.min(bounds.maxY, sectionY + 15);
                        int minZ = Math.max(bounds.minZ, chunkZ << 4);
                        int maxZ = Math.min(bounds.maxZ, (chunkZ << 4) + 15);
                        for (int x = minX; x <= maxX; x++) {
                            for (int y = minY; y <= maxY; y++) {
                                for (int z = minZ; z <= maxZ; z++) {
                                    cursor.set(x, y, z);
                                    if (!isInside(cursor, oldSource) && !isInside(cursor, newSource)) continue;
                                    BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                    if (state.getBlock() instanceof FallingBlock) {
                                        serverLevel.scheduleTick(cursor, state.getBlock(), 2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private static boolean isInside(BlockPos pos, @Nullable GravitySource source) {
            return source != null && pos.getCenter().distanceToSqr(source.center()) <= source.type().radiusSqr();
        }
    }

    private static final class GravityFieldIndex {
        private final Map<BlockPos, GravitySource> sourcesById = new HashMap<>();
        private final Map<Long, Set<GravitySource>> sourcesByChunk = new HashMap<>();

        GravitySource upsert(GravitySource source) {
            GravitySource old = sourcesById.put(source.id(), source);
            if (source.equals(old)) return old;
            if (old != null) removeFromChunks(old);
            addToChunks(source);
            return old;
        }

        @Nullable GravitySource remove(BlockPos id) {
            GravitySource removed = sourcesById.remove(id);
            if (removed != null) removeFromChunks(removed);
            return removed;
        }

        List<BlockPos> idsOwnedByChunk(ChunkPos chunkPos) {
            List<BlockPos> result = new ArrayList<>();
            for (GravitySource source : sourcesById.values()) {
                if (new ChunkPos(source.id()).equals(chunkPos)) {
                    result.add(source.id());
                }
            }
            return result;
        }

        Collection<GravitySource> allSources() {
            return List.copyOf(sourcesById.values());
        }

        Collection<GravitySource> sourcesAt(Vec3 position) {
            int chunkX = ((int) Math.floor(position.x)) >> 4;
            int chunkZ = ((int) Math.floor(position.z)) >> 4;
            return sourcesByChunk.getOrDefault(ChunkPos.asLong(chunkX, chunkZ), Set.of());
        }

        Collection<GravitySource> sourcesAlong(Vec3 start, Vec3 end) {
            Set<GravitySource> result = new LinkedHashSet<>();
            int x = ((int) Math.floor(start.x)) >> 4;
            int z = ((int) Math.floor(start.z)) >> 4;
            int endX = ((int) Math.floor(end.x)) >> 4;
            int endZ = ((int) Math.floor(end.z)) >> 4;
            int stepX = Integer.compare(endX, x);
            int stepZ = Integer.compare(endZ, z);
            double dx = end.x - start.x;
            double dz = end.z - start.z;
            double tdeltax = stepX == 0 ? Double.POSITIVE_INFINITY : 16.0 / Math.abs(dx);
            double tdeltaz = stepZ == 0 ? Double.POSITIVE_INFINITY : 16.0 / Math.abs(dz);
            double nextBoundaryX = stepX > 0 ? (x + 1) * 16.0 : x * 16.0;
            double nextBoundaryZ = stepZ > 0 ? (z + 1) * 16.0 : z * 16.0;
            double tmaxx = stepX == 0 ? Double.POSITIVE_INFINITY : (nextBoundaryX - start.x) / dx;
            double tmaxz = stepZ == 0 ? Double.POSITIVE_INFINITY : (nextBoundaryZ - start.z) / dz;

            int remaining = Math.abs(endX - x) + Math.abs(endZ - z) + 1;
            while (remaining-- > 0) {
                result.addAll(sourcesByChunk.getOrDefault(ChunkPos.asLong(x, z), Set.of()));
                if (x == endX && z == endZ) break;
                if (tmaxx < tmaxz) {
                    x += stepX;
                    tmaxx += tdeltax;
                } else {
                    z += stepZ;
                    tmaxz += tdeltaz;
                }
            }
            return result;
        }

        void clear() {
            sourcesById.clear();
            sourcesByChunk.clear();
        }

        private void addToChunks(GravitySource source) {
            int radius = source.type().radius();
            int minChunkX = ((int) Math.floor(source.center().x - radius)) >> 4;
            int maxChunkX = ((int) Math.floor(source.center().x + radius)) >> 4;
            int minChunkZ = ((int) Math.floor(source.center().z - radius)) >> 4;
            int maxChunkZ = ((int) Math.floor(source.center().z + radius)) >> 4;
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    sourcesByChunk.computeIfAbsent(ChunkPos.asLong(x, z), ignored -> new HashSet<>()).add(source);
                }
            }
        }

        private void removeFromChunks(GravitySource source) {
            for (Set<GravitySource> sources : sourcesByChunk.values()) {
                sources.remove(source);
            }
            sourcesByChunk.values().removeIf(Set::isEmpty);
        }
    }

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        static @Nullable Bounds union(@Nullable GravitySource first, @Nullable GravitySource second) {
            if (first == null && second == null) return null;
            Bounds a = of(first == null ? second : first);
            if (first == null || second == null) return a;
            Bounds b = of(second);
            return new Bounds(
                Math.min(a.minX, b.minX),
                Math.min(a.minY, b.minY),
                Math.min(a.minZ, b.minZ),
                Math.max(a.maxX, b.maxX),
                Math.max(a.maxY, b.maxY),
                Math.max(a.maxZ, b.maxZ)
            );
        }

        private static Bounds of(GravitySource source) {
            double radius = source.type().radius();
            return new Bounds(
                (int) Math.floor(source.center().x - radius),
                (int) Math.floor(source.center().y - radius),
                (int) Math.floor(source.center().z - radius),
                (int) Math.floor(source.center().x + radius),
                (int) Math.floor(source.center().y + radius),
                (int) Math.floor(source.center().z + radius)
            );
        }
    }
}
