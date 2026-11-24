package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 记录大型储罐中一层流体占据的高度，让玩家能够取出所指位置显示的流体。 */
public record TankFluidLayer(FluidStack fluid, double bottom, double top, float opacity, boolean renderBottom) {
    public static final float INSET = 4 / 16f + 0.001f;

    public static List<FluidStack> sorted(List<FluidStack> contents) {
        return contents.stream().filter(fluid -> !fluid.isEmpty())
            .sorted(Comparator.comparingInt(FluidStack::getAmount).reversed()
                .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()))
            .toList();
    }

    public static List<TankFluidLayer> of(List<FluidStack> contents, boolean enhanced) {
        return of(contents, enhanced, TankFluidLayer::new);
    }

    public static <T> List<T> of(List<FluidStack> contents, boolean enhanced, Factory<T> factory) {
        List<FluidStack> fluids = sorted(contents);
        long capacity = LargeFluidTankBlockEntity.BASE_CAPACITY;
        if (enhanced) {
            long total = fluids.stream().mapToLong(FluidStack::getAmount).sum();
            capacity = Math.max(total, LargeFluidTankBlockEntity.infinityThreshold());
        }
        return create(fluids, capacity, factory);
    }

    public static FluidStack atHeight(List<TankFluidLayer> layers, double relativeY) {
        for (TankFluidLayer layer : layers) {
            float surface = (float) (INSET - 1 + layer.top() * (3 - 2 * INSET));
            if (relativeY <= surface) {
                return layer.fluid();
            }
        }
        return layers.isEmpty() ? FluidStack.EMPTY : layers.getLast().fluid();
    }

    /** 根据罐体可用的高度创建液体层和均分剩余空间的气体层。 */
    public static <T> List<T> create(List<FluidStack> fluids, long capacity, Factory<T> factory) {
        if (capacity <= 0) {
            return List.of();
        }
        List<T> layers = new ArrayList<>();
        List<FluidStack> gases = new ArrayList<>();
        double bottom = 0;
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) {
                continue;
            }
            if (fluid.getFluidType().isLighterThanAir()) {
                gases.add(fluid);
            } else if (bottom < 1) {
                double top = Math.min(1, bottom + (double) fluid.getAmount() / capacity);
                layers.add(factory.create(fluid, bottom, top, 1, true));
                bottom = top;
            }
        }
        if (!gases.isEmpty() && bottom < 1) {
            double height = (1 - bottom) / gases.size();
            boolean renderBottom = bottom <= 0;
            for (FluidStack gas : gases) {
                double top = Math.min(1, bottom + height);
                layers.add(factory.create(gas, bottom, top, (float) ((double) gas.getAmount() / capacity), renderBottom));
                bottom = top;
                renderBottom = false;
            }
        }
        return List.copyOf(layers);
    }

    /** 按相同的高度与透明度生成交互层或渲染层。 */
    @FunctionalInterface
    public interface Factory<T> {
        T create(FluidStack fluid, double bottom, double top, float opacity, boolean renderBottom);
    }
}
