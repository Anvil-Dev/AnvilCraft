package dev.dubhe.anvilcraft.api.amulet.fromto;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

public record Effect(InventoryTick inventoryTick, ImmuneDamage immuneDamage) {
    public static final Effect NOP = new Effect(InventoryTick.NOP, ImmuneDamage.NEVER);

    public void inventoryTick(ServerPlayer player, ItemStack amulet, Boolean isEnabled) {
        this.inventoryTick.inventoryTick(player, amulet, isEnabled);
    }

    public boolean shouldImmuneDamage(ServerPlayer player, DamageSource source) {
        return this.immuneDamage.shouldImmuneDamage(player, source);
    }
}
