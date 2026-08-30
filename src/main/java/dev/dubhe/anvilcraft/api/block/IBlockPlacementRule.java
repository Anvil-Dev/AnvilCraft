package dev.dubhe.anvilcraft.api.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 定义放置匹配方块状态所需的物品
 */
public interface IBlockPlacementRule {
    boolean matches(BlockState state);

    List<PlacementItem> getPlacementItems(BlockState state);

    /**
     * 规则优先级，越大越优先，用于决定 Fallback 的尝试顺序
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 判断该规则是否允许创建（放置）对应方块。
     */
    default boolean canCreate(BlockState state) {
        return this.matches(state);
    }

    /**
     * 放置所需的物品与放置后返还的物品。
     *
     * @param placeStack  放置消耗的物品（为空表示禁止放置）
     * @param returnStack 放置后返还的物品（如细雪桶放置后返还空桶），没有则为 {@link ItemStack#EMPTY}
     */
    record PlacementItem(ItemStack placeStack, ItemStack returnStack) {
        public PlacementItem(Item item, int count, @Nullable Item returnItem) {
            this(
                count < 0 ? ItemStack.EMPTY : new ItemStack(item, count),
                returnItem != null ? new ItemStack(returnItem) : ItemStack.EMPTY
            );
        }

        public PlacementItem(Item item, int count) {
            this(item, count, null);
        }

        public boolean isForbidden() {
            return this.placeStack.isEmpty();
        }

        public Item item() {
            return this.placeStack.getItem();
        }

        public int count() {
            return this.placeStack.getCount();
        }
    }
}
