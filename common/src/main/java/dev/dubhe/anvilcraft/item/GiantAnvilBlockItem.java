package dev.dubhe.anvilcraft.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class GiantAnvilBlockItem extends BlockItem {
    public GiantAnvilBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        InteractionResult result = super.useOn(context);
        if (result == InteractionResult.FAIL) {
            return super.useOn(new UseOnContext(
                    context.getLevel(),
                    context.getPlayer(),
                    context.getHand(),
                    context.getItemInHand(),
                    new BlockHitResult(
                            context.getClickLocation().relative(context.getClickedFace(), 2),
                            context.getClickedFace(),
                            context.getClickedPos().relative(context.getClickedFace(), 2),
                            false
                    )
            ));
        }
        return result;
    }
}
