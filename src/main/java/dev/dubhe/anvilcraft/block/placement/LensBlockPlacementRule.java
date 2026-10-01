package dev.dubhe.anvilcraft.block.placement;

import dev.dubhe.anvilcraft.block.LensBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** 透镜的建造材料包含镜框和当前安装的玻璃。 */
public final class LensBlockPlacementRule extends ClassBlockPlacementRule<LensBlock> {
    public static final LensBlockPlacementRule INSTANCE = new LensBlockPlacementRule();

    private LensBlockPlacementRule() {
        super(LensBlock.class);
    }

    @Override
    public List<PlacementItem> getPlacementItems(BlockState state) {
        List<PlacementItem> items = new ArrayList<>(super.getPlacementItems(state));
        ItemStack glass = LensBlock.getGlassItem(state.getValue(LensBlock.TYPE));
        if (!items.isEmpty() && !glass.isEmpty()) items.add(new PlacementItem(glass, ItemStack.EMPTY));
        return items;
    }
}
