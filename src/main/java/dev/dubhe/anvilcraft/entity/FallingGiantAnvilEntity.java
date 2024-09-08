package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FallingGiantAnvilEntity extends FallingBlockEntity {

    private float fallDistance = 0;

    public FallingGiantAnvilEntity(EntityType<? extends FallingGiantAnvilEntity> entityType, Level level) {
        super(entityType, level);
    }

    private FallingGiantAnvilEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.FALLING_GIANT_ANVIL.get(), level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    /**
     * @param level      世界
     * @param pos        方块坐标
     * @param blockState 方块状态
     */
    public static FallingGiantAnvilEntity fall(Level level, BlockPos pos, BlockState blockState, boolean updateBlock) {
        FallingGiantAnvilEntity fallingBlockEntity = new FallingGiantAnvilEntity(
                level,
                (double) pos.getX() + 0.5,
                pos.getY(),
                (double) pos.getZ() + 0.5,
                blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                        ? blockState.setValue(BlockStateProperties.WATERLOGGED, false) : blockState
        );
        if (updateBlock) {
            level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        }
        level.addFreshEntity(fallingBlockEntity);
        return fallingBlockEntity;
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.discard();
        } else {
            ++this.time;
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.getDeltaMovement().y < 0f) {
                this.fallDistance -= (float) this.getDeltaMovement().y;
            }
            if (this.getDeltaMovement().y > 0f) {
                this.fallDistance = 0;
            }
            if (!this.level().isClientSide) {
                BlockPos blockPos = this.blockPosition();
                boolean isConcrete = this.blockState.getBlock() instanceof ConcretePowderBlock;
                boolean shouldHandleWater = isConcrete && this.level().getFluidState(blockPos).is(FluidTags.WATER);
                double d = this.getDeltaMovement().lengthSqr();
                if (isConcrete && d > 1.0) {
                    BlockHitResult blockHitResult = this.level().clip(new ClipContext(
                            new Vec3(this.xo, this.yo, this.zo),
                            this.position(),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.SOURCE_ONLY,
                            this
                    ));
                    if (
                            blockHitResult.getType() != HitResult.Type.MISS
                                    && this.level().getFluidState(blockHitResult.getBlockPos()).is(FluidTags.WATER)
                    ) {
                        blockPos = blockHitResult.getBlockPos();
                        shouldHandleWater = true;
                    }
                }
                Block block = this.blockState.getBlock();
                if (!this.onGround() && !shouldHandleWater) {
                    if (!this.level().isClientSide && (this.time > 100
                            && (blockPos.getY() <= this.level().getMinBuildHeight()
                            || blockPos.getY() > this.level().getMaxBuildHeight())
                            || this.time > 600)
                    ) {
                        if (this.dropItem && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                            this.spawnAtLocation(block);
                        }

                        this.discard();
                    }
                } else {
                    BlockState blockState = this.level().getBlockState(blockPos);
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
                    DirectionalPlaceContext placeContext = new DirectionalPlaceContext(
                            this.level(),
                            blockPos,
                            Direction.DOWN,
                            ItemStack.EMPTY,
                            Direction.UP
                    );
                    boolean isMovingPiston = false;
                    boolean canBeReplaced = true;
                    boolean canSurvive = this.blockState.canSurvive(this.level(), blockPos.below());
                    boolean isFree = true;
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            BlockPos offsetPos = blockPos.offset(i, -1, j);
                            isMovingPiston = isMovingPiston
                                    || this.level().getBlockState(offsetPos).is(Blocks.MOVING_PISTON);
                            for (int k = -1; k <= 1; k++) {
                                canBeReplaced = canBeReplaced
                                        && this.level().getBlockState(blockPos.offset(i, k, j))
                                        .canBeReplaced(placeContext);
                            }
                            isFree = isFree && FallingBlock.isFree(this.level().getBlockState(offsetPos.below()));
                        }
                    }
                    if (!isMovingPiston) {
                        if (canBeReplaced && canSurvive && !isFree) {
                            if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                                    && this.level().getFluidState(blockPos).getType() == Fluids.WATER) {
                                this.blockState = this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
                            }

                            if (this.level().setBlock(blockPos, this.blockState, 3)) {
                                ((ServerLevel) this.level())
                                        .getChunkSource()
                                        .chunkMap
                                        .broadcast(this, new ClientboundBlockUpdatePacket(
                                                blockPos,
                                                this.level().getBlockState(blockPos)
                                        ));
                                this.discard();
                                if (block instanceof GiantAnvilBlock block1) {
                                    block1.onLand(
                                            this.level(),
                                            blockPos,
                                            this.blockState,
                                            blockState,
                                            this,
                                            this.fallDistance
                                    );
                                }
                            } else if (
                                    this.dropItem
                                            && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)
                            ) {
                                this.discard();
                                this.callOnBrokenAfterFall(block, blockPos);
                                this.spawnAtLocation(block);
                            }
                        } else {
                            this.discard();
                            if (this.dropItem && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                this.callOnBrokenAfterFall(block, blockPos);
                                this.spawnAtLocation(block);
                            }
                        }
                    }
                }
            }
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        return EntityDimensions.scalable(3, 3).makeBoundingBox(this.position().add(0, -1, 0));
    }
}
