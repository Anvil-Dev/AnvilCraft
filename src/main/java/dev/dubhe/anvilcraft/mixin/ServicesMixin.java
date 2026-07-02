package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Services.class)
public class ServicesMixin {
    @WrapMethod(method = "profileCache")
    private GameProfileCache setToUtilWhenNotSet(Operation<GameProfileCache> original) {
        GameProfileCache cache = original.call();
        if (dev.anvilcraft.lib.v2.util.Util.isClient() && Util.clientCache == null) {
            Util.clientCache = cache;
        }
        return cache;
    }
}
