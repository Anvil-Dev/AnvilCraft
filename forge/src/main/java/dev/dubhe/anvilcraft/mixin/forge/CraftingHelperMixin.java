package dev.dubhe.anvilcraft.mixin.forge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = CraftingHelper.class, remap = false)
abstract class CraftingHelperMixin {
    @Inject(
        method = "getItemStack(Lcom/google/gson/JsonObject;ZZ)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;<init>(Lnet/minecraft/world/level/ItemLike;I)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private static void getItemStack(
        @NotNull JsonObject json, boolean readNbt, boolean disallowsAirInRecipe, CallbackInfoReturnable<ItemStack> cir,
        String itemName, Item item
    ) {
        if (json.has("data")) {
            ItemStack stack = new ItemStack(item, GsonHelper.getAsInt(json, "count", 1));
            String data = GsonHelper.getAsString(json, "data");
            try {
                stack.setTag(TagParser.parseTag(data));
            } catch (CommandSyntaxException e) {
                throw new JsonParseException(e);
            }
            cir.setReturnValue(stack);
        }
    }
}
