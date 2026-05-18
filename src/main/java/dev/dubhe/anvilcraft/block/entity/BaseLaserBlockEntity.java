package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseLaserBlockEntity extends BlockEntity {
    public static final int[] COOLDOWNS = {
        Integer.MAX_VALUE,
        24 * 20,
        6 * 20,
        2 * 20,
        20
    };
    protected int maxTransmissionDistance = 128;
    protected int tickCount = 0;

    protected HashSet<BaseLaserBlockEntity> irradiateSelfLaserBlockSet = new HashSet<>();
    protected boolean changed = false;
    @Getter
    protected @Nullable BlockPos irradiateBlockPos = null;
    @Getter
    protected int laserLevel = 0;

    public BaseLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private boolean canPassThrough(Direction direction, BlockPos blockPos) {
        if (level == null) return false;
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.is(ModBlockTags.LASER_CAN_PASS_THROUGH)
            || blockState.is(Tags.Blocks.GLASS_BLOCKS)
            || blockState.is(Tags.Blocks.GLASS_PANES)
            || blockState.is(BlockTags.REPLACEABLE)) return true;
        if (!AnvilCraft.CONFIG.isLaserDoImpactChecking) return false;
        AABB laseBoundingBox = switch (direction.getAxis()) {
            case X -> Block.box(0, 7, 7, 16, 9, 9).bounds();
            case Y -> Block.box(7, 0, 7, 9, 16, 9).bounds();
            case Z -> Block.box(7, 7, 0, 9, 9, 16).bounds();
        };
        return blockState.getCollisionShape(level, blockPos).toAabbs().stream().noneMatch(laseBoundingBox::intersects);
    }

    public void updateIrradiateBlockPos(@Nullable BlockPos newPos) {
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

    protected int calculateLaserLevel() {
        return this.getBaseLaserLevel()
            + this.irradiateSelfLaserBlockSet.stream()
            .mapToInt(BaseLaserBlockEntity::calculateLaserLevel)
            .sum();
    }

    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos)
        );
    }

    public void tick(Level level) {
        if (this.changed) {
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    level.getChunkAt(getBlockPos()).getPos(),
                    new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos)
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
        if (this.irradiateBlockPos != null && !tempIrradiateBlockPos.equals(this.irradiateBlockPos)) {
            if (this.level.getBlockEntity(this.irradiateBlockPos) instanceof BaseLaserBlockEntity lastIrradiatedLaserBlockEntity) {
                lastIrradiatedLaserBlockEntity.onCancelingIrradiation(this);
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
                for (Direction direction1 : irradiatedLaserBlockEntity.getIgnoreFace()) {
                    if (direction != direction1) {
                        this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                        irradiatedLaserBlockEntity.onIrradiated(this);
                    }
                }
            }
        }
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);

        if (!(this.level instanceof ServerLevel serverLevel)) return;
        this.updateLaserLevel(this.calculateLaserLevel());
        int hurt = Math.min(16, this.laserLevel - 4);
        if (hurt > 0) {
            AABB trackBoundingBox = new AABB(
                this.getBlockPos()
                    .relative(direction)
                    .getCenter()
                    .add(-0.0625, -0.0625, -0.0625),
                this.irradiateBlockPos.relative(direction.getOpposite())
                    .getCenter()
                    .add(0.0625, 0.0625, 0.0625)
            );
            // noinspection deprecation
            this.level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                trackBoundingBox,
                Entity::isAlive
            ).forEach(livingEntity ->
                livingEntity.hurtOrSimulate(
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
                List<ItemStack> drops = BreakBlockUtil.drop(
                    serverLevel,
                    this.irradiateBlockPos
                );
                this.deliverItem(drops, direction, this.irradiateBlockPos);
            }
        }
    }

    public void deliverItem(List<ItemStack> drops, Direction direction, BlockPos sourceBlockPos) {
        if (this.level == null) return;
        Vec3 blockPos = getBlockPos().relative(direction.getOpposite()).getCenter();
        BlockPos downStreamPos = getBlockPos().relative(this.getFacing().getOpposite());
        if (getLevel() == null) return;
        ResourceHandler<ItemResource> cap = getLevel()
            .getCapability(
                Capabilities.Item.BLOCK,
                downStreamPos,
                this.getFacing()
            );
        BlockState sourceBlock = this.level.getBlockState(sourceBlockPos);
        drops.forEach(itemStack -> {
            if (cap != null) {
                ItemStack outItemStack = ItemHandlerUtil.insertItem(cap, itemStack, true);
                if (outItemStack.isEmpty()) {
                    ItemHandlerUtil.insertItem(cap, itemStack, false);
                } else {
                    this.level.addFreshEntity(new ItemEntity(
                        this.level,
                        blockPos.x,
                        blockPos.y,
                        blockPos.z,
                        outItemStack
                    ));
                }
            } else if (
                this.level.getBlockEntity(downStreamPos) instanceof BaseLaserBlockEntity downStreamBlockEntity
                && downStreamBlockEntity.getFacing() == direction
            ) {
                downStreamBlockEntity.deliverItem(drops, direction, sourceBlockPos);
            } else this.level.addFreshEntity(new ItemEntity(this.level, blockPos.x, blockPos.y, blockPos.z, itemStack));
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
            || this.irradiateSelfLaserBlockSet.contains(baseLaserBlockEntity)
            || this.irradiateSelfLaserBlockSet.stream()
            .anyMatch(baseLaserBlockEntity1 ->
                baseLaserBlockEntity1.isInIrradiateSelfLaserBlockSet(baseLaserBlockEntity));
    }

    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
        this.irradiateSelfLaserBlockSet.add(baseLaserBlockEntity);
    }

    /**
     * 当方块被取消激光照射时调用
     */
    public void onCancelingIrradiation(BaseLaserBlockEntity baseLaserBlockEntity) {
        this.irradiateSelfLaserBlockSet.remove(baseLaserBlockEntity);
        BlockPos tempIrradiateBlockPos = this.irradiateBlockPos;
        this.updateIrradiateBlockPos(null);
        if (level == null) return;
        if (tempIrradiateBlockPos == null) return;
        if (!(level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity)) return;
        irradiateBlockEntity.onCancelingIrradiation(this);
    }

    public abstract Direction getFacing();

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level == null) return;
        if (this.irradiateBlockPos == null) return;
        if (!level.isLoaded(this.irradiateBlockPos)) return;
        if (!(level.getBlockEntity(this.irradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity)) return;
        irradiateBlockEntity.onCancelingIrradiation(this);
        if (level.isClientSide()) {
            CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
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

    public int getLaserColor() {
        return 0x00ff0d0d;
    }

    public void updateLaserLevel(int value) {
        if (this.laserLevel != value) {
            this.markChanged();
        }
        this.laserLevel = value;
    }

    public void clientUpdate(BlockPos irradiateBlockPos, int laserLevel) {
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
    }
}
