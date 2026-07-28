package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.item.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
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
    private static <T> Holder.Reference<T> createModeSpecificItemHolder(
        Holder.Reference.Type type,
        HolderOwner<T> owner,
        ResourceKey<T> key, T value,
        Operation<Holder.Reference<T>> original
    ) {
        return switch (value) {
            case HeavyHalberdItem heavyHalberd ->
            // noinspection unchecked
            (Holder.Reference<T>) new HeavyHalberdItem.HeavyHalberdHolder(
                    type,
                    (HolderOwner<Item>) owner,
                    (ResourceKey<Item>) key,
                    heavyHalberd
                );
            case ResonatorItem resonator ->
            // noinspection unchecked
            (Holder.Reference<T>) new ResonatorItem.ResonatorHolder(
                    type,
                    (HolderOwner<Item>) owner,
                    (ResourceKey<Item>) key,
                    resonator
                );
            case MultitoolItem multitoolItem ->
            // noinspection unchecked
            (Holder.Reference<T>) new MultitoolItem.MultitoolHolder(
                    type,
                    (HolderOwner<Item>) owner,
                    (ResourceKey<Item>) key,
                    multitoolItem
                );
            default -> original.call(type, owner, key, value);
        };
    }
}
