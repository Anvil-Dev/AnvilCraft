package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.PlayerEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.item.ResinBlockItem;
import dev.dubhe.anvilcraft.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerEventListener {
    /**
     * @param event 玩家右键实体事件
     */
    @SubscribeEvent
    public void useEntity(@NotNull PlayerEvent.UseEntity event) {
        InteractionHand hand = event.getHand();
        Player player = event.getEntity();
        ItemStack item = player.getItemInHand(hand);
        Entity target = event.getTarget();
        if (item.is(ModBlocks.RESIN_BLOCK.asItem())) {
            event.setResult(ResinBlockItem.useEntity(player, target, item));
        }
    }

    /**
     * @param event 玩家加入事件
     */
    @SubscribeEvent
    public void onPlayerJoin(@NotNull PlayerEvent.ClientPlayerJoin event) {
        if (Utils.isLoaded("jei") && !Utils.isLoaded("emi")) {
            LocalPlayer entity = event.getEntity();
            entity.sendSystemMessage(
                Component.literal("[").withStyle(ChatFormatting.GREEN).append(
                    Component.translatable("modmenu.nameTranslation.anvilcraft").withStyle(ChatFormatting.GOLD).append(
                        Component.literal("] ").withStyle(ChatFormatting.GREEN).append(
                            Component.translatable(
                                "tooltip.anvilcraft.only_jei",
                                Component.literal("EMI").withStyle(ChatFormatting.BLUE).withStyle(
                                    style -> style.withClickEvent(
                                        new ClickEvent(
                                            ClickEvent.Action.OPEN_URL,
                                            "https://modrinth.com/mod/emi"
                                        )
                                    )
                                )
                            ).withStyle(ChatFormatting.WHITE)
                        )
                    )
                )
            );
        }
    }
}
