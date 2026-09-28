package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.ClientTickRecorder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelTessellateState;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankRenderHooks;
import dev.dubhe.anvilcraft.client.renderer.blockentity.FishTankRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FishTankRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.joml.Vector3f;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FishTankContentsProbe {
    public static boolean hideFire;
    public static int callbacks;
    private static boolean installed;
    private static final List<Integer> HOOK_ORDER = new ArrayList<>();

    public static void install() {
        if (installed) return;
        installed = true;
        FishTankRenderHooks.register(new FishTankRenderHooks.Handler() {
            @Override
            public boolean showVanillaFire(FishTankBlockEntity tank) {
                return !hideFire;
            }

            @Override
            public FishTankRenderHooks.AfterRender extract(FishTankBlockEntity tank, float partialTick) {
                int amount = tank.getItemHandler().getAmountAsInt(0);
                return (pose, collector, light, overlay) -> {
                    callbacks++;
                    HOOK_ORDER.add(amount);
                };
            }
        });
        FishTankRenderHooks.register(new FishTankRenderHooks.Handler() {
            @Override
            public FishTankRenderHooks.AfterRender extract(FishTankBlockEntity tank, float partialTick) {
                return (pose, collector, light, overlay) -> HOOK_ORDER.add(-1);
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

    @SuppressWarnings("unchecked")
    private static List<TropicalFish> normalizeFish(FishTankRenderer renderer, FishTankBlockEntity tank) {
        try {
            var field = FishTankRenderer.class.getDeclaredField("fishCache");
            field.setAccessible(true);
            var cache = (Map<Long, ?>) field.get(renderer);
            var entry = cache.get(tank.getBlockPos().asLong());
            if (entry == null) return List.of();
            var fishes = entry.getClass().getDeclaredField("cachedFishes");
            fishes.setAccessible(true);
            var result = (List<TropicalFish>) fishes.get(entry);
            for (int i = 0; i < result.size(); i++) result.get(i).setId(10000 + i);
            return result;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static Map<String, Object> capture(Minecraft client, FishTankBlockEntity tank) {
        install();
        clock(300);
        var renderer = (FishTankRenderer) (Object) client.getBlockEntityRenderDispatcher().getRenderer(tank);
        var state = renderer.createRenderState();
        extract(client, renderer, tank, state);
        normalizeFish(renderer, tank);
        extract(client, renderer, tank, state);
        extract(client, renderer, tank, state);
        if (state.getStacks().size() != ItemHandlerUtil.getNonEmptyItemsFromHandler(tank.getItemHandler()).size()
            || state.getFishes().size() != tank.getFishes().size()) throw new IllegalStateException("Render state accumulated contents");
        if (state.getSeed() != ItemHandlerUtil.hash(tank.getItemHandler())) throw new IllegalStateException("Unstable inventory seed");
        for (var fish : state.getFishes()) {
            if (fish.lightCoords != state.lightCoords) throw new IllegalStateException("Fish lost tank lighting");
        }
        final int count = tank.getItemHandler().getAmountAsInt(0);
        HOOK_ORDER.clear();
        var batches = new LinkedHashMap<String, List<List<Float>>>();
        clock(900);
        renderer.submit(state, new PoseStack(), collector(batches), new CameraRenderState());
        if (!HOOK_ORDER.equals(List.of(count, -1))) throw new IllegalStateException("Hook order " + HOOK_ORDER);
        if (hideFire && !batches.getOrDefault("fire", List.of()).isEmpty()) throw new IllegalStateException("Fire veto was ignored");
        // Mutate live data after extraction: the extension must still use its captured value.
        if (count > 0) {
            var resource = tank.getItemHandler().getResource(0);
            tank.getItemHandler().set(0, resource, count + 1);
            HOOK_ORDER.clear();
            renderer.submit(state, new PoseStack(), collector(new LinkedHashMap<>()), new CameraRenderState());
            if (!HOOK_ORDER.equals(List.of(count, -1))) throw new IllegalStateException("Hook read live data during submit");
            tank.getItemHandler().set(0, resource, count);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("seed", state.getSeed());
        result.put("ticks", tank.isEmptyOfFish() ? 0 : state.getTicks());
        result.put("geometry", batches);
        clock(300);
        final boolean ignited = tank.isIgnited();
        final var items = new ArrayList<net.minecraft.world.item.ItemStack>();
        for (int slot = 0; slot < tank.getItemHandler().size(); slot++) {
            items.add(tank.getItemHandler().getResource(slot).toStack(tank.getItemHandler().getAmountAsInt(slot)));
        }
        final var fishData = List.copyOf(tank.getFishes());
        for (int slot = 0; slot < tank.getItemHandler().size(); slot++) tank.getItemHandler().set(slot, ItemResource.EMPTY, 0);
        tank.getFishes().clear();
        extract(client, renderer, tank, state);
        if (!state.getStacks().isEmpty() || !state.getFishes().isEmpty()) throw new IllegalStateException("Removed contents lingered");
        for (int slot = 0; slot < items.size(); slot++) {
            var item = items.get(slot);
            tank.getItemHandler().set(slot, ItemResource.of(item), item.getCount());
        }
        if (tank.getItemHandler().getAmountAsInt(0) != count) throw new IllegalStateException("Fixture lost restored items");
        tank.getFishes().addAll(fishData);
        tank.setIgnited(ignited);
        tank.setLevel(null);
        extract(client, renderer, tank, state);
        if (tank.getLevel() != client.level) throw new IllegalStateException("Level-less tank did not use the client level");
        return result;
    }

    private static void extract(Minecraft client, FishTankRenderer renderer, FishTankBlockEntity tank, FishTankRenderState state) {
        renderer.extractRenderState(tank, state, 0.5F, client.gameRenderer.getMainCamera().position(), null);
        for (var fish : state.getFishes()) {
            if (fish.lightCoords != state.lightCoords) throw new IllegalStateException("Fish lost extracted tank lighting");
        }
        var expectedBounds = new net.minecraft.world.phys.AABB(tank.getBlockPos()).expandTowards(0, 2, 0);
        if (!renderer.getRenderBoundingBox(tank).equals(expectedBounds)) throw new IllegalStateException("Fire bounds clipped");
        state.lightCoords = 0xF00000;
        for (var fish : state.getFishes()) fish.lightCoords = 0xF00000;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SubmitNodeCollector collector(Map<String, List<List<Float>>> batches) {
        return (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
            new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                if (method.getName().equals("order")) return proxy;
                if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                String name = method.getName();
                if (name.equals("submitItem")) {
                    addQuads(batches, "items", (PoseStack) args[0], (List<BakedQuad>) args[6]);
                } else if (name.equals("submitBlockModel")) {
                    for (var part : (List<BlockStateModelPart>) args[2]) {
                        for (Direction direction : Direction.values()) {
                            addQuads(batches, "fire", (PoseStack) args[0], part.getQuads(direction));
                        }
                        addQuads(batches, "fire", (PoseStack) args[0], part.getQuads(null));
                    }
                } else if (name.equals("submitModel")) {
                    var recorder = new FluidGeometryRecorder();
                    Model model = (Model) args[0];
                    model.setupAnim(args[1]);
                    model.renderToBuffer((PoseStack) args[2], recorder, (int) args[4], (int) args[5], (int) args[6]);
                    boolean fire = args[1] instanceof BlockStateModelTessellateState;
                    if (fire && (((BlockStateModelTessellateState) args[1]).lighting() || (int) args[4] != 0xF000F0)) {
                        throw new IllegalStateException("Fire lost unshaded full brightness");
                    }
                    addRecorded(batches, fire ? "fire" : "fish", recorder);
                } else if (name.equals("submitCustomGeometry")) {
                    var recorder = new FluidGeometryRecorder();
                    ((SubmitNodeCollector.CustomGeometryRenderer) args[2]).render(((PoseStack) args[0]).last(), recorder);
                    addRecorded(batches, "fluid", recorder);
                }
                return null;
            });
    }

    private static void addQuads(Map<String, List<List<Float>>> batches, String type, PoseStack pose, List<BakedQuad> quads) {
        var vertices = batches.computeIfAbsent(type, ignored -> new ArrayList<>());
        for (var quad : quads) {
            for (int i = 0; i < 4; i++) {
                var p = new Vector3f(quad.position(i)).mulPosition(pose.last().pose());
                vertices.add(List.of(p.x, p.y, p.z));
            }
        }
    }

    private static void addRecorded(Map<String, List<List<Float>>> batches, String type, FluidGeometryRecorder recorder) {
        var vertices = batches.computeIfAbsent(type, ignored -> new ArrayList<>());
        for (var p : recorder.vertices()) vertices.add(List.of(p.x(), p.y(), p.z()));
    }
}
