package dev.dubhe.anvilcraft.integration.rei.client;

import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlockIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasFluidCauldron;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.dubhe.anvilcraft.integration.rei.AnvilCraftCategoryIdentifier;
import dev.dubhe.anvilcraft.integration.rei.AnvilCraftEntryIngredient;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AnvilRecipeDisplay extends BasicDisplay {
    final AnvilCraftCategoryIdentifier anvilCraftCategoryIdentifier;
    final List<BlockState> inputBlockStates;
    final List<BlockState> outputBlockStates;
    final List<EntryIngredient> inputItems;
    final List<AnvilCraftEntryIngredient> outputItems;

    private AnvilRecipeDisplay(
            @NotNull AnvilCraftCategoryIdentifier anvilCraftCategoryIdentifier,
            @NotNull List<BlockState> inputBlockStates,
            @NotNull List<BlockState> outputBlockStates,
            @NotNull List<EntryIngredient> inputItems,
            @NotNull List<AnvilCraftEntryIngredient> outputItems,
            @NotNull List<EntryIngredient> inputs,
            @NotNull List<EntryIngredient> outputs,
            @NotNull ResourceLocation location
    ) {
        super(inputs, outputs, Optional.of(location));
        this.anvilCraftCategoryIdentifier = anvilCraftCategoryIdentifier;
        this.inputBlockStates = inputBlockStates;
        this.outputBlockStates = outputBlockStates;
        this.inputItems = inputItems;
        this.outputItems = outputItems;
    }

    /**
     * 创建AnvilRecipeDisplay
     *
     * @param anvilCraftCategoryIdentifier 铁砧工艺类别标识符
     * @param anvilRecipe                  铁砧配方
     * @return                             {@link AnvilRecipeDisplay}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>, V extends T> AnvilRecipeDisplay of(
            AnvilCraftCategoryIdentifier anvilCraftCategoryIdentifier,
            AnvilRecipe anvilRecipe) {
        ArrayList<EntryIngredient> inputs = new ArrayList<>();
        ArrayList<EntryIngredient> outputs = new ArrayList<>();
        NonNullList<BlockState> inputBlockStates = NonNullList.withSize(2, Blocks.AIR.defaultBlockState());
        NonNullList<BlockState> outputBlockStates = NonNullList.withSize(2, Blocks.AIR.defaultBlockState());
        ArrayList<EntryIngredient> inputItems = new ArrayList<>();
        ArrayList<AnvilCraftEntryIngredient> outputItems = new ArrayList<>();
        for (RecipePredicate recipePredicate : anvilRecipe.getPredicates()) {
            if (recipePredicate instanceof HasBlock hasBlock) {
                boolean flag = hasBlock instanceof HasBlockIngredient;
                HasBlock.ModBlockPredicate matchBlock = hasBlock.getMatchBlock();
                TagKey<Block> tag = matchBlock.getTag();
                Map<String, String> properties = matchBlock.getProperties();
                Set<Block> blockSet = matchBlock.getBlocks();
                if (tag != null) blockSet.addAll(
                        BuiltInRegistries.BLOCK.getOrCreateTag(tag).stream().map(Holder::value).toList()
                );
                List<BlockState> blocks = blockSet.stream()
                        .map(block1 -> {
                            BlockState workBlockState = block1.defaultBlockState();
                            ImmutableSet<Property<?>> keySet = workBlockState.getValues().keySet();
                            for (Property<?> property : keySet) {
                                if (!properties.containsKey(property.getName())) continue;
                                String value = properties.get(property.getName());
                                Optional<?> first = property.getAllValues()
                                        .map(Property.Value::value)
                                        .filter(v -> value.equals(v.toString()))
                                        .findFirst();
                                if (first.isEmpty()) continue;
                                workBlockState = workBlockState.setValue((Property<T>) property, (V) first.get());
                            }
                            return workBlockState;
                        })
                        .toList();
                if (hasBlock.getOffset().equals(new Vec3(0.0d, -1.0d, 0.0d))) {
                    inputBlockStates.set(0, blocks.get(0));
                    if (!flag) outputBlockStates.set(0, blocks.get(0));
                    continue;
                }
                if (hasBlock.getOffset().equals(new Vec3(0.0d, -2.0d, 0.0d))) {
                    inputBlockStates.set(1, blocks.get(0));
                    if (!flag) outputBlockStates.set(1, blocks.get(0));
                    continue;
                }
                inputs.add(EntryIngredient.of(
                        hasBlock.getMatchBlock().getBlocks().stream().map(EntryStacks::of).collect(Collectors.toList()))
                );
            } else if (recipePredicate instanceof HasFluidCauldron hasFluidCauldron) {
                if (hasFluidCauldron.getOffset().equals(new Vec3(0.0d, -1.0d, 0.0d))) {
                    inputBlockStates.set(0, hasFluidCauldron.getMatchBlock().defaultBlockState());
                    outputBlockStates.set(0, hasFluidCauldron.getMatchBlock().defaultBlockState());
                    continue;
                }
                if (hasFluidCauldron.getOffset().equals(new Vec3(0.0d, -2.0d, 0.0d))) {
                    inputBlockStates.set(1, hasFluidCauldron.getMatchBlock().defaultBlockState());
                    outputBlockStates.set(0, hasFluidCauldron.getMatchBlock().defaultBlockState());
                    continue;
                }
                inputs.add(EntryIngredient.of(EntryStacks.of(hasFluidCauldron.getMatchBlock())));
            } else if (recipePredicate instanceof HasItem hasItem && hasItem.getMatchItem().count.getMin() != null) {
                EntryIngredient entryIngredient =
                        !hasItem.getMatchItem().getItems().isEmpty()
                                ? EntryIngredient.of(
                                hasItem.getMatchItem().getItems().stream()
                                        .map(item ->
                                                EntryStacks.of(
                                                        new ItemStack(item, hasItem.getMatchItem().count.getMin())
                                                ))
                                        .collect(Collectors.toList()))
                                : EntryIngredient.of(
                                StreamSupport.stream(
                                                BuiltInRegistries.ITEM.getTagOrEmpty(
                                                        Objects.requireNonNull(hasItem.getMatchItem().getTag())
                                                ).spliterator(),
                                                false)
                                        .map(itemHolder -> EntryStacks.of(new ItemStack(
                                                itemHolder,
                                                hasItem.getMatchItem().count.getMin())))
                                        .toList());
                inputs.add(entryIngredient);
                inputItems.add(entryIngredient);
            }
        }
        for (RecipeOutcome recipeOutcome : anvilRecipe.getOutcomes()) {
            if (recipeOutcome instanceof SpawnItem spawnItem) {
                outputs.add(EntryIngredient.of(EntryStacks.of(spawnItem.getResult())));
                outputItems.add(AnvilCraftEntryIngredient.of(
                        EntryIngredient.of(EntryStacks.of(spawnItem.getResult())),
                        (float) spawnItem.getChance())
                );
            } else if (recipeOutcome instanceof SetBlock setBlock) {
                if (setBlock.getOffset().equals(new Vec3(0.0d, -1.0d, 0.0d))) {
                    outputBlockStates.set(0, setBlock.getResult());
                    continue;
                }
                if (setBlock.getOffset().equals(new Vec3(0.0d, -2.0d, 0.0d))) {
                    outputBlockStates.set(1, setBlock.getResult());
                    continue;
                }
                outputs.add(EntryIngredient.of(EntryStacks.of(setBlock.getResult().getBlock())));
            } else if (recipeOutcome instanceof SelectOne selectOne) {
                ArrayList<EntryStack<?>> entryStacks = new ArrayList<>();
                for (RecipeOutcome selectOneOutcome : selectOne.getOutcomes()) {
                    if (selectOneOutcome instanceof SpawnItem spawnItem) {
                        entryStacks.add(EntryStacks.of(spawnItem.getResult()));
                    } else if (selectOneOutcome instanceof SetBlock setBlock) {
                        entryStacks.add(EntryStacks.of(setBlock.getResult().getBlock()));
                    }
                }
                EntryIngredient selectOneIngredient = EntryIngredient.of(entryStacks);
                outputs.add(selectOneIngredient);
                outputItems.add(AnvilCraftEntryIngredient.of(selectOne));
            }
        }
        return new AnvilRecipeDisplay(
                anvilCraftCategoryIdentifier,
                inputBlockStates, outputBlockStates,
                inputItems, outputItems,
                inputs, outputs,
                anvilRecipe.getId()
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return anvilCraftCategoryIdentifier.getCategoryIdentifier();
    }
}
