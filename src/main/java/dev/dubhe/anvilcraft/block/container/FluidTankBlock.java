package dev.dubhe.anvilcraft.block.container;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModMultiblockDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class FluidTankBlock extends BaseEntityBlock implements HammerRotateBehavior, IHammerRemovable, IController {

    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(FluidTankBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.FLUID_TANK.create(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            if (tank.onPlayerUse(player, hand)) {
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    public Identifier getDefinitionId() {
        return ModMultiblockDefinitions.FLUID_TANK.identifier();
    }

    @Override
    public void onFormed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.FLUID_TANK.get())
            .ifPresent(FluidTankBlockEntity::onFormed);
    }

    @Override
    public void onUnformed(Level level, MultiblockState state) {
        level.getBlockEntity(state.getControllerPos(), ModBlockEntities.FLUID_TANK.get())
            .ifPresent(FluidTankBlockEntity::onUnformed);
    }
}