package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.EmberBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
abstract class BlockMixin {
    @Final
    @Shadow
    private static ThreadLocal<Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>> OCCLUSION_CACHE;

    @Inject(
            method = "shouldRenderFace",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canOcclude()Z"),
            cancellable = true)
    private static void emberMetalBlockFaceSkip(
            BlockState state,
            BlockGetter level,
            BlockPos offset,
            Direction face,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof EmberBlock) {
            BlockState blockstate = level.getBlockState(pos);
            if (blockstate.canOcclude() || blockstate.getBlock() instanceof EmberBlock) {
                Block.BlockStatePairKey blockstatepairkey = new Block.BlockStatePairKey(state, blockstate, face);
                Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2bytelinkedopenhashmap =
                        OCCLUSION_CACHE.get();
                byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(blockstatepairkey);
                if (b0 != 127) {
                    cir.setReturnValue(b0 != 0 || !(blockstate.getBlock() instanceof EmberBlock));
                    return;
                }
                VoxelShape voxelshape = state.getFaceOcclusionShape(level, offset, face);
                if (voxelshape.isEmpty()) {
                    cir.setReturnValue(!(blockstate.getBlock() instanceof EmberBlock));
                    return;
                }
                VoxelShape voxelshape1 = blockstate.getFaceOcclusionShape(level, pos, face.getOpposite());
                boolean flag = Shapes.joinIsNotEmpty(voxelshape, voxelshape1, BooleanOp.ONLY_FIRST);
                if (object2bytelinkedopenhashmap.size() == 2048) {
                    object2bytelinkedopenhashmap.removeLastByte();
                }
                object2bytelinkedopenhashmap.putAndMoveToFirst(blockstatepairkey, (byte) (flag ? 1 : 0));
                cir.setReturnValue(flag || !(blockstate.getBlock() instanceof EmberBlock));
                return;
            }
            cir.setReturnValue(true);
        }
    }
}
