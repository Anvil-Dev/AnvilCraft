package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** 保留方块标签的匹配约束，并使谓词的身份与渲染缓存无关。 */
@Mixin(value = BlockStatePredicate.class, remap = false)
abstract class BlockStatePredicateMixin {
    @Shadow
    @Final
    private HolderSet<Block> blocks;

    @Inject(method = "cannotMatch", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$checkBlockSet(BlockState state, CallbackInfoReturnable<Boolean> callback) {
        // AnvilLib 以空直接集合表示未指定方块；标签与组合集合即使为空，也要检查成员资格。
        if (!(this.blocks instanceof HolderSet.Direct<?>)) {
            callback.setReturnValue(!state.is(this.blocks));
        }
    }

    @Redirect(
        method = {"equals", "hashCode"},
        at = @At(value = "INVOKE", target = "Ldev/anvilcraft/lib/v2/util/predicate/BlockStatePredicate;getStatesCache()Ljava/util/List;")
    )
    private List<BlockState> anvilcraft$excludeRenderCacheFromIdentity(BlockStatePredicate predicate) {
        return List.of();
    }
}
