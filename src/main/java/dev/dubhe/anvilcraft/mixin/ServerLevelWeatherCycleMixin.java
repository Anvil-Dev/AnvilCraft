package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public class ServerLevelWeatherCycleMixin {
    @ModifyExpressionValue(
        method = "advanceWeatherCycle",
        expect = 1,
        require = 1,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/gamerules/GameRules;get"
                     + "(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
        )
    )
    private Object anvilcraft$advanceWeatherTimersOncePerTick(Object original) {
        ResourceKey<Level> dimension = ((ServerLevel) (Object) this).dimension();
        return Level.OVERWORLD.equals(dimension) ? original : Boolean.FALSE;
    }
}
