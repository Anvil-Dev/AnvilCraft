package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Holder.Reference.class)
abstract class HolderReferenceMixin<T> {
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
        ResourceKey<T> key,
        T value,
        Operation<Holder.Reference<T>> original
    ) {
        if (value instanceof ResonatorItem item) {
            return Util.cast(new ResonatorItem.ResonatorHolder(type, Util.cast(owner), Util.cast(key), item));
        } else if (value instanceof MultitoolItem item) {
            return Util.cast(new MultitoolItem.MultitoolHolder(type, Util.cast(owner), Util.cast(key), item));
        } else {
            return original.call(type, owner, key, value);
        }
    }

    @WrapMethod(method = "is(Lnet/minecraft/tags/TagKey;)Z")
    private boolean useOverride(
        TagKey<T> tag,
        Operation<Boolean> original,
        @Share(namespace = AnvilCraft.MOD_ID, value = "stack") LocalRef<ItemStack> stack
    ) {
        ItemStack stored = stack.get();
        // noinspection ConstantValue
        if (stored == null) {
            return original.call(tag);
        }
        if (Util.cast(this) instanceof ResonatorItem.ResonatorHolder holder) {
            return holder.is(stored.get(ModComponents.RESONATE_MODE), Util.cast(tag));
        } else if (Util.cast(this) instanceof MultitoolItem.MultitoolHolder holder) {
            return holder.is(stored.get(ModComponents.MULTITOOL_MODE), Util.cast(tag));
        } else {
            return original.call(tag);
        }
    }
}
