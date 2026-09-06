package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class MDJewelCraftingRecipeComponent extends MDRecipeComponent {
    public static final ResourceLocation TEXTURE = AnvilCraft.of("textures/gui/ageratum/jewelcrafting_table.png");

    private final ItemStack result;
    private final List<Object2IntMap.Entry<Ingredient>> ingredients;

    public MDJewelCraftingRecipeComponent(JewelCraftingRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 142, 62, enableAlignCenter);
        result = recipe.result;
        ingredients = recipe.getMergedIngredients();
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        // 贴图槽位（142x62）：上排 source 槽 (64,7)，下排 4 个材料槽 (7/26/45/64, 35)，
        // 右侧 result 槽 (116,35)。物品图标绘制在槽内 (槽位起点 +1)。
        for (int i = 0; i < Math.min(ingredients.size(), 4); i++) {
            AgeratumUtil.renderItemWithoutSlot(context, ingredients.get(i), mouseX, mouseY, 8 + i * AgeratumUtil.SLOT_SIZE, 36);
        }
        AgeratumUtil.renderItemWithoutSlot(context, result, mouseX, mouseY, 117, 36);
    }

}
