package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// 26.1 起天气计时器由 `MinecraftServer.weatherData` 全局持有，而 `ServerLevel#advanceWeatherCycle()`
/// 会对所有 `canHaveWeather()` 为真的维度各扣减一次同一份 `rainTime`/`thunderTime`。
/// 铁砧工艺额外提供了 mun、void_planet 与 overworld_like 三个可下雨维度，共享计时器因此被扣减四次。
///
/// <p>这里只把 `ADVANCE_WEATHER` 规则读取改成对非主世界维度返回 `false`，跳过计时器推进；
/// 该方法余下的 `rainLevel`/`thunderLevel` 插值与降雨数据包仍按维度各自执行，雨照常渲染。</p>
@Mixin(ServerLevel.class)
abstract class ServerLevelWeatherCycleMixin {
    @ModifyExpressionValue(
        method = "advanceWeatherCycle",
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
