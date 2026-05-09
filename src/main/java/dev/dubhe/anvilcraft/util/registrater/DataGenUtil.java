package dev.dubhe.anvilcraft.util.registrater;

import com.mojang.math.Quadrant;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumItemModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumBlockLootTables;
import dev.anvilcraft.lib.v2.registrum.util.CreativeModeTabModifier;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import dev.anvilcraft.lib.v2.util.nullness.NonNullFunction;
import dev.dubhe.anvilcraft.api.item.property.IIntegerComponent;
import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModDataComponentPredicates;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.property.predicate.IntegerComponentPredicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.conditional.ComponentMatches;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataGenUtil {
    public static <T extends Item> void energy(DataGenContext<Item, T> ctx, CreativeModeTabModifier modifier) {
        ItemStack stack = ctx.get().getDefaultInstance();
        stack.set(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY);
        modifier.accept(stack.copy());
        modifier.accept(ctx.get().getDefaultInstance());
    }

    @SuppressWarnings("unused")
    public static <R, A extends R, T> NonNullBiConsumer<DataGenContext<R, A>, T> noExtraModelOrState() {
        return (ctx, generator) -> {
        };
    }

    public static <T extends BlockItem> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> flatItem() {
        return (ctx, generator) -> generator.createFlatItemModel(ctx.get(), ModelTemplates.FLAT_ITEM);
    }

    public static <T extends BlockItem> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> blockItem() {
        return (ctx, generator) -> generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/"));
    }

    public static <T extends BlockItem> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> blockItem(String suffix) {
        return (ctx, generator) -> generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/").withSuffix(suffix));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> ionocraftBackpack() {
        return DataGenUtil.exhaustable(ModComponents.FLIGHT_TIME);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> energyWeapon() {
        return DataGenUtil.exhaustable(ModComponents.STORED_ENERGY);
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> exhaustable(
        DataComponentType<? extends IIntegerComponent> exhaustable
    ) {
        return (ctx, generator) -> {
            Item item = ctx.get();
            ItemModel.Unbaked normal = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item));
            ItemModel.Unbaked off = ItemModelUtils.plainModel(generator.createFlatItemModel(item, "_off", ModelTemplates.FLAT_ITEM));
            generator.itemModelOutput.accept(
                item,
                ItemModelUtils.conditional(
                    new ComponentMatches(new DataComponentPredicate.Single<>(
                        ModDataComponentPredicates.INT_COMP.get(),
                        new IntegerComponentPredicate(exhaustable, 0)
                    )),
                    off,
                    normal
                )
            );
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> onlyState() {
        return (ctx, generator) -> generator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
            ctx.get(),
            BlockModelGenerators.plainVariant(ctx.getId().withPrefix("block/"))
        ));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> simpleBlock() {
        return (ctx, generator) -> generator.createTrivialCube(ctx.get());
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> transparentBlock() {
        return (ctx, generator) -> {
            Block block = ctx.get();
            generator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
                block,
                BlockModelGenerators.plainVariant(new TexturedModel(
                    new TextureMapping().put(TextureSlot.ALL, new Material(ctx.getId().withPath("block/"), true)),
                    ModelTemplates.CUBE_ALL
                ).create(block, generator.modelOutput))
            ));
        };
    }

    // region slabBlock
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> slabBlock() {
        return DataGenUtil.slabBlock(ctx -> TextureMapping.getBlockTexture(ctx.get()));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> slabBlock(
        Identifier single
    ) {
        return DataGenUtil.slabBlock(new Material(single));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> slabBlock(
        Identifier top,
        Identifier bottom,
        Identifier side
    ) {
        return DataGenUtil.slabBlock(new Material(top), new Material(bottom), new Material(side));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> slabBlock(
        Material single
    ) {
        return DataGenUtil.slabBlock(_ -> single);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> slabBlock(
        Material top,
        Material bottom,
        Material side
    ) {
        return DataGenUtil.slabBlock(_ -> top, _ -> bottom, _ -> side);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> slabBlock(
        NonNullFunction<DataGenContext<Block, ?>, Material> single
    ) {
        return DataGenUtil.slabBlock(single, single, single);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> slabBlock(
        NonNullFunction<DataGenContext<Block, ?>, Material> top,
        NonNullFunction<DataGenContext<Block, ?>, Material> bottom,
        NonNullFunction<DataGenContext<Block, ?>, Material> side
    ) {
        return (ctx, generator) -> {
            Block slab = ctx.get();
            TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.TOP, top.apply(ctx))
                .put(TextureSlot.BOTTOM, bottom.apply(ctx))
                .put(TextureSlot.SIDE, side.apply(ctx));
            Identifier bottomModel = ModelTemplates.SLAB_BOTTOM.create(slab, mapping, generator.modelOutput);
            MultiVariant topVa = BlockModelGenerators.plainVariant(ModelTemplates.SLAB_TOP.create(slab, mapping, generator.modelOutput));
            generator.blockStateOutput.accept(BlockModelGenerators.createSlab(
                slab,
                BlockModelGenerators.plainVariant(bottomModel),
                topVa,
                BlockModelGenerators.plainVariant(ctx.getId().withPath(path -> path.substring(0, path.length() - 5))) // 移除_slab
            ));
            generator.registerSimpleItemModel(slab, bottomModel);
        };
    }
    // endregion

    // region stairsBlock
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> stairsBlock() {
        return DataGenUtil.stairsBlock(ctx -> TextureMapping.getBlockTexture(ctx.get()));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> stairsBlock(
        Identifier single
    ) {
        return DataGenUtil.stairsBlock(new Material(single));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> stairsBlock(
        Identifier top,
        Identifier bottom,
        Identifier side
    ) {
        return DataGenUtil.stairsBlock(new Material(top), new Material(bottom), new Material(side));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> stairsBlock(
        Material single
    ) {
        return DataGenUtil.stairsBlock(_ -> single);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> stairsBlock(
        Material top,
        Material bottom,
        Material side
    ) {
        return DataGenUtil.stairsBlock(_ -> top, _ -> bottom, _ -> side);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> stairsBlock(
        NonNullFunction<DataGenContext<Block, ?>, Material> single
    ) {
        return DataGenUtil.stairsBlock(single, single, single);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> stairsBlock(
        NonNullFunction<DataGenContext<Block, ?>, Material> top,
        NonNullFunction<DataGenContext<Block, ?>, Material> bottom,
        NonNullFunction<DataGenContext<Block, ?>, Material> side
    ) {
        return (ctx, generator) -> {
            Block stairs = ctx.get();
            TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.TOP, top.apply(ctx))
                .put(TextureSlot.BOTTOM, bottom.apply(ctx))
                .put(TextureSlot.SIDE, side.apply(ctx));
            MultiVariant inner = BlockModelGenerators.plainVariant(
                ModelTemplates.STAIRS_INNER.create(stairs, mapping, generator.modelOutput)
            );
            Identifier straight = ModelTemplates.STAIRS_STRAIGHT.create(stairs, mapping, generator.modelOutput);
            MultiVariant outer = BlockModelGenerators.plainVariant(
                ModelTemplates.STAIRS_OUTER.create(stairs, mapping, generator.modelOutput)
            );
            generator.blockStateOutput.accept(BlockModelGenerators.createStairs(
                stairs,
                inner,
                BlockModelGenerators.plainVariant(straight),
                outer
            ));
            generator.registerSimpleItemModel(stairs, straight);
        };
    }
    // endregion

    // region wallBlock
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> wallBlock() {
        return DataGenUtil.wallBlock(ctx -> TextureMapping.getBlockTexture(ctx.get()));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> wallBlock(
        Identifier wall
    ) {
        return DataGenUtil.wallBlock(new Material(wall));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> wallBlock(
        Material wall
    ) {
        return DataGenUtil.wallBlock(_ -> wall);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> wallBlock(
        NonNullFunction<DataGenContext<Block, T>, Material> wall
    ) {
        return (ctx, generator) -> {
            Block block = ctx.get();
            TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.WALL, wall.apply(ctx));
            MultiVariant post = BlockModelGenerators.plainVariant(
                ModelTemplates.WALL_POST.create(block, mapping, generator.modelOutput)
            );
            MultiVariant low = BlockModelGenerators.plainVariant(
                ModelTemplates.WALL_LOW_SIDE.create(block, mapping, generator.modelOutput)
            );
            MultiVariant high = BlockModelGenerators.plainVariant(
                ModelTemplates.WALL_TALL_SIDE.create(block, mapping, generator.modelOutput)
            );
            generator.blockStateOutput.accept(BlockModelGenerators.createWall(block, post, low, high));
            Identifier inventory = ModelTemplates.WALL_INVENTORY.create(block, mapping, generator.modelOutput);
            generator.registerSimpleItemModel(block, inventory);
        };
    }
    // endregion

    // region pressurePlateBlock
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> pressurePlateBlock() {
        return DataGenUtil.pressurePlateBlock(ctx -> TextureMapping.getBlockTexture(ctx.get()));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> pressurePlateBlock(
        Identifier pressurePlate
    ) {
        return DataGenUtil.pressurePlateBlock(new Material(pressurePlate));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> pressurePlateBlock(
        Material pressurePlate
    ) {
        return DataGenUtil.pressurePlateBlock(_ -> pressurePlate);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> pressurePlateBlock(
        NonNullFunction<DataGenContext<Block, T>, Material> pressurePlate
    ) {
        return (ctx, generator) -> {
            Block block = ctx.get();
            TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.TEXTURE, pressurePlate.apply(ctx));
            MultiVariant off = BlockModelGenerators.plainVariant(
                ModelTemplates.PRESSURE_PLATE_UP.create(block, mapping, generator.modelOutput)
            );
            MultiVariant on = BlockModelGenerators.plainVariant(
                ModelTemplates.PRESSURE_PLATE_DOWN.create(block, mapping, generator.modelOutput)
            );
            generator.blockStateOutput.accept(BlockModelGenerators.createPressurePlate(block, off, on));
        };
    }
    // endregion

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> horizontalFacingBlock() {
        return (ctx, generator) -> generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(
            ctx.get(),
            BlockModelGenerators.plainVariant(ctx.getId().withPrefix("block/"))
        ).with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> leveledPressurePlateBlock(
        Identifier texture
    ) {
        return (ctx, provider) -> provider.blockStateOutput.accept(
            MultiVariantGenerator.dispatch(ctx.get())
                .with(BlockModelGenerators.createEmptyOrFullDispatch(
                    PowerLevelPressurePlateBlock.POWER,
                    1,
                    BlockModelGenerators.plainVariant(texture.withPrefix("block/")),
                    BlockModelGenerators.plainVariant(texture.withPrefix("block/").withSuffix("_down"))
                ))
        );
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> columnBlock(
        Identifier side,
        Identifier end
    ) {
        return (ctx, generator) -> {
            Block b = ctx.get();
            TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.SIDE, new Material(side))
                .put(TextureSlot.END, new Material(end));
            MultiVariant model = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN.create(b, mapping, generator.modelOutput));
            generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(b).with(
                PropertyDispatch.initial(BlockStateProperties.AXIS)
                    .select(Direction.Axis.Y, model)
                    .select(Direction.Axis.Z, model.with(BlockModelGenerators.X_ROT_90))
                    .select(Direction.Axis.X, model.with(VariantMutator.Z_ROT.withValue(Quadrant.R90)))
            ));
        };
    }

    @SuppressWarnings("unused")
    public static <T> void noLoot(RegistrumBlockLootTables tables, T value) {
    }

    public static LootItemCondition.Builder hasSilkTouch(HolderLookup.Provider registries) {
        return MatchTool.toolMatches(
            ItemPredicate.Builder.item()
                .withComponents(
                    DataComponentMatchers.Builder.components()
                        .partial(
                            DataComponentPredicates.ENCHANTMENTS,
                            EnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(
                                registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                                MinMaxBounds.Ints.atLeast(1)
                            )))
                        )
                        .build()
                )
        );
    }

    public static void dropOtherAndSelfWhenSilkTouch(RegistrumBlockLootTables tables, Block block, ItemLike other) {
        tables.add(block, LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(AlternativesEntry.alternatives(
                        LootItem.lootTableItem(block).when(DataGenUtil.hasSilkTouch(tables.getRegistries())),
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
                                    CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                        .include(DataComponents.CUSTOM_NAME)
                                        .include(DataComponents.CONTAINER)
                                )
                        )
                )
        );
    }
}
