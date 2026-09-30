package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GasContainerRenderProbe {
    public static void configure(BlockEntity entity, List<FluidStack> fluids, boolean enhanced) {
        var client = Minecraft.getInstance();
        entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), new CompoundTag()));
        if (entity instanceof LargeFluidTankBlockEntity tank) {
            if (enhanced) tank.onFormed();
            try (Transaction tx = Transaction.openRoot()) {
                for (var fluid : fluids) tank.getFluidHandler().insert(FluidResource.of(fluid), fluid.getAmount(), tx);
                tx.commit();
            }
        } else if (entity instanceof LargeCauldronBlockEntity cauldron) {
            cauldron.getFluids().setFluids(fluids);
        } else if (entity instanceof CelestialForgingAnvilFluidInterfaceBlockEntity port) {
            for (int slot = 0; slot < port.getTank().size(); slot++) port.getTank().set(slot, FluidResource.EMPTY, 0);
            for (int slot = 0; slot < fluids.size(); slot++) {
                port.getTank().set(slot, FluidResource.of(fluids.get(slot)), fluids.get(slot).getAmount());
            }
        } else if (!fluids.isEmpty()) {
            var fluid = fluids.getFirst();
            if (entity instanceof FishTankBlockEntity fish) fish.getFluidHandler().set(FluidResource.of(fluid), fluid.getAmount());
            else ((AutoEnchantingTableBlockEntity) entity).getFluidTank().set(0, FluidResource.of(fluid), fluid.getAmount());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map<String, Object> capture(Minecraft client, BlockEntity entity, List<FluidStack> fluids, boolean enhanced) {
        BlockEntityRenderer renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) throw new IllegalStateException("Missing container renderer " + entity.getClass());
        var state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, 0, client.gameRenderer.getMainCamera().position(), null);
        state.lightCoords = 0xF00000;
        var vertices = new ArrayList<FluidGeometryRecorder.Vertex>();
        renderer.submit(state, new PoseStack(), collector(vertices), new CameraRenderState());
        configure(entity, List.of(), false);
        renderer.extractRenderState(entity, state, 0, client.gameRenderer.getMainCamera().position(), null);
        var empty = new ArrayList<FluidGeometryRecorder.Vertex>();
        renderer.submit(state, new PoseStack(), collector(empty), new CameraRenderState());
        if (!empty.isEmpty()) throw new IllegalStateException("Empty container retained fluid geometry");
        configure(entity, fluids, enhanced);
        return Map.of("vertices", vertices, "empty_vertices", empty.size());
    }

    private static SubmitNodeCollector collector(List<FluidGeometryRecorder.Vertex> vertices) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
            new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                if (method.getName().equals("order")) return proxy;
                if (method.getName().equals("submitCustomGeometry")) {
                    var recorder = new FluidGeometryRecorder();
                    ((SubmitNodeCollector.CustomGeometryRenderer) args[2]).render(((PoseStack) args[0]).last(), recorder);
                    vertices.addAll(recorder.vertices());
                }
                return null;
            });
    }
}
