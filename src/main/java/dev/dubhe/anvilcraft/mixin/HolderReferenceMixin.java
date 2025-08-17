package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
                + "Ljava/lang/Object;)Lnet/minecraft/core/Holder$Reference;"))
    private static <T> Holder.Reference<T> createForResonator(
        Holder.Reference.Type type,
        HolderOwner<T> owner,
        ResourceKey<T> key, T value,
        Operation<Holder.Reference<T>> original
    ) {
        if (value instanceof ResonatorItem resonator) {
            //noinspection unchecked
            return (Holder.Reference<T>) new ResonatorItem.ResonatorHolder(type, (HolderOwner<Item>) owner, (ResourceKey<Item>) key, resonator);
        } else if (value instanceof MultitoolItem multitoolItem) {
            //noinspection unchecked
            return (Holder.Reference<T>) new MultitoolItem.MultitoolHolder(type, (HolderOwner<Item>) owner, (ResourceKey<Item>) key, multitoolItem);
        } else {
            return original.call(type, owner, key, value);
        }
    }
}
