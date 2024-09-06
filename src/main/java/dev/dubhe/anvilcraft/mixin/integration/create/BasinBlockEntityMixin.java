package dev.dubhe.anvilcraft.mixin.integration.create;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasinBlockEntity.class)
public class BasinBlockEntityMixin {

    @Inject(method = "getHeatLevelOf", at = @At("HEAD"), cancellable = true)
    private static void getHeatLevelOfHeater(BlockState state, CallbackInfoReturnable<BlazeBurnerBlock.HeatLevel> cir) {
        if (state.is(ModBlocks.HEATER.get())) {
            cir.setReturnValue(state.getValue(HeaterBlock.OVERLOAD)
                    ? BlazeBurnerBlock.HeatLevel.NONE
                    : BlazeBurnerBlock.HeatLevel.KINDLED);
            cir.cancel();
        }
    }
}
