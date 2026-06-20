package dev.dubhe.anvilcraft.item.utility;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.EnergyWeaponMakeMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EnergyWeaponPlatformItem extends Item {
    public static final int STORED_ENERGY = 640_000_000;

    public EnergyWeaponPlatformItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) return InteractionResult.PASS;
        ModMenuTypes.open(
            Util.cast(player),
            new SimpleMenuProvider(
                (cid, inv, ign) -> new EnergyWeaponMakeMenu(cid, inv),
                this.getName(stack)
            )
        );
        return InteractionResult.SUCCESS_SERVER;
    }
}
