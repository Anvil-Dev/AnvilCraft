package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class NeutroniumChargingRecipe extends ShapedRecipe {
    public NeutroniumChargingRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.result, recipe.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        for (ItemStack source : input.items()) {
            if (!source.is(ModItemTags.UNCHARGED_NEUTRONIUM_INGOTS)) continue;
            ItemEnchantments enchantments = source.get(DataComponents.ENCHANTMENTS);
            if (enchantments != null) result.set(DataComponents.ENCHANTMENTS, enchantments);
            break;
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.NEUTRONIUM_CHARGING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<NeutroniumChargingRecipe> {
        private static final MapCodec<NeutroniumChargingRecipe> CODEC =
            ShapedRecipe.Serializer.CODEC.xmap(NeutroniumChargingRecipe::new, recipe -> recipe);
        private static final StreamCodec<RegistryFriendlyByteBuf, NeutroniumChargingRecipe> STREAM_CODEC =
            ShapedRecipe.Serializer.STREAM_CODEC.map(NeutroniumChargingRecipe::new, recipe -> recipe);

        @Override
        public MapCodec<NeutroniumChargingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NeutroniumChargingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
