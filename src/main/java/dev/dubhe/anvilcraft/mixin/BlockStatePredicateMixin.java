package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlockStatePredicate.class, remap = false)
public abstract class BlockStatePredicateMixin {
    @Shadow
    @Final
    private HolderSet<Block> blocks;

    /** 无方块限制的谓词使用空的直接集合，显式指定的标签为空时应当拒绝所有方块。 */
    @ModifyReturnValue(method = "cannotMatch", at = @At("RETURN"))
    private boolean anvilcraft$rejectEmptyBlockSet(boolean original) {
        return original || !(this.blocks instanceof HolderSet.Direct<?>) && this.blocks.size() == 0;
    }
}
