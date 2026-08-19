package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import static dev.dubhe.anvilcraft.util.MagnetUtil.magnetizeItems;
import static dev.dubhe.anvilcraft.util.MagnetUtil.placeMagnetizedNode;

public class MagnetItem extends Item {
    public MagnetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand usedHand
    ) {
        return magnetizeItems(this, level, player, usedHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return placeMagnetizedNode(this, context);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.MAGNET_INGOT);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 1;
    }
}
