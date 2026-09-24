package dev.dubhe.anvilcraft.client.renderer.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelRenderer;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilBlockItem;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CFARenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CFARenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class CelestialForgingAnvilItemRenderer implements SpecialModelRenderer<CelestialForgingAnvilItemRenderer.Argument> {
    private static final Cache<Object, Preview> PREVIEWS = CacheBuilder.newBuilder().maximumSize(16).build();
    private static final Cache<BlockStateModel, AABB> BOUNDS = CacheBuilder.newBuilder().maximumSize(64).build();
    private static final AABB UNIT = new AABB(0, 0, 0, 1, 1, 1);
    private static final AABB HEAD = new AABB(0.234375, 0.234375, 0.234375, 0.765625, 0.765625, 0.765625);
    private static @Nullable ClientLevel previewLevel;
    private final boolean head;

    public CelestialForgingAnvilItemRenderer(boolean head) {
        this.head = head;
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        clear();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        PREVIEWS.invalidateAll();
        BOUNDS.invalidateAll();
        previewLevel = null;
    }

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        var client = Minecraft.getInstance();
        if (client.level == null) return null;
        if (previewLevel != client.level) {
            clear();
            previewLevel = client.level;
        }
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        Object key = data == null ? CustomData.EMPTY : data;
        Preview preview = PREVIEWS.getIfPresent(key);
        if (preview == null) {
            var entity = new CelestialForgingAnvilBlockEntity(ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(),
                BlockPos.ZERO, ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState());
            if (data != null) {
                var tag = data.copyTagWithoutId();
                tag.merge(tag.getCompoundOrEmpty(CelestialForgingAnvilBlockItem.ITEM_RENDER_DATA));
                entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), tag));
            }
            entity.setLevel(client.level);
            preview = new Preview(entity);
            PREVIEWS.put(key, preview);
        }
        var candidate = client.getBlockEntityRenderDispatcher()
            .<CelestialForgingAnvilBlockEntity, CFARenderState>getRenderer(preview.entity);
        if (!(candidate instanceof CFARenderer renderer)) return null;
        var state = renderer.createRenderState();
        renderer.extractItemState(preview.entity, state, this.head);
        return new Argument(renderer, state, this.head ? preview.parts() : List.of(), bounds(state), preview.entity.getBodySeed(), key);
    }

    private static AABB bounds(CFARenderState state) {
        if (state.getEffectiveBodyData() instanceof SpecialCelestialBodyData special && special.isPlayerHead()) return HEAD;
        var tessellation = state.getComplexBodyModel() == null ? state.getBodyModel() : state.getComplexBodyModel();
        if (tessellation == null) return UNIT;
        var model = Minecraft.getInstance().getModelManager().getStandaloneModel(tessellation.key());
        return modelBounds(model);
    }

    public static AABB modelBounds(@Nullable BlockStateModel model) {
        if (model == null) return UNIT;
        var cached = BOUNDS.getIfPresent(model);
        if (cached != null) return cached;
        double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        BlockStateModelRenderer.INSTANCE.getTessellatorNoLighting().tesselateBlock((x, y, z, quad, instance) -> {
            for (int vertex = 0; vertex < 4; vertex++) {
                var point = quad.position(vertex);
                for (int axis = 0; axis < 3; axis++) {
                    min[axis] = Math.min(min[axis], point.get(axis));
                    max[axis] = Math.max(max[axis], point.get(axis));
                }
            }
        }, 0, 0, 0, BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), model, 42);
        var result = Double.isFinite(min[0]) && Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2])) > 0.0001
            ? new AABB(min[0], min[1], min[2], max[0], max[1], max[2]) : UNIT;
        BOUNDS.put(model, result);
        return result;
    }

    @Override
    public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outlineColor) {
        if (argument == null) return;
        if (this.head) {
            for (Part part : argument.parts()) {
                pose.pushPose();
                pose.translate(part.offset().getX(), part.offset().getY(), part.offset().getZ());
                part.model().submit(pose, collector, light, overlay, outlineColor);
                pose.popPose();
            }
            argument.renderer().submitHeadItem(argument.state(), pose, collector);
        } else {
            argument.renderer().submitItemBody(argument.state(), argument.bounds(), argument.seed(), pose, collector);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        AABB bounds = this.head ? new AABB(-4, 0, -4, 5, 12, 5) : new AABB(-0.5, 0.8, -0.5, 1.5, 2.9, 1.5);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    output.accept(new Vector3f((float) (x == 0 ? bounds.minX : bounds.maxX),
                        (float) (y == 0 ? bounds.minY : bounds.maxY), (float) (z == 0 ? bounds.minZ : bounds.maxZ)));
                }
            }
        }
    }

    private static final class Preview {
        private final CelestialForgingAnvilBlockEntity entity;
        private @Nullable List<Part> parts;

        private Preview(CelestialForgingAnvilBlockEntity entity) {
            this.entity = entity;
        }

        private List<Part> parts() {
            if (this.parts == null) {
                var block = ModBlocks.CELESTIAL_FORGING_ANVIL.get();
                List<Part> result = new ArrayList<>();
                for (var part : block.getParts()) {
                    result.add(new Part(new BlockPos(part.getOffset()),
                        FeatureRendererSupport.initialize(block.placedState(part, block.defaultBlockState()), this.entity)));
                }
                this.parts = List.copyOf(result);
            }
            return this.parts;
        }
    }

    public record Part(BlockPos offset, BlockModelRenderState model) {
    }

    public record Argument(CFARenderer renderer, CFARenderState state, List<Part> parts, AABB bounds, long seed, Object identity) {
        public Argument(CFARenderer renderer, CFARenderState state, List<Part> parts, AABB bounds, long seed) {
            this(renderer, state, parts, bounds, seed, state);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Argument argument && this.renderer == argument.renderer && this.seed == argument.seed
                && this.identity.equals(argument.identity) && this.parts.equals(argument.parts) && this.bounds.equals(argument.bounds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.renderer, this.identity, this.parts, this.bounds, this.seed);
        }
    }

    public record Unbaked(boolean head) implements SpecialModelRenderer.Unbaked<Argument> {
        public static final MapCodec<Unbaked> CODEC = Codec.BOOL.optionalFieldOf("head", false).xmap(Unbaked::new, Unbaked::head);

        @Override
        public CelestialForgingAnvilItemRenderer bake(BakingContext context) {
            return new CelestialForgingAnvilItemRenderer(this.head);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
