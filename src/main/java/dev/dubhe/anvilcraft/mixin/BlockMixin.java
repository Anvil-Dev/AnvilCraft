package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.block.INegativeShapeBlock;
import dev.dubhe.anvilcraft.api.injection.block.IBlockExtension;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
    private static ThreadLocal<Object2ByteLinkedOpenHashMap<Block.ShapePairKey>> OCCLUSION_CACHE;

    @Inject(
        method = "shouldRenderFace("
                 + "Lnet/minecraft/world/level/BlockGetter;"
                 + "Lnet/minecraft/core/BlockPos;"
                 + "Lnet/minecraft/world/level/block/state/BlockState;"
                 + "Lnet/minecraft/world/level/block/state/BlockState;"
                 + "Lnet/minecraft/core/Direction;)Z",
        // TODO: HEAD
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;"
                     + "getFaceOcclusionShape(Lnet/minecraft/core/Direction;)"
                     + "Lnet/minecraft/world/phys/shapes/VoxelShape;",
            ordinal = 1
        ),
        cancellable = true
    )
    private static void emberMetalBlockFaceSkip(
        BlockGetter level,
        BlockPos pos,
        BlockState state,
        BlockState neighborState,
        Direction direction,
        CallbackInfoReturnable<Boolean> cir,
        @Local(index = 5) VoxelShape occluder
    ) {
        if (state.getBlock() instanceof INegativeShapeBlock<?> block) {
            boolean b = anvilcraft$NegativeShapeFaceSkip(
                block,
                level,
                pos,
                state,
                neighborState,
                direction,
                occluder
            );
            cir.setReturnValue(b);
        }
    }

    @Unique
    private static boolean anvilcraft$NegativeShapeFaceSkip(
        Predicate<BlockState> predicate,
        BlockGetter level,
        BlockPos pos,
        BlockState state,
        BlockState neighborState,
        Direction direction,
        VoxelShape occluder
    ) {
        VoxelShape shape = state.getFaceOcclusionShape(direction);
        if (shape == Shapes.empty() && !predicate.test(state)) {
            return true;
        }
        Block.ShapePairKey key = new Block.ShapePairKey(shape, occluder);
        Object2ByteLinkedOpenHashMap<Block.ShapePairKey> cache = OCCLUSION_CACHE.get();
        byte cached = cache.getAndMoveToFirst(key);
        if (cached != 127) {
            return cached != 0;
        } else {
            boolean result = Shapes.joinIsNotEmpty(shape, occluder, BooleanOp.ONLY_FIRST);
            if (cache.size() == 256) {
                cache.removeLastByte();
            }

            cache.putAndMoveToFirst(key, (byte) (result ? 1 : 0));
            return result;
        }

    }
}
