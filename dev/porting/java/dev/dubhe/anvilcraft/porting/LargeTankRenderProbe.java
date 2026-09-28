package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeFluidTankRenderer;
import dev.dubhe.anvilcraft.client.renderer.item.LargeFluidTankItemRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

public final class LargeTankRenderProbe {
    public static final List<ItemStack> ITEMS = new ArrayList<>();

    public record Batch(String type, List<FluidGeometryRecorder.Vertex> vertices) {
    }

    public static Map<String, List<Batch>> capture(Minecraft client, BlockPos pos) {
        var tank = (LargeFluidTankBlockEntity) client.level.getBlockEntity(pos);
        var renderer = (LargeFluidTankRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
        var state = renderer.createRenderState();
        renderer.extractRenderState(tank, state, 0, client.gameRenderer.getMainCamera().position(), null);
        state.lightCoords = 0xF00000;
        List<Batch> world = new ArrayList<>();
        renderer.submit(state, new PoseStack(), collector(world), new CameraRenderState());
        var stack = ModBlocks.LARGE_FLUID_TANK.asStack();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, client.level.registryAccess());
        output.store("Tank", CompoundTag.CODEC, tank.getUpdateTag(client.level.registryAccess()).getCompoundOrEmpty("Tank"));
        BlockItem.setBlockEntityData(stack, tank.getType(), output);
        ITEMS.add(stack.copy());
        var itemRenderer = new LargeFluidTankItemRenderer();
        var item = itemRenderer.extractArgument(stack);
        List<Batch> inventory = new ArrayList<>();
        itemRenderer.submit(item, new PoseStack(), collector(inventory), 0xF00000, 0, false, 0);
        return Map.of("world", world, "item", inventory);
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
