package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.laser.RubyPrismBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

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
    @Getter
    private boolean requiredGamma = false;

    // Gamma laser state (set by CFA controller for Penrose Sphere output)
    @Getter
    private boolean emittingGamma = false;
    @Getter
    private int gammaLevel = 0;

    // Wormhole laser output (set by CFA controller's syncWormholeLasers each tick)
    private int wormholeOutputLevel = 0;
    private boolean wormholeOutputGamma = false;

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

    public CelestialForgingAnvilLaserInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.get(), pos, blockState);
    }

    public static CelestialForgingAnvilLaserInterfaceBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState state
    ) {
        return new CelestialForgingAnvilLaserInterfaceBlockEntity(type, pos, state);
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
            if (this.wormholeOutputLevel > 0) {
                return this.wormholeOutputLevel;
            }
            return 1;
        }
        return 0;
    }

    @Override
    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.emittingGamma)
        );
    }

    /**
     * Whether this laser interface is in active (redstone-powered) mode.
     */
    public boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
            && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    /**
     * Set the wormhole laser output level and gamma flag.
     * Called by the CFA controller's {@code syncWormholeLasers()} each tick.
     */
    public void setWormholeLaserOutput(int level, boolean gamma) {
        this.wormholeOutputLevel = level;
        this.wormholeOutputGamma = gamma;
    }

    @Override
    public float getLaserOffset() {
        return 0.125f;
    }

    @Override
    public int getLaserColor() {
        if (this.emittingGamma) {
            return 0x8040FF; // Blue-purple for gamma laser
        }
        return super.getLaserColor(); // Red for normal laser (0x00ff0d0d)
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
        this.onLaserReceived(level, gamma);
        // Do not chain — do not call super.onIrradiated(source)
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        this.resetLaser();
    }

    /**
     * Only accept lasers from the face.
     * Lasers from the sides and back are ignored.
     */
    @Override
    public Set<Direction> getIgnoreFace() {
        EnumSet<Direction> ignore = EnumSet.allOf(Direction.class);
        ignore.remove(this.getFacing().getOpposite());
        return ignore;
    }

    // === CFA laser tracking ===

    /**
     * Set the laser requirement for this interface, called by the CFA controller.
     * When {@code requiredLevel > 0}, incoming lasers are validated against this requirement.
     *
     * @param requiredLevel the minimum laser level required, or 0 to clear requirement
     * @param gamma         whether a gamma laser is required
     */
    public void setLaserRequirement(int requiredLevel, boolean gamma) {
        this.requiredLaserLevel = requiredLevel;
        this.requiredGamma = gamma;
        // Re-evaluate validity with the new requirement
        if (this.requiredLaserLevel > 0 && this.receivedLaserLevel > 0) {
            this.laserValid = this.receivedLaserLevel >= this.requiredLaserLevel
                && this.receivedGamma == this.requiredGamma;
        } else {
            this.laserValid = false;
        }
        this.setChanged();
    }

    public void onLaserReceived(int level, boolean gamma) {
        this.receivedLaserLevel = level;
        this.receivedGamma = gamma;
        this.laserValid = (this.requiredLaserLevel > 0
            && level >= this.requiredLaserLevel
            && gamma == this.requiredGamma);
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
        this.updateLaserLevel(level);
    }

    // === Tick ===

    /**
     * Server-side tick called by the block ticker.
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        boolean active = state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);

        // If receiving an incoming laser, only receive — never emit,
        // regardless of active/passive mode or gamma state.
        if (this.receivedLaserLevel > 0) {
            // Passive: clear emission since we are receiving
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            irradiateSelfLaserBlockSet.clear();
            updateLaserLevel(0); // clear stale emission level for HUD
        } else if (this.emittingGamma && this.gammaLevel > 0) {
            // Emit gamma laser (Penrose Sphere output)
            Direction facing = this.getFacing();
            this.emitGammaLaserBeam(facing);
            // Don't reset emittingGamma yet — tickWithGamma needs it for packet sending
        } else if (this.wormholeOutputGamma && this.wormholeOutputLevel > 0 && active) {
            // Emit gamma laser via wormhole (summed from passive interfaces across the network).
            // We borrow gammaLevel for the emission but restore it afterward so Penrose Sphere
            // state is preserved. emittingGamma is left true so tickWithGamma sends a gamma
            // packet; it will be reset by the cleanup at the end of serverTick().
            int savedGammaLevel = this.gammaLevel;
            this.gammaLevel = this.wormholeOutputLevel;
            this.emittingGamma = true;
            Direction facing = this.getFacing();
            this.emitGammaLaserBeam(facing);
            this.gammaLevel = savedGammaLevel;
        } else if (active) {
            // Emit normal laser when active (includes wormholeOutputLevel via getBaseLaserLevel)
            Direction facing = this.getFacing();
            // Only emit if not already part of a laser chain
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                emitLaser(facing);
            }
        } else {
            // Passive: clear laser emission
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            irradiateSelfLaserBlockSet.clear();
            updateLaserLevel(0); // clear stale emission level for HUD
        }

        // Custom tick with gamma-aware packet sending
        this.tickWithGamma(level);

        // Reset gamma emission after packet is sent
        if (this.emittingGamma) {
            this.emittingGamma = false;
        }

        // Register as heat producer if currently hitting a heatable block.
        // BaseLaserBlockEntity.tick() normally does this, but we override tick()
        // and only delegate to super on the client, so we must do it here on server.
        if (level instanceof ServerLevel serverLevel
            && irradiateBlockPos != null
            && serverLevel.getBlockState(irradiateBlockPos).is(ModBlockTags.HEATABLE_BLOCKS)) {
            HeaterManager.addProducer(getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
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
        this.tickCount++;
    }

    /**
     * Client-side update for normal laser rendering.
     * Always calls through to super so that {@code irradiatePos=null} correctly
     * clears the rendering pipeline (e.g. when redstone is removed).
     */
    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = false;
        this.gammaLevel = 0;
        super.clientUpdate(irradiateBlockPos, laserLevel);
    }

    /**
     * Client-side update for gamma laser rendering.
     */
    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = true;
        this.gammaLevel = laserLevel;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
    }

    /**
     * Emit a gamma laser beam with special properties:
     * - Blue-purple color (rendered client-side via gamma flag in packet)
     * - Max 16 block range
     * - Does NOT pass through glass or laser-transparent blocks; stops at the first non-air block
     * - Destroys prisms on contact
     * - Block breaking based on level
     * - 16x entity damage
     * - Heats ember metal blocks in a cross-sectional area to overheated state
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void emitGammaLaserBeam(Direction direction) {
        if (this.level == null) return;
        int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;

        // Gamma laser: only pass through air/replaceable blocks.
        // All other blocks (including glass) stop the gamma laser.
        BlockPos tempIrradiateBlockPos = this.getGammaIrradiateBlockPos(16, direction, this.getBlockPos());
        if (this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
            tempIrradiateBlockPos = this.getGammaIrradiateBlockPos(
                16, direction, this.getBlockPos().relative(direction));
        }

        // Handle prism destruction along the beam path
        this.destroyPrismsAlongPath(direction, tempIrradiateBlockPos);

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
        this.updateLaserLevel(this.gammaLevel);

        if (!(this.level instanceof ServerLevel)) {
            this.maxTransmissionDistance = originalMaxDistance;
            return;
        }

        // Entity damage: 16x normal laser damage
        int hurt = Math.min(16, this.gammaLevel - 4) * 16;
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
                livingEntity.hurtOrSimulate(ModDamageTypes.gammaLaser(this.level), hurt)
            );
        }

        // Gamma laser block breaking — continuous exposure per block position
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int requiredExposure = GAMMA_EXPOSURE_TICKS[Math.clamp(this.gammaLevel / 4, 0, 4)];

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
                if (this.gammaLevel >= 16) {
                    // ≥16: destroy without drops (entire multipart structure)
                    this.level.destroyBlock(breakPos, false);
                } else {
                    // ≥4-15: break with drops at the block position
                    this.level.destroyBlock(this.irradiateBlockPos, true);
                }
            }
        } else {
            this.gammaExposureTicks = 0;
        }

        // Gamma laser heating: upgrade wither-immune ember metal blocks in area.
        // Area size and thickness scale with gamma level:
        // ≥4: 1×1×1  ≥8: 3×3×1  ≥12: 5×5×2  ≥16: 7×7×3
        this.tryHeatEmberMetal(direction);

        this.maxTransmissionDistance = originalMaxDistance;
    }

    /**
     * Heat all ember metal blocks in a cross-sectional area normal to the beam.
     * Area scales with gamma level: ≥4→1×1, ≥8→3×3, ≥12→5×5×2, ≥16→7×7×3.
     */
    private void tryHeatEmberMetal(Direction direction) {
        if (this.level == null || this.gammaLevel < 4) return;
        if (this.level.getGameTime() % 20 != 0) return;

        int areaSize;
        int thickness;
        if (this.gammaLevel >= 16) {
            areaSize = 7;
            thickness = 3;
        } else if (this.gammaLevel >= 12) {
            areaSize = 5;
            thickness = 2;
        } else if (this.gammaLevel >= 8) {
            areaSize = 3;
            thickness = 1;
        } else {
            areaSize = 1;
            thickness = 1;
        }

        int halfSize = areaSize / 2;
        BlockPos hitPos = this.irradiateBlockPos;
        if (hitPos == null) return;

        Direction[] perpendiculars = switch (direction.getAxis()) {
            case X -> new Direction[]{Direction.UP, Direction.NORTH};
            case Z -> new Direction[]{Direction.UP, Direction.EAST};
            default -> new Direction[]{Direction.NORTH, Direction.EAST};
        };

        for (int depth = 0; depth < thickness; depth++) {
            BlockPos depthPos = hitPos.relative(direction, depth);
            for (int a = -halfSize; a <= halfSize; a++) {
                for (int b = -halfSize; b <= halfSize; b++) {
                    BlockPos target = depthPos
                        .relative(perpendiculars[0], a)
                        .relative(perpendiculars[1], b);
                    this.tryHeatEmberMetalAt(target);
                }
            }
        }
    }

    /**
     * Upgrade or refresh a single ember metal block at the given position.
     */
    private void tryHeatEmberMetalAt(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);

        if (state.is(ModBlocks.EMBER_METAL_BLOCK.get())) {
            Block overheatedBlock = ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get();
            this.level.setBlock(pos, overheatedBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (overheatedBlock instanceof EntityBlock entityBlock) {
                BlockEntity be = entityBlock.newBlockEntity(pos, overheatedBlock.defaultBlockState());
                if (be instanceof HeatableBlockEntity heatable) {
                    this.level.setBlockEntity(heatable);
                    heatable.addDurationInTick(80);
                }
            }
        } else if (state.is(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())) {
            BlockEntity be = this.level.getBlockEntity(pos);
            if (be instanceof HeatableBlockEntity heatable) {
                heatable.addDurationInTick(80);
            }
        }
    }

    /**
     * Destroy prisms along the gamma laser path.
     */
    private void destroyPrismsAlongPath(Direction direction, BlockPos targetPos) {
        if (level == null) return;
        BlockPos.MutableBlockPos checkPos = getBlockPos().relative(direction).mutable();
        while (!checkPos.equals(targetPos)) {
            BlockState checkState = level.getBlockState(checkPos);
            if (checkState.getBlock() instanceof RubyPrismBlock) {
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
            if (!this.gammaCanPassThrough(checkPos)) return checkPos;
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

    // === Persistence (26.1: ValueOutput / ValueInput for disk) ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("receivedLaserLevel", this.receivedLaserLevel);
        output.putBoolean("receivedGamma", this.receivedGamma);
        output.putInt("requiredLaserLevel", this.requiredLaserLevel);
        output.putBoolean("requiredGamma", this.requiredGamma);
        output.putBoolean("laserValid", this.laserValid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.receivedLaserLevel = input.getIntOr("receivedLaserLevel", 0);
        this.receivedGamma = input.getBooleanOr("receivedGamma", false);
        this.requiredLaserLevel = input.getIntOr("requiredLaserLevel", 0);
        this.requiredGamma = input.getBooleanOr("requiredGamma", false);
        this.laserValid = input.getBooleanOr("laserValid", false);
        this.emittingGamma = input.getBooleanOr("gamma", false);
        this.gammaLevel = input.getIntOr("gammaLevel", 0);
    }

    // === Network sync (26.1: getUpdateTag → client receives via loadAdditional(ValueInput)) ===

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("receivedLaserLevel", this.receivedLaserLevel);
        tag.putBoolean("receivedGamma", this.receivedGamma);
        tag.putInt("requiredLaserLevel", this.requiredLaserLevel);
        tag.putBoolean("requiredGamma", this.requiredGamma);
        tag.putBoolean("laserValid", this.laserValid);
        tag.putBoolean("gamma", this.emittingGamma);
        tag.putInt("gammaLevel", this.gammaLevel);
        return tag;
    }

    // === Network sync helpers ===

    /**
     * Sync block entity data to all tracking clients.
     */
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = this.getUpdatePacket();
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
            this.syncToClients();
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
            CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && level.isClientSide()) {
            CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
        }
    }
}
