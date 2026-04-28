package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (anvilcraft$isAttracts(level.getBlockState(pos.above()))) {
            return;
        }
        super.tick(state, level, pos, random);
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
    private boolean anvilcraft$isAttracts(BlockState state) {
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
        BlockState state1 = level.getBlockState(pos.above());
        if (!this.anvilcraft$isAttracts(state1)) this.anvilcraft$wasAttracted(state, level, pos);
    }

    @Unique
    private void anvilcraft$wasAttracted(BlockState state, Level level, BlockPos anvil) {
        BlockPos magnet = anvil;
        BlockState aboveState = level.getBlockState(anvil.above());
        if (aboveState.is(ModBlockTags.MAGNET) || aboveState.getBlock() instanceof MagnetBlock) return;
        int distance = AnvilCraft.CONFIG.magnetAttractsDistance;
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
            TriggerUtil.liftingAnvil(level, magnet.below());
            return;
        }
    }

    @Inject(method = "damage", at = @At("RETURN"), cancellable = true)
    private static void damage(BlockState state, CallbackInfoReturnable<BlockState> cir) {
        if (state.is(ModBlockTags.CANT_BROKEN_ANVIL)) cir.setReturnValue(state);
    }
}
