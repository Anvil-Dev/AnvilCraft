package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CreativeFluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.FluidTankMinecartRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.CreativeFluidTankItemRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.FluidTankItemRenderer;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SingleTankRenderProbe {
    public static final List<ItemStack> ITEMS = new ArrayList<>();
    public record Batch(String type, List<FluidGeometryRecorder.Vertex> vertices) {
    }
    private record Pending(String type, FluidGeometryRecorder recorder) {
    }

    public static Map<String, List<Batch>> capture(Minecraft client, BlockPos pos, FluidTankMinecartEntity cart) {
        var entity = client.level.getBlockEntity(pos);
        var world = new ArrayList<Pending>();
        boolean creative = entity instanceof CreativeFluidTankBlockEntity;
        if (entity instanceof FluidTankBlockEntity tank) {
            var renderer = (FluidTankBlockEntityRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
            renderer.render(tank, 0, new PoseStack(), buffers(world), 0xF00000, 0);
        } else {
            var tank = (CreativeFluidTankBlockEntity) entity;
            var renderer = (CreativeFluidTankBlockEntityRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
            renderer.render(tank, 0, new PoseStack(), buffers(world), 0xF00000, 0);
        }
        var stack = creative ? ModBlocks.CREATIVE_FLUID_TANK.asStack() : ModBlocks.FLUID_TANK.asStack();
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(entity.getUpdateTag(client.level.registryAccess())));
        ITEMS.add(stack.copy());
        var inventory = new ArrayList<Pending>();
        if (creative) {
            CreativeFluidTankItemRenderer.getInstance().renderByItem(
                stack, ItemDisplayContext.GUI, new PoseStack(), buffers(inventory), 0xF00000, 0);
        } else {
            FluidTankItemRenderer.getInstance().renderByItem(
                stack, ItemDisplayContext.GUI, new PoseStack(), buffers(inventory), 0xF00000, 0);
        }
        var cartVertices = new ArrayList<Pending>();
        var renderer = (FluidTankMinecartRenderer) (Object) client.getEntityRenderDispatcher().getRenderer(cart);
        try {
            var method = FluidTankMinecartRenderer.class.getDeclaredMethod("renderMinecartContents",
                FluidTankMinecartEntity.class, float.class, BlockState.class, PoseStack.class, MultiBufferSource.class, int.class);
            method.setAccessible(true);
            method.invoke(renderer, cart, 0F, ModBlocks.FLUID_TANK.getDefaultState(), new PoseStack(), buffers(cartVertices), 0xF00000);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return Map.of("world", finish(world), "item", finish(inventory), "cart", finish(cartVertices));
    }

    private static List<Batch> finish(List<Pending> pending) {
        return pending.stream().map(entry -> new Batch(entry.type(), entry.recorder().vertices())).toList();
    }

    private static MultiBufferSource buffers(List<Pending> batches) {
        return type -> {
            var recorder = new FluidGeometryRecorder();
            if (type == RenderType.translucent() || type == RenderType.cutout()) {
                batches.add(new Pending(type == RenderType.cutout() ? "cutout" : "translucent", recorder));
            }
            return recorder;
        };
    }
}
