package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.anvilcraft.lib.recipe.data.advancement.predicate.item.NotPredicate;
import dev.anvilcraft.lib.recipe.init.LibItemSubPredicates;
import dev.anvilcraft.lib.recipe.outcome.ProduceExplosion;
import dev.anvilcraft.lib.recipe.outcome.SpawnItem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemSubPredicates;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.item.property.predicate.ItemSavedEntityPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;

public class ItemCompressRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ItemCompressRecipe.builder()
            .requires(Items.BONE, 3)
            .result(new ItemStack(Items.BONE_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItemTags.CREAM, 4)
            .requires(Items.SUGAR)
            .result(ModBlocks.CREAM_BLOCK)
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItemTags.CREAM, 4)
            .requires(Items.SUGAR)
            .requires(Items.SWEET_BERRIES)
            .result(ModBlocks.BERRY_CREAM_BLOCK)
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItemTags.CREAM, 4)
            .requires(Items.SUGAR)
            .requires(ModFoodItems.CHOCOLATE)
            .result(ModBlocks.CHOCOLATE_CREAM_BLOCK)
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItemTags.IRON_PLATES, 2)
            .requires(
                ItemIngredientPredicate
                    .of(ModBlocks.RESIN_BLOCK.asItem())
                    .withSubPredicate(
                        ModItemSubPredicates.SAVED_ENTITY.get(),
                        ItemSavedEntityPredicate.of(EntityType.CREEPER)
                    )
                    .withSubPredicate(
                        LibItemSubPredicates.NOT.get(),
                        NotPredicate.of(
                            ModItemSubPredicates.SAVED_ENTITY.get(),
                            ItemSavedEntityPredicate.of(EntityType.CREEPER)
                                .predicate(b ->
                                    b.compare(NumericTagValuePredicate.ValueFunction.GREATER_OR_EQUAL)
                                        .lhs("powered")
                                        .rhs(1)
                                )
                        )
                    )
                    .build()
            )
            .result(ModItems.SUPER_CAPACITOR_EMPTY)
            .save(provider);

        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .hasItemIngredient(builder -> builder
                .of(ModBlocks.RESIN_BLOCK.asItem())
                .with(
                    ModItemSubPredicates.SAVED_ENTITY.get(),
                    ItemSavedEntityPredicate.of(EntityType.CREEPER)
                        .predicate(b ->
                            b.compare(NumericTagValuePredicate.ValueFunction.GREATER_OR_EQUAL)
                                .lhs("powered")
                                .rhs(1)
                        )
                )
                .offset(0.0, -0.375, 0.0)
                .range(0.75, 0.75, 0.75)
            )
            .hasItemIngredient(builder -> builder
                .of(ModItemTags.IRON_PLATES)
                .count(2)
                .offset(0.0, -0.375, 0.0)
                .range(0.75, 0.75, 0.75)
            )
            .hasCauldron(0, -1, 0)
            .chooseOne(builder -> builder
                .choice(
                    new ProduceExplosion(
                        new Vec3(0.0, -0.75, 0.0),
                        1f,
                        true,
                        Level.ExplosionInteraction.BLOCK,
                        // 同权重二选一已经包含50%概率了，这里的概率要填1.0
                        ConstantValue.exactly(1f)
                    ),
                    0.5f
                )
                .choice(
                    SpawnItem.builder()
                        .item(ModItems.SUPER_CAPACITOR.asStack())
                        .offset(new Vec3(0.0, -0.75, 0.0))
                        .build(),
                    0.5f
                )
            )
            .group("item_compress")
            .icon(ModItems.SUPER_CAPACITOR.asStack())
            .save(provider, AnvilCraft.of("supercapacitor"));
    }
}
