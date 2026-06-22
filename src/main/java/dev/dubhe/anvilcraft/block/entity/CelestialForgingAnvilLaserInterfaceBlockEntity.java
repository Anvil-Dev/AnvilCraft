package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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

    @Getter
    private boolean emittingGamma = false;
    @Getter
    private int gammaLevel = 0;

    private int wormholeOutputLevel = 0;
    private boolean wormholeOutputGamma = false;

    private static final int[] GAMMA_EXPOSURE_TICKS = {
        Integer.MAX_VALUE,
        60, 20, 5, 1
    };

    @Nullable
    private BlockPos gammaIrradiatingPos = null;
    private int gammaExposureTicks = 0;

    public CelestialForgingAnvilLaserInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

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
            if (wormholeOutputLevel > 0) {
                return wormholeOutputLevel;
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

    public boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
            && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    public void setWormholeLaserOutput(int level, boolean gamma) {
        this.wormholeOutputLevel = level;
        this.wormholeOutputGamma = gamma;
    }

    @Override
    public float getLaserOffset() {
        return 0.125f;
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        int level = source.getLaserLevel();
        boolean gamma = source instanceof CelestialForgingAnvilLaserInterfaceBlockEntity cfaSource
            && cfaSource.isEmittingGamma();
        onLaserReceived(level, gamma);
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        resetLaser();
    }

    @Override
    public Set<Direction> getIgnoreFace() {
        EnumSet<Direction> ignore = EnumSet.allOf(Direction.class);
        ignore.remove(getFacing().getOpposite());
        return ignore;
    }

    public void setLaserRequirement(int requiredLevel, boolean gamma) {
        this.requiredLaserLevel = requiredLevel;
        this.requiredGamma = gamma;
        if (requiredLaserLevel > 0 && receivedLaserLevel > 0) {
            this.laserValid = receivedLaserLevel >= requiredLaserLevel
                && receivedGamma == requiredGamma;
        } else {
            this.laserValid = false;
        }
        this.setChanged();
    }

    public void onLaserReceived(int level, boolean gamma) {
        this.receivedLaserLevel = level;
        this.receivedGamma = gamma;
        this.laserValid = (requiredLaserLevel > 0
            && level >= requiredLaserLevel
            && gamma == requiredGamma);
        this.setChanged();
    }

    public void resetLaser() {
        this.receivedLaserLevel = 0;
        this.receivedGamma = false;
        this.laserValid = false;
        this.setChanged();
    }

    public void emitGammaLaser(int level) {
        this.emittingGamma = true;
        this.gammaLevel = level;
        this.updateLaserLevel(level);
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        boolean active = state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);

        if (receivedLaserLevel > 0) {
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            irradiateSelfLaserBlockSet.clear();
            updateLaserLevel(0);
        } else if (emittingGamma && gammaLevel > 0) {
            Direction facing = getFacing();
            emitGammaLaserBeam(facing);
        } else if (wormholeOutputGamma && wormholeOutputLevel > 0 && active) {
            int savedGammaLevel = this.gammaLevel;
            this.gammaLevel = wormholeOutputLevel;
            this.emittingGamma = true;
            Direction facing = getFacing();
            emitGammaLaserBeam(facing);
            this.gammaLevel = savedGammaLevel;
        } else if (active) {
            Direction facing = getFacing();
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                emitLaser(facing);
            }
        } else {
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            irradiateSelfLaserBlockSet.clear();
            updateLaserLevel(0);
        }

        tickWithGamma(level);

        if (emittingGamma) {
            emittingGamma = false;
        }

        if (level instanceof ServerLevel serverLevel
            && irradiateBlockPos != null
            && serverLevel.getBlockState(irradiateBlockPos).is(ModBlockTags.HEATABLE_BLOCKS)) {
            HeaterManager.addProducer(getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
        }
    }

    @Override
    public void tick(Level level) {
        if (level.isClientSide()) {
            super.tick(level);
        }
    }

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

    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = false;
        this.gammaLevel = 0;
        super.clientUpdate(irradiateBlockPos, laserLevel);
    }

    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = true;
        this.gammaLevel = laserLevel;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void emitGammaLaserBeam(Direction direction) {
        if (this.level == null) return;
        int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;

        BlockPos tempIrradiateBlockPos = getGammaIrradiateBlockPos(16, direction, this.getBlockPos());
        if (this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
            tempIrradiateBlockPos = getGammaIrradiateBlockPos(
                16, direction, this.getBlockPos().relative(direction));
        }

        destroyPrismsAlongPath(direction, tempIrradiateBlockPos);

        if (!tempIrradiateBlockPos.equals(this.irradiateBlockPos)) {
            if (this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
            }
        }

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
            //noinspection deprecation
            this.level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                trackBoundingBox,
                Entity::isAlive
            ).forEach(livingEntity ->
                livingEntity.hurtOrSimulate(
                    this.level.damageSources().magic(),
                    hurt
                )
            );
        }

        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int requiredExposure = GAMMA_EXPOSURE_TICKS[Math.clamp(gammaLevel / 4, 0, 4)];

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
                BlockPos breakPos = this.irradiateBlockPos;
                if (irradiateBlock.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?> multiPartBlock) {
                    breakPos = multiPartBlock.getMainPartPos(this.irradiateBlockPos, irradiateBlock);
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

        tryHeatEmberMetal(direction);

        this.maxTransmissionDistance = originalMaxDistance;
    }

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

    private void destroyPrismsAlongPath(Direction direction, BlockPos targetPos) {
        if (level == null) return;
        BlockPos.MutableBlockPos checkPos = getBlockPos().relative(direction).mutable();
        while (!checkPos.equals(targetPos)) {
            BlockState checkState = level.getBlockState(checkPos);
            if (checkState.getBlock() instanceof dev.dubhe.anvilcraft.block.laser.RubyPrismBlock) {
                level.destroyBlock(checkPos.immutable(), true);
            }
            checkPos.move(direction);
        }
    }

    private BlockPos getGammaIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            BlockPos checkPos = originPos.relative(direction, length);
            if (!gammaCanPassThrough(checkPos)) return checkPos;
        }
        return originPos.relative(direction, expectedLength);
    }

    private boolean gammaCanPassThrough(BlockPos blockPos) {
        if (this.level == null) return false;
        BlockState blockState = this.level.getBlockState(blockPos);
        return blockState.is(BlockTags.REPLACEABLE);
    }

    // === Persistence ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("receivedLaserLevel", receivedLaserLevel);
        output.putBoolean("receivedGamma", receivedGamma);
        output.putInt("requiredLaserLevel", requiredLaserLevel);
        output.putBoolean("requiredGamma", requiredGamma);
        output.putBoolean("laserValid", laserValid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.receivedLaserLevel = input.getIntOr("receivedLaserLevel", 0);
        this.receivedGamma = input.getBooleanOr("receivedGamma", false);
        this.requiredLaserLevel = input.getIntOr("requiredLaserLevel", 0);
        this.requiredGamma = input.getBooleanOr("requiredGamma", false);
        this.laserValid = input.getBooleanOr("laserValid", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("receivedLaserLevel", receivedLaserLevel);
        tag.putBoolean("receivedGamma", receivedGamma);
        tag.putInt("requiredLaserLevel", requiredLaserLevel);
        tag.putBoolean("requiredGamma", requiredGamma);
        tag.putBoolean("laserValid", laserValid);
        tag.putBoolean("gamma", emittingGamma);
        tag.putInt("gammaLevel", gammaLevel);
        return tag;
    }

    // === Network sync ===

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
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && this.level.isClientSide()) {
            CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
        }
    }
}
