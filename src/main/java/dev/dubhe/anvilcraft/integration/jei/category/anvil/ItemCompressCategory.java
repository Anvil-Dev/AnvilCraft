package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemSubPredicates;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.item.property.predicate.ItemSavedEntityPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class ItemCompressCategory extends AbstractProgressCategory<ItemCompressRecipe> {
    public ItemCompressCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), Blocks.CAULDRON.defaultBlockState()),
            Component.translatable("gui.anvilcraft.category.item_compress")
        );
    }

    @Override
    public RecipeType<RecipeHolder<ItemCompressRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.ITEM_COMPRESS;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<ItemCompressRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        ItemCompressRecipe recipe = recipeHolder.value();
        List<ItemIngredientPredicate> inputs = recipe.getInputItems();
        int size = inputs.size();
        if (size > 0) {
            int cols = (int) Math.ceil(Math.sqrt(size));
            int rows = Math.ceilDiv(size, cols);
            int startX = JeiSlotUtil.INPUT_X - (cols - 1) * JeiSlotUtil.OFFSET / 2;
            int startY = JeiSlotUtil.DEFAULT_Y - (rows - 1) * JeiSlotUtil.OFFSET / 2;
            for (int i = 0; i < size; i++) {
                int x = startX + (i % cols) * JeiSlotUtil.OFFSET;
                int y = startY + (i / cols) * JeiSlotUtil.OFFSET;
                ItemIngredientPredicate input = inputs.get(i);
                // getItems() 只保留 items/count/components，子谓词（如树脂块需封存苦力怕）无法表达，
                // 这里按配方实际携带的子谓词补充示例物品与说明，而不是按配方 id 硬编码。
                ItemStack sample = savedEntitySample(input);
                if (sample == null) {
                    builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .addIngredients(Ingredient.of(input.getItems()));
                    continue;
                }
                boolean powered = isPoweredCreeperSample(input);
                builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStack(sample)
                    .addRichTooltipCallback((slotView, tooltip) ->
                        tooltip.add(Component.translatable(powered
                            ? "gui.anvilcraft.category.item_compress.supercapacitor.resin"
                            : "gui.anvilcraft.category.item_compress.supercapacitor_empty.resin")));
            }
        }
        JeiItemUtil.addDefaultOutputSlots(builder, recipe.getResultItems());
    }

    @Override
    public void draw(
        RecipeHolder<ItemCompressRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        final ItemCompressRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics, Blocks.CAULDRON.defaultBlockState(), 81, 40, 10, 12, RenderSupport.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 30);
        arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawDefaultInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
        if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<ItemCompressRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_COMPRESS_TYPE.get())
        );
        recipes.add(new RecipeHolder<>(
            AnvilCraft.of("item_compress/supercapacitor"),
            specialSupercapacitorRecipe()
        ));
        registration.addRecipes(
            AnvilCraftJeiPlugin.ITEM_COMPRESS, recipes);
    }

    private static ItemCompressRecipe specialSupercapacitorRecipe() {
        return ItemCompressRecipe.builder()
            .requires(ModItemTags.IRON_PLATES, 2)
            .requires(ItemIngredientPredicate.Builder.item().of(resinWithCreeper(true)).build())
            // 数据配方中充能超电容是 in_world 配方（铁砧落下 50% 概率爆炸 / 50% 产出），
            // 该 JEI 分类无法直接收集，这里以概率结果表达
            .result(ModItems.SUPER_CAPACITOR, 0.5f)
            .buildRecipe();
    }

    /**
     * 若配方输入要求树脂块封存指定生物（子谓词无法由 JEI 通用物品槽表达），
     * 构造一个带封存实体组件的示例物品；否则返回 {@code null} 走通用渲染。
     */
    private static @Nullable ItemStack savedEntitySample(ItemIngredientPredicate input) {
        ItemSubPredicate sub = input.subPredicates().get(ModItemSubPredicates.SAVED_ENTITY.get());
        if (!(sub instanceof ItemSavedEntityPredicate savedEntity)) return null;
        if (savedEntity.entitys().isEmpty()) return null;
        EntityType<?> type = savedEntity.entitys().get().iterator().next().value();
        ItemStack[] items = input.getItems();
        if (items.length == 0) return null;
        ItemStack sample = items[0].copy();
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", EntityType.getKey(type).toString());
        boolean powered = savedEntity.predicates().stream().anyMatch(predicate ->
            predicate.tagKeyPath().equals("powered")
                && predicate.requirement() == NumericTagValuePredicate.ValueFunction.GREATER_OR_EQUAL
                && predicate.expected() >= 1);
        entityTag.putBoolean("powered", powered);
        sample.set(ModComponents.SAVED_ENTITY, new SavedEntity(entityTag, true));
        sample.setCount(items[0].getCount());
        return sample;
    }

    private static boolean isPoweredCreeperSample(ItemIngredientPredicate input) {
        ItemSubPredicate sub = input.subPredicates().get(ModItemSubPredicates.SAVED_ENTITY.get());
        if (!(sub instanceof ItemSavedEntityPredicate savedEntity)) return false;
        return savedEntity.predicates().stream().anyMatch(predicate ->
            predicate.tagKeyPath().equals("powered")
                && predicate.requirement() == NumericTagValuePredicate.ValueFunction.GREATER_OR_EQUAL
                && predicate.expected() >= 1);
    }

    private static ItemStack resinWithCreeper(boolean powered) {
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", "minecraft:creeper");
        if (powered) entityTag.putBoolean("powered", true);
        ItemStack resin = ModBlocks.RESIN_BLOCK.asStack();
        resin.set(ModComponents.SAVED_ENTITY, new SavedEntity(entityTag, true));
        return resin;
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilCauldronCatalysts(registration, AnvilCraftJeiPlugin.ITEM_COMPRESS);
    }
}
