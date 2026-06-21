package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CelestialForgingAnvilPortalBlockEntity extends BaseLaserBlockEntity {

    /**
     * Set of entity UUIDs currently touching this portal.
     * Used to track enter/leave so that teleport fires once per touch,
     * and resets when the entity steps away.
     */
    private final Set<UUID> touchingEntities = new HashSet<>();

    /**
     * Last known WATERLOGGED state, used to detect changes and sync to connected portal.
     */
    private boolean lastWaterlogged = false;

    /**
     * Laser level and type received from an external laser source hitting this portal front face.
     */
    private int incomingLaserLevel = 0;
    private boolean incomingLaserGamma = false;

    /**
     * Laser level and type to emit, set by the connected portal via wormhole sync.
     */
    private int wormholeLaserLevel = 0;
    private boolean wormholeLaserGamma = false;

    /**
     * Gamma rendering state for client-side laser beam color.
     */
    @Getter
    private boolean emittingGamma = false;
    private int gammaLevel = 0;

    /**
     * Gamma laser block breaking: track which block is being irradiated and for how long.
     */
    @Nullable
    private BlockPos gammaIrradiatingPos = null;
    private int gammaExposureTicks = 0;

    // [disabled, ≥4:3s, ≥8:1s, ≥12:5gt, ≥16:1gt]
    private static final int[] GAMMA_EXPOSURE_TICKS = {
        Integer.MAX_VALUE, 60, 20, 5, 1
    };

    @Override
    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.emittingGamma)
        );
    }

    public CelestialForgingAnvilPortalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // === BaseLaserBlockEntity overrides ===

    @Override
    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(CelestialForgingAnvilPortalBlock.FACING)) {
            return state.getValue(CelestialForgingAnvilPortalBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public Set<Direction> getIgnoreFace() {
        // Only accept lasers from the front (the direction opposite to FACING)
        EnumSet<Direction> ignore = EnumSet.allOf(Direction.class);
        ignore.remove(getFacing().getOpposite());
        return ignore;
    }

    @Override
    protected int getBaseLaserLevel() {
        if (wormholeLaserLevel > 0) return wormholeLaserLevel;
        return 0;
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        this.incomingLaserLevel = source.getLaserLevel();
        this.incomingLaserGamma = source instanceof CelestialForgingAnvilLaserInterfaceBlockEntity cfaSource
            && cfaSource.isEmittingGamma();
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        this.incomingLaserLevel = 0;
        this.incomingLaserGamma = false;
    }

    /**
     * Set the wormhole laser output from the connected portal.
     * Called by the connected portal's tick via wormhole sync.
     */
    public void setWormholeLaser(int level, boolean gamma) {
        this.wormholeLaserLevel = level;
        this.wormholeLaserGamma = gamma;
        this.setChanged();
    }

    /**
     * Client-side update for gamma laser rendering on the portal.
     */
    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = true;
        this.gammaLevel = laserLevel;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CacheableBERenderingPipeline.getInstance().update(this);
    }

    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = false;
        this.gammaLevel = 0;
        super.clientUpdate(irradiateBlockPos, laserLevel);
    }

    /**
     * Find the parent CFA block entity that this portal is attached to.
     */
    @Nullable
    public CelestialForgingAnvilBlockEntity findParentCfa() {
        if (level == null) return null;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CelestialForgingAnvilPortalBlock)) return null;

        // FACING points away from CFA; look opposite to find CFA
        Direction towardsCfa = state.getValue(CelestialForgingAnvilPortalBlock.FACING).getOpposite();
        BlockPos cfaPos = worldPosition.relative(towardsCfa);
        BlockState cfaState = level.getBlockState(cfaPos);
        if (cfaState.getBlock() instanceof CelestialForgingAnvilBlock) {
            Cube323PartHalf half = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
            BlockPos controllerPos = cfaPos.offset(half.getOffset().multiply(-1));
            if (level.getBlockEntity(controllerPos) instanceof CelestialForgingAnvilBlockEntity cfaBe) {
                return cfaBe;
            }
        }
        return null;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        CelestialForgingAnvilBlockEntity parent = findParentCfa();
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CelestialForgingAnvilPortalBlock)) return;

        // If the parent CFA is gone (destroyed by water, etc.), destroy this portal too
        if (parent == null) {
            level.destroyBlock(worldPosition, false);
            return;
        }

        // If the amplifier is missing, the portal should be closed
        if (!parent.isAmplifierPresent()) {
            if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
                level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, false), 3);
            }
            touchingEntities.clear();
            return;
        }

        Cube323PartHalf side = findSideFromParent(parent);
        if (side == null) return;

        // Query wormhole network to determine if portal should be open.
        // Portal opens only when exactly 2 CFAs in the network group have a portal
        // on this same side. If there are more (>2), all doors close for security.
        WormholeNetwork network = WormholeNetwork.get();
        UUID hash = parent.getWormholeParamsHash();
        if (hash == null) {
            // No wormhole identity — close portal and clean up laser
            if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
                level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, false), 3);
                touchingEntities.clear();
            }
            if (wormholeLaserLevel > 0) {
                if (irradiateBlockPos != null) {
                    BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                    if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                        lastIrradiated.onCancelingIrradiation(this);
                    }
                    updateIrradiateBlockPos(null);
                }
                clearIrradiateSelfLaserBlockSet();
                updateLaserLevel(0);
                wormholeLaserLevel = 0;
                wormholeLaserGamma = false;
                this.emittingGamma = false;
                this.gammaLevel = 0;
                this.gammaIrradiatingPos = null;
                this.gammaExposureTicks = 0;
                this.setChanged();
                sendLaserPackets();
            }
            return;
        }
        List<WormholeNetwork.Entry> connected = network.getConnected(
            hash, level.dimension(), parent.getBlockPos()
        );

        // Ensure this portal's side is registered in the wormhole network.
        // After a megastructure clear + rebuild, portal sides may be missing
        // from the network because onClear() cleared the local portal map.
        if (!network.hasPortalAt(level.dimension(), parent.getBlockPos(), side)) {
            parent.addPortal(side, worldPosition);
        }

        // Count other CFAs in the group that have a portal on the same side
        long sameSideCount = connected.stream()
            .filter(e -> network.hasPortalAt(e.dimension(), e.pos(), side))
            .count();
        // Include self (this portal)
        sameSideCount++;
        boolean shouldBeOpen = sameSideCount == 2;

        if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN) != shouldBeOpen) {
            level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, shouldBeOpen), 3);
            state = state.setValue(CelestialForgingAnvilPortalBlock.OPEN, shouldBeOpen);
        }

        // Sync waterlogged state with connected portal (only when this portal's state changed)
        if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            boolean thisWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
            if (thisWaterlogged != lastWaterlogged) {
                lastWaterlogged = thisWaterlogged;
                CelestialForgingAnvilPortalBlockEntity connectedPortal = findConnectedPortal(parent, side);
                if (connectedPortal != null) {
                    BlockPos targetPortalPos = connectedPortal.getBlockPos();
                    Level targetLevel = connectedPortal.getLevel();
                    if (targetLevel != null) {
                        BlockState targetState = targetLevel.getBlockState(targetPortalPos);
                        if (targetState.getBlock() instanceof CelestialForgingAnvilPortalBlock
                            && targetState.getValue(BlockStateProperties.WATERLOGGED) != thisWaterlogged) {
                            targetLevel.setBlock(targetPortalPos,
                                targetState.setValue(BlockStateProperties.WATERLOGGED, thisWaterlogged), 3);
                            if (thisWaterlogged) {
                                targetLevel.scheduleTick(targetPortalPos, Fluids.WATER,
                                    Fluids.WATER.getTickDelay(targetLevel));
                            }
                        }
                    }
                }
            }

            // Sync laser: transmit incoming laser to connected portal
            CelestialForgingAnvilPortalBlockEntity connectedPortal = findConnectedPortal(parent, side);
            if (connectedPortal != null) {
                connectedPortal.setWormholeLaser(incomingLaserLevel, incomingLaserGamma);
            }
        } else {
            // Portal closed: clear any stale wormhole laser on the other side
            CelestialForgingAnvilPortalBlockEntity connectedPortal = findConnectedPortal(parent, side);
            if (connectedPortal != null && incomingLaserLevel == 0) {
                connectedPortal.setWormholeLaser(0, false);
            }
        }

        // Emit laser if this portal has a wormhole laser set from connected portal
        if (wormholeLaserLevel > 0) {
            Direction facing = getFacing();
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                if (wormholeLaserGamma) {
                    emitPortalGammaLaser(facing);
                } else {
                    emitLaser(facing);
                }
            }
            this.emittingGamma = wormholeLaserGamma;
            this.gammaLevel = wormholeLaserLevel;
        } else {
            // Stop laser emission
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            clearIrradiateSelfLaserBlockSet();
            updateLaserLevel(0);
            this.emittingGamma = false;
            this.gammaLevel = 0;
            this.gammaIrradiatingPos = null;
            this.gammaExposureTicks = 0;
        }

        // Increment tickCount for ore extraction cooldown
        this.tickCount++;

        // Send laser render packets
        sendLaserPackets();

        // Register as heat producer if the laser is hitting a heatable block
        if (level instanceof ServerLevel serverLevel
            && irradiateBlockPos != null
            && serverLevel.getBlockState(irradiateBlockPos).is(ModBlockTags.HEATABLE_BLOCKS)) {
            HeaterManager.addProducer(getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
        }

        // Detect entities in the portal block and teleport them.
        if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            AABB detectionBox = new AABB(worldPosition);
            List<Entity> entities = level.getEntitiesOfClass(Entity.class, detectionBox);

            for (Entity entity : entities) {
                UUID uuid = entity.getUUID();
                if (!touchingEntities.contains(uuid)) {
                    tryTouchTeleport(entity);
                    touchingEntities.add(uuid);
                }
            }

            Set<UUID> currentUuids = entities.stream()
                .map(Entity::getUUID)
                .collect(Collectors.toSet());
            touchingEntities.removeIf(uuid -> !currentUuids.contains(uuid));
        } else {
            touchingEntities.clear();
        }
    }

    /**
     * when an entity overlaps the portal block.
     * Checks {@link #touchingEntities} to ensure teleport fires only once
     * per touch — subsequent ticks while still inside the block are skipped.
     * The BE tick clears the tracking when the entity leaves the block.
     */
    public void tryTouchTeleport(Entity entity) {
        UUID uuid = entity.getUUID();
        if (touchingEntities.contains(uuid)) return;

        CelestialForgingAnvilBlockEntity parent = findParentCfa();
        if (parent == null) return;
        Cube323PartHalf side = findSideFromParent(parent);
        if (side == null) return;

        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(
            parent.getWormholeParamsHash(), level.dimension(), parent.getBlockPos()
        );
        // Only teleport if exactly one other CFA has a portal on the same side
        List<WormholeNetwork.Entry> matching = connected.stream()
            .filter(e -> network.hasPortalAt(e.dimension(), e.pos(), side))
            .toList();
        if (matching.size() != 1) return;

        WormholeNetwork.Entry target = matching.getFirst();
        if (!network.hasPortalAt(target.dimension(), target.pos(), side)) return;

        ServerLevel targetLevel = level.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;

        BlockPos targetPortalPos = target.pos().offset(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        Direction outwardFacing = CelestialForgingAnvilPortalBlock.getFacingFromSide(side);
        BlockPos destPos = targetPortalPos.relative(outwardFacing, 2);

        double dx = destPos.getX() + 0.5;
        double dy = destPos.getY();
        double dz = destPos.getZ() + 0.5;

        // Items and projectiles spawn 1 block higher so they don't land at feet level
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity
            || entity instanceof net.minecraft.world.entity.projectile.Projectile) {
            dy += 1;
        }

        // Only reverse the component perpendicular to the portal face;
        // horizontal and vertical movement parallel to the slab is preserved.
        net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
        net.minecraft.world.phys.Vec3 momentum;
        if (outwardFacing.getAxis() == Direction.Axis.Z) {
            momentum = new net.minecraft.world.phys.Vec3(vel.x, vel.y, -vel.z);
        } else {
            momentum = new net.minecraft.world.phys.Vec3(-vel.x, vel.y, vel.z);
        }
        float targetYRot = (entity.getYRot() + 180.0f) % 360.0f;
        entity.teleportTo(targetLevel, dx, dy, dz,
            Set.of(), targetYRot, entity.getXRot());
        entity.setDeltaMovement(momentum);

        touchingEntities.add(uuid);
    }

    /**
     * Find the connected portal block entity on the other side of the wormhole.
     * Returns null if not connected or target portal is not found.
     */
    @Nullable
    private CelestialForgingAnvilPortalBlockEntity findConnectedPortal(
        CelestialForgingAnvilBlockEntity parent, Cube323PartHalf side
    ) {
        WormholeNetwork network = WormholeNetwork.get();
        UUID hash = parent.getWormholeParamsHash();
        if (hash == null) return null;
        List<WormholeNetwork.Entry> connected = network.getConnected(
            hash, level.dimension(), parent.getBlockPos()
        );
        List<WormholeNetwork.Entry> matching = connected.stream()
            .filter(e -> network.hasPortalAt(e.dimension(), e.pos(), side))
            .toList();
        if (matching.size() != 1) return null;

        WormholeNetwork.Entry target = matching.getFirst();
        Direction outwardFacing = CelestialForgingAnvilPortalBlock.getFacingFromSide(side);
        BlockPos targetPortalPos = target.pos()
            .offset(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ())
            .relative(outwardFacing);
        ServerLevel targetLevel = level.getServer().getLevel(target.dimension());
        if (targetLevel == null) return null;
        if (targetLevel.getBlockEntity(targetPortalPos) instanceof CelestialForgingAnvilPortalBlockEntity targetPortal) {
            return targetPortal;
        }
        return null;
    }

    /**
     * Gamma laser emission with special properties: 16 block range, air-only pass-through,
     * prism destruction, 16x entity damage, and block breaking based on gamma level.
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void emitPortalGammaLaser(Direction direction) {
        if (this.level == null) return;
        int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;

        BlockPos tempIrradiateBlockPos = getGammaIrradiateBlockPos(direction);

        // Destroy prisms along the beam path
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

        // Chain with other BaseLaserBlockEntity targets
        if (this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiated
            && !this.isInIrradiateSelfLaserBlockSet(irradiated)) {
            if (irradiated.getIgnoreFace().isEmpty()) {
                this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                irradiated.onIrradiated(this);
            } else {
                for (Direction dir : irradiated.getIgnoreFace()) {
                    if (direction != dir) {
                        this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                        irradiated.onIrradiated(this);
                    }
                }
            }
        }
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);
        this.updateLaserLevel(gammaLevel);

        if (!(this.level instanceof ServerLevel serverLevel)) {
            this.maxTransmissionDistance = originalMaxDistance;
            return;
        }

        // Gamma entity damage: 16x normal laser damage
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
                net.minecraft.world.level.entity.EntityTypeTest.forClass(
                    net.minecraft.world.entity.LivingEntity.class),
                trackBoundingBox,
                net.minecraft.world.entity.Entity::isAlive
            ).forEach(livingEntity ->
                livingEntity.hurt(
                    dev.dubhe.anvilcraft.init.entity.ModDamageTypes.gammaLaser(this.level), hurt)
            );
        }

        // Gamma laser block breaking — continuous exposure per block position
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int requiredExposure = GAMMA_EXPOSURE_TICKS[Math.clamp(gammaLevel / 4, 0, 4)];

        BlockPos currentTarget = this.irradiateBlockPos.immutable();
        if (!currentTarget.equals(this.gammaIrradiatingPos)) {
            this.gammaIrradiatingPos = currentTarget;
            this.gammaExposureTicks = 0;
        }

        boolean canBreak = !irradiateBlock.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)
            && !irradiateBlock.isAir()
            && irradiateBlock.getDestroySpeed(this.level, this.irradiateBlockPos) >= 0;

        if (canBreak) {
            this.gammaExposureTicks++;
            if (this.gammaExposureTicks >= requiredExposure) {
                this.gammaExposureTicks = 0;
                BlockPos breakPos = this.irradiateBlockPos;
                if (irradiateBlock.getBlock() instanceof dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock<?, ?, ?> multi) {
                    breakPos = multi.getMainPartPos(this.irradiateBlockPos, irradiateBlock);
                }
                if (gammaLevel >= 16) {
                    this.level.destroyBlock(breakPos, false);
                } else {
                    this.level.destroyBlock(this.irradiateBlockPos, true);
                }
            }
        } else {
            this.gammaExposureTicks = 0;
        }

        // Gamma laser heating: upgrade ember metal blocks in cross-sectional area
        tryHeatEmberMetal(direction);

        this.maxTransmissionDistance = originalMaxDistance;
    }

    /**
     * Find the gamma laser irradiated block position using air-only pass-through rules.
     */
    private BlockPos getGammaIrradiateBlockPos(Direction direction) {
        for (int length = 1; length <= 16; length++) {
            BlockPos checkPos = this.getBlockPos().relative(direction, length);
            if (!gammaCanPassThrough(checkPos)) return checkPos;
        }
        return this.getBlockPos().relative(direction, 16);
    }

    /**
     * Gamma laser can only pass through air and replaceable blocks (tall grass, etc.).
     */
    private boolean gammaCanPassThrough(BlockPos blockPos) {
        if (this.level == null) return false;
        BlockState blockState = this.level.getBlockState(blockPos);
        return blockState.is(net.minecraft.tags.BlockTags.REPLACEABLE);
    }

    /**
     * Destroy ruby prisms along the gamma laser path.
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
     * Heat ember metal blocks in a cross-sectional area normal to the beam.
     * Area scales with gamma level: ≥4→1×1, ≥8→3×3, ≥12→5×5×2, ≥16→7×7×3.
     */
    private void tryHeatEmberMetal(Direction direction) {
        if (this.level == null || gammaLevel < 4) return;
        if (this.level.getGameTime() % 20 != 0) return;

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
                    tryHeatEmberMetalAt(target);
                }
            }
        }
    }

    /**
     * Upgrade or refresh a single ember metal block at the given position.
     */
    private void tryHeatEmberMetalAt(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        if (state.is(dev.dubhe.anvilcraft.init.block.ModBlocks.EMBER_METAL_BLOCK.get())) {
            net.minecraft.world.level.block.Block overheatedBlock =
                dev.dubhe.anvilcraft.init.block.ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get();
            this.level.setBlock(pos, overheatedBlock.defaultBlockState(), 3);
            if (overheatedBlock instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                BlockEntity be = entityBlock.newBlockEntity(pos, overheatedBlock.defaultBlockState());
                if (be instanceof dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity heatable) {
                    this.level.setBlockEntity(heatable);
                    heatable.addDurationInTick(80);
                }
            }
        } else if (state.is(dev.dubhe.anvilcraft.init.block.ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())) {
            BlockEntity be = this.level.getBlockEntity(pos);
            if (be instanceof dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity heatable) {
                heatable.addDurationInTick(80);
            }
        }
    }

    @Nullable
    private Cube323PartHalf findSideFromParent(CelestialForgingAnvilBlockEntity parent) {
        for (var entry : parent.getPortals().entrySet()) {
            if (entry.getValue().equals(worldPosition)) {
                return entry.getKey();
            }
        }
        // Fallback: compute from position.
        // The portal is placed adjacent to the CFA side center, so its offset
        // from the controller is twice the side center offset (±2 instead of ±1).
        BlockPos cfaPos = parent.getBlockPos();
        int dx = worldPosition.getX() - cfaPos.getX();
        int dz = worldPosition.getZ() - cfaPos.getZ();
        if (dx == -2 && dz == 0) return Cube323PartHalf.BOTTOM_W;
        if (dx == 2 && dz == 0) return Cube323PartHalf.BOTTOM_E;
        if (dx == 0 && dz == -2) return Cube323PartHalf.BOTTOM_N;
        if (dx == 0 && dz == 2) return Cube323PartHalf.BOTTOM_S;
        return null;
    }

    /**
     * Send laser render packets to clients tracking this chunk.
     */
    private void sendLaserPackets() {
        if (level instanceof ServerLevel serverLevel && changed) {
            PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                level.getChunkAt(getBlockPos()).getPos(),
                new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.emittingGamma)
            );
            resetState();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    /**
     * Drop mined items at the broken ore position rather than behind the portal,
     * so drops don't get trapped inside the CFA structure.
     */
    @Override
    public void deliverItem(List<ItemStack> drops, Direction direction, BlockPos sourceBlockPos) {
        if (this.level == null) return;
        for (ItemStack itemStack : drops) {
            this.level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                this.level,
                sourceBlockPos.getX() + 0.5,
                sourceBlockPos.getY() + 1.5,
                sourceBlockPos.getZ() + 0.5,
                itemStack
            ));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("gamma", emittingGamma);
        tag.putInt("gammaLevel", gammaLevel);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        this.emittingGamma = tag.getBoolean("gamma");
        this.gammaLevel = tag.getInt("gammaLevel");
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
