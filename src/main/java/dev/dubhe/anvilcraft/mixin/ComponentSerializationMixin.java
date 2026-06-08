package dev.dubhe.anvilcraft.mixin;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.component.TranslatableContents;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ComponentSerialization.class)
public class ComponentSerializationMixin {
    @Inject(method = "bootstrap", at = @At("HEAD"))
    private static void register(
        ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ComponentContents>> contentTypes,
        CallbackInfo ci
    ) {
        contentTypes.put(AnvilCraft.of("translatable").toString(), TranslatableContents.MAP_CODEC);
    }
}
