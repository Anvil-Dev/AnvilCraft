package dev.dubhe.anvilcraft.integration.jei.handlers;

import dev.dubhe.anvilcraft.client.gui.screen.TranscendenceSmithingScreen;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** JEI 拖入只添加通过配方校验的模板，不生成真实物品。 */
public final class SmithingTemplateGhostHandler implements IGhostIngredientHandler<TranscendenceSmithingScreen> {
    @Override
    public <T> List<Target<T>> getTargetsTyped(TranscendenceSmithingScreen screen, ITypedIngredient<T> ingredient, boolean doStart) {
        ItemStack stack = ingredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        if (!screen.getMenu().canAddTemplate(stack)) {
            return List.of();
        }
        return List.of(new Target<>() {
            @Override
            public Rect2i getArea() {
                return screen.getTemplateDropArea();
            }

            @Override
            public void accept(T ingredient) {
                if (ingredient instanceof ItemStack item) {
                    screen.addTemplate(item);
                }
            }
        });
    }

    @Override
    public void onComplete() {
    }
}
