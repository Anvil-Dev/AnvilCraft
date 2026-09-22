package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Comrades;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.function.Consumer;

public class ComradeAmuletItem extends Item {
    public ComradeAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipComponents, flag);
        Comrades comrades = stack.get(ModComponents.COMRADES);
        if (comrades == null) return;
        tooltipComponents.accept(Component.translatable("item.anvilcraft.comrade_amulet.tooltip").withStyle(ChatFormatting.GRAY));
        for (UUID id : comrades.players()) {
            Level level = context.level();
            Component entry;
            if (level != null) {
                Player player = level.getPlayerInAnyDimension(id);
                if (player == null) {
                    entry = Component.literal(
                        Util.getServices(level).nameToIdCache().get(id).map(NameAndId::name).orElse(id.toString())
                    );
                } else {
                    entry = player.getDisplayName();
                }
            } else {
                entry = Component.literal(id.toString());
            }
            tooltipComponents.accept(Component.translatable("tooltip.anvilcraft.comrade_amulet.player", entry));
        }
    }
}
