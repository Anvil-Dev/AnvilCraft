package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemSubPredicates;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.item.property.predicate.ItemSavedEntityPredicate;
import dev.dubhe.anvilcraft.item.property.predicate.NotPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.builder.InWorldRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ChooseOneOutcome;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.ProduceExplosion;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.item.HasItemIngredient;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.item.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ItemCompressRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ItemCompressRecipe.builder()
            .requires(Items.BONE, 3)
            .result(new ItemStack(Items.BONE_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItems.CREAM, 4)
            .requires(Items.SUGAR)
            .result(new ItemStack(ModBlocks.CREAM_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItems.CREAM, 4)
            .requires(Items.SUGAR)
            .requires(Items.SWEET_BERRIES)
            .result(new ItemStack(ModBlocks.BERRY_CREAM_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItems.CREAM, 4)
            .requires(Items.SUGAR)
            .requires(ModItems.CHOCOLATE)
            .result(new ItemStack(ModBlocks.CHOCOLATE_CREAM_BLOCK))
            .save(provider);

        ItemCompressRecipe.builder()
            .requires(ModItems.NEUTRONIUM_INGOT)
            .requires(ModItems.URANIUM_INGOT)
            .result(ModItems.PLUTONIUM_NUGGET, 6)
            .result(ModItems.LIME_POWDER)
            .result(ModItems.NEUTRONIUM_INGOT)
            .save(provider, AnvilCraft.of("item_compress/plutonium_nugget_from_neutronium_ingot"));

        ItemCompressRecipe.builder()
            .requires(ModItems.STABLE_NEUTRONIUM_INGOT)
            .requires(ModItems.URANIUM_INGOT)
            .result(ModItems.PLUTONIUM_NUGGET, 6)
            .result(ModItems.LIME_POWDER)
            .result(ModItems.STABLE_NEUTRONIUM_INGOT)
            .save(provider, AnvilCraft.of("item_compress/plutonium_nugget_from_stable_neutronium_ingot"));

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
                        ModItemSubPredicates.NOT.get(),
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

        InWorldRecipeBuilder.compatible(ModRecipeTriggers.ON_ANVIL_FALL_ON.get())
            .with(
                HasItemIngredient.builder()
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
                    .build()
            )
            .with(
                HasItemIngredient.builder()
                    .of(ModItemTags.IRON_PLATES)
                    .count(2)
                    .offset(0.0, -0.375, 0.0)
                    .range(0.75, 0.75, 0.75)
                    .build()
            )
            .hasCauldron(0, -1, 0)
            .out(
                ChooseOneOutcome.builder()
                    .choice(
                        new ProduceExplosion(
                            new Vec3(0.0, -0.75, 0.0),
                            1f,
                            true,
                            Level.ExplosionInteraction.BLOCK,
                            0.5f
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
                    .build()
            )
            .group("item_compress")
            .icon(ModItems.SUPER_CAPACITOR.asStack())
            .save(provider, AnvilCraft.of("supercapacitor"));
    }
}
