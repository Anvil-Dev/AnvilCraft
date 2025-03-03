package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModDamageTypes;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
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
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;

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
    @Getter
    protected BlockPos irradiateBlockPos = null;
    @Getter
    protected int laserLevel = 0;
    private boolean changed = false;

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
        if (!AnvilCraft.config.isLaserDoImpactChecking) return false;
        AABB laseBoundingBox =
            switch (direction.getAxis()) {
                case X -> Block.box(0, 7, 7, 16, 9, 9).bounds();
                case Y -> Block.box(7, 0, 7, 9, 16, 9).bounds();
                case Z -> Block.box(7, 7, 0, 9, 9, 16).bounds();
            };
        return blockState.getCollisionShape(level, blockPos).toAabbs().stream().noneMatch(laseBoundingBox::intersects);
    }

    public void updateIrradiateBlockPos(BlockPos newPos) {
        if (irradiateBlockPos == null) {
            if (newPos != null)
                markChanged();
            irradiateBlockPos = newPos;
            return;
        }
        if (!irradiateBlockPos.equals(newPos))
            markChanged();
        irradiateBlockPos = newPos;
    }

    public void resetState() {
        changed = false;
    }

    public void markChanged() {
        changed = true;
    }

    private BlockPos getIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            if (!canPassThrough(direction, originPos.relative(direction, length)))
                return originPos.relative(direction, length);
        }
        return originPos.relative(direction, expectedLength);
    }

    protected int getBaseLaserLevel() {
        return 1;
    }

    protected int calculateLaserLevel() {
        return getBaseLaserLevel()
            + irradiateSelfLaserBlockSet.stream()
            .mapToInt(BaseLaserBlockEntity::calculateLaserLevel)
            .sum();
    }

    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(laserLevel, getBlockPos(), irradiateBlockPos)
        );
    }

    public void tick(@NotNull Level level) {
        if (changed) {
            if (level instanceof ServerLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(
                    (ServerLevel) level,
                    level.getChunkAt(getBlockPos()).getPos(),
                    new LaserEmitPacket(laserLevel, getBlockPos(), irradiateBlockPos)
                );
            }
        }
        tickCount++;
    }

    /**
     * 发射激光
     */
    public void emitLaser(Direction direction) {
        if (level == null) return;
        BlockPos tempIrradiateBlockPos = getIrradiateBlockPos(maxTransmissionDistance, direction, getBlockPos());
        if (!tempIrradiateBlockPos.equals(irradiateBlockPos)) {
            if (irradiateBlockPos != null
                && level.getBlockEntity(irradiateBlockPos)
                instanceof BaseLaserBlockEntity lastIrradiatedLaserBlockEntity)
                lastIrradiatedLaserBlockEntity.onCancelingIrradiation(this);
        }
        if (level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiatedLaserBlockEntity
            && !isInIrradiateSelfLaserBlockSet(irradiatedLaserBlockEntity))
            irradiatedLaserBlockEntity.onIrradiated(this);
        updateIrradiateBlockPos(tempIrradiateBlockPos);

        if (!(level instanceof ServerLevel serverLevel)) return;
        updateLaserLevel(calculateLaserLevel());
        int hurt = Math.min(16, laserLevel - 4);
        if (hurt > 0) {
            AABB trackBoundingBox = new AABB(
                getBlockPos()
                    .relative(direction)
                    .getCenter()
                    .add(-0.0625, -0.0625, -0.0625),
                irradiateBlockPos.relative(direction.getOpposite())
                    .getCenter()
                    .add(0.0625, 0.0625, 0.0625)
            );
            level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                trackBoundingBox,
                Entity::isAlive
            ).forEach(livingEntity ->
                livingEntity.hurt(
                    ModDamageTypes.laser(level),
                    hurt
                )
            );
        }
        BlockState irradiateBlock = level.getBlockState(irradiateBlockPos);
        int cooldown = COOLDOWNS[Math.clamp(laserLevel / 4, 0, 4)];
        if (tickCount >= cooldown) {
            tickCount = 0;
            if (irradiateBlock.is(Tags.Blocks.ORES)) {
                List<ItemStack> drops = Block.getDrops(
                    irradiateBlock,
                    serverLevel,
                    irradiateBlockPos,
                    level.getBlockEntity(irradiateBlockPos)
                );
                Vec3 blockPos = getBlockPos().relative(direction.getOpposite()).getCenter();
                IItemHandler cap = getLevel()
                    .getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        getBlockPos().relative(getFacing().getOpposite()),
                        getFacing()
                    );
                drops.forEach(itemStack -> {
                    if (cap != null) {
                        ItemStack outItemStack = ItemHandlerHelper.insertItem(cap, itemStack, true);
                        if (outItemStack.isEmpty()) {
                            ItemHandlerHelper.insertItem(cap, itemStack, false);
                        } else {
                            level.addFreshEntity(
                                new ItemEntity(
                                    level,
                                    blockPos.x,
                                    blockPos.y,
                                    blockPos.z,
                                    outItemStack)
                            );
                        }
                    } else level.addFreshEntity(new ItemEntity(level, blockPos.x, blockPos.y, blockPos.z, itemStack));
                });
                if (irradiateBlock.is(Blocks.ANCIENT_DEBRIS)) {
                    level.setBlockAndUpdate(irradiateBlockPos, Blocks.NETHERRACK.defaultBlockState());
                } else if (irradiateBlock.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)) {
                    level.setBlockAndUpdate(irradiateBlockPos, Blocks.DEEPSLATE.defaultBlockState());
                } else if (irradiateBlock.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)) {
                    level.setBlockAndUpdate(irradiateBlockPos, Blocks.NETHERRACK.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(irradiateBlockPos, Blocks.STONE.defaultBlockState());
                }
                /* else {
                    if (level.getBlockState(irradiateBlockPos).getBlock().defaultDestroyTime() >= 0
                        && !(level.getBlockEntity(irradiateBlockPos) instanceof BaseLaserBlockEntity)) {
                        level.getBlockState(irradiateBlockPos).getBlock()
                            .playerWillDestroy(
                                level,
                                irradiateBlockPos,
                                level.getBlockState(irradiateBlockPos),
                                anvilCraftBlockPlacer.getPlayer());
                        level.destroyBlock(irradiateBlockPos, false);
                    }
                }*/
            }
        }
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

    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
        irradiateSelfLaserBlockSet.add(baseLaserBlockEntity);
    }

    /**
     * 当方块被取消激光照射时调用
     */
    public void onCancelingIrradiation(BaseLaserBlockEntity baseLaserBlockEntity) {
        irradiateSelfLaserBlockSet.remove(baseLaserBlockEntity);
        BlockPos tempIrradiateBlockPos = irradiateBlockPos;
        updateIrradiateBlockPos(null);
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
        if (irradiateBlockPos == null) return;
        if (!(level.getBlockEntity(irradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity)) return;
        irradiateBlockEntity.onCancelingIrradiation(this);
        if (level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
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
            Double.POSITIVE_INFINITY);
    }


    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && this.level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    public void updateLaserLevel(int value) {
        if (laserLevel != value) {
            markChanged();
        }
        laserLevel = value;
    }

    public void clientUpdate(BlockPos irradiateBlockPos, int laserLevel) {
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CacheableBERenderingPipeline.getInstance().update(this);
    }
}
