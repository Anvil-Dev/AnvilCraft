package dev.dubhe.anvilcraft.block.placement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.block.IBlockPlacementRule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * The state-to-item mappings loaded from one block's placement rule file.
 */
public record BlockPlacementRuleSet(List<StateRule> rules, Map<String, String> state) implements IBlockPlacementRule {
    public static final Codec<BlockPlacementRuleSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        StateRule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(BlockPlacementRuleSet::rules),
        Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .optionalFieldOf("state", Map.of())
            .forGetter(BlockPlacementRuleSet::state)
    ).apply(instance, BlockPlacementRuleSet::new));

    public BlockPlacementRuleSet {
        rules = List.copyOf(rules);
        state = Collections.unmodifiableMap(new LinkedHashMap<>(state));
    }

    @Override
    public boolean matches(BlockState state) {
        return this.rules.stream().anyMatch(rule -> rule.matches(state));
    }

    @Override
    public List<PlacementItem> getPlacementItems(BlockState state) {
        return this.rules.stream()
            .filter(rule -> rule.matches(state))
            .map(StateRule::placementItem)
            .toList();
    }

    public BlockState applyStateRules(BlockState blueprintState, BlockState resultState) {
        BlockState result = resultState;
        for (Map.Entry<String, String> entry : this.state.entrySet()) {
            if (matchesExpression(blueprintState, entry.getKey())) {
                result = applyStateDirectives(blueprintState, result, entry.getValue());
            }
        }
        return result;
    }

    public static BlockState inheritBlueprintState(BlockState baseState, BlockState blueprintState) {
        if (!baseState.is(blueprintState.getBlock())) {
            return baseState;
        }
        BlockState result = baseState;
        for (Property<?> property : baseState.getProperties()) {
            result = inheritProperty(result, blueprintState, property);
        }
        return result;
    }

    private static BlockState applyStateDirectives(
        BlockState blueprintState,
        BlockState resultState,
        String directives
    ) {
        String trimmed = directives.trim();
        // 方块转换语法：block:id[prop->,prop=value]（prop-> 从蓝图复制属性）
        if (trimmed.indexOf('[') >= 0 && trimmed.endsWith("]")) {
            return applyBlockTransformation(blueprintState, trimmed);
        }
        BlockState result = resultState;
        for (String rawDirective : trimmed.split(",")) {
            String directive = rawDirective.trim();
            if (directive.isEmpty()) {
                continue;
            }
            if (directive.startsWith("!") && !directive.contains("=")) {
                result = copyNamedProperty(
                    result,
                    result.getBlock().defaultBlockState(),
                    directive.substring(1)
                );
                continue;
            }
            String[] assignment = directive.split("=", 2);
            if (assignment.length == 1) {
                result = copyNamedProperty(result, blueprintState, assignment[0]);
            } else {
                result = setNamedProperty(result, assignment[0], assignment[1]);
            }
        }
        return result;
    }

    /**
     * 将方块转换为另一个方块：{@code block:id[prop->,prop=value]}。
     * {@code prop->} 从蓝图状态复制属性（目标方块无该属性则忽略），
     * {@code prop=value} 设置属性值。
     */
    private static BlockState applyBlockTransformation(BlockState blueprintState, String expression) {
        int bracketIndex = expression.indexOf('[');
        String blockId = expression.substring(0, bracketIndex).trim();
        String propertiesPart = expression.substring(bracketIndex + 1, expression.length() - 1).trim();
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id == null) {
            return blueprintState;
        }
        Block targetBlock = BuiltInRegistries.BLOCK.get(id);
        if (targetBlock == Blocks.AIR) {
            return blueprintState;
        }
        BlockState result = targetBlock.defaultBlockState();
        for (String rawProperty : propertiesPart.split(",")) {
            String property = rawProperty.trim();
            if (property.isEmpty()) {
                continue;
            }
            if (property.endsWith("->")) {
                result = copyNamedProperty(result, blueprintState, property.substring(0, property.length() - 2));
            } else {
                String[] assignment = property.split("=", 2);
                if (assignment.length == 2) {
                    result = setNamedProperty(result, assignment[0], assignment[1]);
                }
            }
        }
        return result;
    }

    private static BlockState copyNamedProperty(BlockState target, BlockState source, String propertyName) {
        return target.getProperties().stream()
            .filter(property -> property.getName().equals(propertyName.trim()))
            .findFirst()
            .map(property -> inheritProperty(target, source, property))
            .orElse(target);
    }

    private static BlockState setNamedProperty(BlockState state, String propertyName, String valueName) {
        return state.getProperties().stream()
            .filter(property -> property.getName().equals(propertyName.trim()))
            .findFirst()
            .map(property -> setPropertyValue(state, property, valueName.trim()))
            .orElse(state);
    }

    private static <T extends Comparable<T>> BlockState inheritProperty(
        BlockState target,
        BlockState source,
        Property<T> property
    ) {
        return source.hasProperty(property) ? target.setValue(property, source.getValue(property)) : target;
    }

    private static <T extends Comparable<T>> BlockState setPropertyValue(
        BlockState state,
        Property<T> property,
        String valueName
    ) {
        return property.getValue(valueName).map(value -> state.setValue(property, value)).orElse(state);
    }

    /**
     * 放置物品（默认 1 数量、无组件），与返还物品（可为空）。
     *
     * @param properties 匹配的方块状态表达式（任一匹配即应用）
     * @param item       放置消耗的物品栈；空栈表示该状态禁止放置
     * @param returnItem 放置后返还的物品栈，没有则为 {@link ItemStack#EMPTY}
     */
    public record StateRule(List<String> properties, ItemStack item, ItemStack returnItem) {
        private static final Codec<List<String>> PROPERTIES_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf())
            .xmap(
                either -> either.map(List::of, Function.identity()),
                values -> values.size() == 1 ? Either.left(values.getFirst()) : Either.right(values)
            );

        /**
         * 物品栈 codec：字符串形式（如 {@code "minecraft:stone"}）表示 1 数量、无组件；
         * 对象形式（如 {@code {"id":"minecraft:stone","count":2,"components":{...}}}）可定义
         * 数量与组件；空对象 {@code {}} 表示空栈（禁止放置）。
         */
        private static final Codec<ItemStack> ITEM_CODEC = Codec.either(
            ItemStack.SIMPLE_ITEM_CODEC,
            ItemStack.OPTIONAL_CODEC
        ).xmap(
            either -> either.map(Function.identity(), Function.identity()),
            stack -> stack.isEmpty() || stack.getCount() != 1 || !stack.getComponentsPatch().isEmpty()
                     ? Either.right(stack)
                     : Either.left(stack)
        );

        public static final Codec<StateRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PROPERTIES_CODEC.fieldOf("properties").forGetter(StateRule::properties),
            ITEM_CODEC.fieldOf("item").forGetter(StateRule::item),
            ITEM_CODEC.optionalFieldOf("return_item")
                .forGetter(rule -> rule.returnItem.isEmpty() ? Optional.empty() : Optional.of(rule.returnItem))
        ).apply(instance, (properties, item, returnItem) ->
            new StateRule(properties, item, returnItem.orElse(ItemStack.EMPTY))));

        public StateRule(List<String> properties, ItemStack item) {
            this(properties, item, ItemStack.EMPTY);
        }

        public StateRule(List<String> properties, Item item, int count) {
            this(properties, count < 0 ? ItemStack.EMPTY : new ItemStack(item, count));
        }

        public StateRule(List<String> properties, Item item, int count, @Nullable Item returnItem) {
            this(
                properties,
                count < 0 ? ItemStack.EMPTY : new ItemStack(item, count),
                returnItem != null ? new ItemStack(returnItem) : ItemStack.EMPTY
            );
        }

        public StateRule {
            properties = List.copyOf(properties);
        }

        public boolean matches(BlockState state) {
            return this.properties.stream().anyMatch(expression -> matchesExpression(state, expression));
        }

        public PlacementItem placementItem() {
            return new PlacementItem(this.item, this.returnItem);
        }

    }

    /**
     * 匹配方块状态表达式。支持逗号分隔的多条件（全部满足才匹配）：
     * <ul>
     *     <li>{@code "a=b"} —— 属性 {@code a} 的值为 {@code b}</li>
     *     <li>{@code "!a=b"} —— 属性 {@code a} 的值不是 {@code b}</li>
     *     <li>{@code "a"} —— 方块状态包含属性 {@code a}</li>
     *     <li>{@code "!a"} —— 方块状态不包含属性 {@code a}（非 a 属性）</li>
     * </ul>
     * 空表达式匹配任意状态。
     */
    private static boolean matchesExpression(BlockState state, String expression) {
        if (expression.isBlank()) {
            return true;
        }
        for (String rawAssignment : expression.split(",")) {
            String assignment = rawAssignment.trim();
            boolean negated = assignment.startsWith("!");
            if (negated) {
                assignment = assignment.substring(1).trim();
            }
            String[] parts = assignment.split("=", 2);
            if (parts.length == 1) {
                // 仅属性名：按属性存在性匹配（!a = 不具有属性 a）
                boolean hasProperty = state.getProperties().stream()
                    .anyMatch(property -> property.getName().equals(parts[0].trim()));
                if (hasProperty == negated) {
                    return false;
                }
                continue;
            }
            boolean matches = matchesProperty(state, parts[0].trim(), parts[1].trim());
            if (matches == negated) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesProperty(BlockState state, String propertyName, String expectedValue) {
        return state.getProperties().stream()
            .filter(property -> property.getName().equals(propertyName))
            .findFirst()
            .map(property -> getValueName(state, property).equals(expectedValue))
            .orElse(false);
    }

    private static <T extends Comparable<T>> String getValueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
