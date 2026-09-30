package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GasContainerRenderProbe {
    public static void configure(BlockEntity entity, List<FluidStack> fluids, boolean enhanced) {
        var client = Minecraft.getInstance();
        entity.loadWithComponents(new CompoundTag(), client.level.registryAccess());
        if (entity instanceof LargeFluidTankBlockEntity tank) {
            if (enhanced) tank.onFormed();
            for (var fluid : fluids) tank.getFluidHandler().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        } else if (entity instanceof LargeCauldronBlockEntity cauldron) {
            cauldron.getFluids().setFluids(fluids);
        } else if (entity instanceof CelestialForgingAnvilFluidInterfaceBlockEntity port) {
            for (var tank : port.getTanks()) tank.setFluid(FluidStack.EMPTY);
            for (int slot = 0; slot < fluids.size(); slot++) port.getTanks()[slot].setFluid(fluids.get(slot));
        } else if (!fluids.isEmpty()) {
            var fluid = fluids.getFirst();
            if (entity instanceof FishTankBlockEntity fish) fish.getFluidHandler().setFluid(fluid);
            else ((FluidTank) ((AutoEnchantingTableBlockEntity) entity).getFluidHandler()).setFluid(fluid);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map<String, Object> capture(Minecraft client, BlockEntity entity, List<FluidStack> fluids, boolean enhanced) {
        BlockEntityRenderer renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) throw new IllegalStateException("Missing source renderer " + entity.getClass());
        var pending = new ArrayList<FluidGeometryRecorder>();
        renderer.render(entity, 0, new PoseStack(), buffers(pending), 0xF00000, 0);
        var vertices = pending.stream().flatMap(recorder -> recorder.vertices().stream()).toList();
        configure(entity, List.of(), false);
        var empty = new ArrayList<FluidGeometryRecorder>();
        renderer.render(entity, 0, new PoseStack(), buffers(empty), 0xF00000, 0);
        long emptyVertices = empty.stream().mapToLong(recorder -> recorder.vertices().size()).sum();
        if (emptyVertices != 0) throw new IllegalStateException("Empty source container retained fluid geometry");
        configure(entity, fluids, enhanced);
        return Map.of("vertices", vertices, "empty_vertices", emptyVertices);
    }

    private static MultiBufferSource buffers(List<FluidGeometryRecorder> pending) {
        return type -> {
            var recorder = new FluidGeometryRecorder();
            if (type == RenderType.translucent() || type == RenderType.cutout()) pending.add(recorder);
            return recorder;
        };
    }
}
