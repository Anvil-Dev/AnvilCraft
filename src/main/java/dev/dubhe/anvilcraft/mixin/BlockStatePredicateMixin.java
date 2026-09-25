package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** 谓词身份只取决于匹配条件，不能因比较渲染缓存而在数据生成时解引用标签。 */
@Mixin(value = BlockStatePredicate.class, remap = false)
abstract class BlockStatePredicateMixin {
    @Redirect(
        method = {"equals", "hashCode"},
        at = @At(value = "INVOKE", target = "Ldev/anvilcraft/lib/v2/util/predicate/BlockStatePredicate;getStatesCache()Ljava/util/List;")
    )
    private List<BlockState> anvilcraft$excludeRenderCacheFromIdentity(BlockStatePredicate predicate) {
        return List.of();
    }
}
