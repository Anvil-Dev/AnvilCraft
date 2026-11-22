package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.OilCauldronBlock;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.FishTankBlock;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

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
               || (stack.is(ModItems.MULTITOOL_ITEM.asItem())
                   && MultitoolItem.isActingAs(stack, MultitoolMode.FLINT_AND_STEEL));
    }

    /**
     * 玩家手持点燃物品右键容器，点燃成功才消耗耐久或数量。
     *
     * @return {@code null} 表示手持物品不是点燃物品，调用方应继续其它交互
     */
    public static @Nullable InteractionResult useOn(
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        ItemStack stack,
        BooleanSupplier ignite
    ) {
        if (!CauldronIgnition.isIgnitionItem(stack)) return null;
        if (!ignite.getAsBoolean()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (stack.is(Items.FIRE_CHARGE)) {
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return Util.sidedSuccess(level);
    }

    /** 手持锹（含可执行锹动作的工具）。 */
    public static boolean isShovel(ItemStack stack) {
        return stack.getItem() instanceof ShovelItem
               || stack.getItem().canPerformAction(stack, ItemAbilities.SHOVEL_DOUSE);
    }

    /**
     * 用锹熄灭着火的锅，消耗 1 点耐久，音效与粒子与熄灭营火一致。
     */
    public static InteractionResult useShovelToExtinguish(
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        ItemStack stack,
        Runnable extinguish
    ) {
        if (!level.isClientSide()) {
            extinguish.run();
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
        }
        return Util.sidedSuccess(level);
    }

    /**
     * 发射器代玩家点燃前方容器的原油，成功时调用方需要消耗物品耐久或数量。
     */
    public static boolean dispenseIgnite(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FishTankBlock tank) return tank.tryIgnite(level, pos);
        if (state.getBlock() instanceof OilCauldronBlock) {
            OilCauldronBlock.ignite(level, pos, state);
            return true;
        }
        if (state.getBlock() instanceof LargeCauldronBlock
            && LargeCauldronBlockEntity.getMain(level, pos, state) instanceof LargeCauldronBlockEntity cauldron) {
            return cauldron.tryIgnite();
        }
        return false;
    }
}
