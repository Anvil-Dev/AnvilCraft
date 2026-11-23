package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.FishTankBlock;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.OilCauldronBlock;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * 装有原油的炼药锅、鱼缸、大型炼药锅共用的点燃逻辑。
 */
public final class CauldronIgnition {
    private CauldronIgnition() {
    }

    /** 打火石、烈焰弹与打火石模式的多功能工具。 */
    public static boolean isIgnitionItem(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL)
               || stack.is(Items.FIRE_CHARGE)
               || (stack.is(ModItems.MULTITOOL_ITEM) && MultitoolItem.getMode(stack) == MultitoolItem.FLINT_AND_STEEL_MODE);
    }

    static ItemInteractionResult useOn(
        BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack
    ) {
        if (!CauldronIgnition.isIgnitionItem(stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!CauldronIgnition.tryIgnite(level, pos, state)) {
            return ItemInteractionResult.FAIL;
        }
        return CauldronIgnition.consumeIgnitionItem(level, pos, player, hand, stack);
    }

    public static ItemInteractionResult consumeIgnitionItem(
        Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack
    ) {
        if (!level.isClientSide()) {
            if (stack.is(Items.FIRE_CHARGE)) {
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS);
            } else {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** 手持锹（含可执行锹动作的工具）。 */
    public static boolean isShovel(ItemStack stack) {
        return stack.getItem() instanceof ShovelItem
               || stack.getItem().canPerformAction(stack, ItemAbilities.SHOVEL_DOUSE);
    }

    /**
     * 用锹熄灭着火的锅，消耗 1 点耐久，音效与粒子与熄灭营火一致。
     */
    public static ItemInteractionResult useShovelToExtinguish(
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        ItemStack stack,
        Runnable extinguish
    ) {
        if (!level.isClientSide()) {
            extinguish.run();
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** 客户端仅预测点燃结果，服务端负责修改容器。 */
    public static boolean tryIgnite(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FishTankBlock tank) {
            return tank.tryIgnite(level, pos);
        }
        if (state.getBlock() instanceof OilCauldronBlock) {
            if (!level.isClientSide()) {
                OilCauldronBlock.ignite(level, pos, state);
            }
            return true;
        }
        if (state.getBlock() instanceof LargeCauldronBlock) {
            LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(level, pos, state);
            return cauldron != null && cauldron.tryIgnite(level.isClientSide());
        }
        return false;
    }
}
