package dev.dubhe.anvilcraft.api.entity;

import dev.dubhe.anvilcraft.util.GravityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public interface IAnvilCraftEntityExtension {
    default GravityType anvilcraft$getGravityType() {
        return null;
    }

    default Vec3 anvilcraft$getAdditionalGravity(double baseGravity) {
        return Vec3.ZERO;
    }

    default boolean anvilcraft$isMagnetized() {
        return false;
    }

    default boolean anvilcraft$canCollisionCraft() {
        return true;
    }

    default boolean anvilcraft$acceptMagnetization(Player player, ItemStack stack) {
        return false;
    }
}
