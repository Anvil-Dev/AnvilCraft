package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum LargeFluidTankProvider implements IServerExtensionProvider<FluidView.Data> {
    INSTANCE;
    public static final Identifier UID = AnvilCraft.of("large_fluid_tank");
    /** 大型储罐的流体组标识，客户端据此生成共享存量标题 */
    public static final String LARGE_TANK_GROUP = "large_tank";
    /** 单流体容器的组标识：小储罐与仓储流体端口复用同一套总存量标题 */
    public static final String FLUID_TANK_GROUP = "fluid_tank";
    public static final String FLUID_PORT_GROUP = "fluid_port";
    public static final String TOTAL_KEY = "Total";
    public static final String CAPACITY_KEY = "Capacity";
    public static final String ENHANCED_KEY = "Enhanced";
    public static final String INFINITE_KEY = "Infinite";

    @Override
    public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return null;
        if (blockAccessor.getBlockEntity() instanceof FluidTankBlockEntity tank) {
            ResourceHandler<FluidResource> handler = tank.getFluidHandler();
            FluidResource resource = handler.getResource(0);
            long capacity = tank.isInfinite()
                ? Integer.MAX_VALUE
                : handler.getCapacityAsLong(0, resource);
            return List.of(singleFluidGroup(
                LargeFluidTankProvider.FLUID_TANK_GROUP,
                resource,
                handler.getAmountAsLong(0),
                capacity,
                tank.isInfinite()
            ));
        }
        if (blockAccessor.getBlockEntity() instanceof StorageFluidPortBlockEntity port) {
            FluidStack remembered = port.getRememberedFluid();
            FluidResource resource = FluidResource.of(remembered);
            boolean infinite = port.isInfinite();
            return List.of(singleFluidGroup(
                LargeFluidTankProvider.FLUID_PORT_GROUP,
                resource,
                port.getFluidHandler().getAmountAsLong(0),
                infinite ? Integer.MAX_VALUE : StorageFluidPortBlockEntity.CAPACITY_MB,
                infinite
            ));
        }
        if (blockAccessor.getBlockEntity() instanceof LargeCauldronBlockEntity cauldron) {
            ResourceHandler<FluidResource> handler = cauldron.getFluidHandler();
            List<FluidView.Data> fluids = new ArrayList<>();
            boolean hasComponents = false;
            for (int tank = 0; tank < handler.size(); tank++) {
                FluidResource resource = handler.getResource(tank);
                if (resource.isEmpty()) continue;
                hasComponents |= !resource.isComponentsPatchEmpty();
                fluids.add(new FluidView.Data(
                    JadeFluidObject.of(
                        resource.getFluid(),
                        handler.getAmountAsLong(tank),
                        resource.getComponentsPatch()
                    ),
                    handler.getCapacityAsLong(tank, resource)
                ));
            }
            return hasComponents && !fluids.isEmpty() ? List.of(new ViewGroup<>(fluids)) : null;
        }
        if (!(blockAccessor.getBlockEntity() instanceof LargeFluidTankBlockEntity tank)) return null;

        ResourceHandler<FluidResource> handler = tank.getFluidHandler();
        boolean enhanced = tank.isEnhanced();
        boolean hasInfiniteFluid = tank.getStoredFluids().stream().anyMatch(tank::isInfinite);
        long totalAmount = 0;
        int infiniteCount = 0;
        List<FluidView.Data> fluids = new ArrayList<>();
        for (int index = 0; index < handler.size(); index++) {
            FluidResource resource = handler.getResource(index);
            if (resource.isEmpty() && hasInfiniteFluid) continue;
            long amount = handler.getAmountAsLong(index);
            boolean infinite = amount == Integer.MAX_VALUE;
            if (!resource.isEmpty()) {
                if (infinite) {
                    infiniteCount++;
                } else {
                    totalAmount += amount;
                }
            }
            long capacity;
            if (enhanced) {
                capacity = infinite ? Integer.MAX_VALUE : handler.getCapacityAsLong(index, resource);
            } else {
                // 未增强时所有流体共用基础容量，条形与文字都按同一分母显示
                capacity = LargeFluidTankBlockEntity.BASE_CAPACITY;
            }
            fluids.add(new FluidView.Data(
                JadeFluidObject.of(resource.getFluid(), amount, resource.getComponentsPatch()), capacity));
        }
        ViewGroup<FluidView.Data> group = new ViewGroup<>(fluids);
        group.id = LargeFluidTankProvider.LARGE_TANK_GROUP;
        group.getExtraData().putLong(LargeFluidTankProvider.TOTAL_KEY, totalAmount);
        group.getExtraData().putLong(
            LargeFluidTankProvider.CAPACITY_KEY,
            enhanced ? 0L : LargeFluidTankBlockEntity.BASE_CAPACITY
        );
        group.getExtraData().putBoolean(LargeFluidTankProvider.ENHANCED_KEY, enhanced);
        group.getExtraData().putInt(LargeFluidTankProvider.INFINITE_KEY, infiniteCount);
        return List.of(group);
    }

    /**
     * 单流体容器的预览组：同步总存量、容量与无限标记，客户端据此生成统一的存量标题。
     */
    private static ViewGroup<FluidView.Data> singleFluidGroup(
        String id,
        FluidResource resource,
        long amount,
        long capacity,
        boolean infinite
    ) {
        FluidView.Data data = new FluidView.Data(
            JadeFluidObject.of(resource.getFluid(), amount, resource.getComponentsPatch()), capacity);
        ViewGroup<FluidView.Data> group = new ViewGroup<>(List.of(data));
        group.id = id;
        group.getExtraData().putLong(LargeFluidTankProvider.TOTAL_KEY, infinite ? 0L : amount);
        group.getExtraData().putLong(LargeFluidTankProvider.CAPACITY_KEY, capacity);
        group.getExtraData().putInt(LargeFluidTankProvider.INFINITE_KEY, infinite ? 1 : 0);
        return group;
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return true;
    }

    @Override
    public Identifier getUid() {
        return LargeFluidTankProvider.UID;
    }
}
