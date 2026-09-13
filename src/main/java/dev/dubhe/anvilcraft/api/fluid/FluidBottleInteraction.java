package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** 瓶子按 250 mB 交换；附魔之瓶保留源版一半概率入液、成功交互必定返还空瓶的规则。 */
public final class FluidBottleInteraction {
    private static final int AMOUNT = 250;

    private FluidBottleInteraction() {
    }

    public static boolean tryInteract(
        Player player, InteractionHand hand, ResourceHandler<FluidResource> handler, Level level, BlockPos pos
    ) {
        ItemStack held = player.getItemInHand(hand);
        FluidResource input = input(held);
        boolean filling = !input.isEmpty();
        ItemStack result;
        try (Transaction transaction = Transaction.openRoot()) {
            if (filling) {
                if (handler.insert(input, AMOUNT, transaction) != AMOUNT) return false;
                result = new ItemStack(Items.GLASS_BOTTLE);
                if (!level.isClientSide() && (!held.is(Items.EXPERIENCE_BOTTLE) || level.getRandom().nextBoolean())) transaction.commit();
            } else {
                if (!held.is(Items.GLASS_BOTTLE)) return false;
                FluidResource fluid = FluidResource.EMPTY;
                for (int index = 0; index < handler.size(); index++) {
                    if (!handler.getResource(index).isEmpty()) {
                        fluid = handler.getResource(index);
                        break;
                    }
                }
                result = bottle(fluid);
                if (result.isEmpty() || handler.extract(fluid, AMOUNT, transaction) != AMOUNT) return false;
                if (!level.isClientSide()) transaction.commit();
            }
        }
        if (level.isClientSide()) return true;
        var usedItem = held.getItem();
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
        player.awardStat(filling ? Stats.FILL_CAULDRON : Stats.USE_CAULDRON);
        player.awardStat(Stats.ITEM_USED.get(usedItem));
        level.playSound(null, pos, filling ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1, 1);
        level.gameEvent(null, filling ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
        return true;
    }

    private static FluidResource input(ItemStack stack) {
        if (stack.is(Items.HONEY_BOTTLE)) return FluidResource.of(ModFluids.HONEY.get());
        if (stack.is(Items.EXPERIENCE_BOTTLE)) return FluidResource.of(ModFluids.EXP_FLUID.get());
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        if (stack.is(Items.POTION) && potion != null && potion.potion().filter(value -> value == Potions.WATER).isPresent()) {
            return FluidResource.of(Fluids.WATER);
        }
        return FluidResource.EMPTY;
    }

    private static ItemStack bottle(FluidResource resource) {
        if (resource.isEmpty()) return ItemStack.EMPTY;
        if (resource.getFluid() == Fluids.WATER) return PotionContents.createItemStack(Items.POTION, Potions.WATER);
        if (resource.getFluid() instanceof HoneyFluid) return new ItemStack(Items.HONEY_BOTTLE);
        if (resource.getFluid() == ModFluids.EXP_FLUID.get()) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        return ItemStack.EMPTY;
    }
}
