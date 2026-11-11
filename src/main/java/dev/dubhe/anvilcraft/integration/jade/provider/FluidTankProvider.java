package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FluidTankProvider implements IServerExtensionProvider<CompoundTag> {
    public static final FluidTankProvider INSTANCE = new FluidTankProvider();
    public static final ResourceLocation UID = AnvilCraft.of("fluid_tank");

    public static final String LARGE_TANK_GROUP = "large_tank";
    public static final String FLUID_TANK_GROUP = "fluid_tank";
    public static final String FLUID_PORT_GROUP = "fluid_port";
    public static final String TOTAL_KEY = "Total";
    public static final String CAPACITY_KEY = "Capacity";
    public static final String ENHANCED_KEY = "Enhanced";
    public static final String INFINITE_KEY = "Infinite";

    private FluidTankProvider() {
    }

    @Override
    public @Nullable List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) {
            return null;
        }
        if (blockAccessor.getBlockEntity() instanceof FluidTankBlockEntity tank) {
            FluidStack fluid = tank.getFluidHandler().getFluidInTank(0);
            long capacity = tank.isInfinite()
                ? Integer.MAX_VALUE
                : tank.getFluidHandler().getTankCapacity(0);
            return List.of(singleGroup(
                FluidTankProvider.FLUID_TANK_GROUP,
                fluid,
                capacity,
                tank.isInfinite()
            ));
        }
        if (blockAccessor.getBlockEntity() instanceof StorageFluidPortBlockEntity port) {
            FluidStack fluid = port.getFluid();
            if (fluid.isEmpty()) {
                fluid = port.getRememberedFluid().copyWithAmount(0);
            }
            boolean infinite = port.isInfinite();
            return List.of(singleGroup(
                FluidTankProvider.FLUID_PORT_GROUP,
                fluid,
                infinite ? Integer.MAX_VALUE : StorageFluidPortBlockEntity.CAPACITY_MB,
                infinite
            ));
        }
        if (!(blockAccessor.getBlockEntity() instanceof LargeFluidTankBlockEntity tank)) {
            return null;
        }
        boolean enhanced = tank.isEnhanced();
        IFluidHandler handler = tank.getFluidHandler();
        long baseCapacity = enhanced
            ? LargeFluidTankBlockEntity.infinityThreshold()
            : LargeFluidTankBlockEntity.BASE_CAPACITY;
        boolean hasStoredFluid = false;
        for (int index = 0; index < handler.getTanks(); index++) {
            if (!handler.getFluidInTank(index).isEmpty()) {
                hasStoredFluid = true;
                break;
            }
        }
        long total = 0;
        int infiniteCount = 0;
        List<CompoundTag> views = new ArrayList<>();
        for (int index = 0; index < handler.getTanks(); index++) {
            FluidStack fluid = handler.getFluidInTank(index);
            // 空槽只在整罐为空时作为占位条显示；已有流体时不再显示空接纳槽
            if (fluid.isEmpty() && hasStoredFluid) {
                continue;
            }
            boolean infinite = tank.isInfinite(fluid)
                || (enhanced && !fluid.isEmpty()
                    && fluid.getAmount() >= LargeFluidTankBlockEntity.infinityThreshold());
            long capacity = enhanced
                ? (infinite ? Integer.MAX_VALUE : handler.getTankCapacity(index))
                : baseCapacity;
            if (infinite) {
                infiniteCount++;
            } else {
                total += fluid.getAmount();
            }
            views.add(FluidView.writeDefault(
                fluid.isEmpty()
                    ? JadeFluidObject.empty()
                    : JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch()),
                capacity
            ));
        }
        if (views.isEmpty()) {
            views.add(FluidView.writeDefault(JadeFluidObject.empty(), baseCapacity));
        }
        ViewGroup<CompoundTag> group = new ViewGroup<>(views);
        group.id = FluidTankProvider.LARGE_TANK_GROUP;
        group.getExtraData().putLong(FluidTankProvider.TOTAL_KEY, total);
        group.getExtraData().putLong(
            FluidTankProvider.CAPACITY_KEY,
            enhanced ? 0L : LargeFluidTankBlockEntity.BASE_CAPACITY
        );
        group.getExtraData().putBoolean(FluidTankProvider.ENHANCED_KEY, enhanced);
        group.getExtraData().putInt(FluidTankProvider.INFINITE_KEY, infiniteCount);
        return List.of(group);
    }

    private static ViewGroup<CompoundTag> singleGroup(
        String id,
        FluidStack fluid,
        long capacity,
        boolean infinite
    ) {
        JadeFluidObject fluidObject = fluid.isEmpty()
            ? JadeFluidObject.empty()
            : JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch());
        ViewGroup<CompoundTag> group = new ViewGroup<>(List.of(
            FluidView.writeDefault(fluidObject, capacity)
        ));
        group.id = id;
        group.getExtraData().putLong(FluidTankProvider.TOTAL_KEY, infinite ? 0L : fluid.getAmount());
        group.getExtraData().putLong(FluidTankProvider.CAPACITY_KEY, capacity);
        group.getExtraData().putInt(FluidTankProvider.INFINITE_KEY, infinite ? 1 : 0);
        return group;
    }

    @Override
    public int getDefaultPriority() {
        // 必须排在 Jade 通用流体提供器之前，否则 getServerExtensionData 会先选中通用提供器
        return 0;
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return accessor instanceof BlockAccessor;
    }

    @Override
    public ResourceLocation getUid() {
        return FluidTankProvider.UID;
    }
}
