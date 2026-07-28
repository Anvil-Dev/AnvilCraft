package dev.dubhe.anvilcraft.block.placement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.api.block.IBlockPlacementRule;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
        BlockState result = resultState;
        for (String rawDirective : directives.split(",")) {
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

    public record StateRule(List<String> properties, Item item, int count) {
        private static final Codec<List<String>> PROPERTIES_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf())
            .xmap(
                either -> either.map(List::of, Function.identity()),
                values -> values.size() == 1 ? Either.left(values.getFirst()) : Either.right(values)
            );
        private static final Codec<Integer> COUNT_CODEC = Codec.INT.validate(
            count -> count == -1 || count > 0
                     ? DataResult.success(count)
                     : DataResult.error(() -> "Block placement item count must be -1 or positive")
        );
        public static final Codec<StateRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PROPERTIES_CODEC.fieldOf("properties").forGetter(StateRule::properties),
            CodecUtil.ITEM.fieldOf("item").forGetter(StateRule::item),
            COUNT_CODEC.fieldOf("count").forGetter(StateRule::count)
        ).apply(instance, StateRule::new));

        public StateRule {
            properties = List.copyOf(properties);
            if (count != -1 && count <= 0) {
                throw new IllegalArgumentException("Block placement item count must be -1 or positive");
            }
        }

        public boolean matches(BlockState state) {
            return this.properties.stream().anyMatch(expression -> matchesExpression(state, expression));
        }

        public PlacementItem placementItem() {
            return new PlacementItem(this.item, this.count);
        }

    }

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
            if (parts.length != 2) {
                return false;
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
