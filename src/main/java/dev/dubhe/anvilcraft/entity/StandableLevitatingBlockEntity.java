package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class StandableLevitatingBlockEntity extends LevitatingBlockEntity {
    public StandableLevitatingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    private StandableLevitatingBlockEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.STANDABLE_LEVITATING_BLOCK.get(), level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    @SuppressWarnings("UnusedReturnValue")
    public static StandableLevitatingBlockEntity levitate(Level level, BlockPos pos, BlockState blockState) {
        StandableLevitatingBlockEntity levitating = new StandableLevitatingBlockEntity(
            level,
            (double) pos.getX() + 0.5,
            pos.getY(),
            (double) pos.getZ() + 0.5,
            blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                : blockState
        );
        level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(levitating);
        return levitating;
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        if (this.blockState.isAir()) {
            this.discard();
        } else {
            Block block = this.blockState.getBlock();
            ++this.time;
            BlockPos blockPos = this.blockPosition();

            if (blockPos.getY() <= this.level().getMinBuildHeight() || blockPos.getY() > this.level().getMaxBuildHeight() + 64) {
                this.discard();
            } else if (FallingBlock.isFree(this.level().getBlockState(blockPos.above()))) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04, 0.0));
            } else {
                if (!this.level().isClientSide) {
                    BlockState blockState = this.level().getBlockState(blockPos);
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
                    if (!blockState.is(Blocks.MOVING_PISTON)) {
                        if (!this.cancelDrop) {
                            boolean canBeReplaced = blockState.canBeReplaced(
                                new DirectionalPlaceContext(this.level(), blockPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)
                            );
                            boolean canSurvive = this.blockState.canSurvive(this.level(), blockPos);
                            if (canBeReplaced && canSurvive) {
                                if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                                    && this.level().getFluidState(blockPos).getType() == Fluids.WATER) {
                                    this.blockState = this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
                                }

                                if (this.level().setBlock(blockPos, this.blockState, 3)) {
                                    ((ServerLevel) this.level())
                                        .getChunkSource()
                                        .chunkMap
                                        .broadcast(this, new ClientboundBlockUpdatePacket(blockPos, this.level().getBlockState(blockPos)));
                                    this.discard();
                                    if (block instanceof Fallable) {
                                        ((Fallable) block).onLand(this.level(), blockPos, this.blockState, blockState, this);
                                    }
                                } else if (this.dropItem && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
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
                        } else {
                            this.discard();
                            this.callOnBrokenAfterFall(block, blockPos);
                        }
                    }
                }
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
        super.move(type, motion);
        if (motion.x == 0 && motion.y == 0 && motion.z == 0) return;
        List<Entity> list = this.level().getEntities(
            this,
            this.getBoundingBox().expandTowards(0, 0.5F, 0),
            EntitySelector.pushableBy(this)
        );
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (entity instanceof StandableLevitatingBlockEntity) continue;
                entity.setDeltaMovement(
                    entity.getDeltaMovement().x,
                    entity.getDeltaMovement().y > 0 ? motion.y * 1.08 : entity.getDeltaMovement().y + motion.y * 2.8,
                    entity.getDeltaMovement().z
                );
            }
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
}
