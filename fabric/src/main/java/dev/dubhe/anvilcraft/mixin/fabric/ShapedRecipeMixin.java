package dev.dubhe.anvilcraft.mixin.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShapedRecipe.class)
abstract class ShapedRecipeMixin {
    @ModifyExpressionValue(method = "itemStackFromJson",
        at = @At(value = "INVOKE", target = "Lcom/google/gson/JsonObject;has(Ljava/lang/String;)Z", remap = false))
    private static boolean validNbt(boolean original) {
        return false;
    }

    @ModifyExpressionValue(method = "itemStackFromJson",
        at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack readNbt(ItemStack original, @NotNull JsonObject stackObject) {
        if (stackObject.has("data")) {
            String data = GsonHelper.getAsString(stackObject, "data");
            try {
                original.setTag(TagParser.parseTag(data));
            } catch (CommandSyntaxException e) {
                throw new JsonParseException(e);
            }
        }
        return original;
    }
}
