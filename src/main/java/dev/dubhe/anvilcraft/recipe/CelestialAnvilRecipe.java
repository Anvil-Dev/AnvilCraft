package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyPresets;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/** 用黑洞或白洞方块合成对应天体预设，合成结果在每次放置时独立生成。 */
public final class CelestialAnvilRecipe extends CustomRecipe {
    public static final RecipeSerializer<CelestialAnvilRecipe> SERIALIZER =
        new SimpleCraftingRecipeSerializer<>(CelestialAnvilRecipe::new);

    public CelestialAnvilRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int anvils = 0;
        int holes = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem())) {
                anvils++;
            } else if (stack.is(ModBlocks.BLACK_HOLE.asItem()) || stack.is(ModBlocks.WHITE_HOLE.asItem())) {
                holes++;
            } else {
                return false;
            }
        }
        return anvils == 1 && holes == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        boolean white = input.items().stream().anyMatch(stack -> stack.is(ModBlocks.WHITE_HOLE.asItem()));
        return CelestialBodyPresets.stack(white ? CelestialBodyPresets.WHITE_HOLE : CelestialBodyPresets.BLACK_HOLE,
            CelestialBodyPresets.NAME_PREFIX + (white ? "special.white_hole" : "black_hole"));
    }

    @Override
    public RecipeSerializer<CelestialAnvilRecipe> getSerializer() {
        return SERIALIZER;
    }
}
