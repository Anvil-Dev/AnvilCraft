package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractProgressCategory<T extends AbstractProcessRecipe<?>> implements IRecipeCategory<RecipeHolder<T>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    protected final IDrawable icon;
    protected final IDrawable slot;
    protected final Component title;
    protected final ITickTimer timer;

    protected final IDrawable arrowIn;
    protected final IDrawable arrowOut;

    public AbstractProgressCategory(IGuiHelper helper, IDrawable icon, Component title) {
        this.icon = icon;
        this.slot = helper.getSlotDrawable();
        this.title = title;
        this.timer = helper.createTickTimer(30, 60, true);

        this.arrowIn = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 31, 16, 8);
        this.arrowOut = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 40, 16, 10);
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    protected List<ChanceItemStack> getResults(T recipe) {
        List<ChanceItemStack> results = new ArrayList<>(recipe.getResultItems());
        Object2IntMap<Item> remains = new Object2IntArrayMap<>();
        for (ItemIngredientPredicate ingredient : recipe.getInputItems()) {
            for (ItemStack stack : ingredient.getItems()) {
                if (stack.hasCraftingRemainingItem()) {
                    ItemStack remain = stack.getCraftingRemainingItem();
                    remains.mergeInt(remain.getItem(), remain.getCount(), Integer::sum);
                }
            }
        }
        remains.object2IntEntrySet().forEach(entry ->
            results.add(ChanceItemStack.of(new ItemStack(entry.getKey(), entry.getIntValue()), 1))
        );
        return results;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> recipeHolder, IFocusGroup focuses) {
        T recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        JeiSlotUtil.addOutputSlots(builder, this.getResults(recipe));
    }
}
