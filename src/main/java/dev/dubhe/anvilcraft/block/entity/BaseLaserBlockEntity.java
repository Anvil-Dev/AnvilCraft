package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.LensBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("checkstyle:JavadocParagraph")
public abstract class BaseLaserBlockEntity extends BlockEntity {
    public static final int[] COOLDOWNS = {
        Integer.MAX_VALUE,
        24 * 20,
        6 * 20,
        2 * 20,
        20
    };
    /// 伽马激光方块破坏：每个等级所需的连续照射 tick 数。
    protected static final int[] GAMMA_EXPOSURE_TICKS = {
        Integer.MAX_VALUE,
        60,
        20,
        5,
        1
    };
    protected int maxTransmissionDistance = 128;
    protected int tickCount = 0;

    protected HashSet<BaseLaserBlockEntity> irradiateSelfLaserBlockSet = new HashSet<>();
    protected boolean changed = false;
    @Getter
    protected @UnknownNullability BlockPos irradiateBlockPos = null;
    protected @Nullable BaseLaserBlockEntity irradiatedLaserTarget = null;
    protected int laserLinkRevision = 0;
    protected int irradiatedLaserTargetRevision = -1;
    private BlockMiningEffect lastEmittedMiningEffect = BlockMiningEffect.NORMAL;
    private boolean lastEmittedGamma = false;
    @Getter
    protected int laserLevel = 0;
    /// 跟踪正在被伽马激光照射的方块位置及持续时间。
    @Nullable
    protected BlockPos gammaIrradiatingPos = null;
    protected int gammaExposureTicks = 0;

    public BaseLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    protected boolean canPassThrough(Direction direction, BlockPos blockPos) {
        if (this.level == null) return false;
        BlockState blockState = level.getBlockState(blockPos);
        if (
            blockState.is(ModBlockTags.LASER_CAN_PASS_THROUGH)
            || blockState.is(Tags.Blocks.GLASS_BLOCKS)
            || blockState.is(Tags.Blocks.GLASS_PANES)
            || blockState.is(BlockTags.REPLACEABLE)
        ) {
            return true;
        }
        // Empty lens aligned with the laser axis is transparent
        if (blockState.getBlock() instanceof LensBlock
            && blockState.getValue(LensBlock.TYPE) == LensType.NONE
            && direction.getAxis() == blockState.getValue(LensBlock.AXIS)) {
            return true;
        }
        if (!AnvilCraft.CONFIG.isLaserDoImpactChecking) return false;
        AABB laseBoundingBox = switch (direction.getAxis()) {
            case X -> Block.box(0, 7, 7, 16, 9, 9).bounds();
            case Y -> Block.box(7, 0, 7, 9, 16, 9).bounds();
            case Z -> Block.box(7, 7, 0, 9, 9, 16).bounds();
        };
        return blockState.getCollisionShape(this.level, blockPos).toAabbs().stream().noneMatch(laseBoundingBox::intersects);
    }

    public void updateIrradiateBlockPos(@Nullable BlockPos newPos) {
        if (newPos == null) {
            this.irradiatedLaserTarget = null;
            this.irradiatedLaserTargetRevision = -1;
        }
        if (this.irradiateBlockPos == null) {
            if (newPos != null) this.markChanged();
            this.irradiateBlockPos = newPos;
            return;
        }
        if (!this.irradiateBlockPos.equals(newPos)) this.markChanged();
        this.irradiateBlockPos = newPos;
    }

    public void resetState() {
        this.changed = false;
    }

    public void markChanged() {
        this.changed = true;
    }

    private BlockPos getIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            if (!this.canPassThrough(direction, originPos.relative(direction, length))) return originPos.relative(direction, length);
        }
        return originPos.relative(direction, expectedLength);
    }

    public Set<Direction> getIgnoreFace() {
        return Set.of();
    }

    protected int getBaseLaserLevel() {
        return 1;
    }

    /**
     * Whether this emitter is currently producing a gamma beam.
     *
     * Keeping this on the common laser type lets receivers preserve the beam
     * type for every emitter (including creative lasers), instead of coupling
     * their validation to a list of concrete block entities.
     */
    public boolean isEmittingGamma() {
        return false;
    }

    protected int calculateLaserLevel() {
        return getBaseLaserLevel()
               + this.irradiateSelfLaserBlockSet.stream()
                   .mapToInt(BaseLaserBlockEntity::calculateLaserLevel)
                   .sum();
    }

    public BlockMiningEffect getMiningEffect() {
        BlockMiningEffect effect = null;
        for (BaseLaserBlockEntity source : irradiateSelfLaserBlockSet) {
            BlockMiningEffect sourceEffect = source.getMiningEffect();
            if (effect == null) {
                effect = sourceEffect;
            } else if (!effect.equals(sourceEffect)) {
                return BlockMiningEffect.NORMAL;
            }
        }
        return effect == null ? BlockMiningEffect.NORMAL : effect;
    }

    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, false)
        );
    }

    public void tick(Level level) {
        if (changed) {
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    level.getChunkAt(getBlockPos()).getPos(),
                    new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, false)
                );
            }
        }
        if (
            level instanceof ServerLevel serverLevel
            && getIrradiateBlockPos() != null
            && serverLevel.getBlockState(getIrradiateBlockPos()).is(ModBlockTags.HEATABLE_BLOCKS)
        ) {
            HeaterManager.addProducer(this.getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
        }
        this.tickCount++;
    }

    /**
     * 发射激光
     */
    public void emitLaser(Direction direction) {
        if (this.level == null) return;
        BlockPos tempIrradiateBlockPos = this.getIrradiateBlockPos(this.maxTransmissionDistance, direction, this.getBlockPos());
        if (this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
            tempIrradiateBlockPos = this.getIrradiateBlockPos(
                this.maxTransmissionDistance,
                direction,
                this.getBlockPos().relative(direction)
            );
        }
        BaseLaserBlockEntity newLaserTarget =
            this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity target ? target : null;
        boolean targetChanged = !tempIrradiateBlockPos.equals(this.irradiateBlockPos);
        boolean targetEntityChanged = newLaserTarget != this.irradiatedLaserTarget;
        boolean targetRevisionChanged = newLaserTarget != null
                                        && newLaserTarget.laserLinkRevision != this.irradiatedLaserTargetRevision;
        if (targetChanged || targetEntityChanged || targetRevisionChanged) {
            if (this.irradiatedLaserTarget != null) {
                this.irradiatedLaserTarget.onCancelingIrradiation(this);
            } else if (targetChanged && this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiatedLaserBlockEntity) {
                    lastIrradiatedLaserBlockEntity.onCancelingIrradiation(this);
                }
            }
            this.irradiatedLaserTarget = null;
            this.irradiatedLaserTargetRevision = -1;
        }
        int newLaserLevel = this.calculateLaserLevel();
        boolean laserLevelChanged = this.laserLevel != newLaserLevel;
        BlockMiningEffect miningEffect = getMiningEffect();
        boolean miningEffectChanged = !lastEmittedMiningEffect.equals(miningEffect);
        boolean gammaChanged = this.lastEmittedGamma;
        this.updateLaserLevel(newLaserLevel);
        if (
            newLaserTarget != null
            && !this.isInIrradiateSelfLaserBlockSet(newLaserTarget)
        ) {
            boolean needsIrradiationUpdate = targetChanged
                                              || targetEntityChanged
                                              || targetRevisionChanged
                                              || laserLevelChanged
                                              || miningEffectChanged
                                              || gammaChanged;
            if (needsIrradiationUpdate && !newLaserTarget.getIgnoreFace().contains(direction)) {
                this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                newLaserTarget.onIrradiated(this);
                this.irradiatedLaserTarget = newLaserTarget;
                this.irradiatedLaserTargetRevision = newLaserTarget.laserLinkRevision;
            }
        }
        this.lastEmittedMiningEffect = miningEffect;
        this.lastEmittedGamma = false;
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);

        if (!(this.level instanceof ServerLevel serverLevel)) return;
        int hurt = Math.min(16, this.laserLevel - 4);
        if (hurt > 0) {
            Vec3 startPos = this.getBlockPos()
                .relative(direction)
                .getCenter()
                .add(-0.0625, -0.0625, -0.0625);
            if (this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
                startPos = this.getBlockPos()
                    .relative(direction, 2)
                    .getCenter()
                    .add(-0.0625, -0.0625, -0.0625);
            }
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
                livingEntity.hurt(
                    ModDamageTypes.laser(this.level),
                    hurt
                )
            );
        }
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int cooldown = COOLDOWNS[Math.clamp(this.laserLevel / 4, 0, 4)];
        if (this.tickCount >= cooldown) {
            this.tickCount = 0;
            if (irradiateBlock.is(Tags.Blocks.ORES)) {
                List<ItemStack> drops = BreakBlockUtil.dropForLaser(
                    serverLevel,
                    this.irradiateBlockPos,
                    getMiningEffect()
                );
                this.deliverItem(drops, direction, this.irradiateBlockPos);
            }
        }
    }

    public void deliverItem(List<ItemStack> drops, Direction direction, BlockPos sourceBlockPos) {
        if (this.level == null) return;
        Vec3 dropPos = getBlockPos().relative(direction.getOpposite()).getCenter();
        BlockPos downStreamPos = getBlockPos().relative(getFacing().getOpposite());
        if (this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
            dropPos = getBlockPos().relative(direction.getOpposite(), 2).getCenter();
            downStreamPos = getBlockPos().relative(getFacing().getOpposite(), 2);
        }
        if (getLevel() == null) return;
        IItemHandler cap = getLevel()
            .getCapability(
                Capabilities.ItemHandler.BLOCK,
                downStreamPos,
                getFacing()
            );
        BlockState sourceBlock = this.level.getBlockState(sourceBlockPos);
        BlockPos finalDownStreamPos = downStreamPos;
        Vec3 finalDropPos = dropPos;
        drops.forEach(itemStack -> {
            if (cap != null) {
                ItemStack outItemStack = ItemHandlerHelper.insertItem(cap, itemStack, true);
                if (outItemStack.isEmpty()) {
                    ItemHandlerHelper.insertItem(cap, itemStack, false);
                } else {
                    this.level.addFreshEntity(new ItemEntity(
                        this.level,
                        finalDropPos.x,
                        finalDropPos.y,
                        finalDropPos.z,
                        outItemStack
                    ));
                }
            } else if (
                this.level.getBlockEntity(finalDownStreamPos) instanceof BaseLaserBlockEntity downStreamBlockEntity
                && downStreamBlockEntity.getFacing() == direction
            ) {
                downStreamBlockEntity.deliverItem(drops, direction, sourceBlockPos);
            } else {
                this.level.addFreshEntity(new ItemEntity(this.level, finalDropPos.x, finalDropPos.y, finalDropPos.z, itemStack));
            }
        });
        if (this.level.getBlockEntity(downStreamPos) instanceof BaseLaserBlockEntity) return;
        if (sourceBlock.is(Blocks.ANCIENT_DEBRIS)) {
            this.level.setBlockAndUpdate(sourceBlockPos, Blocks.NETHERRACK.defaultBlockState());
        } else if (sourceBlock.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)) {
            this.level.setBlockAndUpdate(sourceBlockPos, Blocks.DEEPSLATE.defaultBlockState());
        } else if (sourceBlock.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)) {
            this.level.setBlockAndUpdate(sourceBlockPos, Blocks.NETHERRACK.defaultBlockState());
        } else {
            this.level.setBlockAndUpdate(sourceBlockPos, Blocks.STONE.defaultBlockState());
        }
        /* else {
            if (this.level.getBlockState(sourceBlockPos).getBlock().defaultDestroyTime() >= 0
                && !(this.level.getBlockEntity(sourceBlockPos) instanceof BaseLaserBlockEntity)
            ) {
                this.level.getBlockState(sourceBlockPos).getBlock()
                    .playerWillDestroy(
                        this.level,
                        sourceBlockPos,
                        this.level.getBlockState(sourceBlockPos),
                        AnvilCraftFakePlayers.anvilcraftBlockPlacer.getPlayer()
                    );
                this.level.destroyBlock(sourceBlockPos, false);
            }
        }*/
    }

    /**
     * 检测光学原件是否在链接表中
     */
    public boolean isInIrradiateSelfLaserBlockSet(BaseLaserBlockEntity baseLaserBlockEntity) {
        return baseLaserBlockEntity == this
               || irradiateSelfLaserBlockSet.contains(baseLaserBlockEntity)
               || irradiateSelfLaserBlockSet.stream()
                   .anyMatch(baseLaserBlockEntity1 ->
                       baseLaserBlockEntity1.isInIrradiateSelfLaserBlockSet(baseLaserBlockEntity));
    }

    public void clearIrradiateSelfLaserBlockSet() {
        this.irradiateSelfLaserBlockSet.clear();
    }

    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
        if (this.irradiateSelfLaserBlockSet.add(baseLaserBlockEntity)) {
            this.markChanged();
        }
    }

    /**
     * 当方块被取消激光照射时调用
     */
    public void onCancelingIrradiation(BaseLaserBlockEntity baseLaserBlockEntity) {
        if (!this.irradiateSelfLaserBlockSet.remove(baseLaserBlockEntity)) return;
        this.markChanged();
        if (!this.irradiateSelfLaserBlockSet.isEmpty()) return;
        BlockPos tempIrradiateBlockPos = irradiateBlockPos;
        this.updateIrradiateBlockPos(null);
        if (this.level == null) return;
        if (tempIrradiateBlockPos == null) return;
        if (!(this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity)) return;
        irradiateBlockEntity.onCancelingIrradiation(this);
    }

    public void resetLaserStateAfterMove() {
        this.laserLinkRevision++;
        Set.copyOf(this.irradiateSelfLaserBlockSet)
            .forEach(source -> source.updateIrradiateBlockPos(null));
        this.irradiateSelfLaserBlockSet.clear();

        BlockPos oldTargetPos = this.irradiateBlockPos;
        this.updateIrradiateBlockPos(null);
        if (this.level != null
            && oldTargetPos != null
            && this.level.getBlockEntity(oldTargetPos) instanceof BaseLaserBlockEntity oldTarget) {
            oldTarget.onCancelingIrradiation(this);
        }

        this.updateLaserLevel(this.getBaseLaserLevel());
        this.markChanged();
    }

    public abstract Direction getFacing();

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level == null) return;
        if (this.level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().blockRemoved(this);
            return;
        }
        if (this.irradiateBlockPos == null) return;
        if (!this.level.isLoaded(this.irradiateBlockPos)) return;
        BlockEntity targetBe = this.level.getBlockEntity(this.irradiateBlockPos);
        if (targetBe instanceof BaseLaserBlockEntity irradiateBlockEntity) {
            irradiateBlockEntity.onCancelingIrradiation(this);
        }
    }

    public float getLaserOffset() {
        return 0;
    }

    /**
     * 为了适配forge中修改的渲染逻辑所添加的函数
     * 返回一个无限碰撞箱
     *
     * @return forge中为原版信标生成的无限碰撞箱
     */
    @SuppressWarnings("unused")
    public AABB getRenderBoundingBox() {
        return new AABB(
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY
        );
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && this.level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    public void updateLaserLevel(int value) {
        if (this.laserLevel != value) {
            markChanged();
        }
        this.laserLevel = value;
    }

    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CacheableBERenderingPipeline.getInstance().update(this);
    }

    protected int getGammaLaserLevel() {
        return this.getBaseLaserLevel();
    }

    /// 激光束起点是否从方块正面外一格开始。柔性多方块默认偏移；发光面在本格正面的方块可覆写为 false。
    protected boolean isLaserOriginOffset() {
        return this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>;
    }

    /// 发射伽马激光束：最大 16 格、仅穿透可替换方块、摧毁沿途棱镜、按等级破坏方块、16 倍实体伤害、加热余烬金属。
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    protected void emitGammaLaserBeam(Direction direction) {
        if (this.level == null) return;
        int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;
        int gammaLevel = this.getGammaLaserLevel();
        BlockPos gammaOrigin = this.getBlockPos();
        if (this.isLaserOriginOffset()) {
            gammaOrigin = gammaOrigin.relative(direction);
        }
        BlockPos tempIrradiateBlockPos = CfaGammaLaserEffects.findTarget(this.level, gammaOrigin, direction);
        CfaGammaLaserEffects.destroyPrisms(this.level, this.getBlockPos(), direction, tempIrradiateBlockPos);
        BaseLaserBlockEntity newLaserTarget =
            this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity target ? target : null;
        boolean targetChanged = !tempIrradiateBlockPos.equals(this.irradiateBlockPos);
        boolean targetEntityChanged = newLaserTarget != this.irradiatedLaserTarget;
        boolean targetRevisionChanged = newLaserTarget != null
                                        && newLaserTarget.laserLinkRevision != this.irradiatedLaserTargetRevision;
        if (targetChanged || targetEntityChanged || targetRevisionChanged) {
            if (this.irradiatedLaserTarget != null) {
                this.irradiatedLaserTarget.onCancelingIrradiation(this);
            } else if (targetChanged && this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
            }
            this.irradiatedLaserTarget = null;
            this.irradiatedLaserTargetRevision = -1;
        }
        boolean laserLevelChanged = this.laserLevel != gammaLevel;
        BlockMiningEffect miningEffect = this.getMiningEffect();
        boolean miningEffectChanged = !this.lastEmittedMiningEffect.equals(miningEffect);
        boolean gammaChanged = !this.lastEmittedGamma;
        this.updateLaserLevel(gammaLevel);
        if (newLaserTarget != null && !this.isInIrradiateSelfLaserBlockSet(newLaserTarget)) {
            boolean needsIrradiationUpdate = targetChanged
                                              || targetEntityChanged
                                              || targetRevisionChanged
                                              || laserLevelChanged
                                              || miningEffectChanged
                                              || gammaChanged;
            if (needsIrradiationUpdate && !newLaserTarget.getIgnoreFace().contains(direction)) {
                this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                newLaserTarget.onIrradiated(this);
                this.irradiatedLaserTarget = newLaserTarget;
                this.irradiatedLaserTargetRevision = newLaserTarget.laserLinkRevision;
            }
        }
        this.lastEmittedMiningEffect = miningEffect;
        this.lastEmittedGamma = true;
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);
        if (!(this.level instanceof ServerLevel)) {
            this.maxTransmissionDistance = originalMaxDistance;
            return;
        }
        CfaGammaLaserEffects.damageEntities(
            this.level, this.getBlockPos(), this.irradiateBlockPos, direction, gammaLevel
        );
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
        CfaGammaLaserEffects.heatEmberMetal(
            this.level, this.irradiateBlockPos, direction, gammaLevel, Block.UPDATE_CLIENTS
        );
        this.maxTransmissionDistance = originalMaxDistance;
    }
}