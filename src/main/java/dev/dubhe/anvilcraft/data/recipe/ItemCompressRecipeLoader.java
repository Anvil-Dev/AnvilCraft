package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.recipe.data.advancement.predicate.item.NotPredicate;
import dev.anvilcraft.lib.v2.recipe.init.LibDataComponentPredicates;
import dev.anvilcraft.lib.v2.recipe.outcome.ProduceExplosion;
import dev.anvilcraft.lib.v2.recipe.outcome.SpawnItem;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModDataComponentPredicates;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.item.property.predicate.ItemSavedEntityPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.builder.ExtendInWorldRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class ItemCompressRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
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
                    .hasComponents(new DataComponentMatchers(
                        DataComponentExactPredicate.builder().build(),
                        Map.of(
                            ModDataComponentPredicates.SAVED_ENTITY.get(),
                            ItemSavedEntityPredicate.of(EntityType.CREEPER)
                        )
                    ))
                    .hasComponents(new DataComponentMatchers(
                        DataComponentExactPredicate.builder().build(),
                        Map.of(
                            LibDataComponentPredicates.NOT.get(),
                            NotPredicate.of(
                                ModDataComponentPredicates.SAVED_ENTITY.get(),
                                ItemSavedEntityPredicate.of(EntityType.CREEPER)
                                    .predicate(
                                        b ->
                                            b.compare(NumericTagValuePredicate.ValueFunction.GREATER_OR_EQUAL)
                                                .lhs("powered")
                                                .rhs(1)
                                    )
                            )
                        )
                    ))
                    .build()
            )
            .result(ModItems.SUPER_CAPACITOR_EMPTY)
            .save(provider);

        ExtendInWorldRecipeBuilder.extendCompatible(ModRecipeTriggers.ON_ANVIL_FALL_ON)
            .hasItemIngredient(builder -> builder
                .of(ModBlocks.RESIN_BLOCK.asItem())
                .has(new DataComponentMatchers(
                    DataComponentExactPredicate.builder().build(),
                    Map.of(
                        ModDataComponentPredicates.SAVED_ENTITY.get(),
                        ItemSavedEntityPredicate.of(EntityType.CREEPER)
                            .predicate(
                                b ->
                                    b.compare(NumericTagValuePredicate.ValueFunction.GREATER_OR_EQUAL)
                                        .lhs("powered")
                                        .rhs(1)
                            )
                    )
                ))
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
                        1F,
                        true,
                        Level.ExplosionInteraction.BLOCK,
                        // 同权重二选一已经包含50%概率了，这里的概率要填1.0
                        ConstantValue.exactly(1F)
                    ),
                    0.5F
                )
                .choice(
                    SpawnItem.builder()
                        .item(
                            ItemStackTemplate.fromNonEmptyStack(ModItems.SUPER_CAPACITOR.asStack()))
                        .offset(new Vec3(0.0, -0.75, 0.0))
                        .build(),
                    0.5F
                )
            )
            .group("item_compress")
            .icon(ItemStackTemplate.fromNonEmptyStack(ModItems.SUPER_CAPACITOR.asStack()))
            .save(provider, AnvilCraft.of("supercapacitor"));
    }
}
