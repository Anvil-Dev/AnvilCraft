package dev.dubhe.anvilcraft.util;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataGenUtil {
    public static void powerLevelPressurePlate(
        RegistrateBlockstateProvider provider, ResourceLocation id,
        PowerLevelPressurePlateBlock block, ResourceLocation texture
    ) {
        ModelFile pressurePlate = provider.models().pressurePlate(id.getPath(), texture);
        ModelFile pressurePlateDown = provider.models().pressurePlateDown(id.getPath() + "_down", texture);

        provider.getVariantBuilder(block)
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 0).addModels(new ConfiguredModel(pressurePlate))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 1).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 2).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 3).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 4).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 5).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 6).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 7).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 8).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 9).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 10).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 11).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 12).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 13).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 14).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 15).addModels(new ConfiguredModel(pressurePlateDown));
    }

    @SuppressWarnings("unused")
    public static <T extends RegistrateProvider> void noExtraModelOrState(DataGenContext<?, ?> context, T provider) {
    }

    public static <T extends RegistrateBlockstateProvider> void horizontalFacingBlock(
        @NotNull DataGenContext<Block, ?> context,
        @NotNull T provider
    ) {
        ModelFile model = new ModelFile.ExistingModelFile(
            context.getId().withPrefix("block/"),
            provider.models().existingFileHelper
        );

        provider.getVariantBuilder(context.get())
            .forAllStates(
                state -> ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360)
                    .build()
            );
    }

    @SuppressWarnings("unused")
    public static <T> void noLoot(RegistrateBlockLootTables tables, T value) {
    }

    public static <E extends Block> void simple(DataGenContext<Block, E> context, RegistrateBlockstateProvider provider) {
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

    public static void dropOtherAndSelfWhenSilkTouch(RegistrateBlockLootTables tables, Block block, ItemLike other) {
        tables.add(
            block, LootTable.lootTable()
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

    public static void createNestingShulkerBoxDrop(RegistrateBlockLootTables lootTables, Block block) {
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
