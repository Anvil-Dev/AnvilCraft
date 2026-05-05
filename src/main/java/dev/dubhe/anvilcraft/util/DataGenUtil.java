package dev.dubhe.anvilcraft.util;

import com.mojang.math.Quadrant;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumProvider;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumBlockLootTables;
import dev.anvilcraft.lib.v2.registrum.util.CreativeModeTabModifier;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullConsumer;
import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataGenUtil {
    public static void powerLevelPressurePlate(
        RegistrumBlockModelGenerator provider,
        Identifier id,
        PowerLevelPressurePlateBlock block
    ) {
        PropertyDispatch.C1<MultiVariant, Integer> builder = PropertyDispatch.initial(PowerLevelPressurePlateBlock.POWER)
            .select(
                0,
                new MultiVariant(
                    WeightedList.<Variant>builder()
                        .add(new Variant(id.withPrefix("block/")))
                        .build()
                )
            );
        for (int i = 0; i < 15; i++) {
            builder.select(
                i + 1,
                new MultiVariant(
                    WeightedList.<Variant>builder()
                        .add(new Variant(id.withPrefix("block/").withSuffix("_down")))
                        .build()
                )
            );
        }
        provider.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(builder));
    }

    public static <T extends Item> void energy(DataGenContext<Item, T> ctx, CreativeModeTabModifier modifier) {
        ItemStack stack = ctx.get().getDefaultInstance();
        stack.set(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        modifier.accept(stack.copy());
        modifier.accept(ctx.get().getDefaultInstance());
    }

    @SuppressWarnings("unused")
    public static <R, A extends R, T> NonNullBiConsumer<DataGenContext<R, A>, T> noExtraModelOrState() {
    }

    public static <T extends RegistrumBlockModelGenerator> void horizontalFacingBlock(
        DataGenContext<Block, ?> context,
        T provider
    ) {
        provider.blockStateOutput.accept(MultiVariantGenerator.dispatch(
            context.get()
        ).with(
            PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING)
                .select(
                    Direction.SOUTH,
                    new MultiVariant(
                        WeightedList.<Variant>builder()
                            .add(new Variant(context.getId().withPrefix("block/")).withYRot(Quadrant.R0))
                            .build()
                    )
                )
                .select(
                    Direction.WEST,
                    new MultiVariant(
                        WeightedList.<Variant>builder()
                            .add(new Variant(context.getId().withPrefix("block/")).withYRot(Quadrant.R90))
                            .build()
                    )
                )
                .select(
                    Direction.NORTH,
                    new MultiVariant(
                        WeightedList.<Variant>builder()
                            .add(new Variant(context.getId().withPrefix("block/")).withYRot(Quadrant.R180))
                            .build()
                    )
                )
                .select(
                    Direction.EAST,
                    new MultiVariant(
                        WeightedList.<Variant>builder()
                            .add(new Variant(context.getId().withPrefix("block/")).withYRot(Quadrant.R270))
                            .build()
                    )
                )
        ));
    }

    @SuppressWarnings("unused")
    public static <T> void noLoot(RegistrumBlockLootTables tables, T value) {
    }

    public static <E extends Block> void simple(DataGenContext<Block, E> context, RegistrumBlockstateProvider provider) {
        provider.simpleBlock(
            context.get(),
            DangerUtil.genConfiguredModel("block/" + context.getId().getPath()).get()
        );
    }

    public static LootItemCondition.Builder hasSilkTouch(HolderLookup.Provider registries) {
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        return MatchTool.toolMatches(
            ItemPredicate.Builder.item()
                .withSubPredicate(
                    ItemSubPredicates.ENCHANTMENTS,
                    ItemEnchantmentsPredicate.enchantments(
                        List.of(new EnchantmentPredicate(lookup.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))
                    )
                )
        );
    }

    public static void dropOtherAndSelfWhenSilkTouch(RegistrumBlockLootTables tables, Block block, ItemLike other) {
        tables.add(block, LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(AlternativesEntry.alternatives(
                        LootItem.lootTableItem(block).when(hasSilkTouch(tables.getRegistries())),
                        LootItem.lootTableItem(other).when(ExplosionCondition.survivesExplosion())
                    ))
            )
        );
    }

    public static void nestingShulkerBoxLoot(RegistrumBlockLootTables lootTables, Block block) {
        lootTables.add(
            block,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .when(ExplosionCondition.survivesExplosion())
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(
                            LootItem.lootTableItem(block)
                                .apply(
                                    CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                        .include(DataComponents.CUSTOM_NAME)
                                        .include(DataComponents.CONTAINER)
                                )
                        )
                )
        );
    }
}
