package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.CreativeLaserBlock;
import dev.dubhe.anvilcraft.block.RubyPrismBlock;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.inventory.CreativeLaserMenu;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

import java.util.Arrays;
import javax.annotation.Nullable;

@Getter
public class CreativeLaserBlockEntity extends BaseLaserBlockEntity implements MenuProvider {
    /// 伽马激光方块破坏：每个等级所需的连续照射 tick 数。
    private static final int[] GAMMA_EXPOSURE_TICKS = {
        Integer.MAX_VALUE,
        60,
        20,
        5,
        1
    };

    private int configuredLevel = 16;
    private LensType lensType = LensType.NONE;
    private boolean gamma = false;

    @Nullable
    private BlockPos gammaIrradiatingPos = null;
    private int gammaExposureTicks = 0;

    public static CreativeLaserBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new CreativeLaserBlockEntity(type, pos, blockState);
    }

    public CreativeLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setConfiguredLevel(int level) {
        this.configuredLevel = Math.clamp(level, 0, 64);
        this.setChanged();
    }

    public void setLensType(LensType lensType) {
        this.lensType = lensType;
        this.setChanged();
    }

    public void setGamma(boolean gamma) {
        this.gamma = gamma;
        this.setChanged();
    }

    @Override
    protected int getBaseLaserLevel() {
        return this.configuredLevel;
    }

    @Override
    public BlockMiningEffect getMiningEffect() {
        return this.lensType.getMiningEffect();
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(CreativeLaserBlock.FACING);
    }

    @Override
    public float getLaserOffset() {
        return -0.5f;
    }

    @Override
    public void tick(Level level) {
        this.resetState();
        if (level.isClientSide()) {
            super.tick(level);
            return;
        }
        if (this.isRedstoneOff() || this.configuredLevel <= 0) {
            this.cancelLaserEmission();
        } else if (this.gamma) {
            this.emitGammaLaserBeam(getFacing());
        } else {
            this.emitLaser(getFacing());
        }
        if (this.changed && level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                level.getChunkAt(getBlockPos()).getPos(),
                new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.gamma)
            );
        }
        this.tickCount++;
        if (level instanceof ServerLevel serverLevel
            && this.irradiateBlockPos != null
            && serverLevel.getBlockState(this.irradiateBlockPos).is(ModBlockTags.HEATABLE_BLOCKS)
        ) {
            HeaterManager.addProducer(this.getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
        }
    }

    private boolean isRedstoneOff() {
        return this.level != null && this.level.hasNeighborSignal(this.getBlockPos());
    }

    private void cancelLaserEmission() {
        if (this.irradiateBlockPos != null
            && this.level != null
            && this.level.getBlockEntity(this.irradiateBlockPos) instanceof BaseLaserBlockEntity target
        ) {
            target.onCancelingIrradiation(this);
        }
        this.updateIrradiateBlockPos(null);
        this.clearIrradiateSelfLaserBlockSet();
        this.updateLaserLevel(0);
        this.gammaIrradiatingPos = null;
        this.gammaExposureTicks = 0;
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void emitGammaLaserBeam(Direction direction) {
        if (this.level == null) return;
        int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;
        BlockPos tempIrradiateBlockPos = this.getGammaIrradiateBlockPos(16, direction, this.getBlockPos());
        this.destroyPrismsAlongPath(direction, tempIrradiateBlockPos);
        if (!tempIrradiateBlockPos.equals(this.irradiateBlockPos)) {
            if (this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
            }
        }
        if (this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiated
            && !this.isInIrradiateSelfLaserBlockSet(irradiated)
        ) {
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
        this.updateLaserLevel(this.configuredLevel);
        if (!(this.level instanceof ServerLevel serverLevel)) {
            this.maxTransmissionDistance = originalMaxDistance;
            return;
        }
        int hurt = Math.min(16, this.configuredLevel - 4) * 16;
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
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int requiredExposure = GAMMA_EXPOSURE_TICKS[Math.clamp(this.configuredLevel / 4, 0, 4)];
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
                if (this.configuredLevel >= 16) {
                    this.level.destroyBlock(breakPos, false);
                } else {
                    this.level.destroyBlock(this.irradiateBlockPos, true);
                }
            }
        } else {
            this.gammaExposureTicks = 0;
        }
        this.tryHeatEmberMetal(direction);
        this.maxTransmissionDistance = originalMaxDistance;
    }

    /// ???????????????????????????????????
    /// ???????????4?1?1?1??8?3?3?1??12?5?5?2??16?7?7?3?
    private void tryHeatEmberMetal(Direction direction) {
        if (this.level == null || this.configuredLevel < 4) return;
        if (this.level.getGameTime() % 20 != 0) return;

        BlockPos hitPos = this.irradiateBlockPos;
        if (hitPos == null) return;
        BlockState hitState = this.level.getBlockState(hitPos);
        if (!hitState.is(ModBlocks.EMBER_METAL_BLOCK.get())
            && !hitState.is(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())) {
            return;
        }

        int areaSize;
        int thickness;
        if (this.configuredLevel >= 16) {
            areaSize = 7;
            thickness = 3;
        } else if (this.configuredLevel >= 12) {
            areaSize = 5;
            thickness = 2;
        } else if (this.configuredLevel >= 8) {
            areaSize = 3;
            thickness = 1;
        } else {
            areaSize = 1;
            thickness = 1;
        }

        int halfSize = areaSize / 2;

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

    private BlockPos getGammaIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            BlockPos checkPos = originPos.relative(direction, length);
            if (!this.gammaCanPassThrough(checkPos)) return checkPos;
        }
        return originPos.relative(direction, expectedLength);
    }

    private boolean gammaCanPassThrough(BlockPos blockPos) {
        if (this.level == null) return false;
        return this.level.getBlockState(blockPos).is(BlockTags.REPLACEABLE);
    }

    private void destroyPrismsAlongPath(Direction direction, BlockPos targetPos) {
        if (this.level == null) return;
        BlockPos.MutableBlockPos checkPos = this.getBlockPos().relative(direction).mutable();
        while (!checkPos.equals(targetPos)) {
            BlockState checkState = this.level.getBlockState(checkPos);
            if (checkState.getBlock() instanceof RubyPrismBlock) {
                this.level.destroyBlock(checkPos.immutable(), true);
            }
            checkPos.move(direction);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("laserLevel", this.configuredLevel);
        tag.putString("lensType", this.lensType.getSerializedName());
        tag.putBoolean("gamma", this.gamma);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.configuredLevel = tag.getInt("laserLevel");
        this.lensType = Arrays.stream(LensType.values())
            .filter(type -> type.getSerializedName().equals(tag.getString("lensType")))
            .findFirst()
            .orElse(LensType.NONE);
        this.gamma = tag.getBoolean("gamma");
        if (this.level != null && this.level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    @Override
    public Component getDisplayName() {
        return ModBlocks.CREATIVE_LASER.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CreativeLaserMenu(containerId, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }

    @Override
    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.gamma)
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("laserLevel", this.configuredLevel);
        tag.putString("lensType", this.lensType.getSerializedName());
        tag.putBoolean("gamma", this.gamma);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        this.configuredLevel = tag.getInt("laserLevel");
        this.lensType = Arrays.stream(LensType.values())
            .filter(type -> type.getSerializedName().equals(tag.getString("lensType")))
            .findFirst()
            .orElse(LensType.NONE);
        this.gamma = tag.getBoolean("gamma");
        if (this.level != null && this.level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && level instanceof ServerLevel serverLevel) {
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
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.gamma = false;
        super.clientUpdate(irradiateBlockPos, laserLevel);
    }

    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.gamma = true;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CacheableBERenderingPipeline.getInstance().update(this);
    }
}
