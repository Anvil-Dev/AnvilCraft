package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StoragePortConsolidatorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.item.ChuteBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RepeaterBlock;

/** 只读判断交互优先级，预览阶段不能调用 use 方法打开界面或改变世界。 */
public final class PlacementInteractions {
    private PlacementInteractions() {
    }

    public static boolean allowsPlacement(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || player.isSpectator()) return false;
        if (BuildingRodItem.isHeld(player)) return true;
        if (context.getHand() == InteractionHand.OFF_HAND && player.getMainHandItem().getUseAnimation() != UseAnim.NONE) return false;
        if (context.getItemInHand().getItem() instanceof ChuteBlockItem && ChuteBlockItem.isStorageInteraction(context)) return false;
        if (player.isSecondaryUseActive()) return true;
        var state = context.getLevel().getBlockState(context.getClickedPos());
        var block = state.getBlock();
        BlockPos menuPos = block instanceof AbstractMultiPartBlock<?> multipart
            ? multipart.getMainPartPos(context.getClickedPos(), state) : context.getClickedPos();
        var entity = context.getLevel().getBlockEntity(menuPos);
        if (entity instanceof StoragePortBlockEntity || entity instanceof StoragePortConsolidatorBlockEntity) {
            if (context.getItemInHand().getItem() instanceof ChuteBlockItem) return !ChuteBlockItem.isStorageInteraction(context);
            return context.getHand() != InteractionHand.MAIN_HAND;
        }
        if (state.getMenuProvider(context.getLevel(), menuPos) != null
            || entity instanceof MenuProvider || entity instanceof StorageBlockEntity) return false;
        return !(block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof BedBlock || block instanceof BellBlock
            || block instanceof RepeaterBlock || block instanceof ComparatorBlock || block instanceof NoteBlock
            || block instanceof FenceGateBlock || block instanceof CakeBlock && player.canEat(false)
            || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(ModBlocks.BIG_RED_BUTTON));
    }
}
