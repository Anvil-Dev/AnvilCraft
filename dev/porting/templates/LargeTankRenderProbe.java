package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeFluidTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.LargeFluidTankItemRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LargeTankRenderProbe {
    public static final List<ItemStack> ITEMS = new ArrayList<>();

    public record Batch(String type, List<FluidGeometryRecorder.Vertex> vertices) {
    }
    private record Pending(String type, FluidGeometryRecorder recorder) {
    }

    public static Map<String, List<Batch>> capture(Minecraft client, BlockPos pos) {
        var tank = (LargeFluidTankBlockEntity) client.level.getBlockEntity(pos);
        var renderer = (LargeFluidTankBlockEntityRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
        List<Pending> world = new ArrayList<>();
        renderer.render(tank, 0, new PoseStack(), buffers(world), 0xF00000, 0);
        var stack = ModBlocks.LARGE_FLUID_TANK.asStack();
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tank.getUpdateTag(client.level.registryAccess())));
        ITEMS.add(stack.copy());
        List<Pending> inventory = new ArrayList<>();
        LargeFluidTankItemRenderer.getInstance().renderByItem(
            stack, ItemDisplayContext.GUI, new PoseStack(), buffers(inventory), 0xF00000, 0);
        return Map.of("world", finish(world), "item", finish(inventory));
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
