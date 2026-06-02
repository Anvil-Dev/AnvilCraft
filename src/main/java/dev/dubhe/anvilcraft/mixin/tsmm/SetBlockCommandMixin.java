package dev.dubhe.anvilcraft.mixin.tsmm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.saved.trading.TradingStationMessageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SetBlockCommand.class)
public class SetBlockCommandMixin {
    @Unique
    private static final ThreadLocal<Boolean> IS_BROKEN = ThreadLocal.withInitial(() -> false);

    @WrapOperation(
        method = "setBlock",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z")
    )
    private static boolean markPlayerBreak(
        ServerLevel instance,
        BlockPos pos,
        boolean b,
        Operation<Boolean> original,
        @Local(argsOnly = true) CommandSourceStack stack
    ) {
        if (stack.getEntity() instanceof ServerPlayer sp) {
            TradingStationMessageManager.get().onPlayerBreak(instance, pos, sp);
            IS_BROKEN.set(true);
        }
        return original.call(instance, pos, b);
    }

    @WrapOperation(
        method = "setBlock",
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
        if (IS_BROKEN.get() && stack.getEntity() instanceof ServerPlayer sp) {
            TradingStationMessageManager.get().onPlayerBreak(level, pos, sp);
        }
        IS_BROKEN.remove();
        return original.call(instance, level, pos, flags);
    }
}
