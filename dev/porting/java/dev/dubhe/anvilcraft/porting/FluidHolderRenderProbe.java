package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.item.StorageFluidPortItemRenderer;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FluidHolderRenderProbe {
    public record Batch(String type, List<FluidGeometryRecorder.Vertex> vertices, List<String> textures) {
    }

    public static void configure(BlockEntity entity, FluidStack fluid, int column) {
        FluidResource resource = FluidResource.of(fluid);
        if (entity instanceof DrainBlockEntity drain) {
            drain.getTank().set(resource, fluid.getAmount());
            setColumn(drain, column);
        } else if (entity instanceof FishTankBlockEntity fish) {
            fish.getFluidHandler().set(resource, fluid.getAmount());
        } else if (entity instanceof ExpCollectorBlockEntity collector) {
            ((FluidStacksResourceHandler) collector.getInternalFluidHandler()).set(0, resource, fluid.getAmount());
        } else {
            ((StorageFluidPortBlockEntity) entity).getTank().set(0, resource, fluid.getAmount());
        }
    }

    public static void setColumn(DrainBlockEntity drain, int value) {
        try {
            var field = DrainBlockEntity.class.getDeclaredField("columnBottomY");
            field.setAccessible(true);
            field.setInt(drain, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map<String, List<Batch>> capture(Minecraft client, BlockPos pos, FluidStack fluid, int column) {
        BlockEntity entity = client.level.getBlockEntity(pos);
        BlockEntityRenderer renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
        var state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, 0, client.gameRenderer.getMainCamera().position(), null);
        state.lightCoords = 0xF00000;
        var world = new ArrayList<Batch>();
        renderer.submit(state, new PoseStack(), collector(world, fluid), new CameraRenderState());
        var second = new ArrayList<Batch>();
        if (entity instanceof StorageFluidPortBlockEntity port) {
            var stack = ModBlocks.STORAGE_FLUID_PORT.asStack();
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, client.level.registryAccess());
            output.store("Tank", CompoundTag.CODEC, port.getUpdateTag(client.level.registryAccess()).getCompoundOrEmpty("Tank"));
            BlockItem.setBlockEntityData(stack, port.getType(), output);
            var item = new StorageFluidPortItemRenderer();
            item.submit(item.extractArgument(stack), new PoseStack(), collector(second, fluid), 0xF00000, 0, false, 0);
        } else if (entity instanceof DrainBlockEntity drain && column != Integer.MIN_VALUE) {
            setColumn(drain, Integer.MIN_VALUE);
            renderer.extractRenderState(entity, state, 0, client.gameRenderer.getMainCamera().position(), null);
            state.lightCoords = 0xF00000;
            renderer.submit(state, new PoseStack(), collector(second, fluid), new CameraRenderState());
            if (second.size() != 1) throw new IllegalStateException("Stopped column retained geometry in reused render state");
            setColumn(drain, column);
        }
        return Map.of("world", world, "secondary", second);
    }

    private static SubmitNodeCollector collector(List<Batch> batches, FluidStack fluid) {
        var model = FluidRenderHelper.getModel(Minecraft.getInstance().getModelManager().getFluidStateModelSet(), fluid.getFluid());
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
            new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                if (method.getName().equals("order")) return proxy;
                if (method.getName().equals("submitCustomGeometry")) {
                    var recorder = new FluidGeometryRecorder();
                    ((SubmitNodeCollector.CustomGeometryRenderer) args[2]).render(((PoseStack) args[0]).last(), recorder);
                    var vertices = recorder.vertices();
                    batches.add(new Batch(args[1].toString().contains("cutout") ? "cutout" : "translucent", vertices,
                        textures(recorder.coordinates(), model.stillMaterial().sprite(), model.flowingMaterial().sprite())));
                }
                return null;
            });
    }

    public static List<String> textures(List<FluidGeometryRecorder.Uv> values, TextureAtlasSprite still, TextureAtlasSprite flowing) {
        return values.stream().map(uv -> {
            boolean inStill = inside(uv, still);
            boolean inFlow = inside(uv, flowing);
            return inStill && inFlow ? "shared" : inStill ? "still" : inFlow ? "flowing" : "unknown";
        }).toList();
    }

    private static boolean inside(FluidGeometryRecorder.Uv uv, TextureAtlasSprite sprite) {
        return uv.u() >= sprite.getU0() && uv.u() <= sprite.getU1() && uv.v() >= sprite.getV0() && uv.v() <= sprite.getV1();
    }
}
