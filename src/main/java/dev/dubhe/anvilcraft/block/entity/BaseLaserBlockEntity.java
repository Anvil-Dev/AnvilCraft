package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.laser.GammaLaserBehavior;
import dev.dubhe.anvilcraft.api.laser.ILaserComponent;
import dev.dubhe.anvilcraft.api.laser.ILaserComponentOwner;
import dev.dubhe.anvilcraft.api.laser.ILaserComponentType;
import dev.dubhe.anvilcraft.api.laser.LaserComponentMap;
import dev.dubhe.anvilcraft.api.laser.LaserComponentTypes;
import dev.dubhe.anvilcraft.api.laser.LaserDamageBehavior;
import dev.dubhe.anvilcraft.api.laser.LaserHitBehavior;
import dev.dubhe.anvilcraft.api.laser.LaserMiningComponent;
import dev.dubhe.anvilcraft.api.laser.LaserParticleBehavior;
import dev.dubhe.anvilcraft.api.laser.LaserStrengthComponent;
import dev.dubhe.anvilcraft.api.laser.LaserTypeComponent;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseLaserBlockEntity extends BlockEntity implements ILaserComponentOwner {
    private final LaserComponentMap laserComponents = new LaserComponentMap();
    private final LaserComponentMap configuredComponents = new LaserComponentMap();
    private Direction laserDirection = Direction.NORTH;
    private int laserMaxLength = 128;
    protected int maxTransmissionDistance = 128;
    protected int tickCount = 0;

    protected HashSet<BaseLaserBlockEntity> irradiateSelfLaserBlockSet = new HashSet<>();
    protected boolean changed = false;
    @Getter
    protected @Nullable BlockPos irradiateBlockPos = null;
    protected @Nullable BaseLaserBlockEntity irradiatedLaserTarget = null;
    protected int laserLinkRevision = 0;
    protected int irradiatedLaserTargetRevision = -1;
    private BlockMiningEffect lastEmittedMiningEffect = BlockMiningEffect.NORMAL;
    private boolean lastEmittedGamma = false;
    @Getter
    protected int laserLevel = 0;

    public BaseLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getLaserMaxLength() {
        return this.laserMaxLength;
    }

    @Override
    public void setLaserMaxLength(int value) {
        this.laserMaxLength = Math.max(0, value);
    }

    @Override
    public List<ILaserComponent> allComponents() {
        return this.laserComponents.allComponents();
    }

    @Override
    @Nullable
    public <T extends ILaserComponent> T getComponent(ILaserComponentType<T, ?> type) {
        return this.laserComponents.get(type);
    }

    @Override
    @Nullable
    public <T extends ILaserComponent, E> T setOrCreateComponent(
        ILaserComponentType<T, E> type, @Nullable T instance, @Nullable E creationEnvironment
    ) {
        if (instance == null) {
            instance = this.getComponent(type);
            if (instance != null || creationEnvironment == null) return instance;
            instance = type.createInstance(creationEnvironment);
        }
        this.configuredComponents.put(type, instance);
        this.laserComponents.put(type, instance);
        this.markChanged();
        return instance;
    }

    @Override
    public BlockPos getLaserSourcePos() {
        return getBlockPos();
    }

    @Override
    public BlockPos getLaserOrigin() {
        return this.isLaserOriginOffset() ? getBlockPos().relative(this.laserDirection) : getBlockPos();
    }

    @Override
    public Direction getLaserDirection() {
        return this.laserDirection;
    }

    @Override
    public int getLaserTicks() {
        return this.tickCount;
    }

    @Override
    public void deliverLaserDrops(List<ItemStack> drops, BlockPos sourceBlockPos) {
        this.deliverItem(drops, this.laserDirection, sourceBlockPos);
    }

    protected void configureLaserComponents(LaserComponentMap components) {
    }

    private LaserComponentMap createLaserComponents(boolean gamma) {
        List<LaserComponentMap> incoming = new ArrayList<>();
        this.irradiateSelfLaserBlockSet.stream()
            .sorted(Comparator.comparing(BaseLaserBlockEntity::getBlockPos))
            .forEach(source -> incoming.add(source.createLaserComponents(source.isGammaLaserConfigured())));
        int baseStrength = gamma ? this.getGammaLaserLevel() : this.getBaseLaserLevel();
        if (baseStrength > 0 || incoming.isEmpty()) {
            LaserComponentMap base = new LaserComponentMap();
            base.put(LaserComponentTypes.STRENGTH, new LaserStrengthComponent(Math.max(0, baseStrength)));
            base.put(LaserComponentTypes.LASER_TYPE, new LaserTypeComponent(gamma));
            base.put(LaserComponentTypes.MINING, new LaserMiningComponent(BlockMiningEffect.NORMAL, false));
            base.put(LaserComponentTypes.DAMAGE_BEHAVIOR, new LaserDamageBehavior());
            base.put(LaserComponentTypes.PARTICLE_BEHAVIOR, new LaserParticleBehavior());
            base.put(LaserComponentTypes.GAMMA_BEHAVIOR, new GammaLaserBehavior());
            base.put(LaserComponentTypes.HIT_BEHAVIOR, new LaserHitBehavior());
            incoming.add(base);
        }
        LaserComponentMap result = LaserComponentMap.mergeIncoming(incoming);
        this.configureLaserComponents(result);
        result.putAll(this.configuredComponents);
        return result;
    }

    protected boolean canPassThrough(Direction direction, BlockPos blockPos) {
        if (this.level == null) return false;
        LaserTypeComponent type = this.getComponent(LaserComponentTypes.LASER_TYPE);
        return type != null && type.canPassThrough(this.level, direction, blockPos);
    }

    protected void resetLaserComponentState() {
        for (ILaserComponent component : this.allComponents()) component.onEmissionStopped(this);
    }

    public void updateIrradiateBlockPos(@Nullable BlockPos newPos) {
        if (newPos == null) {
            this.resetLaserComponentState();
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

    public boolean isEmittingGamma() {
        LaserTypeComponent type = this.getComponent(LaserComponentTypes.LASER_TYPE);
        return type == null ? this.isGammaLaserConfigured() : type.gamma();
    }

    protected boolean isGammaLaserConfigured() {
        return false;
    }

    protected int calculateLaserLevel() {
        LaserStrengthComponent strength = this.createLaserComponents(this.isGammaLaserConfigured()).get(LaserComponentTypes.STRENGTH);
        return strength == null ? 0 : strength.strength();
    }

    public BlockMiningEffect getMiningEffect() {
        LaserMiningComponent mining = this.createLaserComponents(this.isGammaLaserConfigured()).get(LaserComponentTypes.MINING);
        return mining == null ? BlockMiningEffect.NORMAL : mining.effect();
    }

    public int getLaserColor() {
        return this.isEmittingGamma() ? 0x9919FF : this.getMiningEffect().getLaserColor();
    }

    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.isEmittingGamma())
        );
    }

    public void tick(Level level) {
        if (this.changed) {
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    level.getChunkAt(getBlockPos()).getPos(),
                    new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.isEmittingGamma())
                );
            }
        }

        this.tickCount++;
    }

    /**
     * 发射激光
     */
    public void emitLaser(Direction direction) {
        this.emitLaserComponents(direction, this.isGammaLaserConfigured());
    }

    private void emitLaserComponents(Direction direction, boolean gamma) {
        if (this.level == null) return;
        this.laserDirection = direction;
        this.laserComponents.replaceWith(this.createLaserComponents(gamma));
        this.laserMaxLength = this.maxTransmissionDistance;
        this.laserComponents.onEmitPre(this);
        if (this.laserMaxLength <= 0) {
            if (this.irradiatedLaserTarget != null) this.irradiatedLaserTarget.onCancelingIrradiation(this);
            this.updateIrradiateBlockPos(null);
            this.updateLaserLevel(0);
            return;
        }
        BlockPos tempIrradiateBlockPos = this.getIrradiateBlockPos(this.laserMaxLength, direction, this.getLaserOrigin());
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
        int newLaserLevel = LaserStrengthComponent.getStrength(this);
        boolean laserLevelChanged = this.laserLevel != newLaserLevel;
        BlockMiningEffect miningEffect = LaserMiningComponent.getEffect(this);
        boolean miningEffectChanged = !this.lastEmittedMiningEffect.equals(miningEffect);
        boolean emittedGamma = LaserTypeComponent.isGamma(this);
        boolean gammaChanged = this.lastEmittedGamma != emittedGamma;
        this.updateLaserLevel(newLaserLevel);
        if (gammaChanged || miningEffectChanged) this.markChanged();
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
        this.lastEmittedGamma = emittedGamma;
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);

        this.laserComponents.onHitBlock(this, this.level, tempIrradiateBlockPos);
    }

    public void deliverItem(List<ItemStack> drops, Direction direction, BlockPos sourceBlockPos) {
        if (this.level == null) return;
        Vec3 dropPos = getBlockPos().relative(direction.getOpposite()).getCenter();
        BlockPos downStreamPos = getBlockPos().relative(this.getFacing().getOpposite());
        if (this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
            dropPos = getBlockPos().relative(direction.getOpposite(), 2).getCenter();
            downStreamPos = getBlockPos().relative(this.getFacing().getOpposite(), 2);
        }
        if (getLevel() == null) return;
        ResourceHandler<ItemResource> cap = getLevel()
            .getCapability(
                Capabilities.Item.BLOCK,
                downStreamPos,
                this.getFacing()
            );
        if (cap == null
            && this.level.getBlockEntity(downStreamPos) instanceof BaseLaserBlockEntity downStreamBlockEntity
            && downStreamBlockEntity.getFacing() == direction) {
            downStreamBlockEntity.deliverItem(drops, direction, sourceBlockPos);
            return;
        }
        for (ItemStack stack : drops) {
            ItemStack remainder = cap == null ? stack : ItemHandlerUtil.insertItem(cap, stack, false);
            if (!remainder.isEmpty()) {
                this.level.addFreshEntity(new ItemEntity(this.level, dropPos.x, dropPos.y, dropPos.z, remainder));
            }
        }
    }

    /**
     * 检测光学原件是否在链接表中
     */
    public boolean isInIrradiateSelfLaserBlockSet(BaseLaserBlockEntity baseLaserBlockEntity) {
        return baseLaserBlockEntity == this
               || this.irradiateSelfLaserBlockSet.contains(baseLaserBlockEntity)
               || this.irradiateSelfLaserBlockSet.stream()
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
        BlockPos tempIrradiateBlockPos = this.irradiateBlockPos;
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
            CachedBlockEntityRenderingPipeline.getInstance().blockRemoved(this);
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

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && this.level.isClientSide()) {
            CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
        }
    }

    public void updateLaserLevel(int value) {
        if (this.laserLevel != value) {
            this.markChanged();
        }
        this.laserLevel = value;
    }

    public void clientUpdateComponents(int strength, boolean gamma) {
        this.laserComponents.put(LaserComponentTypes.STRENGTH, new LaserStrengthComponent(Math.max(0, strength)));
        this.laserComponents.put(LaserComponentTypes.LASER_TYPE, new LaserTypeComponent(gamma));
    }

    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
    }

    protected int getGammaLaserLevel() {
        return this.getBaseLaserLevel();
    }

    /// 激光束起点是否从方块正面外一格开始。柔性多方块默认偏移；发光面在本格正面的方块可覆写为 false。
    protected boolean isLaserOriginOffset() {
        return this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>;
    }

    protected void emitGammaLaserBeam(Direction direction) {
        this.emitLaserComponents(direction, true);
    }
}
