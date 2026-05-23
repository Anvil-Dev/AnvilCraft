package dev.dubhe.anvilcraft.api.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.bus.api.Event;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public class AppendCustomHoverTextEvent extends Event {
    private final ItemStack stack;
    private final Item.TooltipContext context;
    private final TooltipDisplay display;
    private final @Nullable Player player;
    private final TooltipFlag tooltipFlag;
    private final Consumer<Component> builder;
}
