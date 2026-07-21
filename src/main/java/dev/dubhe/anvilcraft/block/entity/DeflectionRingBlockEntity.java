package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.power.ring.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.network.DeflectionRingUpdateLastSpeedPacket;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import dev.dubhe.anvilcraft.util.DistanceComparator;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2d;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class DeflectionRingBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final double DEFLECTION_RADIUS_SQR = 0.56747 * 0.56747;
    // A 0.98-block-wide falling anvil needs a small gap from the ring's block boundary.
    private static final double DEFLECTION_EXIT_OFFSET = 1.01;
    private static final HashMap<Level, RingIndex> LEVEL_DEFLECTION_BLOCK_MAP = new HashMap<>();
    @Getter
    @Setter
    private PowerGrid grid;

    @Setter
    @Getter
    private double lastEntitySpeed = 0;
    private int resetEntitySpeedTickCounter = 0;

    @Getter
    private boolean overSpeed = false;
    private int overSpeedTick = 0;

    public DeflectionRingBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DEFLECTION_RING.get(), pos, blockState);
    }

    private DeflectionRingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static DeflectionRingBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new DeflectionRingBlockEntity(type, pos, blockState);
    }

    public static Iterable<BlockPos> getAllBlocks(Level level) {
        RingIndex index = LEVEL_DEFLECTION_BLOCK_MAP.get(level);
        return index == null ? List.of() : index.positions;
    }

    public static boolean isInsideWorkingRing(Entity entity) {
        Level level = entity.level();
        RingIndex index = LEVEL_DEFLECTION_BLOCK_MAP.get(level);
        if (index == null) return false;
        AABB boundingBox = entity.getBoundingBox();
        int minChunkX = Mth.floor(boundingBox.minX) >> 4;
        int maxChunkX = Mth.floor(boundingBox.maxX) >> 4;
        int minChunkZ = Mth.floor(boundingBox.minZ) >> 4;
        int maxChunkZ = Mth.floor(boundingBox.maxZ) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                HashSet<BlockPos> positions = index.byChunk.get(ChunkPos.pack(chunkX, chunkZ));
                if (positions == null) continue;
                for (BlockPos pos : positions) {
                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof DeflectionRingBlock)) continue;
                    if (state.getValue(DeflectionRingBlock.SWITCH) != Switch.ON
                        || state.getValue(DeflectionRingBlock.OVERLOAD)) {
                        continue;
                    }
                    if (new AABB(pos).intersects(boundingBox)) return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static BlockPos findFirstRing(Entity entity, Vec3 start, Vec3 movement) {
        double movementSqr = movement.lengthSqr();
        if (!Double.isFinite(movementSqr) || movementSqr < 1.0E-12) return null;
        RingIndex index = LEVEL_DEFLECTION_BLOCK_MAP.get(entity.level());
        if (index == null) return null;

        BlockPos nearestRing = null;
        double nearestProgress = Double.POSITIVE_INFINITY;
        Vec3 end = start.add(movement);
        int chunkX = Mth.floor(start.x) >> 4;
        int chunkZ = Mth.floor(start.z) >> 4;
        int endChunkX = Mth.floor(end.x) >> 4;
        int endChunkZ = Mth.floor(end.z) >> 4;
        int stepX = movement.x > 0.0 ? 1 : movement.x < 0.0 ? -1 : 0;
        int stepZ = movement.z > 0.0 ? 1 : movement.z < 0.0 ? -1 : 0;
        double nextBoundaryX = stepX > 0 ? (chunkX + 1) * 16.0 : chunkX * 16.0;
        double nextBoundaryZ = stepZ > 0 ? (chunkZ + 1) * 16.0 : chunkZ * 16.0;
        double nextChunkProgressX = stepX == 0
                                    ? Double.POSITIVE_INFINITY
                                    : (nextBoundaryX - start.x) / movement.x;
        double nextChunkProgressZ = stepZ == 0
                                    ? Double.POSITIVE_INFINITY
                                    : (nextBoundaryZ - start.z) / movement.z;
        double chunkProgressStepX = stepX == 0
                                    ? Double.POSITIVE_INFINITY
                                    : 16.0 / Math.abs(movement.x);
        double chunkProgressStepZ = stepZ == 0
                                    ? Double.POSITIVE_INFINITY
                                    : 16.0 / Math.abs(movement.z);
        LongOpenHashSet checkedChunks = chunkX == endChunkX && chunkZ == endChunkZ ? null : new LongOpenHashSet();
        int remainingChunks = Math.abs(endChunkX - chunkX) + Math.abs(endChunkZ - chunkZ) + 1;

        while (remainingChunks-- > 0) {
            // The hit radius is below one block, so adjacent chunks cover the whole swept cylinder.
            for (int candidateChunkX = chunkX - 1; candidateChunkX <= chunkX + 1; candidateChunkX++) {
                for (int candidateChunkZ = chunkZ - 1; candidateChunkZ <= chunkZ + 1; candidateChunkZ++) {
                    long chunkKey = ChunkPos.pack(candidateChunkX, candidateChunkZ);
                    if (checkedChunks != null && !checkedChunks.add(chunkKey)) continue;
                    HashSet<BlockPos> positions = index.byChunk.get(chunkKey);
                    if (positions == null) continue;
                    for (BlockPos pos : positions) {
                        BlockState state = entity.level().getBlockState(pos);
                        if (!(state.getBlock() instanceof DeflectionRingBlock)
                            || state.getValue(DeflectionRingBlock.SWITCH) != Switch.ON
                            || state.getValue(DeflectionRingBlock.OVERLOAD)) {
                            continue;
                        }
                        Vec3 toCenter = pos.getCenter().subtract(start);
                        double progress = toCenter.dot(movement) / movementSqr;
                        if (progress <= 0 || progress > 1 || progress >= nearestProgress) continue;
                        Vec3 closest = start.add(movement.scale(progress));
                        if (closest.distanceToSqr(pos.getCenter()) <= DEFLECTION_RADIUS_SQR) {
                            nearestProgress = progress;
                            nearestRing = pos;
                        }
                    }
                }
            }
            if (chunkX == endChunkX && chunkZ == endChunkZ) break;
            if (nextChunkProgressX < nextChunkProgressZ) {
                chunkX += stepX;
                nextChunkProgressX += chunkProgressStepX;
            } else if (nextChunkProgressZ < nextChunkProgressX) {
                chunkZ += stepZ;
                nextChunkProgressZ += chunkProgressStepZ;
            } else {
                chunkX += stepX;
                chunkZ += stepZ;
                nextChunkProgressX += chunkProgressStepX;
                nextChunkProgressZ += chunkProgressStepZ;
            }
        }
        return nearestRing;
    }

    public static void clear(Level level) {
        LEVEL_DEFLECTION_BLOCK_MAP.remove(level);
    }

    private void addSelfToMap() {
        if (level == null) return;
        LEVEL_DEFLECTION_BLOCK_MAP.computeIfAbsent(level, ignored -> new RingIndex()).add(getBlockPos());
    }

    private void removeSelfFromMap() {
        if (level == null) return;
        RingIndex index = LEVEL_DEFLECTION_BLOCK_MAP.get(level);
        if (index == null) return;
        index.remove(getBlockPos());
        if (index.positions.isEmpty()) LEVEL_DEFLECTION_BLOCK_MAP.remove(level);
    }

    private void updateLastEntitySpeed(Double speed) {
        this.resetEntitySpeedTickCounter = 0;
        this.lastEntitySpeed = speed;
        BlockState state = getBlockState();
        if (level == null) return;
        if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
        block.forEachPart(level, getBlockPos(), it -> level.updateNeighbourForOutputSignal(it, level.getBlockState(it).getBlock()));
        if (!(level instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            ChunkPos.containing(getBlockPos()),
            new DeflectionRingUpdateLastSpeedPacket(getBlockPos(), this.lastEntitySpeed)
        );
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("lastEntitySpeed", this.lastEntitySpeed);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        this.lastEntitySpeed = input.getDoubleOr("lastEntitySpeed", 0.0);
        super.loadAdditional(input);
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return level;
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public PowerComponentType getComponentType() {
        if (level == null) return PowerComponentType.INVALID;
        if (!level.getBlockState(getBlockPos()).hasProperty(DeflectionRingBlock.HALF)) return PowerComponentType.INVALID;
        if (level.getBlockState(getBlockPos()).getValue(DeflectionRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) {
            return PowerComponentType.CONSUMER;
        }
        return PowerComponentType.INVALID;
    }

    @Override
    public int getRange() {
        return 1;
    }

    public boolean isWork() {
        BlockState state = getBlockState();
        return state.getValue(DeflectionRingBlock.SWITCH) == Switch.ON && !state.getValue(DeflectionRingBlock.OVERLOAD);
    }

    public void tick() {
        if (level == null) return;
        if (this.resetEntitySpeedTickCounter >= 40 && !level.isClientSide()) this.updateLastEntitySpeed(0.0);
        else this.resetEntitySpeedTickCounter++;
        if (this.overSpeed && this.overSpeedTick > 1) {
            this.overSpeed = false;
            this.overSpeedTick = 0;
            BlockState state = getBlockState();
            if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
            block.updateState(level, getBlockPos(), DeflectionRingBlock.OVERLOAD, state.getValue(DeflectionRingBlock.OVERLOAD), 3);
        } else if (this.overSpeed) {
            this.overSpeedTick++;
        }
        if (level.isClientSide()) {
            if (!getBlockState().getValue(DeflectionRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
            if (this.isWork()) {
                this.addSelfToMap();
                this.accelerate();
            } else this.removeSelfFromMap();
        }
        if (this.grid == null) return;
        BlockState state = getBlockState();
        if (!state.getValue(DeflectionRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
        if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
        if (this.grid.isWorking() && state.getValue(DeflectionRingBlock.OVERLOAD)) {
            block.updateState(level, getBlockPos(), DeflectionRingBlock.OVERLOAD, false, 3);
        } else if (!this.grid.isWorking() && !state.getValue(DeflectionRingBlock.OVERLOAD)) {
            block.updateState(level, getBlockPos(), DeflectionRingBlock.OVERLOAD, true, 3);
        }
        if (!this.isWork()) {
            this.removeSelfFromMap();
            return;
        }
        this.addSelfToMap();
        if (state.getValue(DeflectionRingBlock.FACING).getAxis().equals(Direction.Axis.Y)) this.attractGianAnvil();
        this.accelerate();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void accelerate() {
        if (this.level == null) return;
        BlockState ringState = this.getBlockState();
        boolean waterloggedChannel = ringState.getBlock() instanceof DeflectionRingBlock block
                                     && block.isChannelWaterlogged(this.level, this.getBlockPos(), ringState);
        List<Entity> entities2 = this.level.getEntitiesOfClass(
            Entity.class,
            new AABB(getBlockPos()),
            AccelerateManager::canBeAccelerated
        );
        for (Entity entity : entities2) {
            entity.setDeltaMovement(AccelerateManager.clampMovement(entity, entity.getDeltaMovement()));
            if (entity.getDeltaMovement().length() > Integer.MAX_VALUE * 0.99f) {
                this.overSpeed = true;
                BlockState state = getBlockState();
                if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
                block.updateState(this.level, getBlockPos(), DeflectionRingBlock.OVERLOAD, state.getValue(DeflectionRingBlock.OVERLOAD), 3);
            }
            Vec3 v = entity.getDeltaMovement();
            Direction facing = getBlockState().getValue(DeflectionRingBlock.FACING);
            v = switch (facing) {
                case UP -> new Vec3(v.z, 0, -v.x);
                case DOWN -> new Vec3(-v.z, 0, v.x);
                case NORTH -> new Vec3(v.y, -v.x, 0);
                case SOUTH -> new Vec3(-v.y, v.x, 0);
                case WEST -> new Vec3(0, v.z, -v.y);
                case EAST -> new Vec3(0, -v.z, v.y);
            };
            if (waterloggedChannel) v = AccelerateManager.limitAnvilSpeed(entity, v);
            Vec3 fixedPos = v.normalize()
                .scale(DEFLECTION_EXIT_OFFSET)
                .subtract(AccelerateManager.getMovementOffset(entity));
            entity.setDeltaMovement(AccelerateManager.clampMovement(entity, v));
            if (entity instanceof Player) {
                double d0 = v.x;
                double d1 = v.y;
                double d2 = v.z;
                double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                entity.setXRot(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 180.0F / (float) Math.PI))));
                entity.setYRot(Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 180.0F / (float) Math.PI) - 90.0F));
                entity.setYHeadRot(entity.getYRot());
            }
            Vec3 blockCenter = getBlockPos().getCenter();
            entity.setPos(fixedPos.add(blockCenter));
        }
        Direction.Axis axis = getBlockState().getValue(DeflectionRingBlock.FACING).getAxis();
        BlockPos min = getBlockPos().offset(axis == Direction.Axis.X ? 0 : -1, axis == Direction.Axis.Y ? 0 : -1,
            axis == Direction.Axis.Z ? 0 : -1);
        BlockPos max = getBlockPos().offset(axis == Direction.Axis.X ? 0 : 1, axis == Direction.Axis.Y ? 0 : 1,
            axis == Direction.Axis.Z ? 0 : 1);
        AABB accelerationArea = AABB.encapsulatingFullBlocks(min, max);
        List<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            accelerationArea,
            AccelerateManager::canBeAccelerated
        );
        for (Entity entity : entities) {
            if (!accelerationArea.contains(AccelerateManager.getMovementCenter(entity))) continue;
            Vec3 acceleratedMovement = AccelerateManager.clampMovement(
                entity,
                entity.getDeltaMovement().scale(1.0204081632653061)
            );
            if (waterloggedChannel) {
                acceleratedMovement = AccelerateManager.limitAnvilSpeed(entity, acceleratedMovement);
            }
            entity.setDeltaMovement(acceleratedMovement);
            if (level.isClientSide()) continue;
            this.updateLastEntitySpeed(entity.getDeltaMovement().length());
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public void attractGianAnvil() {
        assert level != null;
        if (
            level.getBlockState(getBlockPos().below(2)).hasProperty(GiantAnvilBlock.HALF)
            && level.getBlockState(getBlockPos().below(2)).getValue(GiantAnvilBlock.HALF) == Cube3x3PartHalf.TOP_CENTER
        ) {
            return;
        }
        BlockPos giantAnvilPos = null;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        checkPos.set(getBlockPos().below(2));
        for (int y = 0; y < 11; y++) {
            BlockState checkState = level.getBlockState(checkPos);
            if (!checkState.hasProperty(GiantAnvilBlock.HALF)) {
                checkPos.move(Direction.DOWN);
                continue;
            }
            Cube3x3PartHalf cube3x3PartHalf = checkState.getValue(GiantAnvilBlock.HALF);
            if (cube3x3PartHalf == Cube3x3PartHalf.MID_CENTER) {
                giantAnvilPos = checkPos.west(0);
                break;
            }
            checkPos.move(Direction.DOWN);
        }
        Vector2d vector2d = new Vector2d(getBlockPos().getCenter().x, getBlockPos().getCenter().z);
        Optional<FallingGiantAnvilEntity> fallingGiantAnvilEntity = level.getEntitiesOfClass(FallingGiantAnvilEntity.class, new AABB(
                        getBlockPos().getX(),
                        getBlockPos().getY() - 2,
                        getBlockPos().getZ(),
                        getBlockPos().getX() + 1,
                        getBlockPos().getY() - 12,
                        getBlockPos().getZ() + 1
                )).stream()
                .sorted((e1, e2) -> new DistanceComparator(getBlockPos().getCenter()).compare(e1.position(), e2.position()))
                .filter(entity -> vector2d.distance(entity.position().x, entity.position().z) <= 0.25)
                .findFirst();
        if (fallingGiantAnvilEntity.isPresent()) {
            if (
                giantAnvilPos != null
                && fallingGiantAnvilEntity.get().position().distanceTo(getBlockPos().getCenter())
                   < giantAnvilPos.getCenter().distanceTo(getBlockPos().getCenter())
            ) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            } else if (giantAnvilPos == null) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            }
        }
        if (giantAnvilPos == null) return;
        checkPos.set(giantAnvilPos);
        checkPos.move(-1, 2, -1);
        while (checkPos.getY() < getBlockPos().getY() - 1) {
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    BlockState checked = level.getBlockState(checkPos);
                    if (!checked.canBeReplaced()) return;
                    checkPos.move(0, 0, 1);
                }
                checkPos.move(0, 0, -3);
                checkPos.move(1, 0, 0);
            }
            checkPos.move(-3, 1, 0);
        }
        Block block = level.getBlockState(giantAnvilPos.below()).getBlock();
        if (block instanceof GiantAnvilBlock giantAnvilBlock) {
            giantAnvilBlock.removePartsAndUpdate(level, giantAnvilPos.below());
        }
        BlockPos newPos = getBlockPos().below(4);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            level.setBlockAndUpdate(newPos.offset(part.getOffset()), ModBlocks.GIANT_ANVIL.getDefaultState()
                    .setValue(GiantAnvilBlock.HALF, part)
                    .setValue(GiantAnvilBlock.CUBE, part.equals(Cube3x3PartHalf.MID_CENTER) ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER)
            );
        }
        fallingGiantAnvilEntity.ifPresent(Entity::discard);
    }

    @Override
    public int getInputPower() {
        return getBlockState().getValue(DeflectionRingBlock.SWITCH) == Switch.ON ? 256 : 0;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.removeSelfFromMap();
    }

    private static final class RingIndex {
        private final HashSet<BlockPos> positions = new HashSet<>();
        private final Long2ObjectOpenHashMap<HashSet<BlockPos>> byChunk = new Long2ObjectOpenHashMap<>();

        private void add(BlockPos pos) {
            BlockPos immutablePos = pos.immutable();
            if (!this.positions.add(immutablePos)) return;
            long chunkKey = ChunkPos.pack(immutablePos.getX() >> 4, immutablePos.getZ() >> 4);
            this.byChunk.computeIfAbsent(chunkKey, ignored -> new HashSet<>()).add(immutablePos);
        }

        private void remove(BlockPos pos) {
            if (!this.positions.remove(pos)) return;
            long chunkKey = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
            HashSet<BlockPos> chunkPositions = this.byChunk.get(chunkKey);
            if (chunkPositions == null) return;
            chunkPositions.remove(pos);
            if (chunkPositions.isEmpty()) this.byChunk.remove(chunkKey);
        }
    }
}
