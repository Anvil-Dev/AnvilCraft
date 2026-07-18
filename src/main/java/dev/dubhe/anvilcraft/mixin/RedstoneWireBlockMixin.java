package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.tooltip.ITooltipProviderExtension;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements ITooltipProviderExtension {
    @Shadow
    @Final
    public static IntegerProperty POWER;

    @Override
    public List<Component> anvilcraft$getTooltip(BlockState state) {
        final ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.power", state.getValue(POWER)).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Inject(method = "calculateTargetStrength", at = @At("RETURN"), cancellable = true)
    private void anvilcraft$includeUpwardWireSignal(
        Level level, BlockPos pos, CallbackInfoReturnable<Integer> callback
    ) {
        callback.setReturnValue(Math.max(callback.getReturnValue(), RedstoneWireBlock.getUpwardDustSignal(level, pos)));
    }
}
