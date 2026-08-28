package dev.dubhe.anvilcraft.data.provider;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.Layered4LevelCauldronBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet.StateRule;
import dev.dubhe.anvilcraft.block.placement.SimpleBlockPlacementRule;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.mixin.accessor.CropBlockAccessor;
import dev.dubhe.anvilcraft.mixin.accessor.GrowingPlantAccessor;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Generates built-in state-to-placement-item mappings, one file per block.
 * Covers every block that has a corresponding BlockItem, replicating the logic
 * formerly in {@code DefaultBlockPlacementRule}.
 */
public final class ModBlockPlacementRuleProvider implements DataProvider {
    private static final Set<Property<?>> SIMPLE_FACING_PROPERTIES = Set.of(
        BlockStateProperties.FACING,
        BlockStateProperties.HORIZONTAL_FACING,
        BlockStateProperties.ROTATION_16
    );

    private final PackOutput.PathProvider pathProvider;

    public ModBlockPlacementRuleProvider(PackOutput output) {
        this.pathProvider = output.createRegistryElementsPathProvider(ModRegistryKeys.BLOCK_PLACEMENT_RULES);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        final Map<Block, List<StateRule>> rulesByBlock = new LinkedHashMap<>();
        final Map<Block, Map<String, String>> stateRulesByBlock = new LinkedHashMap<>();

        // 1. Generate explicit per-block rules (overrides the later default)
        BuiltInRegistries.BLOCK.forEach(block -> addBlockRules(rulesByBlock, block));

        // 2. Special items that have no BlockItem (asItem() == AIR)
        addSpecialItemRules(rulesByBlock);

        // 3. Default rule for every remaining block that has a BlockItem and is not
        //    covered by a code-level Fallback (those resolve at runtime instead)
        BuiltInRegistries.BLOCK.forEach(block -> {
            if (rulesByBlock.containsKey(block)) return;
            if (shouldSkipDataPack(block)) return;
            Item item = block.asItem();
            if (item == Items.AIR) return;
            addRule(rulesByBlock, block, "", item, 1, getReturnItemForBlock(block));
        });

        // 4. Blueprint state rules (pistons)
        addBlueprintStateRules(stateRulesByBlock);

        List<CompletableFuture<?>> saves = new ArrayList<>(rulesByBlock.size() + stateRulesByBlock.size() + 1);
        Map<String, String> defaultStateRules = new LinkedHashMap<>();
        defaultStateRules.put("", "!powered,!lit,!waterlogged");
        this.saveRuleSet(
            output,
            saves,
            AnvilCraft.of("default"),
            new BlockPlacementRuleSet(List.of(), defaultStateRules)
        );

        Stream.concat(rulesByBlock.keySet().stream(), stateRulesByBlock.keySet().stream())
            .distinct()
            .sorted(Comparator.comparing(BuiltInRegistries.BLOCK::getKey))
            .forEach(block -> this.saveRuleSet(
                output,
                saves,
                BuiltInRegistries.BLOCK.getKey(block),
                new BlockPlacementRuleSet(
                    rulesByBlock.getOrDefault(block, List.of()),
                    stateRulesByBlock.getOrDefault(block, Map.of())
                )
            ));
        return CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new));
    }

    // ---------- per-block rule generation ----------

    /**
     * Whether this block must not get a data pack rule: it is either covered by a
     * code-level Fallback class (priority 100) or resolvable at runtime by a
     * dynamically generated {@link SimpleBlockPlacementRule}.
     */
    private static boolean shouldSkipDataPack(Block block) {
        if (block instanceof SlabBlock
            || block instanceof StairBlock
            || block instanceof TrapDoorBlock
            || isFacingOnly(block)) {
            return true;
        }
        return SimpleBlockPlacementRule.canTakeOver(block);
    }

    private static boolean isFacingOnly(Block block) {
        Collection<Property<?>> properties = block.getStateDefinition().getProperties();
        return !properties.isEmpty() && SIMPLE_FACING_PROPERTIES.containsAll(properties);
    }

    private static void addBlockRules(Map<Block, List<StateRule>> rulesByBlock, Block block) {
        // Blocks covered by code-level Fallbacks or resolved dynamically at runtime
        // have no data pack rule.
        if (shouldSkipDataPack(block)) return;

        // Multi-block secondary parts
        if (block instanceof BedBlock) {
            Item item = block.asItem();
            if (item != Items.AIR) {
                addRule(rulesByBlock, block, "part=foot", item, 1);
                addRule(rulesByBlock, block, "part=head", item, -1);
            }
            return;
        }
        if (block instanceof DoorBlock || block instanceof DoublePlantBlock) {
            Item item = block.asItem();
            if (item != Items.AIR) {
                addRule(rulesByBlock, block, "half=lower", item, 1);
                addRule(rulesByBlock, block, "half=upper", item, -1);
            }
            return;
        }
        if (block instanceof AbstractMultiPartBlock<?> multiPartBlock) {
            addMultipartRules(rulesByBlock, block, multiPartBlock);
            return;
        }

        Item baseItem = getBaseItem(block);
        if (baseItem == Items.AIR) return;

        // Crops with an age property: only the lowest age may be placed
        IntegerProperty ageProperty = getCropAgeProperty(block);
        if (ageProperty != null) {
            String ageName = ageProperty.getName();
            String minAgeName = ageProperty.getName(ageProperty.getPossibleValues().iterator().next());
            addRule(rulesByBlock, block, ageName + "=" + minAgeName, baseItem, 1);
            return;
        }

        // Fluid cauldrons: place with one bucket of the matching fluid, full state only
        Item bucketItem = getCauldronBucketItem(block);
        if (bucketItem != null) {
            addCauldronRules(rulesByBlock, block, bucketItem);
            return;
        }

        // Honey cauldron: each level needs that many honey bottles
        if (block == ModBlocks.HONEY_CAULDRON.get()) {
            IntegerProperty levelProperty = Layered4LevelCauldronBlock.LEVEL;
            levelProperty.getPossibleValues().forEach(level -> addRule(
                rulesByBlock,
                block,
                levelProperty.getName() + "=" + levelProperty.getName(level),
                Items.HONEY_BOTTLE,
                level
            ));
            return;
        }

        // Fire cauldron: placing it turns it into an oil cauldron (state rule
        // transformation, see addBlueprintStateRules). It uses the same rules
        // as the oil cauldron. Obsidian cauldron cannot be placed by the block
        // placer: forbidden.
        if (block == ModBlocks.FIRE_CAULDRON.get()) {
            addCauldronRules(rulesByBlock, block, ModItems.OIL_BUCKET.get());
            return;
        }
        if (block == ModBlocks.OBSIDIAN_CAULDRON.get()) {
            addRule(rulesByBlock, block, "", Items.AIR, -1);
            return;
        }

        // Integer-count properties (layers, pickles, eggs, candles, flower_amount)
        for (IntegerProperty property : BlockPlacementUtil.COUNT_PROPERTIES) {
            if (!block.defaultBlockState().hasProperty(property)) continue;
            property.getPossibleValues().forEach(value -> addRule(
                rulesByBlock,
                block,
                property.getName() + "=" + property.getName(value),
                baseItem,
                value
            ));
            return;
        }

        // Multiface / vine → count by face combination
        if (BlockPlacementUtil.isMultifaceLike(block)) {
            addMultifaceRules(rulesByBlock, block, baseItem);
            return;
        }

        // Ordinary block: single rule + additional items
        addRule(rulesByBlock, block, "", baseItem, 1, getReturnItemForBlock(block));
        addAdditionalItemRules(rulesByBlock, block);
    }

    /**
     * Compute the placed item for a block, matching the logic of the former
     * {@code DefaultBlockPlacementRule.getBaseItem()} (without the state-dependent
     * count adjustments, which are handled by the caller).
     */
    private static Item getBaseItem(Block block) {
        return switch (block) {
            case CropBlock crop -> ((CropBlockAccessor) crop).invokeGetBaseSeedId().asItem();
            case FlowerPotBlock ignored -> Items.FLOWER_POT;
            case GrowingPlantBodyBlock plantBody -> ((GrowingPlantAccessor) plantBody).invokeGetHeadBlock().asItem();
            case CandleCakeBlock ignored -> Items.CAKE;
            default -> block.asItem();
        };
    }

    /**
     * 含流体炼药锅对应的一桶流体（水锅→水桶、岩浆锅→岩浆桶、细雪锅→细雪桶，
     * 经验液锅→经验桶、油锅→油桶、奶锅→奶桶、熔化宝石锅→熔化宝石桶、水泥锅→水泥桶），
     * 非含流体炼药锅返回 {@code null}
     */
    @Nullable
    private static Item getCauldronBucketItem(Block block) {
        if (block == Blocks.WATER_CAULDRON) {
            return Items.WATER_BUCKET;
        }
        if (block == Blocks.LAVA_CAULDRON) {
            return Items.LAVA_BUCKET;
        }
        if (block == Blocks.POWDER_SNOW_CAULDRON) {
            return Items.POWDER_SNOW_BUCKET;
        }
        if (block == ModBlocks.LAVA_CAULDRON.get()) {
            return Items.LAVA_BUCKET;
        }
        if (block == ModBlocks.EXP_FLUID_CAULDRON.get()) {
            return ModItems.EXP_BUCKET.get();
        }
        if (block == ModBlocks.OIL_CAULDRON.get()) {
            return ModItems.OIL_BUCKET.get();
        }
        if (block == ModBlocks.MILK_CAULDRON.get()) {
            return Items.MILK_BUCKET;
        }
        if (block == ModBlocks.MELT_GEM_CAULDRON.get()) {
            return ModItems.MELT_GEM_BUCKET.get();
        }
        if (block instanceof CementCauldronBlock cement) {
            return ModItems.CEMENT_BUCKETS.get(cement.getColor()).get();
        }
        return null;
    }

    /**
     * 作物的年龄属性（{@link CropBlock}、{@link StemBlock}、{@link CocoaBlock}、
     * 地狱疣、甜浆果丛等含 {@code age} 属性的方块），无年龄属性的方块返回 {@code null}
     */
    @Nullable
    private static IntegerProperty getCropAgeProperty(Block block) {
        return switch (block) {
            case CropBlock crop -> ((CropBlockAccessor) crop).invokeGetAgeProperty();
            case StemBlock ignored -> StemBlock.AGE;
            case CocoaBlock ignored -> CocoaBlock.AGE;
            default -> (IntegerProperty) block.getStateDefinition().getProperties().stream()
                .filter(property -> property.getName().equals("age") && property instanceof IntegerProperty)
                .findFirst()
                .orElse(null);
        };
    }

    /**
     * 含流体炼药锅：放置消耗 1 桶对应流体，且只有满的状态允许放置。
     * 放置后返还空桶。
     */
    private static void addCauldronRules(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        Item bucketItem
    ) {
        IntegerProperty levelProperty = null;
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            if (property.getName().equals("level") && property instanceof IntegerProperty integerProperty) {
                levelProperty = integerProperty;
                break;
            }
        }
        if (levelProperty == null) {
            // 无 level 属性的锅恒定满（岩浆锅、熔化宝石锅、水泥锅）
            addRule(rulesByBlock, block, "", bucketItem, 1, Items.BUCKET);
            return;
        }
        int maxLevel = levelProperty.getPossibleValues().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
        addRule(
            rulesByBlock,
            block,
            levelProperty.getName() + "=" + levelProperty.getName(maxLevel),
            bucketItem,
            1,
            Items.BUCKET
        );
    }

    /**
     * Additional placement items beyond the base block item
     * (e.g. the candle on a candle cake, the plant in a flower pot).
     */
    private static void addAdditionalItemRules(
        Map<Block, List<StateRule>> rulesByBlock, Block block
    ) {
        if (block instanceof CandleCakeBlock candleCake) {
            Item candle = candleCake.candleBlock.asItem();
            if (candle != Items.AIR) {
                addRule(rulesByBlock, block, "", candle, 1);
            }
        } else if (block instanceof FlowerPotBlock pot) {
            Item plant = pot.getPotted().asItem();
            if (plant != Items.AIR) {
                addRule(rulesByBlock, block, "", plant, 1);
            }
        }
    }

    // ---------- custom multi-part blocks ----------

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addMultipartRules(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        AbstractMultiPartBlock multiPartBlock
    ) {
        Item item = block.asItem();
        if (item == Items.AIR) return;
        Property partProperty = multiPartBlock.getPart();
        List<String> mainSelectors = new ArrayList<>();
        List<String> notMainSelectors = new ArrayList<>();
        for (Enum part : multiPartBlock.getParts()) {
            String partName = partProperty.getName(part);
            BlockState partState = multiPartBlock.placedState(part, block.defaultBlockState());
            if (multiPartBlock.isMainPart(partState)) {
                mainSelectors.add(partProperty.getName() + "=" + partName);
                // 非主部件 = "属性值不是任何主部件值"（逗号为 AND 语义）
                notMainSelectors.add("!" + partProperty.getName() + "=" + partName);
            }
        }
        if (!mainSelectors.isEmpty()) {
            addRule(rulesByBlock, block, mainSelectors, item, 1);
        }
        if (!notMainSelectors.isEmpty()) {
            addRule(rulesByBlock, block, String.join(",", notMainSelectors), item, -1);
        }
    }

    // ---------- multiface / vine ----------

    private static void addMultifaceRules(
        Map<Block, List<StateRule>> rulesByBlock, Block block, Item item
    ) {
        List<BooleanProperty> faceProperties = PipeBlock.PROPERTY_BY_DIRECTION.values().stream()
            .filter(block.defaultBlockState()::hasProperty)
            .sorted(Comparator.comparing(Property::getName))
            .toList();
        Map<Integer, TreeSet<String>> selectorsByCount = new TreeMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            int count = (int) faceProperties.stream().filter(state::getValue).count();
            if (count == 0) continue;
            String selector = faceProperties.stream()
                .map(property -> property.getName() + "=" + property.getName(state.getValue(property)))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
            selectorsByCount.computeIfAbsent(count, ignored -> new TreeSet<>()).add(selector);
        }
        selectorsByCount.forEach((count, selectors) -> addRule(
            rulesByBlock,
            block,
            List.copyOf(selectors),
            item,
            count
        ));
    }

    // ---------- special items with no BlockItem ----------

    private static void addSpecialItemRules(Map<Block, List<StateRule>> rulesByBlock) {
        addRuleIfAbsent(rulesByBlock, Blocks.ATTACHED_MELON_STEM, "", Items.MELON_SEEDS, 1);
        addRuleIfAbsent(rulesByBlock, Blocks.ATTACHED_PUMPKIN_STEM, "", Items.PUMPKIN_SEEDS, 1);
        addRuleIfAbsent(rulesByBlock, Blocks.BAMBOO_SAPLING, "", Items.BAMBOO, 1);
        addRuleIfAbsent(rulesByBlock, Blocks.BIG_DRIPLEAF_STEM, "", Items.BIG_DRIPLEAF, 1);
        addRuleIfAbsent(rulesByBlock, Blocks.PISTON_HEAD, "", Items.PISTON, -1);
    }

    // ---------- blueprint state rules ----------

    private static void addBlueprintStateRules(Map<Block, Map<String, String>> stateRulesByBlock) {
        addStateRule(stateRulesByBlock, Blocks.PISTON, "", "extended=false");
        addStateRule(stateRulesByBlock, Blocks.STICKY_PISTON, "", "extended=false");
        // 火锅放置时转换为油锅（复制 level）
        addStateRule(
            stateRulesByBlock,
            ModBlocks.FIRE_CAULDRON.get(),
            "",
            "anvilcraft:oil_cauldron[level->]"
        );
    }

    // ---------- return item (e.g. powder snow → bucket) ----------

    @Nullable
    private static Item getReturnItemForBlock(Block block) {
        if (block == Blocks.POWDER_SNOW) {
            return Items.BUCKET;
        }
        return null;
    }

    // ---------- helpers ----------

    /**
     * Add a rule only if the block has no rules yet, preventing duplicates
     * when {@code addBlockRules} already produced a rule for the same block.
     */
    private static void addRuleIfAbsent(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        @SuppressWarnings("SameParameterValue")
        String properties,
        Item item,
        int count
    ) {
        if (rulesByBlock.containsKey(block)) return;
        addRule(rulesByBlock, block, properties, item, count);
    }

    @SuppressWarnings("SameParameterValue")
    private static void addStateRule(
        Map<Block, Map<String, String>> stateRulesByBlock,
        Block block,
        String properties,
        String directives
    ) {
        stateRulesByBlock.computeIfAbsent(block, ignored -> new LinkedHashMap<>())
            .put(properties, directives);
    }

    private static void addRule(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        String properties,
        Item item,
        int count
    ) {
        addRule(rulesByBlock, block, List.of(properties), item, count, null);
    }

    private static void addRule(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        String properties,
        Item item,
        @SuppressWarnings("SameParameterValue")
        int count,
        @Nullable Item returnItem
    ) {
        addRule(rulesByBlock, block, List.of(properties), item, count, returnItem);
    }

    private static void addRule(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        List<String> properties,
        Item item,
        int count
    ) {
        addRule(rulesByBlock, block, properties, item, count, null);
    }

    private static void addRule(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        List<String> properties,
        Item item,
        int count,
        @Nullable Item returnItem
    ) {
        rulesByBlock.computeIfAbsent(block, ignored -> new ArrayList<>())
            .add(new StateRule(properties, item, count, returnItem));
    }

    private void saveRuleSet(
        CachedOutput output,
        List<CompletableFuture<?>> saves,
        ResourceLocation id,
        BlockPlacementRuleSet ruleSet
    ) {
        JsonElement json = BlockPlacementRuleSet.CODEC.encodeStart(JsonOps.INSTANCE, ruleSet).getOrThrow();
        saves.add(DataProvider.saveStable(output, json, this.pathProvider.json(id)));
    }

    @Override
    public String getName() {
        return "AnvilCraft Block Placement Rules";
    }
}
