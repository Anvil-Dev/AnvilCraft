package dev.dubhe.anvilcraft.mixin.tsmm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.saved.trading.TradingStationMessageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FillCommand.class)
public class FillCommandMixin {
    @WrapOperation(
        method = "fillBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/commands/arguments/blocks/BlockInput;"
                     + "place(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)Z"
        )
    )
    private static boolean markPlayerBreak(
        BlockInput instance,
        ServerLevel level,
        BlockPos pos,
        int flags,
        Operation<Boolean> original,
        @Local(argsOnly = true) CommandSourceStack stack
    ) {
        if (stack.getEntity() instanceof ServerPlayer sp) {
            TradingStationMessageManager.get().onPlayerBreak(level, pos, sp);
        }
        return original.call(instance, level, pos, flags);
    }
}
