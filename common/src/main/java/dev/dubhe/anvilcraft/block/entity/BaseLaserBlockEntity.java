package dev.dubhe.anvilcraft.block.entity;

import static dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer.anvilCraftBlockPlacer;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.network.LaserEmitPack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import org.jetbrains.annotations.NotNull;

public abstract class BaseLaserBlockEntity extends BlockEntity {
    private final HashMap<Integer, Integer> levelToTimeMap = new HashMap<>() {{
            put(1, 24);
            put(2, 6);
            put(3, 2);
            put(4, 1);
        }};
    protected int maxTransmissionDistance = 128;
    protected int tickCount = 0;

    protected HashSet<BaseLaserBlockEntity> irradiateSelfLaserBlockSet = new HashSet<>();
    public BlockPos irradiateBlockPos = null;

    public BaseLaserBlockEntity(BlockEntityType<?> type,
        BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private boolean canPassThrough(Direction direction, BlockPos blockPos) {
        if (level == null) return false;
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.is(ModBlockTags.LASE_CAN_PASS_THROUGH)
            || blockState.is(ModBlockTags.GLASS_BLOCKS)
            || blockState.is(ModBlockTags.FORGE_GLASS_BLOCKS)
            || blockState.is(ModBlockTags.GLASS_PANES)
            || blockState.is(ModBlockTags.FORGE_GLASS_PANES)
            || blockState.is(BlockTags.REPLACEABLE)) return true;
        if (!AnvilCraft.config.isLaserDoImpactChecking) return false;
        AABB laseBoundingBox = switch (direction.getAxis()) {
            case X -> Block.box(0, 7, 7, 16, 9, 9).bounds();
            case Y -> Block.box(7, 0, 7, 9, 16, 9).bounds();
            case Z -> Block.box(7, 7, 0, 9, 9, 16).bounds();
        };
        return blockState.getShape(level, blockPos).toAabbs().stream().noneMatch(
            laseBoundingBox::intersects);
    }

    private BlockPos getIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            if (!canPassThrough(direction, originPos.relative(direction, length)))
                return originPos.relative(direction, length);
        }
        return  originPos.relative(direction, expectedLength);
    }

    protected int getBasePower() {
        return 1;
    }

    protected int getPower() {
        return getBasePower() + irradiateSelfLaserBlockSet.stream().mapToInt(BaseLaserBlockEntity::getPower).sum();
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
        if (level.getBlockEntity(tempIrradiateBlockPos)
            instanceof BaseLaserBlockEntity irradiatedLaserBlockEntity
            && !isInIrradiateSelfLaserBlockSet(irradiatedLaserBlockEntity))
            irradiatedLaserBlockEntity.onIrradiated(this);
        irradiateBlockPos = tempIrradiateBlockPos;
        new LaserEmitPack(getBlockPos(), irradiateBlockPos).broadcast();

        if (!(level instanceof ServerLevel serverLevel)) return;
        int power = getPower();
        AABB trackBoundingBox =
            new AABB(getBlockPos().relative(direction).getCenter().add(-0.0625, -0.0625, -0.0625),
                irradiateBlockPos.relative(direction.getOpposite()).getCenter().add(0.0625, 0.0625, 0.0625)
                );
        int hurt = Math.min(16, power);
        level.getEntities(EntityTypeTest.forClass(LivingEntity.class), trackBoundingBox, Entity::isAlive)
            .forEach(livingEntity -> livingEntity.hurt(level.damageSources().inFire(), hurt));
        BlockState irradiateBlock = level.getBlockState(irradiateBlockPos);
        List<ItemStack> drops = Block.getDrops(irradiateBlock, serverLevel, irradiateBlockPos,
            level.getBlockEntity(irradiateBlockPos));
        if (power <= 16) {
            if (levelToTimeMap.containsKey((power / 4)) && tickCount >= levelToTimeMap.get((power / 4)) * 20) {
                tickCount = 0;
                if (irradiateBlock.is(ModBlockTags.FORGE_ORES) || irradiateBlock.is(ModBlockTags.ORES)) {
                    Vec3 blockPos = getBlockPos().relative(direction.getOpposite()).getCenter();
                    drops.forEach(itemStack -> level.addFreshEntity(new ItemEntity(
                        level,
                        blockPos.x,
                        blockPos.y,
                        blockPos.z,
                        itemStack
                        )));
                    if (irradiateBlock.is(ModBlockTags.ORES_IN_GROUND_DEEPSLATE)
                        || irradiateBlock.is(ModBlockTags.FORGE_ORES_IN_GROUND_DEEPSLATE))
                        level.setBlockAndUpdate(irradiateBlockPos, Blocks.DEEPSLATE.defaultBlockState());
                    else if (irradiateBlock.is(ModBlockTags.ORES_IN_GROUND_NETHERRACK)
                        || irradiateBlock.is(ModBlockTags.FORGE_ORES_IN_GROUND_NETHERRACK))
                        level.setBlockAndUpdate(irradiateBlockPos, Blocks.NETHERRACK.defaultBlockState());
                    else level.setBlockAndUpdate(irradiateBlockPos, Blocks.STONE.defaultBlockState());
                }
            }

        } else {
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
        }
    }

    /**
     * 检测光学原件是否在链接表中
     */
    public boolean isInIrradiateSelfLaserBlockSet(BaseLaserBlockEntity baseLaserBlockEntity) {
        return baseLaserBlockEntity == this
            || irradiateSelfLaserBlockSet.contains(baseLaserBlockEntity)
            || irradiateSelfLaserBlockSet.stream().anyMatch(baseLaserBlockEntity1 ->
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
        irradiateBlockPos = null;
        if (level == null) return;
        if (tempIrradiateBlockPos == null) return;
        if (!(level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity)) return;
        irradiateBlockEntity.onCancelingIrradiation(this);
    }

    public void tick(@NotNull Level level) {
        tickCount++;
    }

    public boolean isSwitch() {
        return false;
    }

    public Direction getDirection() {
        return Direction.UP;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level == null) return;
        if (irradiateBlockPos == null) return;
        if (!(level.getBlockEntity(irradiateBlockPos) instanceof BaseLaserBlockEntity irradiateBlockEntity)) return;
        irradiateBlockEntity.onCancelingIrradiation(this);
    }

    /**
     * 为了适配forge中修改的渲染逻辑所添加的函数
     * 返回一个无限碰撞箱
     *
     * @return forge中为原版信标生成的无限碰撞箱
     */
    @SuppressWarnings("unused")
    public AABB getRenderBoundingBox() {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
}
