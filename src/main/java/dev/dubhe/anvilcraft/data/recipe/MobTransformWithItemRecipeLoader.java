package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

public class MobTransformWithItemRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        MobTransformWithItemRecipe.from(EntityType.ZOMBIE, Items.ANVIL, EntityType.GIANT, ModBlocks.GIANT_ANVIL.asStack())
            .setItemChancePercentagePerItem(5)
            .save(provider);

        MobTransformWithItemRecipe.from(EntityType.ZOMBIE, ModBlocks.MONOLITH_CORE, EntityType.GIANT, ModBlocks.GIANT_MONOLITH_CORE.asStack())
            .setItemChancePercentagePerItem(5)
            .save(provider);

        MobTransformWithItemRecipe.from(EntityType.ZOMBIE, ModBlocks.MONOLITH_LINE, EntityType.GIANT, ModBlocks.GIANT_MONOLITH_LINE.asStack())
            .setItemChancePercentagePerItem(5)
            .save(provider);
    }
}
