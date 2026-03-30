package dev.dubhe.anvilcraft.client.event;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber
public class InputEventListener {
    @Getter
    private static ItemStack hotbarSelectedItem;

    static {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            hotbarSelectedItem = ItemStack.EMPTY;
        } else {
            hotbarSelectedItem = player.getInventory().getItem(player.getInventory().selected);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int selected = player.getInventory().selected;
        ItemStack itemStack = player.getInventory().items.get(selected);
        if (!itemStack.isEmpty()) {
            hotbarSelectedItem = itemStack;
        } else {
            hotbarSelectedItem = ItemStack.EMPTY;
        }
    }
}
