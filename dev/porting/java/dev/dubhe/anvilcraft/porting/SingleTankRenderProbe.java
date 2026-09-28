package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeFluidTankRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FluidTankRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.FluidTankMinecartRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.state.FluidTankMinecartRenderState;
import dev.dubhe.anvilcraft.client.renderer.item.CreativeFluidTankItemRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.FluidTankItemRenderer;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SingleTankRenderProbe {
    public static final List<ItemStack> ITEMS = new ArrayList<>();

    public record Batch(String type, List<FluidGeometryRecorder.Vertex> vertices) {
    }

    public static Map<String, List<Batch>> capture(Minecraft client, BlockPos pos, FluidTankMinecartEntity cart) {
        var entity = client.level.getBlockEntity(pos);
        var world = new ArrayList<Batch>();
        boolean creative = entity instanceof CreativeFluidTankBlockEntity;
        if (entity instanceof FluidTankBlockEntity tank) {
            var renderer = (FluidTankRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
            var state = renderer.createRenderState();
            renderer.extractRenderState(tank, state, 0, client.gameRenderer.getMainCamera().position(), null);
            state.lightCoords = 0xF00000;
            renderer.submit(state, new PoseStack(), collector(world), new CameraRenderState());
        } else {
            var tank = (CreativeFluidTankBlockEntity) entity;
            var renderer = (CreativeFluidTankRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
            var state = renderer.createRenderState();
            renderer.extractRenderState(tank, state, 0, client.gameRenderer.getMainCamera().position(), null);
            state.lightCoords = 0xF00000;
            renderer.submit(state, new PoseStack(), collector(world), new CameraRenderState());
        }
        var stack = creative ? ModBlocks.CREATIVE_FLUID_TANK.asStack() : ModBlocks.FLUID_TANK.asStack();
        if (!creative) {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, client.level.registryAccess());
            output.store("Tank", CompoundTag.CODEC, entity.getUpdateTag(client.level.registryAccess()).getCompoundOrEmpty("Tank"));
            BlockItem.setBlockEntityData(stack, entity.getType(), output);
        }
        stack.applyComponents(entity.collectComponents());
        ITEMS.add(stack.copy());
        var inventory = new ArrayList<Batch>();
        if (creative) {
            var renderer = new CreativeFluidTankItemRenderer();
            renderer.submit(renderer.extractArgument(stack), new PoseStack(), collector(inventory), 0xF00000, 0, false, 0);
        } else {
            var renderer = new FluidTankItemRenderer();
            renderer.submit(renderer.extractArgument(stack), new PoseStack(), collector(inventory), 0xF00000, 0, false, 0);
        }
        var cartVertices = new ArrayList<Batch>();
        var renderer = (FluidTankMinecartRenderer) (Object) client.getEntityRenderDispatcher().getRenderer(cart);
        var state = renderer.createRenderState();
        renderer.extractRenderState(cart, state, 0);
        try {
            var method = FluidTankMinecartRenderer.class.getDeclaredMethod("submitMinecartContents",
                FluidTankMinecartRenderState.class, BlockModelRenderState.class, PoseStack.class, SubmitNodeCollector.class, int.class);
            method.setAccessible(true);
            method.invoke(renderer, state, new BlockModelRenderState(), new PoseStack(), collector(cartVertices), 0xF00000);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return Map.of("world", world, "item", inventory, "cart", cartVertices);
    }

    private static SubmitNodeCollector collector(List<Batch> batches) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
            new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                if (method.getName().equals("order")) return proxy;
                if (method.getName().equals("submitCustomGeometry")) {
                    var recorder = new FluidGeometryRecorder();
                    ((SubmitNodeCollector.CustomGeometryRenderer) args[2]).render(((PoseStack) args[0]).last(), recorder);
                    batches.add(new Batch(args[1].toString().contains("cutout") ? "cutout" : "translucent", recorder.vertices()));
                }
                return null;
            });
    }
}
