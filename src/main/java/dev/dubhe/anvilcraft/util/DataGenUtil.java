package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumProvider;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumBlockLootTables;
import dev.anvilcraft.lib.v2.registrum.util.CreativeModeTabModifier;
import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemEnchantmentsPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicates;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
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

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataGenUtil {
    public static void powerLevelPressurePlate(
        RegistrumBlockstateProvider provider, ResourceLocation id,
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

    public static void diodeBlock(RegistrumBlockstateProvider provider, ResourceLocation id, DiodeBlock block) {
        ModelFile diode = new ModelFile.ExistingModelFile(id.withPrefix("block/"), provider.models().existingFileHelper);
        ModelFile diodeOn = new ModelFile.ExistingModelFile(
            id.withPrefix("block/").withSuffix("_on"),
            provider.models().existingFileHelper
        );

        provider.getVariantBuilder(block)
            .partialState()
            .with(DiodeBlock.FACING, Direction.SOUTH).with(DiodeBlock.POWERED, false)
            .addModels(new ConfiguredModel(diode))
            .partialState()
            .with(DiodeBlock.FACING, Direction.WEST).with(DiodeBlock.POWERED, false)
            .addModels(new ConfiguredModel(diode, 0, 90, false))
            .partialState()
            .with(DiodeBlock.FACING, Direction.NORTH).with(DiodeBlock.POWERED, false)
            .addModels(new ConfiguredModel(diode, 0, 180, false))
            .partialState()
            .with(DiodeBlock.FACING, Direction.EAST).with(DiodeBlock.POWERED, false)
            .addModels(new ConfiguredModel(diode, 0, 270, false))
            .partialState()
            .with(DiodeBlock.FACING, Direction.SOUTH).with(DiodeBlock.POWERED, true)
            .addModels(new ConfiguredModel(diodeOn))
            .partialState()
            .with(DiodeBlock.FACING, Direction.WEST).with(DiodeBlock.POWERED, true)
            .addModels(new ConfiguredModel(diodeOn, 0, 90, false))
            .partialState()
            .with(DiodeBlock.FACING, Direction.NORTH).with(DiodeBlock.POWERED, true)
            .addModels(new ConfiguredModel(diodeOn, 0, 180, false))
            .partialState()
            .with(DiodeBlock.FACING, Direction.EAST).with(DiodeBlock.POWERED, true)
            .addModels(new ConfiguredModel(diodeOn, 0, 270, false));
    }

    public static <T extends Item> void energy(DataGenContext<Item, T> ctx, CreativeModeTabModifier modifier) {
        ItemStack stack = ctx.get().getDefaultInstance();
        modifier.accept(stack.copy());
        stack.set(ModComponents.STORED_ENERGY, 320000);
        modifier.accept(stack.copy());
    }

    @SuppressWarnings("unused")
    public static <T extends RegistrumProvider> void noExtraModelOrState(DataGenContext<?, ?> context, T provider) {
    }

    public static <T extends RegistrumBlockstateProvider> void horizontalFacingBlock(
        DataGenContext<Block, ?> context,
        T provider
    ) {
        ModelFile model = new ModelFile.ExistingModelFile(
            context.getId().withPrefix("block/"),
            provider.models().existingFileHelper
        );

        provider.getVariantBuilder(context.get()).forAllStates(
            state -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360)
                .build()
        );
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
