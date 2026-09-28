package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.item.StorageFluidPortItemRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FluidHolderRenderProbe {
    public record Batch(String type, List<FluidGeometryRecorder.Vertex> vertices, List<String> textures) {
    }
    private record Pending(String type, FluidGeometryRecorder recorder) {
    }

    public static void configure(BlockEntity entity, FluidStack fluid, int column) {
        if (entity instanceof DrainBlockEntity drain) {
            drain.getTank().setFluid(fluid);
            setColumn(drain, column);
        } else if (entity instanceof FishTankBlockEntity fish) {
            fish.getFluidHandler().setFluid(fluid);
        } else if (entity instanceof ExpCollectorBlockEntity collector) {
            collector.getFluidTank().setFluid(fluid);
        } else {
            ((StorageFluidPortBlockEntity) entity).getTank().setFluid(fluid);
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
        var entity = client.level.getBlockEntity(pos);
        BlockEntityRenderer renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
        var world = new ArrayList<Pending>();
        renderer.render(entity, 0, new PoseStack(), buffers(world), 0xF00000, 0);
        var second = new ArrayList<Pending>();
        if (entity instanceof StorageFluidPortBlockEntity port) {
            var stack = ModBlocks.STORAGE_FLUID_PORT.asStack();
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(port.getUpdateTag(client.level.registryAccess())));
            StorageFluidPortItemRenderer.getInstance().renderByItem(stack, ItemDisplayContext.GUI,
                new PoseStack(), buffers(second), 0xF00000, 0);
        } else if (entity instanceof DrainBlockEntity drain && column != Integer.MIN_VALUE) {
            setColumn(drain, Integer.MIN_VALUE);
            renderer.render(entity, 0, new PoseStack(), buffers(second), 0xF00000, 0);
            setColumn(drain, column);
        }
        return Map.of("world", finish(world, fluid), "secondary", finish(second, fluid));
    }

    private static List<Batch> finish(List<Pending> pending, FluidStack fluid) {
        var ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        var still = atlas.apply(ext.getStillTexture(fluid));
        var flow = atlas.apply(ext.getFlowingTexture(fluid));
        return pending.stream().map(entry -> new Batch(entry.type(), entry.recorder().vertices(),
            textures(entry.recorder().coordinates(), still, flow))).toList();
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
