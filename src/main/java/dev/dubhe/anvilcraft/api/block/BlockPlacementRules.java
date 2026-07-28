package dev.dubhe.anvilcraft.api.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet;
import dev.dubhe.anvilcraft.block.placement.DefaultBlockPlacementRule;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Resolves the data pack placement rule named after a block.
 */
public final class BlockPlacementRules {
    private static final ResourceKey<BlockPlacementRuleSet> DEFAULT_RULE_KEY = ResourceKey.create(
        ModRegistries.BLOCK_PLACEMENT_RULES_KEY,
        AnvilCraft.of("default")
    );

    private BlockPlacementRules() {
    }

    public static List<IBlockPlacementRule.PlacementItem> getPlacementItems(
        @Nullable HolderLookup.Provider registries,
        BlockState state
    ) {
        IBlockPlacementRule rule = getRule(registries, state);
        List<IBlockPlacementRule.PlacementItem> placementItems = rule.getPlacementItems(state);
        return placementItems.isEmpty() ? DefaultBlockPlacementRule.INSTANCE.getPlacementItems(state) : placementItems;
    }

    private static IBlockPlacementRule getRule(
        @Nullable HolderLookup.Provider registries,
        BlockState state
    ) {
        if (registries == null) {
            return DefaultBlockPlacementRule.INSTANCE;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        ResourceKey<BlockPlacementRuleSet> ruleKey = ResourceKey.create(
            ModRegistries.BLOCK_PLACEMENT_RULES_KEY,
            blockId
        );
        return registries.lookup(ModRegistries.BLOCK_PLACEMENT_RULES_KEY)
            .flatMap(lookup -> lookup.get(ruleKey))
            .map(holder -> (IBlockPlacementRule) holder.value())
            .orElse(DefaultBlockPlacementRule.INSTANCE);
    }

    public static BlockState applyBlueprintStateRules(
        HolderLookup.Provider registries,
        BlockState baseState,
        BlockState blueprintState
    ) {
        BlockState result = BlockPlacementRuleSet.inheritBlueprintState(baseState, blueprintState);
        return registries.lookup(ModRegistries.BLOCK_PLACEMENT_RULES_KEY).map(lookup -> {
            BlockState transformed = lookup.get(DEFAULT_RULE_KEY)
                .map(holder -> holder.value().applyStateRules(blueprintState, result))
                .orElse(result);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blueprintState.getBlock());
            ResourceKey<BlockPlacementRuleSet> blockRuleKey = ResourceKey.create(
                ModRegistries.BLOCK_PLACEMENT_RULES_KEY,
                blockId
            );
            return lookup.get(blockRuleKey)
                .map(holder -> holder.value().applyStateRules(blueprintState, transformed))
                .orElse(transformed);
        }).orElse(result);
    }

    public static List<ItemStack> getPlacementIngredients(
        @Nullable HolderLookup.Provider registries,
        BlockState state
    ) {
        List<IBlockPlacementRule.PlacementItem> placementItems = getPlacementItems(registries, state);
        if (placementItems.stream().anyMatch(IBlockPlacementRule.PlacementItem::isForbidden)) {
            return List.of();
        }
        return placementItems.stream()
            .map(IBlockPlacementRule.PlacementItem::createStack)
            .filter(stack -> !stack.isEmpty())
            .toList();
    }

    public static int getPlacementItemCount(
        HolderLookup.Provider registries,
        BlockState state,
        ItemStack stack
    ) {
        List<IBlockPlacementRule.PlacementItem> placementItems = getPlacementItems(registries, state);
        if (placementItems.stream().anyMatch(IBlockPlacementRule.PlacementItem::isForbidden)) {
            return -1;
        }
        int count = placementItems.stream()
            .filter(item -> !item.isForbidden() && stack.is(item.item()))
            .mapToInt(IBlockPlacementRule.PlacementItem::count)
            .sum();
        return count > 0 ? count : -1;
    }

    public static int getPrimaryPlacementItemCount(HolderLookup.Provider registries, BlockState state) {
        List<IBlockPlacementRule.PlacementItem> placementItems = getPlacementItems(registries, state);
        if (placementItems.stream().anyMatch(IBlockPlacementRule.PlacementItem::isForbidden)) {
            return -1;
        }
        return placementItems.stream()
            .mapToInt(IBlockPlacementRule.PlacementItem::count)
            .filter(count -> count > 0)
            .findFirst()
            .orElse(-1);
    }

    public static @Nullable ItemStack getPrimaryPlacementItem(HolderLookup.Provider registries, BlockState state) {
        List<IBlockPlacementRule.PlacementItem> placementItems = getPlacementItems(registries, state);
        if (placementItems.stream().anyMatch(IBlockPlacementRule.PlacementItem::isForbidden)) {
            return null;
        }
        for (IBlockPlacementRule.PlacementItem item : placementItems) {
            ItemStack stack = item.createStack();
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }
}
