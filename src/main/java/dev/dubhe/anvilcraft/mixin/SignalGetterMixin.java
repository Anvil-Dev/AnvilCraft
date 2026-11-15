package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 让红石元件从输入面读取绕过支撑方块边缘的导线信号。 */
@Mixin(SignalGetter.class)
public interface SignalGetterMixin {
    @ModifyReturnValue(method = "getSignal", at = @At("RETURN"))
    private int anvilcraft$includeCornerWireSignal(int original, BlockPos pos, Direction direction) {
        if (original >= 15) {
            return original;
        }
        return Math.max(original, RedstoneWireBlock.getCornerSignal((SignalGetter) this, pos, direction));
    }

    @ModifyReturnValue(method = "getControlInputSignal", at = @At("RETURN"))
    private int anvilcraft$includeCornerControlSignal(int original, BlockPos pos, Direction direction, boolean onlyDiodes) {
        if (onlyDiodes || original >= 15) {
            return original;
        }
        return Math.max(original, RedstoneWireBlock.getCornerSignal((SignalGetter) this, pos, direction));
    }
}
