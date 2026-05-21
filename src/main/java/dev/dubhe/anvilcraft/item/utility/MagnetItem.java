package dev.dubhe.anvilcraft.item.utility;

import dev.dubhe.anvilcraft.api.item.IChargerChargeable;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.MagnetUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class MagnetItem extends Item implements IChargerChargeable {
    public MagnetItem(Properties properties) {
        super(properties.repairable(ModItemTags.MAGNET_INGOTS).enchantable(1));
    }

    @Override
    public InteractionResult use(
        Level level,
        Player player,
        InteractionHand usedHand
    ) {
        return MagnetUtil.magnetizeItems(level, player, usedHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return MagnetUtil.placeMagnetizedNode(context);
    }

    @Override
    public ItemStack charge(ItemStack input) {
        return ModItems.MAGNET.asStack(1);
    }
}
