package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.block.INegativeShapeBlock;
import dev.dubhe.anvilcraft.api.injection.block.IBlockExtension;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(Block.class)
abstract class BlockMixin implements IBlockExtension {
    @Final
    @Shadow
    private static ThreadLocal<Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey>> OCCLUSION_CACHE;

    @Inject(
        method = "shouldRenderFace",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canOcclude()Z"),
        cancellable = true)
    private static void emberMetalBlockFaceSkip(
        @NotNull BlockState state,
        BlockGetter level,
        BlockPos offset,
        Direction face,
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof INegativeShapeBlock<?> block)
            anvilcraft$NegativeShapeFaceSkip(
                t -> block.getBlockType().isInstance(t.getBlock()),
                state, level, offset, face, pos, cir
            );
    }

    @Unique
    private static void anvilcraft$NegativeShapeFaceSkip(
        Predicate<BlockState> predicate,
        BlockState state,
        @NotNull BlockGetter level,
        BlockPos offset,
        Direction face,
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        BlockState blockstate = level.getBlockState(pos);
        if (blockstate.canOcclude() || predicate.test(blockstate)) {
            Block.BlockStatePairKey blockstatepairkey = new Block.BlockStatePairKey(state, blockstate, face);
            Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> object2bytelinkedopenhashmap =
                OCCLUSION_CACHE.get();
            byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(blockstatepairkey);
            if (b0 != 127) {
                cir.setReturnValue(b0 != 0 || !predicate.test(blockstate));
                return;
            }
            VoxelShape voxelshape = state.getFaceOcclusionShape(level, offset, face);
            if (voxelshape.isEmpty()) {
                cir.setReturnValue(!predicate.test(blockstate));
                return;
            }
            VoxelShape voxelshape1 = blockstate.getFaceOcclusionShape(level, pos, face.getOpposite());
            boolean flag = Shapes.joinIsNotEmpty(voxelshape, voxelshape1, BooleanOp.ONLY_FIRST);
            if (object2bytelinkedopenhashmap.size() == 2048) {
                object2bytelinkedopenhashmap.removeLastByte();
            }
            object2bytelinkedopenhashmap.putAndMoveToFirst(blockstatepairkey, (byte) (flag ? 1 : 0));
            cir.setReturnValue(flag || !predicate.test(blockstate));
            return;
        }
        cir.setReturnValue(true);
    }
}
