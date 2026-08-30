package dev.dubhe.anvilcraft.block.placement;

import dev.dubhe.anvilcraft.api.block.IBlockPlacementRule;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 按方块属性匹配的 Fallback 父类：匹配状态属性仅为指定集合内属性的方块
 * （如仅有 {@code FACING}/{@code HORIZONTAL_FACING}/{@code ROTATION_16} 的方块）。
 */
public abstract class PropertyBlockPlacementRule implements IBlockPlacementRule {
    private final Set<Property<?>> allowedProperties;

    protected PropertyBlockPlacementRule(Set<Property<?>> allowedProperties) {
        this.allowedProperties = Set.copyOf(allowedProperties);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean canCreate(BlockState state) {
        return this.matches(state);
    }

    @Override
    public boolean matches(BlockState state) {
        Collection<Property<?>> properties = state.getProperties();
        return !properties.isEmpty() && this.allowedProperties.containsAll(properties);
    }

    @Override
    public List<PlacementItem> getPlacementItems(BlockState state) {
        return ClassBlockPlacementRule.simplePlacementItems(state.getBlock());
    }
}
