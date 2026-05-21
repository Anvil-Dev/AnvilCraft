package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Holder.Reference.class)
abstract class HolderReferenceMixin {
    @WrapOperation(
        method = "createIntrusive",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/core/Holder$Reference$Type;"
                     + "Lnet/minecraft/core/HolderOwner;"
                     + "Lnet/minecraft/resources/ResourceKey;"
                     + "Ljava/lang/Object;)Lnet/minecraft/core/Holder$Reference;"
        )
    )
    private static <T> Holder.Reference<T> override(
        Holder.Reference.Type type,
        HolderOwner<T> owner,
        ResourceKey<T> key, T value,
        Operation<Holder.Reference<T>> original
    ) {
        if (value instanceof ResonatorItem resonator) {
            return Util.cast(new ResonatorItem.ResonatorHolder(type, Util.cast(owner), Util.cast(key), resonator));
        } else if (value instanceof MultitoolItem multitoolItem) {
            return Util.cast(new MultitoolItem.MultitoolHolder(type, Util.cast(owner), Util.cast(key), multitoolItem));
        } else {
            return original.call(type, owner, key, value);
        }
    }
}
