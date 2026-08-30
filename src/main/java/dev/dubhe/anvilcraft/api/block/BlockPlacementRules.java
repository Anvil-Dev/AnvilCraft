package dev.dubhe.anvilcraft.api.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet;
import dev.dubhe.anvilcraft.block.placement.SimpleBlockPlacementRule;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Resolves the data pack placement rule named after a block, falling back to
 * code-level {@link IBlockPlacementRule Fallback} rules when no data pack rule exists.
 */
public final class BlockPlacementRules {
    private static final ResourceKey<BlockPlacementRuleSet> DEFAULT_RULE_KEY = ResourceKey.create(
        ModRegistryKeys.BLOCK_PLACEMENT_RULES,
        AnvilCraft.of("default")
    );

    /**
     * 代码层注册的 Fallback 规则，按优先级（降序）→ 类名 → 包路径排序。
     */
    private static final List<IBlockPlacementRule> FALLBACKS = new ArrayList<>();

    private static final Comparator<IBlockPlacementRule> FALLBACK_COMPARATOR = Comparator
        .comparingInt(IBlockPlacementRule::getPriority)
        .reversed()
        .thenComparing(rule -> rule.getClass().getSimpleName())
        .thenComparing(rule -> rule.getClass().getPackageName());

    private BlockPlacementRules() {
    }

    /**
     * 在代码层面注册一个 Fallback 规则。
     */
    public static void registerFallback(IBlockPlacementRule rule) {
        FALLBACKS.add(rule);
        FALLBACKS.sort(FALLBACK_COMPARATOR);
    }

    public static List<IBlockPlacementRule.PlacementItem> getPlacementItems(
        @Nullable HolderLookup.Provider registries,
        BlockState state
    ) {
        IBlockPlacementRule rule = getRule(registries, state);
        return rule == null ? List.of() : rule.getPlacementItems(state);
    }

    private static @Nullable IBlockPlacementRule getRule(
        @Nullable HolderLookup.Provider registries,
        BlockState state
    ) {
        // 1. 优先从数据包里找
        if (registries != null) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            ResourceKey<BlockPlacementRuleSet> ruleKey = ResourceKey.create(
                ModRegistryKeys.BLOCK_PLACEMENT_RULES,
                blockId
            );
            IBlockPlacementRule rule = registries.lookup(ModRegistryKeys.BLOCK_PLACEMENT_RULES)
                .flatMap(lookup -> lookup.get(ruleKey))
                .map(holder -> (IBlockPlacementRule) holder.value())
                .orElse(null);
            if (rule != null) {
                return rule;
            }
        }
        // 2. 找不到则尝试 Fallback
        for (IBlockPlacementRule fallback : FALLBACKS) {
            if (fallback.canCreate(state)) {
                return fallback;
            }
        }
        // 3. 无额外方块状态且物品 ID 与方块 ID 一致的方块动态生成 SimpleBlockPlacementRule
        return SimpleBlockPlacementRule.of(state.getBlock());
    }

    public static BlockState applyBlueprintStateRules(
        HolderLookup.Provider registries,
        BlockState baseState,
        BlockState blueprintState
    ) {
        BlockState result = BlockPlacementRuleSet.inheritBlueprintState(baseState, blueprintState);
        return registries.lookup(ModRegistryKeys.BLOCK_PLACEMENT_RULES).map(lookup -> {
            BlockState transformed = lookup.get(DEFAULT_RULE_KEY)
                .map(holder -> holder.value().applyStateRules(blueprintState, result))
                .orElse(result);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blueprintState.getBlock());
            ResourceKey<BlockPlacementRuleSet> blockRuleKey = ResourceKey.create(
                ModRegistryKeys.BLOCK_PLACEMENT_RULES,
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
            .map(IBlockPlacementRule.PlacementItem::placeStack)
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
            ItemStack stack = item.placeStack();
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }

    /**
     * 放置 {@code state} 后需要返还给源的物品，没有则为 {@link ItemStack#EMPTY}
     */
    public static ItemStack getReturnItem(HolderLookup.Provider registries, BlockState state) {
        List<IBlockPlacementRule.PlacementItem> placementItems = getPlacementItems(registries, state);
        if (placementItems.stream().anyMatch(IBlockPlacementRule.PlacementItem::isForbidden)) {
            return ItemStack.EMPTY;
        }
        for (IBlockPlacementRule.PlacementItem item : placementItems) {
            ItemStack returnStack = item.returnStack();
            if (!returnStack.isEmpty()) {
                return returnStack;
            }
        }
        return ItemStack.EMPTY;
    }
}
