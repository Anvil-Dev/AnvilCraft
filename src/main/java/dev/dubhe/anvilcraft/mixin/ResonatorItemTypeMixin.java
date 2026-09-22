package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import net.minecraft.core.HolderSet;
import net.minecraft.core.TypedInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TypedInstance.class)
interface ResonatorItemTypeMixin {
    @ModifyReturnValue(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"))
    private boolean anvilcraft$resonatorModeTag(boolean original, TagKey<?> tag) {
        return original && (!((Object) this instanceof ItemInstance stack) || !(stack.typeHolder().value() instanceof ResonatorItem)
            || ResonatorItem.allowsToolTag(ResonatorItem.getMode(stack), tag));
    }

    @ModifyReturnValue(method = "is(Lnet/minecraft/core/HolderSet;)Z", at = @At("RETURN"))
    private boolean anvilcraft$resonatorModeSet(boolean original, HolderSet<?> set) {
        return original && (!(set instanceof HolderSet.Named<?> named) || !((Object) this instanceof ItemInstance stack)
            || !(stack.typeHolder().value() instanceof ResonatorItem)
            || ResonatorItem.allowsToolTag(ResonatorItem.getMode(stack), named.key()));
    }
}
