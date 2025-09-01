package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.dubhe.anvilcraft.block.MagnetBlock.LIT;

@Mixin(AnvilBlock.class)
abstract class AnvilBlockMixin extends FallingBlock {
    public AnvilBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        this.anvilcraft$wasAttracted(state, level, pos);
        if (anvilcraft$isAttractUp(level.getBlockState(pos.above()))
            || !FallingBlock.isFree(level.getBlockState(pos.below()))
            || pos.getY() < level.getMinBuildHeight()) {
            return;
        }
        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall(level, pos, state);
        this.falling(fallingBlockEntity);
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        this.anvilcraft$wasAttracted(state, level, pos);
    }

    @Unique
    private boolean anvilcraft$isAttractUp(BlockState state) {
        return state.is(ModBlockTags.MAGNET) && !state.getValue(LIT);
    }

    @Override
    public void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        this.anvilcraft$wasAttracted(state, level, pos);
    }

    // -1 -56 7, -1 -57 7
    @Unique
    private void anvilcraft$wasAttracted(BlockState state, Level level, BlockPos anvil) {
        BlockPos magnet = anvil;
        BlockPos irradiator = anvil;
        int distance = AnvilCraft.CONFIG.magnetAttractsDistance;
        if (anvilcraft$isAttractDown(level, anvil)) {
            if (!FallingBlock.isFree(level.getBlockState(anvil.below()))) return;
            for (int i = 0; i < 7; i++) {
                irradiator = irradiator.below();
                if (FallingBlock.isFree(level.getBlockState(irradiator))) continue;
                level.destroyBlock(irradiator.above(), true);
                level.setBlockAndUpdate(irradiator.above(), state);
                level.setBlockAndUpdate(anvil, Blocks.AIR.defaultBlockState());
                return;
            }
        }
        if (level.getBlockState(anvil.above()).is(ModBlockTags.MAGNET)) return;
        for (int i = 0; i < distance; i++) {
            magnet = magnet.above();
            BlockState state1 = level.getBlockState(magnet);
            if (!(state1.getBlock() instanceof MagnetBlock) || state1.getValue(LIT)) {
                if (level.isEmptyBlock(magnet) || state1.getBlock() instanceof LiquidBlock) {
                    continue;
                } else {
                    return;
                }
            }
            level.destroyBlock(magnet.below(), true);
            level.setBlockAndUpdate(magnet.below(), state);
            level.setBlockAndUpdate(anvil, Blocks.AIR.defaultBlockState());
            AnimateAscendingBlockEntity.animate(level, anvil, state, magnet.below());
            return;
        }
    }

    @Unique
    public boolean anvilcraft$isAttractDown(Level level, BlockPos anvil) {
        BlockPos irradiator = anvil;
        for (int i = 0; i < 7; i++) {
            irradiator = irradiator.below();
            if (level.getBlockState(irradiator).is(ModBlocks.NEUTRON_IRRADIATOR.get())) return true;
        }
        return false;
    }

    @Inject(method = "damage", at = @At("RETURN"), cancellable = true)
    private static void damage(BlockState state, CallbackInfoReturnable<BlockState> cir) {
        if (state.is(ModBlockTags.CANT_BROKEN_ANVIL)) cir.setReturnValue(state);
    }
}
