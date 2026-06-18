package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Laser interface for the Celestial Forging Anvil.
 * Extends BaseLaserBlockEntity to participate in the laser chain system.
 *
 * <p>
 * Passive mode (no redstone): receives incoming laser beams, reports level to CFA controller.
 * Active mode (redstone powered): emits laser in facing direction. Also used by Penrose Sphere
 * to emit gamma laser output.
 */
public class CelestialForgingAnvilLaserInterfaceBlockEntity extends BaseLaserBlockEntity {
    @Getter
    private int receivedLaserLevel = 0;
    @Getter
    private boolean receivedGamma = false;
    @Getter
    private boolean laserValid = false;
    @Getter
    private int requiredLaserLevel = 0;

    // Gamma laser state (set by CFA controller for Penrose Sphere output)
    @Getter
    private boolean emittingGamma = false;
    @Getter
    private int gammaLevel = 0;

    // GammaActiveCountdown: set to 21 ticks (GRID_TICK+1) in emitGammaLaser,
    // decremented each non-gamma tick. Ensures HeaterManager.tickAll() sees
    // the gamma state even if it runs on a later Pre-tick event.
    @Getter
    private int gammaActiveCountdown = 0;

    // Gamma laser block breaking: required continuous exposure in ticks
    // [disabled, ≥4:3s, ≥8:1s, ≥12:5gt, ≥16:1gt]
    private static final int[] GAMMA_EXPOSURE_TICKS = {
        Integer.MAX_VALUE,
        60,   // ≥4: 60 ticks (3s) continuous exposure
        20,   // ≥8: 20 ticks (1s)
        5,    // ≥12: 5 ticks
        1     // ≥16: 1 tick
    };

    // Track which block position is being gamma-irradiated and for how long.
    // Reset when the laser changes targets, so exposure is per-block.
    @Nullable
    private BlockPos gammaIrradiatingPos = null;
    private int gammaExposureTicks = 0;

    public CelestialForgingAnvilLaserInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // === BaseLaserBlockEntity abstract methods ===

    @Override
    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(CelestialForgingAnvilInterfaceBlock.FACING)) {
            return state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    protected int getBaseLaserLevel() {
        BlockState state = getBlockState();
        if (state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
            && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE)) {
            return 1;
        }
        return 0;
    }

    @Override
    public float getLaserOffset() {
        return 0.125f;
    }

    /**
     * When irradiated by an external laser, track the received laser level
     * for CFA controller queries. Does NOT participate in laser chaining.
     */
    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        int level = source.getLaserLevel();
        boolean gamma = source instanceof CelestialForgingAnvilLaserInterfaceBlockEntity cfaSource
            && cfaSource.isEmittingGamma();
        onLaserReceived(level, gamma);
        // Do not chain — do not call super.onIrradiated(source)
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        resetLaser();
    }

    /**
     * When active, ignore lasers from the facing direction (since we emit from there).
     * When passive, accept lasers from all directions.
     */
    @Override
    public java.util.Set<Direction> getIgnoreFace() {
        BlockState state = getBlockState();
        if (state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
            && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE)) {
            // Active: don't receive from our own output direction
            return java.util.Set.of(getFacing());
        }
        // Passive: receive from all directions
        return java.util.Set.of();
    }

    // === CFA laser tracking ===
    public void onLaserReceived(int level, boolean gamma) {
        this.receivedLaserLevel = level;
        this.receivedGamma = gamma;
        this.laserValid = (requiredLaserLevel > 0 && level >= requiredLaserLevel);
        this.setChanged();
    }

    public void resetLaser() {
        this.receivedLaserLevel = 0;
        this.receivedGamma = false;
        this.laserValid = false;
        this.setChanged();
    }

    // === Gamma laser (set by CFA for Penrose Sphere output) ===

    /**
     * Called by the CFA controller to make this interface emit a gamma laser.
     */
    public void emitGammaLaser(int level) {
        this.emittingGamma = true;
        this.gammaLevel = level;
        this.gammaActiveCountdown = 21; // persist past one GRID_TICK for heating system
        this.updateLaserLevel(level);
    }

    // === Tick ===

    /**
     * Server-side tick called by the block ticker.
     */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        boolean active = state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);

        // If receiving an incoming laser, only receive — never emit,
        // regardless of active/passive mode or gamma state.
        if (receivedLaserLevel > 0) {
            // Passive: clear emission since we are receiving
            if (gammaActiveCountdown > 0) gammaActiveCountdown--;
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            clearIrradiateSelfLaserBlockSet();
            updateLaserLevel(0); // clear stale emission level for HUD
        } else if (emittingGamma && gammaLevel > 0) {
            // Emit gamma laser
            Direction facing = getFacing();
            emitGammaLaserBeam(facing);
            // Don't reset emittingGamma yet — tickWithGamma needs it for packet sending
        } else if (active) {
            // Emit normal laser when active
            if (gammaActiveCountdown > 0) gammaActiveCountdown--;
            Direction facing = getFacing();
            // Only emit if not already part of a laser chain
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                emitLaser(facing);
            }
        } else {
            // Passive: clear laser emission
            if (gammaActiveCountdown > 0) gammaActiveCountdown--;
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            clearIrradiateSelfLaserBlockSet();
            updateLaserLevel(0); // clear stale emission level for HUD
        }

        // Custom tick with gamma-aware packet sending
        tickWithGamma(level);

        // Reset gamma emission after packet is sent
        if (emittingGamma) {
            emittingGamma = false;
        }
    }

    /**
     * Override tick to send gamma flag in network packets.
     */
    @Override
    public void tick(Level level) {
        // The serverTick method handles everything; client-side tick is handled by super
        if (level.isClientSide()) {
            super.tick(level);
        }
    }

    /**
     * Custom tick that sends gamma-aware network packets.
     */
    private void tickWithGamma(Level level) {
        if (changed) {
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    level.getChunkAt(getBlockPos()).getPos(),
                    new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.emittingGamma)
                );
            }
        }
        if (level instanceof ServerLevel serverLevel
            && getIrradiateBlockPos() != null
            && serverLevel.getBlockState(getIrradiateBlockPos()).is(ModBlockTags.HEATABLE_BLOCKS)
        ) {
            HeaterManager.addProducer(this.getBlockPos(), serverLevel, ModHeaterInfos.GAMMA_LASER_EMITTER);
        }
        this.tickCount++;
    }

    /**
     * Client-side update for normal laser rendering.
     * Overrides parent to reset gamma flags when a non-gamma packet arrives.
     */
    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = false;
        this.gammaLevel = 0;
        if (irradiateBlockPos != null) {
            super.clientUpdate(irradiateBlockPos, laserLevel);
        }
    }

    /**
     * Client-side update for gamma laser rendering.
     */
    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = true;
        this.gammaLevel = laserLevel;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CacheableBERenderingPipeline.getInstance().update(this);
    }

    /**
     * Emit a gamma laser beam with special properties:
     * - Blue-purple color (rendered client-side via gamma flag in packet)
     * - Max 16 block range
     * - Does NOT pass through glass or laser-transparent blocks; stops at the first non-air block
     * - Destroys prisms on contact
     * - Block breaking based on level
     * - 16x entity damage
     * - Special heating pattern for ember metal
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void emitGammaLaserBeam(Direction direction) {
        if (this.level == null) return;
        int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;

        // Gamma laser: only pass through air/replaceable blocks.
        // All other blocks (including glass) stop the gamma laser.
        BlockPos tempIrradiateBlockPos = getGammaIrradiateBlockPos(16, direction, this.getBlockPos());
        if (this.getBlockState().getBlock() instanceof dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock<?, ?, ?>) {
            tempIrradiateBlockPos = getGammaIrradiateBlockPos(
                16, direction, this.getBlockPos().relative(direction));
        }

        // Handle prism destruction along the beam path
        destroyPrismsAlongPath(direction, tempIrradiateBlockPos);

        // Update old target if changed
        if (!tempIrradiateBlockPos.equals(this.irradiateBlockPos)) {
            if (this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
            }
        }

        // Gamma laser can chain with other BaseLaserBlockEntity instances (e.g., CFA laser interfaces)
        if (
            this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiatedLaserBlockEntity
            && !this.isInIrradiateSelfLaserBlockSet(irradiatedLaserBlockEntity)
        ) {
            if (irradiatedLaserBlockEntity.getIgnoreFace().isEmpty()) {
                this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                irradiatedLaserBlockEntity.onIrradiated(this);
            } else {
                for (Direction dir : irradiatedLaserBlockEntity.getIgnoreFace()) {
                    if (direction != dir) {
                        this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                        irradiatedLaserBlockEntity.onIrradiated(this);
                    }
                }
            }
        }
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);
        this.updateLaserLevel(gammaLevel);

        if (!(this.level instanceof ServerLevel)) {
            this.maxTransmissionDistance = originalMaxDistance;
            return;
        }

        // Entity damage: 16x normal laser damage
        int hurt = Math.min(16, gammaLevel - 4) * 16;
        if (hurt > 0) {
            Vec3 startPos = this.getBlockPos()
                .relative(direction)
                .getCenter()
                .add(-0.0625, -0.0625, -0.0625);
            AABB trackBoundingBox = new AABB(
                startPos,
                this.irradiateBlockPos.relative(direction.getOpposite())
                    .getCenter()
                    .add(0.0625, 0.0625, 0.0625)
            );
            this.level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                trackBoundingBox,
                Entity::isAlive
            ).forEach(livingEntity ->
                livingEntity.hurt(ModDamageTypes.gammaLaser(this.level), hurt)
            );
        }

        // Gamma laser block breaking — continuous exposure per block position
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int requiredExposure = GAMMA_EXPOSURE_TICKS[Math.clamp(gammaLevel / 4, 0, 4)];

        // Track continuous exposure: reset when the irradiated block changes
        BlockPos currentTarget = this.irradiateBlockPos.immutable();
        if (!currentTarget.equals(this.gammaIrradiatingPos)) {
            this.gammaIrradiatingPos = currentTarget;
            this.gammaExposureTicks = 0;
        }

        boolean canBreak = !irradiateBlock.is(BlockTags.WITHER_IMMUNE)
            && !irradiateBlock.isAir()
            && irradiateBlock.getDestroySpeed(this.level, this.irradiateBlockPos) >= 0;

        if (canBreak) {
            this.gammaExposureTicks++;
            if (this.gammaExposureTicks >= requiredExposure) {
                this.gammaExposureTicks = 0;
                // For multipart blocks, find the main part to destroy the whole structure
                BlockPos breakPos = this.irradiateBlockPos;
                if (irradiateBlock.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?> multiPartBlock) {
                    breakPos = multiPartBlock.getMainPartPos(this.irradiateBlockPos, irradiateBlock);
                }
                if (gammaLevel >= 16) {
                    // ≥16: destroy without drops (entire multipart structure)
                    this.level.destroyBlock(breakPos, false);
                } else {
                    // ≥4-15: break with drops at the block position (原地掉落)
                    this.level.destroyBlock(this.irradiateBlockPos, true);
                }
            }
        } else {
            // Block cannot be broken — reset exposure
            this.gammaExposureTicks = 0;
        }

        // Gamma laser heating: 余烬金属 (ember metal) blocks in front
        applyGammaHeating(direction);

        this.maxTransmissionDistance = originalMaxDistance;
    }

    /**
     * Destroy prisms along the gamma laser path.
     */
    private void destroyPrismsAlongPath(Direction direction, BlockPos targetPos) {
        if (level == null) return;
        BlockPos.MutableBlockPos checkPos = getBlockPos().relative(direction).mutable();
        while (!checkPos.equals(targetPos)) {
            BlockState checkState = level.getBlockState(checkPos);
            if (checkState.getBlock() instanceof dev.dubhe.anvilcraft.block.RubyPrismBlock) {
                level.destroyBlock(checkPos.immutable(), true);
            }
            checkPos.move(direction);
        }
    }

    /**
     * Gamma-specific target finder: only passes through air/replaceable blocks.
     * All other blocks (including glass and laser-transparent blocks) stop the gamma laser.
     */
    private BlockPos getGammaIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            BlockPos checkPos = originPos.relative(direction, length);
            if (!gammaCanPassThrough(checkPos)) return checkPos;
        }
        return originPos.relative(direction, expectedLength);
    }

    /**
     * Gamma laser can only pass through air and replaceable blocks (tall grass, etc.).
     * Glass, LASER_CAN_PASS_THROUGH-tagged blocks, etc. all block gamma.
     */
    private boolean gammaCanPassThrough(BlockPos blockPos) {
        if (this.level == null) return false;
        BlockState blockState = this.level.getBlockState(blockPos);
        return blockState.is(BlockTags.REPLACEABLE);
    }

    /**
     * Apply gamma laser heating to ember metal blocks in the beam area.
     * Follows the same pattern as {@code ProduceHeat} recipe outcome:
     * directly calls {@code HeatableBlockEntity.addDurationInTick()} on each
     * heatable block in the area. This bypasses the HeaterManager timing
     * issues — heating is applied immediately every tick.
     *
     * <p>
     * Pattern based on laser level:
     * ≥4: single block ahead
     * ≥8: 3×3 area normal to beam
     * ≥12: 5×5 area, 2 blocks thick
     * ≥16: 7×7 area, 3 blocks thick
     */
    private void applyGammaHeating(Direction direction) {
        if (level == null || !(level instanceof ServerLevel serverLevel)) return;
        if (gammaLevel < 4) return;

        java.util.Set<BlockPos> area = computeGammaHeatingArea(direction, gammaLevel, getBlockPos());

        // Determine the target heat tier and per-tick duration from gamma level
        HeatTierLine line = ModHeaterInfos.GAMMA_LASER_EMITTER.line();
        HeatTierLine.Point point = line.getPoint(gammaLevel).orElse(null);
        if (point == null) return;
        HeatTier targetTier = point.tier();
        // line duration is per-GRID_TICK (20 ticks); convert to per-real-tick
        int durationPerTick = Math.max(1, point.duration() / 20);

        for (BlockPos pos : area) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlockTags.HEATABLE_BLOCKS)) continue;

            HeatableBlockEntity heatable = Util.castSafely(level.getBlockEntity(pos), HeatableBlockEntity.class).orElse(null);
            Optional<ResourceLocation> idOp = HeatRecorder.getId(level, pos, state);
            if (idOp.isEmpty()) continue;
            HeatTier currentTier = HeatRecorder.getTier(level, pos, state)
                .orElseThrow(() -> new IllegalStateException("Unexpected non tier heatable block!"));

            if (targetTier.compareTo(currentTier) > 0) {
                // Upgrade the block to the target tier
                Block deltaBlock = HeatRecorder.getHeatableBlock(idOp.get(), targetTier).orElse(null);
                if (deltaBlock == null) continue;
                level.setBlockAndUpdate(pos, deltaBlock.defaultBlockState());
                if (!(deltaBlock instanceof EntityBlock)) continue;
                BlockEntity deltaBlockEntity = level.getBlockEntity(pos);
                if (!(deltaBlockEntity instanceof HeatableBlockEntity heatableEntity)) continue;
                heatable = heatableEntity;
            } else if (targetTier.compareTo(currentTier) < 0) {
                // Target tier is lower — skip (let it cool down naturally)
                continue;
            }
            if (heatable == null) continue;

            heatable.addDurationInTick(durationPerTick);
        }
    }

    /**
     * Compute the set of block positions heated by a gamma laser.
     * Static helper reused by the heater info registration.
     *
     * @param direction  the direction the laser is facing
     * @param gammaLevel the gamma laser level
     * @param laserPos   the position of the laser block entity
     * @return the set of positions in the gamma heating area
     */
    public static java.util.Set<BlockPos> computeGammaHeatingArea(Direction direction, int gammaLevel, BlockPos laserPos) {
        if (gammaLevel < 4) return java.util.Set.of();

        int areaSize;
        int thickness;
        if (gammaLevel >= 16) {
            areaSize = 7;
            thickness = 3;
        } else if (gammaLevel >= 12) {
            areaSize = 5;
            thickness = 2;
        } else if (gammaLevel >= 8) {
            areaSize = 3;
            thickness = 1;
        } else {
            areaSize = 1;
            thickness = 1;
        }

        int halfSize = areaSize / 2;
        BlockPos startPos = laserPos.relative(direction);

        Direction[] perpendiculars = switch (direction.getAxis()) {
            case X -> new Direction[]{
                Direction.UP,
                Direction.NORTH
            };
            case Z -> new Direction[]{
                Direction.UP,
                Direction.EAST
            };
            default -> // Y
            new Direction[]{
                Direction.NORTH,
                Direction.EAST
            };
        };

        java.util.Set<BlockPos> area = new java.util.HashSet<>();
        for (int depth = 0; depth < thickness; depth++) {
            BlockPos depthPos = startPos.relative(direction, depth);
            for (int a = -halfSize; a <= halfSize; a++) {
                for (int b = -halfSize; b <= halfSize; b++) {
                    area.add(depthPos
                        .relative(perpendiculars[0], a)
                        .relative(perpendiculars[1], b));
                }
            }
        }
        return area;
    }

    // === NBT persistence ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeLaserData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readLaserData(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeLaserData(tag);
        tag.putBoolean("gamma", emittingGamma);
        tag.putInt("gammaLevel", gammaLevel);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readLaserData(tag);
        this.emittingGamma = tag.getBoolean("gamma");
        this.gammaLevel = tag.getInt("gammaLevel");
    }

    private void writeLaserData(CompoundTag tag) {
        tag.putInt("receivedLaserLevel", receivedLaserLevel);
        tag.putBoolean("receivedGamma", receivedGamma);
        tag.putInt("requiredLaserLevel", requiredLaserLevel);
        tag.putBoolean("laserValid", laserValid);
    }

    private void readLaserData(CompoundTag tag) {
        this.receivedLaserLevel = tag.getInt("receivedLaserLevel");
        this.receivedGamma = tag.getBoolean("receivedGamma");
        this.requiredLaserLevel = tag.getInt("requiredLaserLevel");
        this.laserValid = tag.getBoolean("laserValid");
    }

    // === Network sync ===

    /**
     * Sync block entity data to all tracking clients.
     */
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(serverLevel.getChunkAt(worldPosition).getPos(), false)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }
}
