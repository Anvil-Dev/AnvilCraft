package dev.dubhe.anvilcraft.api.amulet.fromto;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.ArrayList;
import java.util.List;

public interface InventoryTick extends TriConsumer<ServerPlayer, ItemStack, Boolean> {
    InventoryTick NOP = (player, amulet, isEnabled) -> {};

    void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled);

    @Override
    default void accept(ServerPlayer player, ItemStack amulet, Boolean isEnabled) {
        this.inventoryTick(player, amulet, isEnabled);
    }

    @Override
    default InventoryTick andThen(TriConsumer<? super ServerPlayer, ? super ItemStack, ? super Boolean> after) {
        return new Multiple(this).andThen(after);
    }

    class Multiple implements InventoryTick {
        private final List<InventoryTick> subs = new ArrayList<>();

        Multiple(InventoryTick inventoryTick) {
            this.subs.add(inventoryTick);
        }

        @Override
        public void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled) {
            for (InventoryTick sub : this.subs) {
                sub.inventoryTick(player, amulet, isEnabled);
            }
        }

        @Override
        public InventoryTick andThen(TriConsumer<? super ServerPlayer, ? super ItemStack, ? super Boolean> after) {
            this.subs.add(after::accept);
            return this;
        }
    }
}
