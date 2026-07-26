package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class NeutronIrradiationCategory extends AbstractLiquidCategory<NeutronIrradiationRecipe> {
    private final IDrawable explosion;

    public NeutronIrradiationCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                ModBlocks.NEUTRON_IRRADIATOR
                    .get()
                    .defaultBlockState()
            ),
            Component.translatable("gui.anvilcraft.category.neutron_irradiation")
        );
        explosion = JeiRenderHelper.getExplosion(helper);
    }

    @Override
    public void draw(
        RecipeHolder<NeutronIrradiationRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        if (isExplosionRecipe(recipeHolder)) {
            this.explosion.draw(graphics, 124, 16);
        }
        super.draw(recipeHolder, recipeSlotsView, graphics, mouseX, mouseY);
    }

    @Override
    public IRecipeHolderType<NeutronIrradiationRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.NEUTRON_IRRADIATION;
    }

    @Override
    protected BlockState getProcessBlock() {
        return ModBlocks.NEUTRON_IRRADIATOR
            .get()
            .defaultBlockState();
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<NeutronIrradiationRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        if (isExplosionRecipe(recipeHolder)
            && mouseX >= 120 && mouseX <= 156
            && mouseY >= 12 && mouseY <= 48) {
            tooltip.add(Component.translatable("gui.anvilcraft.category.neutron_irradiation.explosion"));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        super.registerRecipeCatalysts(registration);
        registration.addCraftingStation(getRecipeType(), ModBlocks.NEUTRON_IRRADIATOR);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<NeutronIrradiationRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.NEUTRON_IRRADIATION.get())
        );
        recipes.add(new RecipeHolder<>(
            ResourceKey.create(Registries.RECIPE, AnvilCraft.of("neutron_irradiation/uranium_block_explosion")),
            NeutronIrradiationRecipe.builder().requires(ModBlocks.URANIUM_BLOCK).buildRecipe()
        ));
        recipes.add(new RecipeHolder<>(
            ResourceKey.create(Registries.RECIPE, AnvilCraft.of("neutron_irradiation/plutonium_block_explosion")),
            NeutronIrradiationRecipe.builder().requires(ModBlocks.PLUTONIUM_BLOCK).buildRecipe()
        ));
        registration.addRecipes(
            AnvilCraftJeiPlugin.NEUTRON_IRRADIATION,
            recipes
        );
    }

    private static boolean isExplosionRecipe(RecipeHolder<NeutronIrradiationRecipe> recipeHolder) {
        return recipeHolder.id().identifier().getPath().endsWith("_block_explosion");
    }
}
