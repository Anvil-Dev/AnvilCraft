package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.resources.Identifier;

public class MDEnergyWeaponMakeRecipeComponent extends MDRecipeComponent {
    public static final Identifier TEXTURE = AnvilCraft.of("textures/gui/ageratum/128back.png");

    private final EnergyWeaponMakeRecipe recipe;

    public MDEnergyWeaponMakeRecipeComponent(EnergyWeaponMakeRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 128, 64, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        AgeratumUtil.renderItems(context, this.recipe.ingredients(), mouseX, mouseY, 30, 24);
        AgeratumUtil.renderItemWithoutSlot(context, ModItems.ENERGY_WEAPON_PLATFORM.asStack(), mouseX, mouseY, 100, 10);
        AgeratumUtil.renderArrow(context.graphics(), 66, 26);
        AgeratumUtil.renderItem(context, this.recipe.result(), mouseX, mouseY, 100, 34);
    }
}
