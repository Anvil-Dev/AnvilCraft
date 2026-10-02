package dev.dubhe.anvilcraft.block.placement;

import dev.dubhe.anvilcraft.api.block.IBlockPlacementRule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 按方块类匹配的 Fallback 父类：匹配特定方块类（如所有台阶、楼梯、活板门）的任意状态，
 * 放置消耗 1 个该方块对应 ID 的 BlockItem。
 *
 * @param <T> 匹配的方块类型
 */
public abstract class ClassBlockPlacementRule<T extends Block> implements IBlockPlacementRule {
    private final Class<T> blockClass;

    protected ClassBlockPlacementRule(Class<T> blockClass) {
        this.blockClass = blockClass;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean canCreate(BlockState state) {
        return this.blockClass.isInstance(state.getBlock());
    }

    @Override
    public boolean matches(BlockState state) {
        return this.blockClass.isInstance(state.getBlock());
    }

    @Override
    public List<PlacementItem> getPlacementItems(BlockState state) {
        return simplePlacementItems(state.getBlock());
    }

    /**
     * 生成 1 个对应 ID BlockItem 的放置物品列表；该方块无对应 ID 的 BlockItem 时返回空列表。
     */
    protected static List<PlacementItem> simplePlacementItems(Block block) {
        Item item = block.asItem();
        if (item == Items.AIR) {
            return List.of();
        }
        if (!BuiltInRegistries.ITEM.getKey(item).equals(BuiltInRegistries.BLOCK.getKey(block))) {
            return List.of();
        }
        return List.of(new PlacementItem(new ItemStack(item, 1), ItemStack.EMPTY));
    }
}
