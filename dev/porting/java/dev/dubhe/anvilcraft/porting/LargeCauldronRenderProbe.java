package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.ClientTickRecorder;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelTessellateState;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronRenderHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3f;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LargeCauldronRenderProbe {
    public static boolean hideFire;
    private static boolean installed;
    private static final List<Integer> ORDER = new ArrayList<>();

    public static void install() {
        if (installed) return;
        installed = true;
        LargeCauldronRenderHooks.register(new LargeCauldronRenderHooks.Handler() {
            @Override
            public boolean showVanillaFire(LargeCauldronBlockEntity tank) {
                return !hideFire;
            }

            @Override
            public LargeCauldronRenderHooks.AfterRender extract(LargeCauldronBlockEntity tank, float partial) {
                int amount = tank.getInputHandler().getAmountAsInt(0);
                return (pose, collector, light, overlay) -> ORDER.add(amount);
            }
        });
        LargeCauldronRenderHooks.register(new LargeCauldronRenderHooks.Handler() {
            @Override
            public LargeCauldronRenderHooks.AfterRender extract(LargeCauldronBlockEntity tank, float partial) {
                return (pose, collector, light, overlay) -> ORDER.add(-1);
            }
        });
    }

    public static void clock(int ticks) {
        try {
            var field = ClientTickRecorder.class.getDeclaredField("ticks");
            field.setAccessible(true);
            field.setInt(null, ticks);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static void ignite(LargeCauldronBlockEntity tank, boolean value) {
        try {
            var field = LargeCauldronBlockEntity.class.getDeclaredField("ignited");
            field.setAccessible(true);
            field.setBoolean(tank, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static Map<String, Object> capture(Minecraft client, LargeCauldronBlockEntity tank) {
        install();
        clock(300);
        var renderer = (LargeCauldronBlockEntityRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
        var state = renderer.createRenderState();
        renderer.extractRenderState(tank, state, 0.5F, client.gameRenderer.getMainCamera().position(), null);
        final int size = state.getItems().size();
        renderer.extractRenderState(tank, state, 0.5F, client.gameRenderer.getMainCamera().position(), null);
        if (state.getItems().size() != size) throw new IllegalStateException("Reused state accumulated items");
        state.lightCoords = 0xF00000;
        final int count = tank.getInputHandler().getAmountAsInt(0);
        var geometry = new LinkedHashMap<String, List<List<Float>>>();
        var fluids = new ArrayList<Map<String, Object>>();
        ORDER.clear();
        clock(900);
        renderer.submit(state, new PoseStack(), collector(geometry, fluids), new CameraRenderState());
        if (!ORDER.equals(List.of(count, -1))) throw new IllegalStateException("Hook order " + ORDER);
        if (hideFire && geometry.containsKey("fire")) throw new IllegalStateException("Fire veto ignored");
        if (count > 0) {
            var resource = tank.getInputHandler().getResource(0);
            tank.getInputHandler().setStackInSlot(0, resource.toStack(count + 1));
            ORDER.clear();
            renderer.submit(state, new PoseStack(), collector(new LinkedHashMap<>(), new ArrayList<>()), new CameraRenderState());
            if (!ORDER.equals(List.of(count, -1))) throw new IllegalStateException("Hook used live data during submit");
            tank.getInputHandler().setStackInSlot(0, resource.toStack(count));
        }
        var child = new LargeCauldronBlockEntity(tank.getType(), tank.getBlockPos(),
            tank.getBlockState().setValue(LargeCauldronBlock.HALF, Cube3x3PartHalf.BOTTOM_CENTER));
        child.setLevel(client.level);
        renderer.extractRenderState(child, state, 0, client.gameRenderer.getMainCamera().position(), null);
        if (!state.getItems().isEmpty() || !state.getFluids().isEmpty() || state.getFire() != null || !state.getAfterRender().isEmpty()) {
            throw new IllegalStateException("Child part retained main-part rendering");
        }
        clock(300);
        return Map.of("geometry", geometry, "fluids", fluids);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SubmitNodeCollector collector(Map<String, List<List<Float>>> geometry, List<Map<String, Object>> fluids) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
            new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                if (method.getName().equals("order")) return proxy;
                if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                if (method.getName().equals("submitItem")) {
                    var vertices = geometry.computeIfAbsent("items", ignored -> new ArrayList<>());
                    for (var quad : (List<BakedQuad>) args[6]) {
                        for (int i = 0; i < 4; i++) {
                            var pos = new Vector3f(quad.position(i)).mulPosition(((PoseStack) args[0]).last().pose());
                            vertices.add(List.of(pos.x, pos.y, pos.z));
                        }
                    }
                } else if (method.getName().equals("submitModel")) {
                    var renderState = (BlockStateModelTessellateState) args[1];
                    if (renderState.lighting() || (int) args[4] != 0xF000F0) throw new IllegalStateException("Fire lighting");
                    var recorder = new FluidGeometryRecorder();
                    Model model = (Model) args[0];
                    model.setupAnim(args[1]);
                    model.renderToBuffer((PoseStack) args[2], recorder, (int) args[4], (int) args[5], (int) args[6]);
                    add(geometry, "fire", recorder);
                } else if (method.getName().equals("submitCustomGeometry")) {
                    var recorder = new FluidGeometryRecorder();
                    ((SubmitNodeCollector.CustomGeometryRenderer) args[2]).render(((PoseStack) args[0]).last(), recorder);
                    fluids.add(Map.of("material", args[1] == RenderTypes.cutoutMovingBlock() ? "cutout" : "translucent",
                        "vertices", recorder.vertices()));
                    add(geometry, "fluid", recorder);
                }
                return null;
            });
    }

    private static void add(Map<String, List<List<Float>>> geometry, String key, FluidGeometryRecorder recorder) {
        var vertices = geometry.computeIfAbsent(key, ignored -> new ArrayList<>());
        for (var p : recorder.vertices()) vertices.add(List.of(p.x(), p.y(), p.z()));
    }
}
