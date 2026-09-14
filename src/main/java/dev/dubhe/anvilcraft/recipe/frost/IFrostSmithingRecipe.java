package dev.dubhe.anvilcraft.recipe.frost;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import javax.annotation.Nullable;

public interface IFrostSmithingRecipe extends Recipe<FrostSmithingRecipeInput> {
    int TEMPLATE_SLOT = 0;
    int INPUT_SLOT = 1;
    int MATERIAL_SLOT = 2;

    @Override
    default boolean matches(FrostSmithingRecipeInput input, Level level) {
        return this.isTemplate(input.template())
               && this.isInput(input.input())
               && !this.options(input).isEmpty();
    }

    boolean isTemplate(ItemStack template);

    /**
     * 装备槽（工具、武器、盔甲、重型物品等）能否放入该物品。
     */
    boolean isInput(ItemStack input);

    /**
     * 该装备在当前模板下可以锻造出的全部结果，按箭头切换顺序排列。
     */
    @Unmodifiable List<FrostSmithingOption> options(ItemStack input);

    /**
     * 材料槽已满足要求的锻造结果，按箭头切换顺序排列。
     */
    default @Unmodifiable List<FrostSmithingOption> options(FrostSmithingRecipeInput input) {
        return this.options(input.input())
            .stream()
            .filter(option -> option.isAvailable(this, input))
            .toList();
    }

    /**
     * 该配方可能产出的全部结果，用于配方书与 JEI 展示。
     */
    @Unmodifiable List<RecipeResult> results();

    /**
     * 可能放入装备槽的物品，用于装备槽为空时判断材料是否合适。
     */
    @Unmodifiable List<ItemStack> possibleInputs();

    /**
     * 材料槽中该物品能否作为材料放入。
     */
    default boolean acceptsMaterial(FrostSmithingRecipeInput input, ItemStack material) {
        return !this.options(new FrostSmithingRecipeInput(input.template(), input.input(), material)).isEmpty();
    }

    @Deprecated
    @Override
    default ItemStack assemble(FrostSmithingRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    default ItemStack assemble(int selected, FrostSmithingRecipeInput inputting, Level level) {
        RecipeResult input = this.options(inputting).get(selected).result();
        ItemStack result = inputting.input().transmuteCopy(input.result());
        if (input.result().components().keySet().contains(DataComponents.TOOL)) {
            result.set(DataComponents.TOOL, input.result().components().get(DataComponents.TOOL));
        }
        if (input.result().components().keySet().contains(DataComponents.ATTRIBUTE_MODIFIERS)) {
            result.set(DataComponents.ATTRIBUTE_MODIFIERS, input.result().components().get(DataComponents.ATTRIBUTE_MODIFIERS));
        }
        var builder = ResultContext.builder(level.registryAccess(), level.getRandom(), result)
            .slot(RecipeInputSlot.TEMPLATE, inputting.template())
            .slot(RecipeInputSlot.MATERIAL, inputting.material())
            .input(0, inputting.input());
        return input.getResult(builder.build());
    }

    /**
     * 取出指定结果时需要消耗的材料数量。
     */
    default int materialCost(FrostSmithingRecipeInput input, int selected) {
        List<FrostSmithingOption> options = this.options(input);
        if (selected < 0 || selected >= options.size()) return 0;
        return options.get(selected).cost(this, input);
    }

    @Override
    default ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.results().getFirst().result().getDefaultInstance();
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    abstract class BaseBuilder<B extends BaseBuilder<B, R>, R extends IFrostSmithingRecipe>
        extends AbstractRecipeBuilder<R> {
        protected @Nullable ItemIngredientPredicate template;

        protected abstract B getThis();

        public B template(ItemIngredientPredicate template) {
            this.template = template;
            return this.getThis();
        }

        public B template(ItemIngredientPredicate.Builder templateBuilder) {
            return this.template(templateBuilder.build());
        }

        public B template(int count, ItemStack template) {
            return this.template(
                ItemIngredientPredicate.of(template.getItem())
                    .withCount(count)
                    .hasComponents(DataComponentPredicate.allOf(template.getComponents()))
            );
        }

        public B template(ItemStack template) {
            return this.template(1, template);
        }

        public B template(int count, ItemLike... templates) {
            return this.template(ItemIngredientPredicate.of(templates).withCount(count));
        }

        public B template(ItemLike... templates) {
            return this.template(1, templates);
        }

        public B template(int count, TagKey<Item> templateTag) {
            return this.template(ItemIngredientPredicate.of(templateTag).withCount(count));
        }

        public B template(TagKey<Item> templateTag) {
            return this.template(1, templateTag);
        }

        @Deprecated
        @Override
        public void save(RecipeOutput output) {
            this.save(output, BuiltInRegistries.ITEM.getKey(this.getResult()).withPrefix(this.getType() + "/"));
        }
    }
}
