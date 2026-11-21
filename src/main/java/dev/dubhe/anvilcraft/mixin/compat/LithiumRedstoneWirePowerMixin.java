package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.lithium.common.block.redstone.RedstoneWirePowerCalculations", remap = false)
abstract class LithiumRedstoneWirePowerMixin {
    @ModifyReturnValue(method = "getNeighborSignal", at = @At("RETURN"))
    private static int anvilcraft$includeUpwardWireSignal(
        int original, Block wireBlock, RedstoneWireEvaluator evaluator, Level level, BlockPos pos,
        boolean ignoreNonWires, boolean ignoreWires
    ) {
        if (ignoreNonWires || original == 15) {
            return original;
        }
        return Math.max(original, RedstoneWireBlock.getUpwardDustSignal(level, pos));
    }
}
