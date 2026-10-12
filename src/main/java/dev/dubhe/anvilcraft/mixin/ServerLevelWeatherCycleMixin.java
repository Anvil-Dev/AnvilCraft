package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// 26.1 天气计时器为服务端全局，四个可下雨维度各扣减一次导致天气四倍速。
/// 非主世界维度读到 ADVANCE_WEATHER 为 false 即跳过扣减；插值与降雨包不受影响。
/// 该方法内 GameRules.get 目前只有这一处；expect/require 让上游再加一处时直接失败，
/// 而不是静默把另一个规则的读取也改成 false。
@Mixin(ServerLevel.class)
abstract class ServerLevelWeatherCycleMixin {
    @ModifyExpressionValue(
        method = "advanceWeatherCycle",
        expect = 1,
        require = 1,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
        )
    )
    private Object anvilcraft$advanceWeatherTimersOncePerTick(Object original) {
        ResourceKey<Level> dimension = ((ServerLevel) (Object) this).dimension();
        return Level.OVERWORLD.equals(dimension) ? original : Boolean.FALSE;
    }
}
