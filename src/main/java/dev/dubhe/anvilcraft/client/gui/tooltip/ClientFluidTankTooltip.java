package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class ClientFluidTankTooltip implements ClientTooltipComponent {
    private static final int MAX_VISIBLE = 5;
    private final FluidTankTooltip tooltip;
    private final List<Line> lines = new ArrayList<>();
    private final int remaining;

    public ClientFluidTankTooltip(FluidTankTooltip tooltip) {
        this.tooltip = tooltip;
        CompoundTag tank = tooltip.tankTag();
        int remaining = 0;
        if (tooltip.multi()) {
            var entries = tank.getListOrEmpty("Fluids");
            for (int index = 0; index < entries.size(); index++) {
                var entry = entries.getCompoundOrEmpty(index);
                FluidStack fluid = read(entry);
                if (fluid.isEmpty()) continue;
                if (this.lines.size() < MAX_VISIBLE) {
                    this.lines.add(new Line(fluid, tank.getBooleanOr("Enhanced", false) && entry.getBooleanOr("Infinite", false)));
                } else {
                    remaining++;
                }
            }
        } else {
            FluidStack fluid = read(tank);
            if (!fluid.isEmpty()) this.lines.add(new Line(fluid, tooltip.infiniteCapacity()));
        }
        this.remaining = remaining;
    }

    private static FluidStack read(CompoundTag tank) {
        var registries = ClientStoragePortTooltip.registries();
        if (registries == null) return FluidStack.EMPTY;
        return FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
            tank.getCompoundOrEmpty("Fluid")).result().orElse(FluidStack.EMPTY);
    }

    private record Line(FluidStack fluid, boolean infinite) {
        Component text() {
            return Component.empty().append(this.fluid.getHoverName()).append(" " + (this.infinite
                ? UnitUtil.INFINITE_POWER : UnitUtil.fluidUnit(this.fluid.getAmount(), false))).withStyle(ChatFormatting.GRAY);
        }
    }

    private Component moreLine() {
        return Component.translatable("tooltip.anvilcraft.fluid_tank.more", this.remaining).withStyle(ChatFormatting.GRAY);
    }

    private Component capacityLine() {
        long amount = this.lines.stream().mapToLong(line -> line.fluid().getAmount()).sum();
        return (this.tooltip.infiniteCapacity()
            ? Component.translatable("tooltip.anvilcraft.fluid_tank.capacity.value.infinity", UnitUtil.fluidUnit(amount, false))
            : Component.translatable("tooltip.anvilcraft.fluid_tank.capacity.value", UnitUtil.fluidUnit(amount, false),
                UnitUtil.fluidUnit(this.tooltip.capacity(), false))).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public int getHeight(Font font) {
        return (this.lines.isEmpty() ? 0 : 10) + this.lines.size() * 16 + (this.remaining > 0 ? 10 : 0)
            + (this.tooltip.showCapacity() ? 20 : 0);
    }

    @Override
    public int getWidth(Font font) {
        int width = this.lines.isEmpty() ? 0 : font.width(Component.translatable("tooltip.anvilcraft.fluid_tank.fluid"));
        for (Line line : this.lines) width = Math.max(width, 17 + font.width(line.text()));
        if (this.remaining > 0) width = Math.max(width, font.width(this.moreLine()));
        if (this.tooltip.showCapacity()) {
            width = Math.max(width, font.width(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity")));
            width = Math.max(width, font.width(this.capacityLine()));
        }
        return width;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        if (!this.lines.isEmpty()) {
            graphics.text(font, Component.translatable("tooltip.anvilcraft.fluid_tank.fluid").withStyle(ChatFormatting.BLUE),
                x, y, -1, true);
            y += 10;
        }
        for (Line line : this.lines) {
            var model = FluidRenderHelper.getModel(Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
                line.fluid().getFluid());
            var tint = model.fluidTintSource();
            int color = tint == null ? -1 : tint.colorAsStack(line.fluid());
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, model.stillMaterial().sprite(), x, y, 16, 16, ARGB.opaque(color));
            graphics.text(font, line.text(), x + 17, y + 3, -1, true);
            y += 16;
        }
        if (this.remaining > 0) {
            graphics.text(font, this.moreLine(), x, y, -1, true);
            y += 10;
        }
        if (this.tooltip.showCapacity()) {
            graphics.text(font, Component.translatable("tooltip.anvilcraft.fluid_tank.capacity").withStyle(ChatFormatting.BLUE),
                x, y, -1, true);
            graphics.text(font, this.capacityLine(), x, y + 10, -1, true);
        }
    }
}
