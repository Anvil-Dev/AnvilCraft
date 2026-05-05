package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.event.AddAttributeTooltipsEvent;

import java.util.List;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class TooltipEventListener {
    private static final Component FIRE_REFORGING = Component.translatable("tooltip.anvilcraft.property.fire_reforging")
        .withStyle(ChatFormatting.GOLD);
    private static final Component PROVIDENCE = Component.translatable(
        "tooltip.anvilcraft.property.providence",
        Minecraft.getInstance().options.keyShift.getKey().getDisplayName()
    ).withColor(0xFFCB62);
    private static final Component PROVIDENCE_SHIFT = Component.translatable(
        "tooltip.anvilcraft.property.providence.shifting",
        ComponentUtils.formatList(
            List.of(
                Component.translatable("enchantment.minecraft.fortune"),
                Component.translatable("enchantment.minecraft.looting"),
                Component.translatable("enchantment.anvilcraft.beheading"),
                Component.translatable("enchantment.minecraft.thorns"),
                Component.translatable("enchantment.minecraft.luck_of_the_sea")
            ),
            ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR
        )
    ).withColor(0xFFCB62);

    @SubscribeEvent
    public static void onTooltip(AddAttributeTooltipsEvent event) {
        final ItemStack stack = event.getStack();
        final AttributeTooltipContext ctx = event.getContext();
        final TooltipDisplay display = ctx.tooltipDisplay();
        final Consumer<Component> consumer = event::addTooltipLines;
        final TooltipFlag flag = ctx.flag();
        final boolean shift = flag.hasShiftDown();
        stack.addToTooltip(ModComponents.CAN_TAKE_OUT_AMMO, ctx, display, consumer, flag);
        stack.addUnitComponentToTooltip(ModComponents.FIRE_REFORGING, FIRE_REFORGING, display, consumer);
        stack.addToTooltip(ModComponents.MERCILESS, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.FEROCIOUS, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.ETERNAL, ctx, display, consumer, flag);
        TooltipEventListener.addShiftUnitTooltip(ModComponents.PROVIDENCE, PROVIDENCE, PROVIDENCE_SHIFT, stack, shift, display, consumer);
        stack.addToTooltip(ModComponents.MULTIPHASE, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.STORED_ENERGY, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.FLIGHT_TIME, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.SIGNED_PLAYERS, ctx, display, consumer, flag);
    }

    @SuppressWarnings("SameParameterValue")
    private static void addShiftUnitTooltip(
        DataComponentType<Unit> type,
        Component normal,
        Component shift,
        ItemStack stack,
        boolean shifting,
        TooltipDisplay display,
        Consumer<Component> consumer
    ) {
        if (!shifting) {
            stack.addUnitComponentToTooltip(type, normal, display, consumer);
        } else {
            stack.addUnitComponentToTooltip(type, shift, display, consumer);
        }
    }
}
