package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.block.CelestialBackGateBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialBackGateBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeResetManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/** Server-side implementation of the coordinate and return rules in {@link CelestialTravelData}. */
public final class CelestialTravelManager {
    public static final ResourceLocation OVERWORLD_LIKE_DIMENSION =
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "overworld_like");
    public static final ResourceKey<Level> OVERWORLD_LIKE_LEVEL =
        ResourceKey.create(Registries.DIMENSION, OVERWORLD_LIKE_DIMENSION);
    public static final ResourceLocation VOID_PLANET_DIMENSION =
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "void_planet");
    public static final ResourceKey<Level> VOID_PLANET_LEVEL =
        ResourceKey.create(Registries.DIMENSION, VOID_PLANET_DIMENSION);
    public static final ResourceLocation MUN_DIMENSION =
        ResourceLocation.fromNamespaceAndPath("anvilcraft", "mun");
    public static final ResourceKey<Level> MUN_LEVEL =
        ResourceKey.create(Registries.DIMENSION, MUN_DIMENSION);

    private static final int SEARCH_RADIUS = 16;
    private static final int SPAWN_SEARCH_RADIUS = 32;
    private static final int GATE_REUSE_RADIUS = 8;
    private static final int GATE_REUSE_VERTICAL_RADIUS = 8;
    private static final int PORTAL_COOLDOWN_TICKS = 20;
    private static final long RANDOM_SALT = 0x6A09E667F3BCC909L;
    private static final long OVERWORLD_LIKE_SALT = 0x6A09E667F3BCC909L;
    private static final long OVERWORLD_LIKE_MULTIPLIER = 0x9E3779B97F4A7C15L;
    private static final long OVERWORLD_LIKE_ADDEND = 0xBF58476D1CE4E5B9L;

    private CelestialTravelManager() {
    }

    /**
     * 强制将目标区块生成到完整状态后自上而下扫描第一个非空气方块，
     * 在地形尚未生成（高度图不可用）时也能得到正确的地表高度。
     */
    public static int findSurfaceY(ServerLevel level, int x, int z) {
        ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 1, z);
        while (pos.getY() > level.getMinBuildHeight() && chunk.getBlockState(pos).isAir()) {
            pos.move(Direction.DOWN);
        }
        return pos.getY();
    }

    public static boolean isOverworldLike(ResourceKey<Level> dimension) {
        return OVERWORLD_LIKE_LEVEL.equals(dimension);
    }

    /**
     * Derives the seed used by the built-in overworld-like dimension.
     *
     * <p>The derivation is deliberately stable: all instances of the
     * overworld-like special planet share one dimension, while every save gets
     * terrain that is independent from its ordinary overworld.</p>
     */
    public static long overworldLikeSeed(long sourceSeed) {
        long mixed = Long.rotateLeft(sourceSeed ^ OVERWORLD_LIKE_SALT, 23);
        return mixed * OVERWORLD_LIKE_MULTIPLIER + OVERWORLD_LIKE_ADDEND;
    }

    /**
     * Sends an entity through a special-planet landing portal and records the
     * source portal on the generated return gate.
     */
    public static boolean tryLand(
        Entity entity,
        ServerLevel sourceLevel,
        BlockPos sourcePortalPos,
        Direction sourceFacing,
        CelestialTravelData travel
    ) {
        if (entity.isOnPortalCooldown()) return false;
        ResourceKey<Level> destinationKey = ResourceKey.create(Registries.DIMENSION, travel.dimension());
        ServerLevel destination = OverworldLikeResetManager.getEntryDestination(sourceLevel.getServer(), destinationKey);
        if (destination == null) return false;

        BlockPos desired = landingOrigin(entity, destination, travel.coordinateRule());
        if (travel.returnRule().type() == CelestialTravelData.ReturnRule.Type.ENTRY_PORTAL) {
            BlockPos existingGate = findMatchingGateNear(
                destination, desired, sourceLevel.dimension(), sourcePortalPos, sourceFacing, true
            );
            if (existingGate != null) {
                return travelThroughGate(
                    entity, sourceLevel, destination, existingGate, existingGate, sourcePortalPos, sourceFacing,
                    travel.returnRule().type()
                );
            }
        }
        boolean keepRequestedY = keepsRequestedHeight(travel.coordinateRule().type());
        BlockPos landing = findSafeLandingOrSpawn(destination, desired, keepRequestedY);
        boolean emergencyLanding = false;
        if (landing == null) {
            landing = createEmergencyLandingPlatform(destination, desired, keepRequestedY);
            if (landing == null) return false;
            emergencyLanding = true;
        }

        BlockPos returnOrigin = returnOrigin(destination, entity, travel.returnRule(), landing);
        boolean keepReturnY = travel.returnRule().type() == CelestialTravelData.ReturnRule.Type.FIXED_PORTAL
            || (travel.returnRule().type() == CelestialTravelData.ReturnRule.Type.ENTRY_PORTAL && keepRequestedY);
        boolean reuseExistingGate = travel.returnRule().type()
            != CelestialTravelData.ReturnRule.Type.RANDOM_PORTAL;
        BlockPos existingGate = reuseExistingGate
            ? findMatchingGateNear(
                destination, returnOrigin, sourceLevel.dimension(), sourcePortalPos, sourceFacing, false
            ) : null;
        BlockPos returnPos = existingGate != null
            ? existingGate
            : (travel.returnRule().type() == CelestialTravelData.ReturnRule.Type.ENTRY_PORTAL
                && emergencyLanding
                ? landing.above()
                : findSafeGateOrSpawn(
                    destination,
                    returnOrigin,
                    sourceFacing,
                    sourceLevel.dimension(),
                    sourcePortalPos,
                    keepReturnY
                ));
        if (returnPos == null) {
            BlockPos emergencyPlatform = createEmergencyLandingPlatform(destination, returnOrigin, keepReturnY);
            returnPos = emergencyPlatform == null ? null : emergencyPlatform.above();
        }
        if (returnPos == null) return false;

        return travelThroughGate(
            entity, sourceLevel, destination, returnPos, landing, sourcePortalPos, sourceFacing,
            travel.returnRule().type()
        );
    }

    private static boolean travelThroughGate(
        Entity entity,
        ServerLevel sourceLevel,
        ServerLevel destination,
        BlockPos returnPos,
        BlockPos landing,
        BlockPos sourcePortalPos,
        Direction sourceFacing,
        CelestialTravelData.ReturnRule.Type returnType
    ) {
        Direction gateFacing = sourceFacing;
        BlockState existingGateState = destination.getBlockState(returnPos);
        boolean reusingGate = existingGateState.getBlock() instanceof CelestialBackGateBlock;
        BlockState gateState = ModBlocks.CELESTIAL_BACK_GATE.getDefaultState()
            .setValue(CelestialBackGateBlock.FACING, gateFacing)
            .setValue(CelestialBackGateBlock.WATERLOGGED, reusingGate
                ? existingGateState.getValue(CelestialBackGateBlock.WATERLOGGED)
                : destination.getFluidState(returnPos).is(Fluids.WATER));
        if (!reusingGate) {
            destination.setBlock(returnPos, gateState, 3);
        } else if (destination.getBlockState(returnPos).getValue(CelestialBackGateBlock.FACING)
            != gateFacing) {
            destination.setBlock(returnPos, gateState, 3);
        }
        if (!(destination.getBlockEntity(returnPos) instanceof CelestialBackGateBlockEntity gate)) {
            if (!reusingGate) destination.removeBlock(returnPos, false);
            return false;
        }
        if (!reusingGate || gate.getReturnDimension() == null || gate.getReturnPortalPos() == null) {
            gate.configure(sourceLevel.dimension(), sourcePortalPos, sourceFacing);
        }

        // An entry portal occupies the requested landing point, so arrive just
        // in front of it.  Other return rules place the gate independently and
        // must preserve the coordinate rule's landing position.
        BlockPos arrivalPos = returnType == CelestialTravelData.ReturnRule.Type.ENTRY_PORTAL
            ? returnPos.relative(gateFacing) : landing;
        Vec3 destinationPosition = Vec3.atBottomCenterOf(arrivalPos);
        Vec3 momentum = reverseFacingMomentum(entity.getDeltaMovement(), sourceFacing);
        Entity moved = move(entity, sourceLevel, destination, destinationPosition, momentum);
        if (moved == null) {
            if (!reusingGate) destination.removeBlock(returnPos, false);
            return false;
        }
        moved.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        cleanupDuplicateGates(destination, returnPos, sourceLevel.dimension(), sourcePortalPos, sourceFacing);
        return true;
    }

    /** Sends an entity standing in a return gate back to its recorded source portal. */
    public static boolean tryReturn(Entity entity, CelestialBackGateBlockEntity gate) {
        if (entity.isOnPortalCooldown()) return false;
        if (!(gate.getLevel() instanceof ServerLevel sourceLevel)) return false;
        ResourceKey<Level> returnDimension = gate.getReturnDimension();
        BlockPos returnPortal = gate.getReturnPortalPos();
        Direction returnFacing = gate.getReturnFacing();
        if (returnDimension == null || returnPortal == null) return false;

        ServerLevel destination = sourceLevel.getServer().getLevel(returnDimension);
        if (destination == null) return false;
        BlockPos desired = returnPortal.relative(returnFacing);
        BlockPos landing = findSafeLandingOrSpawn(destination, desired, true);
        if (landing == null) {
            landing = createEmergencyLandingPlatform(destination, desired, true);
            if (landing == null) return false;
        }

        Vec3 destinationPosition = Vec3.atBottomCenterOf(landing);
        Vec3 momentum = reverseFacingMomentum(entity.getDeltaMovement(), returnFacing);
        Entity moved = move(entity, sourceLevel, destination, destinationPosition, momentum);
        if (moved != null) moved.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        return moved != null;
    }

    private static BlockPos landingOrigin(
        Entity entity, ServerLevel destination, CelestialTravelData.CoordinateRule rule
    ) {
        return switch (rule.type()) {
            case SAME -> BlockPos.containing(entity.getX(), destination.getSharedSpawnPos().getY(), entity.getZ());
            case SAME_3D -> new BlockPos(
                Mth.floor(entity.getX()),
                Math.clamp(
                    Mth.floor(entity.getY()),
                    destination.getMinBuildHeight() + 1,
                    destination.getMaxBuildHeight() - 5
                ),
                Mth.floor(entity.getZ())
            );
            case SCALED -> BlockPos.containing(
                entity.getX() * rule.scale(), destination.getSharedSpawnPos().getY(), entity.getZ() * rule.scale()
            );
            case FIXED -> new BlockPos(rule.x(), rule.y(), rule.z());
            case RANDOM_SPAWN -> randomSpawnOrigin(destination, rule.radius(), entity.getUUID());
        };
    }

    /**
     * Reports whether a coordinate rule describes a landing height of its own.
     *
     * <p>Rules that only describe X/Z have to fall back to the destination
     * heightmap, while height-carrying rules keep their requested Y so that
     * dimensions without terrain still land where the rule asked for.</p>
     */
    private static boolean keepsRequestedHeight(CelestialTravelData.CoordinateRule.Type type) {
        return type == CelestialTravelData.CoordinateRule.Type.FIXED
            || type == CelestialTravelData.CoordinateRule.Type.SAME_3D;
    }

    private static BlockPos returnOrigin(
        ServerLevel destination,
        Entity entity,
        CelestialTravelData.ReturnRule rule,
        BlockPos entry
    ) {
        return switch (rule.type()) {
            case ENTRY_PORTAL -> entry;
            case FIXED_PORTAL -> new BlockPos(rule.x(), rule.y(), rule.z());
            case RANDOM_PORTAL -> randomSpawnOrigin(destination, rule.radius(), entity.getUUID());
        };
    }

    private static BlockPos randomSpawnOrigin(ServerLevel level, int chunkRadius, UUID uuid) {
        BlockPos spawn = level.getSharedSpawnPos();
        int radius = Math.max(0, Math.min(chunkRadius, 1024)) * 16;
        long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits()
            ^ level.getGameTime() ^ RANDOM_SALT;
        int x = spawn.getX() + signedOffset(seed, radius);
        int z = spawn.getZ() + signedOffset(Long.rotateLeft(seed, 21), radius);
        return new BlockPos(x, spawn.getY(), z);
    }

    private static int signedOffset(long seed, int radius) {
        if (radius <= 0) return 0;
        return (int) Math.floorMod(seed, (long) radius * 2L + 1L) - radius;
    }

    /** Finds an air column with a solid floor near the requested coordinate. */
    @Nullable
    public static BlockPos findSafeLandingPos(ServerLevel level, BlockPos origin) {
        return findSafeLandingPos(level, origin, true);
    }

    /**
     * Finds a safe two-block column near an origin.  Coordinate rules that
     * describe X/Z only should use the heightmap instead of treating the
     * origin's Y as a hard landing height.
     */
    private static @Nullable BlockPos findSafeLandingPos(
        ServerLevel level, BlockPos origin, boolean keepRequestedY
    ) {
        return findSafeLandingPos(level, origin, keepRequestedY, SEARCH_RADIUS);
    }

    @Nullable
    private static BlockPos findSafeLandingPos(
        ServerLevel level, BlockPos origin, boolean keepRequestedY, int searchRadius
    ) {
        WorldBorder border = level.getWorldBorder();
        BlockPos clamped = border.clampToBounds(origin);
        BlockPos direct = findSafeLandingInColumn(
            level, origin.getX(), origin.getZ(), origin, keepRequestedY
        );
        if (direct != null && border.isWithinBounds(direct.getX(), direct.getZ())) return direct;
        for (BlockPos.MutableBlockPos column : BlockPos.spiralAround(
            clamped, searchRadius, Direction.EAST, Direction.SOUTH
        )) {
            int x = column.getX();
            int z = column.getZ();
            if (!border.isWithinBounds(x, z)) continue;
            BlockPos candidate = findSafeLandingInColumn(level, x, z, origin, keepRequestedY);
            if (candidate != null) return candidate;
        }
        return null;
    }

    /**
     * Preserves the requested location whenever possible, then falls back to
     * the destination spawn area when that location has no safe ground, such
     * as the middle of a large ocean.
     */
    @Nullable
    private static BlockPos findSafeLandingOrSpawn(ServerLevel level, BlockPos origin, boolean keepRequestedY) {
        BlockPos landing = findSafeLandingPos(level, origin, keepRequestedY);
        if (landing != null) return landing;
        return findSafeLandingPos(level, level.getSharedSpawnPos(), false, SPAWN_SEARCH_RADIUS);
    }

    /**
     * Builds a small obsidian platform when a coordinate rule resolves into an
     * ocean or another area with no natural safe landing.  This mirrors the
     * fallback used by vanilla cross-dimension portals while keeping the
     * generated return gate usable immediately.
     */
    @Nullable
    private static BlockPos createEmergencyLandingPlatform(
        ServerLevel level, BlockPos origin, boolean keepRequestedY
    ) {
        WorldBorder border = level.getWorldBorder();
        BlockPos clamped = border.clampToBounds(origin);
        int floorY = keepRequestedY
            ? origin.getY() - 1
            : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, clamped.getX(), clamped.getZ()) - 1;
        if (floorY < level.getMinBuildHeight() || floorY > level.getMaxBuildHeight() - 4) return null;

        BlockPos floorCenter = new BlockPos(clamped.getX(), floorY, clamped.getZ());
        if (!canCreateEmergencyPlatform(level, floorCenter, border)) return null;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos floor = floorCenter.offset(x, 0, z);
                if (!level.getBlockState(floor).isSolid()) {
                    level.setBlock(floor, Blocks.OBSIDIAN.defaultBlockState(), 3);
                }
                clearEmergencyPlatformSpace(level, floor.above());
                clearEmergencyPlatformSpace(level, floor.above(2));
                clearEmergencyPlatformSpace(level, floor.above(3));
            }
        }
        return floorCenter.above();
    }

    private static boolean canCreateEmergencyPlatform(ServerLevel level, BlockPos floorCenter, WorldBorder border) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos floor = floorCenter.offset(x, 0, z);
                if (!border.isWithinBounds(floor.getX(), floor.getZ())) return false;
                BlockState floorState = level.getBlockState(floor);
                if (!floorState.isSolid() && !canClearForEmergencyPlatform(floorState)) return false;
                if (!canClearForEmergencyPlatform(level.getBlockState(floor.above()))
                    || !canClearForEmergencyPlatform(level.getBlockState(floor.above(2)))
                    || !canClearForEmergencyPlatform(level.getBlockState(floor.above(3)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean canClearForEmergencyPlatform(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced();
    }

    private static void clearEmergencyPlatformSpace(ServerLevel level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Nullable
    private static BlockPos findSafeLandingInColumn(
        ServerLevel level, int x, int z, BlockPos origin, boolean keepRequestedY
    ) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int preferredY = keepRequestedY && origin.getY() > level.getMinBuildHeight()
            ? origin.getY() : height;
        int minY = Math.max(level.getMinBuildHeight() + 1, preferredY - 8);
        int maxY = Math.min(level.getMaxBuildHeight() - 3, preferredY + 8);
        for (int y = maxY; y >= minY; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (canStand(level, candidate)) return candidate;
        }
        return null;
    }

    private static boolean canStand(ServerLevel level, BlockPos feet) {
        BlockState floor = level.getBlockState(feet.below());
        return floor.isSolid() && level.isEmptyBlock(feet) && level.isEmptyBlock(feet.above());
    }

    /** Finds a raised gate position whose outward side has a safe two-block landing column. */
    @Nullable
    private static BlockPos findSafeGateOrSpawn(
        ServerLevel level,
        BlockPos origin,
        Direction facing,
        ResourceKey<Level> sourceDimension,
        BlockPos sourcePortalPos,
        boolean keepRequestedY
    ) {
        BlockPos gate = findSafeGatePos(
            level, origin, facing, sourceDimension, sourcePortalPos, keepRequestedY, SEARCH_RADIUS
        );
        if (gate != null) return gate;
        return findSafeGatePos(
            level, level.getSharedSpawnPos(), facing, sourceDimension, sourcePortalPos, false, SPAWN_SEARCH_RADIUS
        );
    }

    /** Finds a raised gate position whose outward side has a safe two-block landing column. */
    @Nullable
    private static BlockPos findSafeGatePos(
        ServerLevel level,
        BlockPos origin,
        Direction facing,
        ResourceKey<Level> sourceDimension,
        BlockPos sourcePortalPos,
        boolean keepRequestedY,
        int searchRadius
    ) {
        WorldBorder border = level.getWorldBorder();
        BlockPos clamped = border.clampToBounds(origin);
        BlockPos direct = findSafeGateInColumn(
            level, origin.getX(), origin.getZ(), origin, facing, sourceDimension, sourcePortalPos, keepRequestedY
        );
        if (direct != null && border.isWithinBounds(direct.getX(), direct.getZ())) return direct;
        for (BlockPos.MutableBlockPos column : BlockPos.spiralAround(
            clamped, searchRadius, Direction.EAST, Direction.SOUTH
        )) {
            int x = column.getX();
            int z = column.getZ();
            if (!border.isWithinBounds(x, z)) continue;
            BlockPos candidate = findSafeGateInColumn(
                level, x, z, origin, facing, sourceDimension, sourcePortalPos, keepRequestedY
            );
            if (candidate != null) return candidate;
        }
        return null;
    }

    @Nullable
    private static BlockPos findSafeGateInColumn(
        ServerLevel level,
        int x,
        int z,
        BlockPos origin,
        Direction facing,
        ResourceKey<Level> sourceDimension,
        BlockPos sourcePortalPos,
        boolean keepRequestedY
    ) {
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int preferredY = keepRequestedY && origin.getY() > level.getMinBuildHeight()
            ? origin.getY() : height;
        int minY = Math.max(level.getMinBuildHeight() + 1, preferredY - 8);
        int maxY = Math.min(level.getMaxBuildHeight() - 3, preferredY + 8);
        for (int y = maxY; y >= minY; y--) {
            BlockPos landing = new BlockPos(x, y, z);
            BlockPos gate = landing.above();
            BlockState gateState = level.getBlockState(gate);
            boolean gateSpace = gateState.isAir()
                || gateState.getFluidState().is(Fluids.WATER)
                || gateState.getBlock() instanceof CelestialBackGateBlock;
            if (gateState.getBlock() instanceof CelestialBackGateBlock
                && !isReusableGate(level, gate, sourceDimension, sourcePortalPos, facing)) {
                continue;
            }
            boolean gateClearance = level.isEmptyBlock(landing) && level.isEmptyBlock(gate.above());
            if (gateSpace && gateClearance && canStand(level, landing.relative(facing))) return gate;
        }
        return null;
    }

    private static boolean isReusableGate(
        ServerLevel level, BlockPos pos, ResourceKey<Level> sourceDimension, BlockPos sourcePortalPos,
        Direction sourceFacing
    ) {
        if (!(level.getBlockEntity(pos) instanceof CelestialBackGateBlockEntity gate)) return false;
        return isMatchingGate(gate, sourceDimension, sourcePortalPos, sourceFacing, false);
    }

    private static boolean isMatchingGate(
        CelestialBackGateBlockEntity gate, ResourceKey<Level> sourceDimension, BlockPos sourcePortalPos,
        Direction sourceFacing, boolean allowNearbySourcePortal
    ) {
        ResourceKey<Level> returnDimension = gate.getReturnDimension();
        BlockPos returnPortalPos = gate.getReturnPortalPos();
        return returnDimension == null || returnPortalPos == null
            || (returnDimension.equals(sourceDimension)
                && (allowNearbySourcePortal
                    ? isWithinGateReuseRange(sourcePortalPos, returnPortalPos)
                    : sourcePortalPos.equals(returnPortalPos))
                && gate.getReturnFacing() == sourceFacing);
    }

    private static boolean isWithinGateReuseRange(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX()) <= GATE_REUSE_RADIUS
            && Math.abs(first.getY() - second.getY()) <= GATE_REUSE_VERTICAL_RADIUS
            && Math.abs(first.getZ() - second.getZ()) <= GATE_REUSE_RADIUS;
    }

    /** Finds an already generated gate before a new landing search can move it aside. */
    @Nullable
    private static BlockPos findMatchingGateNear(
        ServerLevel level,
        BlockPos origin,
        ResourceKey<Level> sourceDimension,
        BlockPos sourcePortalPos,
        Direction sourceFacing,
        boolean allowNearbySourcePortal
    ) {
        BlockPos nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (int dx = -GATE_REUSE_RADIUS; dx <= GATE_REUSE_RADIUS; dx++) {
            for (int dy = -GATE_REUSE_VERTICAL_RADIUS; dy <= GATE_REUSE_VERTICAL_RADIUS; dy++) {
                for (int dz = -GATE_REUSE_RADIUS; dz <= GATE_REUSE_RADIUS; dz++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(candidate)) continue;
                    if (!(level.getBlockState(candidate).getBlock() instanceof CelestialBackGateBlock)
                        || !(level.getBlockEntity(candidate) instanceof CelestialBackGateBlockEntity gate)
                        || !isMatchingGate(
                            gate, sourceDimension, sourcePortalPos, sourceFacing, allowNearbySourcePortal
                        )) {
                        continue;
                    }
                    long distance = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                    if (distance < nearestDistance) {
                        nearest = candidate.immutable();
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }

    /** Removes only gates that point to the same source portal as the selected gate. */
    public static void cleanupDuplicateGates(
        ServerLevel level,
        BlockPos selected,
        ResourceKey<Level> sourceDimension,
        BlockPos sourcePortalPos,
        Direction sourceFacing
    ) {
        if (!(level.getBlockState(selected).getBlock() instanceof CelestialBackGateBlock)) return;
        for (int dx = -GATE_REUSE_RADIUS; dx <= GATE_REUSE_RADIUS; dx++) {
            for (int dy = -GATE_REUSE_VERTICAL_RADIUS; dy <= GATE_REUSE_VERTICAL_RADIUS; dy++) {
                for (int dz = -GATE_REUSE_RADIUS; dz <= GATE_REUSE_RADIUS; dz++) {
                    BlockPos candidate = selected.offset(dx, dy, dz);
                    if (!level.hasChunkAt(candidate)) continue;
                    if (candidate.equals(selected)
                        || !(level.getBlockState(candidate).getBlock() instanceof CelestialBackGateBlock)
                        || !(level.getBlockEntity(candidate) instanceof CelestialBackGateBlockEntity gate)) {
                        continue;
                    }
                    ResourceKey<Level> returnDimension = gate.getReturnDimension();
                    BlockPos returnPortalPos = gate.getReturnPortalPos();
                    boolean sameSource = returnDimension != null
                        && returnDimension.equals(sourceDimension)
                        && sourcePortalPos.equals(returnPortalPos)
                        && gate.getReturnFacing() == sourceFacing;
                    if (sameSource) level.removeBlock(candidate, false);
                }
            }
        }
    }

    private static Vec3 reverseFacingMomentum(Vec3 velocity, Direction facing) {
        if (facing.getAxis() == Direction.Axis.X) {
            return new Vec3(-velocity.x, velocity.y, velocity.z);
        }
        return new Vec3(velocity.x, velocity.y, -velocity.z);
    }

    @Nullable
    private static Entity move(
        Entity entity,
        ServerLevel source,
        ServerLevel destination,
        Vec3 position,
        Vec3 momentum
    ) {
        float targetYRot = (entity.getYRot() + 180.0f) % 360.0f;
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(destination, position.x, position.y, position.z, targetYRot, entity.getXRot());
            player.setDeltaMovement(momentum);
            player.hasImpulse = true;
            return player;
        }
        if (source == destination) {
            entity.teleportTo(destination, position.x, position.y, position.z, Set.of(), targetYRot, entity.getXRot());
            entity.setDeltaMovement(momentum);
            entity.hasImpulse = true;
            return entity;
        }
        Entity moved = entity.changeDimension(new DimensionTransition(
            destination,
            position,
            momentum,
            targetYRot,
            entity.getXRot(),
            DimensionTransition.DO_NOTHING
        ));
        if (moved != null) {
            moved.setDeltaMovement(momentum);
            moved.hasImpulse = true;
        }
        return moved;
    }
}
