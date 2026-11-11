package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class FluidTankTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof FluidTankBlockEntity || value instanceof LargeFluidTankBlockEntity
            || value instanceof FishTankBlockEntity || value instanceof StorageFluidPortBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        final List<Component> lines = new ArrayList<>();
        if (value instanceof IFluidHandlerHolder tank) {
            boolean original = false;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.isShiftKeyDown()) {
                original = true;
            }
            IFluidHandler handler = tank.getFluidHandler();
            long amount = 0;
            boolean storedInfinite = false;
            List<FluidStack> fluids = new ArrayList<>();
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluid = handler.getFluidInTank(i);
                if (fluid.isEmpty()) continue;
                if (FluidTankTooltipProvider.isInfinite(value, fluid)) {
                    // 无限容器读出来的存量是 Integer.MAX_VALUE，不能参与累加，否则 HUD 显示 21 亿
                    storedInfinite = true;
                } else {
                    amount += fluid.getAmount();
                }
                fluids.add(fluid);
            }
            if (!fluids.isEmpty()) {
                lines.add(Component.translatable("tooltip.anvilcraft.fluid_tank.fluid")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
                for (FluidStack fluid : fluids) {
                    Component fluidAmount = FluidTankTooltipProvider.isInfinite(value, fluid)
                        ? Component.literal(" ∞")
                        : Component.literal(" " + UnitUtil.fluidUnit(fluid.getAmount(), original));
                    lines.add(Component.literal("  ").append(fluid.getHoverName()).append(fluidAmount)
                        .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
                }
            }
            lines.add(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
            boolean infinity = storedInfinite
                || value instanceof LargeFluidTankBlockEntity largeTank && largeTank.isEnhanced();
            if (infinity) {
                lines.add(Component.translatable(
                        "tooltip.anvilcraft.fluid_tank.capacity.value.infinity",
                        storedInfinite
                            ? Component.literal("∞")
                            : Component.literal(UnitUtil.fluidUnit(amount, original))
                    )
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            } else {
                int capacity = value instanceof LargeFluidTankBlockEntity
                    ? LargeFluidTankBlockEntity.BASE_CAPACITY
                    : handler.getTankCapacity(0);
                lines.add(Component.translatable(
                        "tooltip.anvilcraft.fluid_tank.capacity.value",
                        UnitUtil.fluidUnit(amount, original),
                        UnitUtil.fluidUnit(capacity, original)
                    )
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            }
        }
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }

    /**
     * 判断某个方块实体里这种流体是否处于无限存量。
     *
     * <p>小型储罐与仓储流体端口整体无限；大型储罐按流体分别判定；
     * 其余容器（鱼缸等）不支持无限存量。</p>
     *
     * @param value  方块实体
     * @param fluid  待判定的流体
     * @return 该流体是否无限
     */
    private static boolean isInfinite(BlockEntity value, FluidStack fluid) {
        if (value instanceof LargeFluidTankBlockEntity largeTank) {
            return !fluid.isEmpty() && largeTank.isInfinite(fluid);
        }
        if (value instanceof FluidTankBlockEntity smallTank) {
            return smallTank.isInfinite();
        }
        if (value instanceof StorageFluidPortBlockEntity port) {
            return port.isInfinite();
        }
        return false;
    }
}
