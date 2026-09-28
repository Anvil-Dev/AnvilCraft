package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.ClientTickRecorder;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronRenderHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LargeCauldronRenderProbe {
    public static boolean hideFire;
    private static boolean installed;
    private static final List<Integer> ORDER = new ArrayList<>();
    private record Pending(String material, FluidGeometryRecorder recorder) { }

    public static void install() {
        if (installed) return;
        installed = true;
        LargeCauldronRenderHooks.register(new LargeCauldronRenderHooks.Handler() {
            public boolean showVanillaFire(LargeCauldronBlockEntity tank) { return !hideFire; }
            public void afterRender(LargeCauldronBlockEntity tank, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                ORDER.add(tank.getInputHandler().getStackInSlot(0).getCount());
            }
        });
        LargeCauldronRenderHooks.register(new LargeCauldronRenderHooks.Handler() {
            public void afterRender(LargeCauldronBlockEntity tank, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
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

    public static void ignite(LargeCauldronBlockEntity tank, boolean value) {
        try {
            var field = LargeCauldronBlockEntity.class.getDeclaredField("ignited");
            field.setAccessible(true);
            field.setBoolean(tank, value);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }

    public static Map<String, Object> capture(Minecraft client, LargeCauldronBlockEntity tank) {
        install();
        clock(300);
        var renderer = (LargeCauldronBlockEntityRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
        var pending = new ArrayList<Pending>();
        ORDER.clear();
        renderer.render(tank, 0.5F, new PoseStack(), buffers(pending), 0xF00000, 0);
        if (!ORDER.equals(List.of(tank.getInputHandler().getStackInSlot(0).getCount(), -1))) throw new IllegalStateException("Hook order");
        var geometry = new LinkedHashMap<String, List<List<Float>>>();
        var fluids = new ArrayList<Map<String, Object>>();
        for (var entry : pending) {
            String kind = entry.material().equals("items") ? "items" : entry.recorder().vertices().size() == 16 ? "fire" : "fluid";
            var target = geometry.computeIfAbsent(kind, ignored -> new ArrayList<>());
            for (var p : entry.recorder().vertices()) target.add(List.of(p.x(), p.y(), p.z()));
            if (kind.equals("fluid")) fluids.add(Map.of("material", entry.material(), "vertices", entry.recorder().vertices()));
        }
        if (hideFire && geometry.containsKey("fire")) throw new IllegalStateException("Source fire veto");
        return Map.of("geometry", geometry, "fluids", fluids);
    }

    private static MultiBufferSource buffers(List<Pending> pending) {
        return type -> {
            var recorder = new FluidGeometryRecorder();
            String material = type == RenderType.cutout() ? "cutout" : type == RenderType.translucent() ? "translucent" : "items";
            pending.add(new Pending(material, recorder));
            return recorder;
        };
    }
}
