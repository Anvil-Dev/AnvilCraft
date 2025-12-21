package dev.dubhe.anvilcraft.api.sc.upgrade.level;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IUpgradeLevel extends StringRepresentable {
    boolean canUpgrade(Player player, ItemStack upgrade);

    IUpgradeLevel max();
}
