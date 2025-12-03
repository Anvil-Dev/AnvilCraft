package dev.dubhe.anvilcraft.integration.guideme;

import guideme.GuidesCommon;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GuideMEBookItem extends Item {
    public GuideMEBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide) {
            GuidesCommon.openGuide(user, GuideMEIntegration.GME_ID);
        }
        return InteractionResultHolder.consume(user.getItemInHand(hand));
    }
}
