package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeResetManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
abstract class LevelMixin {
    @ModifyReturnValue(method = "getSkyDarken", at = @At("RETURN"))
    private int anvilcraft$addOverworldLikeEclipseDarken(int original) {
        return OverworldLikeResetManager.modifySkyDarken((Level) (Object) this, original);
    }
}
