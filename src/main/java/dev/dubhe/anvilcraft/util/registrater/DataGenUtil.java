package dev.dubhe.anvilcraft.util.registrater;

import com.mojang.math.Quadrant;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumItemModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.model.PropertyDispatchWrap;
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
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdMode;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import dev.dubhe.anvilcraft.item.tool.ResonateMode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.properties.conditional.ComponentMatches;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.client.renderer.item.properties.select.ComponentContents;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;

@SuppressWarnings("Convert2Lambda")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataGenUtil {
    public static <T extends Item> void energy(DataGenContext<Item, T> ctx, CreativeModeTabModifier modifier) {
        ItemStack stack = ctx.get().getDefaultInstance();
        stack.set(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY);
        modifier.accept(stack);
        modifier.accept(ctx.get().getDefaultInstance());
    }

    @SuppressWarnings("unused")
    public static <R, A extends R, T> NonNullBiConsumer<DataGenContext<R, A>, T> noExtraModelOrState() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<R, A> ctx, T generator) {
            }
        };
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> flatItem() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                generator.generateFlatItem(ctx.get(), ModelTemplates.FLAT_ITEM);
            }
        };
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> flatHandheldItem() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                generator.generateFlatItem(ctx.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
            }
        };
    }

    public static <T extends BlockItem> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> blockItem() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/"));
            }
        };
    }

    public static <T extends BlockItem> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> blockItem(String suffix) {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                generator.createWithExistingModel(ctx.get(), ctx.getId().withPrefix("block/").withSuffix(suffix));
            }
        };
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> multitool() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                Item item = ctx.get();
                Identifier prefix = ModelLocationUtils.getModelLocation(item).withSuffix("_");
                generator.itemModelOutput.accept(
                    item,
                    ItemModelUtils.select(
                        new ComponentContents<>(ModComponents.MULTITOOL_MODE),
                        ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item)),
                        ItemModelUtils.when(MultitoolMode.BRUSH, ItemModelUtils.plainModel(prefix.withSuffix("brush"))),
                        ItemModelUtils.when(
                            MultitoolMode.CARROT_ON_A_STICK,
                            ItemModelUtils.plainModel(prefix.withSuffix("carrot_on_a_stick"))
                        ),
                        ItemModelUtils.when(
                            MultitoolMode.FISHING_ROD,
                            ItemModelUtils.conditional(
                                new FishingRodCast(),
                                ItemModelUtils.plainModel(prefix.withSuffix("fishing_rod_cast")),
                                ItemModelUtils.plainModel(prefix.withSuffix("fishing_rod"))
                            )
                        ),
                        ItemModelUtils.when(MultitoolMode.FLINT_AND_STEEL, ItemModelUtils.plainModel(prefix.withSuffix("flint_and_steel"))),
                        ItemModelUtils.when(MultitoolMode.MAGNET, ItemModelUtils.plainModel(prefix.withSuffix("magnet"))),
                        ItemModelUtils.when(MultitoolMode.SHEARS, ItemModelUtils.plainModel(prefix.withSuffix("shears"))),
                        ItemModelUtils.when(MultitoolMode.SPYGLASS, ItemModelUtils.plainModel(prefix.withSuffix("spyglass"))),
                        ItemModelUtils.when(
                            MultitoolMode.WARPED_FUNGUS_ON_A_STICK,
                            ItemModelUtils.plainModel(prefix.withSuffix("warped_fungus_on_a_stick"))
                        )
                    )
                );
            }
        };
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> heavyHalberd() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                Item item = ctx.get();
                Identifier id = ModelLocationUtils.getModelLocation(item);
                generator.itemModelOutput.accept(
                    item,
                    ItemModelUtils.select(
                        new ComponentContents<>(ModComponents.HEAVY_HALBERD_MODE),
                        ItemModelUtils.conditional(
                            new IsUsingItem(),
                            ItemModelUtils.plainModel(id.withSuffix("_throwing")),
                            ItemModelUtils.plainModel(id)
                        ),
                        ItemModelUtils.when(
                            HeavyHalberdMode.TRIDENT,
                            ItemModelUtils.conditional(
                                new IsUsingItem(),
                                ItemModelUtils.plainModel(id.withSuffix("_throwing")),
                                ItemModelUtils.plainModel(id)
                            )
                        ),
                        ItemModelUtils.when(
                            HeavyHalberdMode.SPEAR,
                            ItemModelUtils.plainModel(id.withSuffix("_spear"))
                        ),
                        ItemModelUtils.when(
                            HeavyHalberdMode.SWORD,
                            ItemModelUtils.plainModel(id.withSuffix("_sword"))
                        ),
                        ItemModelUtils.when(
                            HeavyHalberdMode.MACE,
                            ItemModelUtils.plainModel(id.withSuffix("_mace"))
                        )
                    )
                );
            }
        };
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> resonator() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                Item item = ctx.get();
                Identifier prefix = ModelLocationUtils.getModelLocation(item);
                prefix = prefix.withPath(prefix.getPath().substring(0, prefix.getPath().length() - 9)); // 减掉resonator
                generator.itemModelOutput.accept(
                    item,
                    ItemModelUtils.select(
                        new ComponentContents<>(ModComponents.RESONATE_MODE),
                        ItemModelUtils.plainModel(prefix.withSuffix("resonator")),
                        ItemModelUtils.when(ResonateMode.AXE, ItemModelUtils.plainModel(prefix.withSuffix("resonance_axe"))),
                        ItemModelUtils.when(ResonateMode.HOE, ItemModelUtils.plainModel(prefix.withSuffix("resonance_hoe"))),
                        ItemModelUtils.when(ResonateMode.PICKAXE, ItemModelUtils.plainModel(prefix.withSuffix("resonance_pickaxe"))),
                        ItemModelUtils.when(ResonateMode.SHOVEL, ItemModelUtils.plainModel(prefix.withSuffix("resonance_shovel")))
                    )
                );
            }
        };
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
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                Item item = ctx.get();
                generator.itemModelOutput.accept(
                    item,
                    ItemModelUtils.conditional(
                        new ComponentMatches(new DataComponentPredicate.Single<>(
                            ModDataComponentPredicates.INT_COMP.get(),
                            new IntegerComponentPredicate(exhaustable, 0)
                        )),
                        ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item).withSuffix("_exhausted")),
                        ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item))
                    )
                );
            }
        };
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> onlyInfo() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                Item item = ctx.get();
                generator.itemModelOutput.accept(
                    item,
                    ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item))
                );
            }
        };
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> oversizedItem() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                generator.itemModelOutput.accept(
                    ctx.get(),
                    ItemModelUtils.plainModel(ctx.getId().withPrefix("block/")),
                    new ClientItem.Properties(true, true, 1.0F)
                );
            }
        };
    }

    /// 生成带自定义特殊渲染器的方块物品模型
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrumItemModelGenerator> specialBlockItem(
        SpecialModelRenderer.Unbaked<?> renderer,
        boolean oversized
    ) {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Item, T> ctx, RegistrumItemModelGenerator generator) {
                generator.itemModelOutput.accept(
                    ctx.get(),
                    ItemModelUtils.specialModel(ctx.getId().withPrefix("block/"), renderer),
                    new ClientItem.Properties(oversized, oversized, 1.0F)
                );
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> onlyState() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                generator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
                    ctx.get(),
                    BlockModelGenerators.plainVariant(ctx.getId().withPrefix("block/"))
                ));
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> simpleBlock() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                generator.createTrivialCube(ctx.get());
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> transparentBlock() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                Block block = ctx.get();
                generator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(
                    block,
                    BlockModelGenerators.plainVariant(new TexturedModel(
                        new TextureMapping().put(TextureSlot.ALL, new Material(ctx.getId().withPrefix("block/"), true)),
                        ModelTemplates.CUBE_ALL
                    ).create(block, generator.modelOutput))
                ));
            }
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
        return DataGenUtil.slabBlock(
            top,
            bottom,
            side,
            ctx -> ctx.getId().withPath(path -> "block/" + path.replace("_slab", ""))
        );
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> slabBlock(
        NonNullFunction<DataGenContext<Block, ?>, Material> top,
        NonNullFunction<DataGenContext<Block, ?>, Material> bottom,
        NonNullFunction<DataGenContext<Block, ?>, Material> side,
        NonNullFunction<DataGenContext<Block, ?>, Identifier> blockModel
    ) {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                Block slab = ctx.get();
                TextureMapping mapping = new TextureMapping()
                    .put(TextureSlot.TOP, top.apply(ctx))
                    .put(TextureSlot.BOTTOM, bottom.apply(ctx))
                    .put(TextureSlot.SIDE, side.apply(ctx));
                Identifier bottomModel = ModelTemplates.SLAB_BOTTOM.create(slab, mapping, generator.modelOutput);
                MultiVariant topVa = BlockModelGenerators.plainVariant(ModelTemplates.SLAB_TOP.create(
                    slab,
                    mapping,
                    generator.modelOutput
                ));
                generator.blockStateOutput.accept(BlockModelGenerators.createSlab(
                    slab,
                    BlockModelGenerators.plainVariant(bottomModel),
                    topVa,
                    BlockModelGenerators.plainVariant(blockModel.apply(ctx)) // 移除_slab
                ));
                generator.registerSimpleItemModel(slab, bottomModel);
            }
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
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
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
            }
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
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
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
            }
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
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
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
            }
        };
    }
    // endregion

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> horizontalFacingBlock() {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                    ctx.get(),
                    BlockModelGenerators.plainVariant(ctx.getId().withPrefix("block/"))
                ).with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> horizontalFacingBlock(
        BooleanProperty extra,
        NonNullFunction<DataGenContext<Block, T>, Identifier> onTrueFac,
        NonNullFunction<DataGenContext<Block, T>, Identifier> onFalseFac
    ) {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                Identifier onTrue = onTrueFac.apply(ctx);
                Identifier onFalse = onFalseFac.apply(ctx);
                generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                    ctx.get()
                ).with(PropertyDispatchWrap.initial(BlockStateProperties.HORIZONTAL_FACING, extra).select(
                    Direction.NORTH,
                    true,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onTrue).withYRot(Quadrant.R0))
                ).select(
                    Direction.NORTH,
                    false,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onFalse).withYRot(Quadrant.R0))
                ).select(
                    Direction.EAST,
                    true,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onTrue).withYRot(Quadrant.R90))
                ).select(
                    Direction.EAST,
                    false,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onFalse).withYRot(Quadrant.R90))
                ).select(
                    Direction.SOUTH,
                    true,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onTrue).withYRot(Quadrant.R180))
                ).select(
                    Direction.SOUTH,
                    false,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onFalse).withYRot(Quadrant.R180))
                ).select(
                    Direction.WEST,
                    true,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onTrue).withYRot(Quadrant.R270))
                ).select(
                    Direction.WEST,
                    false,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onFalse).withYRot(Quadrant.R270))
                ).dispatch()));
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> horizontalFacingBlockInverted(
    ) {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                    ctx.get(),
                    BlockModelGenerators.plainVariant(ctx.getId().withPrefix("block/"))
                ).with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING_ALT));
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> horizontalFacingBlockInverted(
        BooleanProperty extra,
        NonNullFunction<DataGenContext<Block, T>, Identifier> onTrueFac,
        NonNullFunction<DataGenContext<Block, T>, Identifier> onFalseFac
    ) {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                Identifier onTrue = onTrueFac.apply(ctx);
                Identifier onFalse = onFalseFac.apply(ctx);
                generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(
                    ctx.get()
                ).with(PropertyDispatchWrap.initial(BlockStateProperties.HORIZONTAL_FACING, extra).select(
                    Direction.NORTH,
                    true,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onTrue).withYRot(Quadrant.R180))
                ).select(
                    Direction.NORTH,
                    false,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onFalse).withYRot(Quadrant.R180))
                ).select(
                    Direction.EAST,
                    true,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onTrue).withYRot(Quadrant.R270))
                ).select(
                    Direction.EAST,
                    false,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onFalse).withYRot(Quadrant.R270))
                ).select(
                    Direction.SOUTH,
                    true,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onTrue).withYRot(Quadrant.R0))
                ).select(
                    Direction.SOUTH,
                    false,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onFalse).withYRot(Quadrant.R0))
                ).select(
                    Direction.WEST,
                    true,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onTrue).withYRot(Quadrant.R90))
                ).select(
                    Direction.WEST,
                    false,
                    BlockModelGenerators.variant(BlockModelGenerators.plainModel(onFalse).withYRot(Quadrant.R90))
                ).dispatch()));
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> leveledPressurePlateBlock(
        Identifier texture
    ) {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                Block block = ctx.get();
                TextureMapping mapping = new TextureMapping()
                    .put(TextureSlot.TEXTURE, new Material(texture));
                MultiVariant off = BlockModelGenerators.plainVariant(
                    ModelTemplates.PRESSURE_PLATE_UP.create(block, mapping, generator.modelOutput)
                );
                MultiVariant on = BlockModelGenerators.plainVariant(
                    ModelTemplates.PRESSURE_PLATE_DOWN.create(block, mapping, generator.modelOutput)
                );
                generator.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(ctx.get())
                        .with(BlockModelGenerators.createEmptyOrFullDispatch(
                            PowerLevelPressurePlateBlock.POWER,
                            1,
                            on,
                            off
                        ))
                );
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrumBlockModelGenerator> columnBlock(
        Identifier side,
        Identifier end
    ) {
        return new NonNullBiConsumer<>() {
            @Override
            public void accept(DataGenContext<Block, T> ctx, RegistrumBlockModelGenerator generator) {
                Block b = ctx.get();
                TextureMapping mapping = new TextureMapping()
                    .put(TextureSlot.SIDE, new Material(side))
                    .put(TextureSlot.END, new Material(end));
                MultiVariant model = BlockModelGenerators.plainVariant(ModelTemplates.CUBE_COLUMN.create(
                    b,
                    mapping,
                    generator.modelOutput
                ));
                generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(b).with(
                    PropertyDispatchWrap.initial(BlockStateProperties.AXIS)
                        .select(Direction.Axis.Y, model)
                        .select(Direction.Axis.Z, model.with(BlockModelGenerators.X_ROT_90))
                        .select(Direction.Axis.X, model.with(VariantMutator.Z_ROT.withValue(Quadrant.R90)))
                        .dispatch()
                ));
            }
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
        tables.add(
            block, LootTable.lootTable()
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

}
