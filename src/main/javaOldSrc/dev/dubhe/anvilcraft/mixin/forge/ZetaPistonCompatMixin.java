package dev.dubhe.anvilcraft.mixin.forge;

import dev.dubhe.anvilcraft.event.PistonMoveBlockListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.violetmoon.zeta.piston.ZetaPistonStructureResolver;

import java.util.ArrayList;
import java.util.List;


@Mixin(ZetaPistonStructureResolver.class)
public class ZetaPistonCompatMixin {

    @Shadow(remap = false)
    @Final
    private List<BlockPos> toMove;

    @Inject(method = "resolve", at = @At("RETURN"))
    private void onPistonResolve(CallbackInfoReturnable<Boolean> cir) {
        Level level = ((PistonStructorResolverAccessor) this).getLevel();
        if (level.isClientSide()) {
            return;
        }
        if (!cir.getReturnValue()) return;
        List<BlockPos> toPushBlocks = new ArrayList<>(toMove);
        PistonMoveBlockListener.onPistonMoveBlocks(level, toPushBlocks);
    }
}
