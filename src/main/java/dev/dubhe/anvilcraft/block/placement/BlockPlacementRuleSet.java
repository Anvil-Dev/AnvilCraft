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

import java.util.List;
import java.util.function.Function;

/**
 * The state-to-item mappings loaded from one block's placement rule file.
 */
public record BlockPlacementRuleSet(List<StateRule> rules) implements IBlockPlacementRule {
    public static final Codec<BlockPlacementRuleSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        StateRule.CODEC.listOf().fieldOf("rules").forGetter(BlockPlacementRuleSet::rules)
    ).apply(instance, BlockPlacementRuleSet::new));

    public BlockPlacementRuleSet {
        rules = List.copyOf(rules);
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

    public record StateRule(List<String> properties, Item item, int count) {
        private static final Codec<List<String>> PROPERTIES_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf())
            .xmap(
                either -> either.map(List::of, Function.identity()),
                values -> values.size() == 1 ? Either.left(values.getFirst()) : Either.right(values)
            );
        private static final Codec<Integer> COUNT_CODEC = Codec.INT.validate(count -> count == -1 || count > 0
            ? DataResult.success(count)
            : DataResult.error(() -> "Block placement item count must be -1 or positive"));
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

        private static boolean matchesExpression(BlockState state, String expression) {
            if (expression.isBlank()) {
                return true;
            }
            for (String assignment : expression.split(",")) {
                String[] parts = assignment.trim().split("=", 2);
                if (parts.length != 2 || !matchesProperty(state, parts[0].trim(), parts[1].trim())) {
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
}
