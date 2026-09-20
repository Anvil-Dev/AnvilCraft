package dev.dubhe.anvilcraft.block.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * 水面放置物品：玩家不在水下时贴着水面放置，在水下时改用普通方块放置逻辑。
 *
 * <p>两者落点来源不同：水面靠流体射线找到水面格；水下时玩家自身泡在水里，
 * 流体射线会命中紧贴自己的那格水，而该格随后又被玩家自己的碰撞箱判为阻塞
 * （玩家 {@code blocksBuilding}），落点既不可预期也不符合直觉。水下按普通方块放置即可。</p>
 */
public class PlaceInWaterBlockItem extends BlockItem {

    public PlaceInWaterBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * 玩家在水下时按普通方块放置；否则返回 {@link InteractionResult#PASS}，
     * 把交互交给 {@link #use} 用流体射线决定水面落点。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isUnderWater()) {
            return super.useOn(context);
        }
        return InteractionResult.PASS;
    }

    /**
     * 列出水面放置会依次尝试的落点上下文，顺序与实际放置完全一致。
     *
     * <p>落点由流体射线（{@link ClipContext.Fluid#SOURCE_ONLY}）决定，首个落点放不下时
     * 还会沿玩家朝向再试一格。预览必须按同样顺序逐个判定，否则会「预览在此、实际在彼」。</p>
     *
     * @param level  所在世界
     * @param player 玩家
     * @param hand   使用的手
     * @return 依次尝试的放置上下文；射线未命中时为空
     */
    public static List<UseOnContext> surfaceCandidates(Level level, Player player, InteractionHand hand) {
        BlockHitResult fluidHit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (fluidHit.getType() == HitResult.Type.MISS) {
            return List.of();
        }
        return List.of(
            new UseOnContext(player, hand, fluidHit.withPosition(fluidHit.getBlockPos())),
            new UseOnContext(player, hand, fluidHit.withPosition(
                fluidHit.getBlockPos().relative(player.getDirection())))
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand usedHand
    ) {
        ItemStack stack = player.getItemInHand(usedHand);
        // 水下已由 useOn 走普通放置，这里不再做水面放置
        if (player.isUnderWater()) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult interactionResult = InteractionResult.PASS;
        for (UseOnContext candidate : PlaceInWaterBlockItem.surfaceCandidates(level, player, usedHand)) {
            interactionResult = super.useOn(candidate);
            if (interactionResult.indicateItemUse()) {
                break;
            }
        }
        return new InteractionResultHolder<>(interactionResult, stack);
    }
}
