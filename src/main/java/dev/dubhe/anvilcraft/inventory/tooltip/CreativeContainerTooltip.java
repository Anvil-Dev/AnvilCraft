package dev.dubhe.anvilcraft.inventory.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public record CreativeContainerTooltip(List<Entry> entries) implements TooltipComponent {
    public record Entry(ItemStack item, FluidStack fluid, Component text) {
        public static Entry item(ItemStack item) {
            ItemStack icon = item.copyWithCount(1);
            return new Entry(icon, FluidStack.EMPTY, contentText(item.getHoverName()));
        }

        public static Entry fluid(FluidStack fluid) {
            return new Entry(ItemStack.EMPTY, fluid.copy(), contentText(fluid.getHoverName()));
        }

        public boolean isFluid() {
            return !this.fluid.isEmpty();
        }

        private static Component contentText(Component content) {
            return Component.translatable("tooltip.anvilcraft.creative_container.content", content);
        }
    }
}
