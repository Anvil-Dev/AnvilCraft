package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.ClientTickRecorder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankRenderHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.TropicalFish;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FishTankContentsProbe {
    public static boolean hideFire;
    private static boolean installed;
    private static final List<Integer> ORDER = new ArrayList<>();
    private record Pending(String type, FluidGeometryRecorder recorder) { }

    public static void install() {
        if (installed) return;
        installed = true;
        FishTankRenderHooks.register(new FishTankRenderHooks.Handler() {
            public boolean showVanillaFire(FishTankBlockEntity tank) { return !hideFire; }
            public void afterRender(FishTankBlockEntity tank, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                ORDER.add(tank.getItemHandler().getStackInSlot(0).getCount());
            }
        });
        FishTankRenderHooks.register(new FishTankRenderHooks.Handler() {
            public void afterRender(FishTankBlockEntity tank, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                ORDER.add(-1);
            }
        });
    }

    public static void clock(int ticks) {
        try {
            var field = ClientTickRecorder.class.getDeclaredField("ticks");
            field.setAccessible(true);
            field.setInt(null, ticks);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }

    @SuppressWarnings("unchecked")
    private static List<TropicalFish> normalize(FishTankBlockEntityRenderer renderer, FishTankBlockEntity tank) {
        try {
            var field = FishTankBlockEntityRenderer.class.getDeclaredField("fishCache");
            field.setAccessible(true);
            var cache = (Map<Long, ?>) field.get(renderer);
            var entry = cache.get(tank.getBlockPos().asLong());
            if (entry == null) return List.of();
            var fishes = entry.getClass().getDeclaredField("cachedFishes");
            fishes.setAccessible(true);
            var result = (List<TropicalFish>) fishes.get(entry);
            for (int i = 0; i < result.size(); i++) result.get(i).setId(10000 + i);
            return result;
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }

    public static Map<String, Object> capture(Minecraft client, FishTankBlockEntity tank) {
        install();
        clock(300);
        var renderer = (FishTankBlockEntityRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
        renderer.render(tank, 0.5F, new PoseStack(), buffers(new ArrayList<>()), 0xF00000, 0);
        var fishes = normalize(renderer, tank);
        ORDER.clear();
        var pending = new ArrayList<Pending>();
        renderer.render(tank, 0.5F, new PoseStack(), buffers(pending), 0xF00000, 0);
        if (!ORDER.equals(List.of(tank.getItemHandler().getStackInSlot(0).getCount(), -1))) throw new IllegalStateException("Source hook order");
        var batches = new LinkedHashMap<String, List<List<Float>>>();
        for (var entry : pending) {
            var target = batches.computeIfAbsent(entry.type(), ignored -> new ArrayList<>());
            for (var p : entry.recorder().vertices()) target.add(List.of(p.x(), p.y(), p.z()));
        }
        var random = RandomSource.create();
        random.setSeed(fishes.hashCode() + tank.getBlockPos().hashCode());
        float ticks = ClientTickRecorder.getTicks() + 0.5F + random.nextInt(1297361);
        return Map.of("seed", ItemHandlerUtil.hash(tank.getItemHandler()), "ticks", tank.isEmptyOfFish() ? 0 : ticks, "geometry", batches);
    }

    private static MultiBufferSource buffers(List<Pending> pending) {
        return type -> {
            var recorder = new FluidGeometryRecorder();
            String name = type.toString();
            String key = type == RenderType.translucent() ? "fluid" : type == RenderType.cutout() ? "fire"
                : name.contains("tropical") ? "fish" : name.contains("blocks.png") ? "items" : "ignored";
            if (!key.equals("ignored")) pending.add(new Pending(key, recorder));
            return recorder;
        };
    }
}
