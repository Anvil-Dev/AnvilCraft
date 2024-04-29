package dev.dubhe.anvilcraft.mixin.forge;

import dev.dubhe.anvilcraft.event.PistonMoveBlockListener;
import dev.dubhe.anvilcraft.mixin.forge.accessor.PistonStructorResolverAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.violetmoon.zeta.piston.ZetaPistonStructureResolver;

import java.util.ArrayList;
import java.util.List;


@Mixin(ZetaPistonStructureResolver.class)
public abstract class ZetaPistonCompatMixin extends PistonStructureResolver {

    public ZetaPistonCompatMixin(Level level, BlockPos pistonPos, Direction pistonDirection, boolean extending) {
        super(level, pistonPos, pistonDirection, extending);
    }

    @Inject(method = "resolve", at = @At("RETURN"))
    private void onPistonResolve(CallbackInfoReturnable<Boolean> cir) {
        Level level = ((PistonStructorResolverAccessor) this).getLevel();
        final List<BlockPos> toPush = ((PistonStructorResolverAccessor) this).getToPush();
        if (level.isClientSide()) {
            return;
        }
        if (!cir.getReturnValue()) return;
        List<BlockPos> toPushBlocks = new ArrayList<>(toPush);
        PistonMoveBlockListener.onPistonMoveBlocks(level, toPushBlocks);
    }
}
