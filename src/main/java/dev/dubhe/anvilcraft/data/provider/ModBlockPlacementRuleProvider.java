package dev.dubhe.anvilcraft.data.provider;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet.StateRule;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Generates built-in state-to-placement-item mappings, one file per block.
 */
public final class ModBlockPlacementRuleProvider implements DataProvider {
    private static final List<IntegerProperty> COUNT_PROPERTIES = List.of(
        BlockStateProperties.LAYERS,
        BlockStateProperties.PICKLES,
        BlockStateProperties.EGGS,
        BlockStateProperties.CANDLES,
        BlockStateProperties.FLOWER_AMOUNT
    );

    private final PackOutput.PathProvider pathProvider;

    public ModBlockPlacementRuleProvider(PackOutput output) {
        this.pathProvider = output.createRegistryElementsPathProvider(ModRegistries.BLOCK_PLACEMENT_RULES_KEY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        Map<Block, List<StateRule>> rulesByBlock = new LinkedHashMap<>();
        Map<Block, Map<String, String>> stateRulesByBlock = new LinkedHashMap<>();
        BuiltInRegistries.BLOCK.forEach(block -> addStateCountRules(rulesByBlock, block));
        addSpecialItemRules(rulesByBlock);
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

    private void saveRuleSet(
        CachedOutput output,
        List<CompletableFuture<?>> saves,
        ResourceLocation id,
        BlockPlacementRuleSet ruleSet
    ) {
        JsonElement json = BlockPlacementRuleSet.CODEC.encodeStart(JsonOps.INSTANCE, ruleSet).getOrThrow();
        saves.add(DataProvider.saveStable(output, json, this.pathProvider.json(id)));
    }

    private static void addStateCountRules(Map<Block, List<StateRule>> rulesByBlock, Block block) {
        Item item = block.asItem();
        if (item == Items.AIR) {
            return;
        }
        if (block instanceof SlabBlock) {
            addRule(rulesByBlock, block, "type=bottom", item, 1);
            addRule(rulesByBlock, block, "type=top", item, 1);
            addRule(rulesByBlock, block, "type=double", item, 2);
            return;
        }
        for (IntegerProperty property : COUNT_PROPERTIES) {
            if (!block.defaultBlockState().hasProperty(property)) {
                continue;
            }
            property.getPossibleValues().forEach(value -> addRule(
                rulesByBlock,
                block,
                property.getName() + "=" + property.getName(value),
                item,
                value
            ));
            return;
        }
        if (block instanceof MultifaceBlock || block instanceof VineBlock) {
            addMultifaceRules(rulesByBlock, block, item);
        }
    }

    private static void addMultifaceRules(Map<Block, List<StateRule>> rulesByBlock, Block block, Item item) {
        List<BooleanProperty> faceProperties = PipeBlock.PROPERTY_BY_DIRECTION.values().stream()
            .filter(block.defaultBlockState()::hasProperty)
            .sorted(Comparator.comparing(Property::getName))
            .toList();
        Map<Integer, TreeSet<String>> selectorsByCount = new TreeMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            int count = (int) faceProperties.stream().filter(state::getValue).count();
            if (count == 0) {
                continue;
            }
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

    private static void addSpecialItemRules(Map<Block, List<StateRule>> rulesByBlock) {
        addRule(rulesByBlock, Blocks.ATTACHED_MELON_STEM, "", Items.MELON_SEEDS, 1);
        addRule(rulesByBlock, Blocks.ATTACHED_PUMPKIN_STEM, "", Items.PUMPKIN_SEEDS, 1);
        addRule(rulesByBlock, Blocks.BAMBOO_SAPLING, "", Items.BAMBOO, 1);
        addRule(rulesByBlock, Blocks.BIG_DRIPLEAF_STEM, "", Items.BIG_DRIPLEAF, 1);
        addRule(rulesByBlock, Blocks.TALL_GRASS, "", Items.TALL_GRASS, 1);
        addRule(rulesByBlock, Blocks.LARGE_FERN, "", Items.LARGE_FERN, 1);
        addRule(rulesByBlock, Blocks.PISTON_HEAD, "", Items.PISTON, -1);

        BuiltInRegistries.BLOCK.forEach(block -> {
            if (block instanceof CandleCakeBlock candleCake) {
                addRule(rulesByBlock, block, "", Items.CAKE, 1);
                addRule(rulesByBlock, block, "", candleCake.candleBlock.asItem(), 1);
            }
        });
    }

    private static void addBlueprintStateRules(Map<Block, Map<String, String>> stateRulesByBlock) {
        addStateRule(stateRulesByBlock, Blocks.PISTON, "", "extended=false");
        addStateRule(stateRulesByBlock, Blocks.STICKY_PISTON, "", "extended=false");
    }

    @SuppressWarnings("SameParameterValue")
    private static void addStateRule(
        Map<Block, Map<String, String>> stateRulesByBlock,
        Block block,
        String properties,
        String directives
    ) {
        stateRulesByBlock.computeIfAbsent(block, ignored -> new LinkedHashMap<>()).put(properties, directives);
    }

    private static void addRule(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        String properties,
        Item item,
        int count
    ) {
        addRule(rulesByBlock, block, List.of(properties), item, count);
    }

    private static void addRule(
        Map<Block, List<StateRule>> rulesByBlock,
        Block block,
        List<String> properties,
        Item item,
        int count
    ) {
        rulesByBlock.computeIfAbsent(block, ignored -> new ArrayList<>())
            .add(new StateRule(properties, item, count));
    }

    @Override
    public String getName() {
        return "AnvilCraft Block Placement Rules";
    }
}
