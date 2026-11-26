package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.preview.level.SandboxRenderLevel;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.block.utility.redstone.ButtonMount;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.SmartBlockPlacerRenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.WorldFluidRenderState;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.config.NeoForgeClientConfig;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class MountedBlockProjection {
    private static final Cache<MountedButtonBlock, MountedBlockProjection> ITEMS = CacheBuilder.newBuilder().maximumSize(64).build();
    private static final Map<BigRedButtonBlockEntity, MountedBlockProjection> HOSTS = new WeakHashMap<>();
    private static final ThreadLocal<Set<MountedButtonBlock>> ACTIVE = ThreadLocal.withInitial(HashSet::new);
    private static @Nullable RegistryAccess registries;
    private static @Nullable BlockStateModelSet models;
    private final MountedButtonBlock appearance;
    private final BlockPos origin;
    private final Map<BlockPos, BlockState> sourceStates;
    private final boolean chromatic;
    private final List<Part> parts;
    private final @Nullable PreviewLevel world;
    private final Fit fit;
    private final boolean entityOnly;
    private final boolean hasFeatures;
    private @Nullable SurfaceCache lastSurfaces;
    private @Nullable ModelBlockRenderer renderer;
    private @Nullable MeshSettings rendererSettings;

    private MountedBlockProjection(@Nullable ClientLevel level, MountedButtonBlock block, @Nullable BigRedButtonBlockEntity host) {
        this.appearance = block;
        this.origin = host == null ? BlockPos.ZERO : host.getBlockPos().immutable();
        var mount = level == null ? ButtonMount.forBlock(block.state()) : ButtonMount.forMounted(block, level.registryAccess());
        this.chromatic = block.state().is(ModBlocks.CHROMATIC_STONE);
        if (this.origin.equals(BlockPos.ZERO)) {
            this.sourceStates = mount.parts();
        } else {
            Map<BlockPos, BlockState> states = new HashMap<>();
            mount.parts().forEach((offset, state) -> states.put(this.origin.offset(offset), state));
            this.sourceStates = Map.copyOf(states);
        }
        boolean hasEntities = this.sourceStates.values().stream().anyMatch(BlockState::hasBlockEntity);
        this.world = level == null || !hasEntities ? null
            : new PreviewLevel(level.registryAccess(), level.enabledFeatures(), this.sourceStates, block);
        var measurement = new PoseStack();
        Direction facing = host == null ? Direction.UP : host.getBlockState().getValue(BigRedButtonBlock.FACING);
        measurement.translate(0.5, 0.5, 0.5);
        measurement.mulPose(BlockModelRotation.get(MountedBlockView.rotation(facing)).transformation().getMatrix());
        measurement.translate(-0.5, -0.5, -0.5);
        if (!this.chromatic) {
            Fit.fromMount(mount).apply(measurement);
        }
        var view = this.view(host, facing, LightCoordsUtil.FULL_BRIGHT, measurement.last().pose());
        var footprint = new MountedModelFootprint();
        Supplier<List<Part>> prepare = () -> mount.parts().entrySet().stream()
            .map(entry -> new Part(view, this.world, entry.getKey(), this.origin.offset(entry.getKey()), entry.getValue(),
                block, this.chromatic, footprint)).toList();
        this.parts = this.world == null ? prepare.get() : this.world.withEnvironment(view, prepare);
        this.fit = this.chromatic ? Fit.fromMount(mount) : footprint.fit(mount);
        this.entityOnly = this.parts.stream().noneMatch(part -> part.hasGeometry);
        this.hasFeatures = this.parts.stream().anyMatch(Part::hasFeatures);
    }

    public record Fit(Vec3 anchor, double scale, AABB bounds) {
        static Fit fromMount(ButtonMount mount) {
            return new Fit(mount.anchor(), mount.scale(), mount.bounds());
        }

        void apply(PoseStack pose) {
            pose.translate(0.5, ButtonMount.FLOOR, 0.5);
            pose.scale((float) this.scale, (float) this.scale, (float) this.scale);
            pose.translate(-this.anchor.x, -this.anchor.y, -this.anchor.z);
        }
    }

    static MountedButtonBlock displayBlock(MountedButtonBlock mounted, boolean powered) {
        BlockState state = mounted.state();
        BlockState display = state.trySetValue(IPowerComponent.OVERLOAD, !powered);
        return display == state ? mounted : mounted.withState(display);
    }

    public static Fit fit(MountedButtonBlock block) {
        return fit(block, null);
    }

    static Fit fit(MountedButtonBlock block, @Nullable BigRedButtonBlockEntity host) {
        var projection = inspect(block, host);
        return projection == null ? Fit.fromMount(ButtonMount.forBlock(block.state())) : projection.fit;
    }

    static boolean entityOnly(MountedButtonBlock block, BigRedButtonBlockEntity host) {
        var projection = inspect(block, host);
        return projection != null && projection.entityOnly;
    }

    private static @Nullable MountedBlockProjection inspect(MountedButtonBlock block, @Nullable BigRedButtonBlockEntity host) {
        var client = Minecraft.getInstance();
        if (client == null || !ACTIVE.get().add(block)) {
            return null;
        }
        try {
            return cached(client, block, host);
        } finally {
            ACTIVE.get().remove(block);
        }
    }

    private static MountedBlockProjection cached(Minecraft client, MountedButtonBlock block, @Nullable BigRedButtonBlockEntity host) {
        var currentModels = client.getModelManager().getBlockStateModelSet();
        var level = client.level;
        RegistryAccess access = level == null ? null : level.registryAccess();
        if (registries != access || models != currentModels) {
            ITEMS.invalidateAll();
            HOSTS.clear();
            registries = access;
            models = currentModels;
        }
        var projection = host == null ? ITEMS.getIfPresent(block) : HOSTS.get(host);
        if (projection == null || !projection.appearance.equals(block)) {
            projection = new MountedBlockProjection(level, block, host);
            if (host == null) {
                ITEMS.put(block, projection);
            } else {
                HOSTS.put(host, projection);
            }
        }
        return projection;
    }

    /** 物品与方块实体共用这一帧的安装物几何和方块实体渲染状态。 */
    @FunctionalInterface
    public interface Frame {
        void submit(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, int light, int overlay, int outline);
    }

    public static @Nullable Frame extract(MountedButtonBlock block, float partialTick, Vec3 camera) {
        return extract(block, partialTick, camera, null, null, Direction.UP, LightCoordsUtil.FULL_BRIGHT, ModelState.NO_TRANSFORM);
    }

    static @Nullable Frame extract(MountedButtonBlock block, float partialTick, Vec3 camera, @Nullable Double danceTime,
                                   @Nullable BigRedButtonBlockEntity host, Direction facing, int packedLight, Matrix4fc capPose) {
        Minecraft client = Minecraft.getInstance();
        if (!ACTIVE.get().add(block)) {
            return null;
        }
        try {
            var projection = cached(client, block, host);
            return projection.frame(block, partialTick, camera, danceTime, host, facing, packedLight, capPose);
        } finally {
            ACTIVE.get().remove(block);
        }
    }

    private Frame frame(MountedButtonBlock block, float partialTick, Vec3 camera, @Nullable Double danceTime,
                        @Nullable BigRedButtonBlockEntity host, Direction facing, int packedLight, Matrix4fc capPose) {
        var transform = new PoseStack();
        transform.mulPose(capPose);
        if (!this.chromatic) {
            this.fit.apply(transform);
        }
        Matrix4fc modelToButton = transform.last().pose();
        var environment = this.view(host, facing, packedLight, modelToButton);
        Vec3 sourceCamera = host == null || !this.hasFeatures ? camera : Vec3.atLowerCornerOf(this.origin)
            .add(new Vec3(new Matrix4f(modelToButton).invert().transformPosition(camera.toVector3f())));
        var worldFeatures = host == null ? null : this.features(environment, partialTick, sourceCamera, danceTime);
        var worldGeometry = host == null ? null : this.surfaces(host, environment, modelToButton, packedLight);
        return (pose, collector, view, light, overlay, outline) -> {
            if (!ACTIVE.get().add(block)) {
                return;
            }
            pose.pushPose();
            try {
                if (!this.chromatic) {
                    this.fit.apply(pose);
                }
                var itemView = host == null && light != packedLight ? this.view(null, Direction.UP, light, modelToButton) : environment;
                var surfaces = worldGeometry != null ? worldGeometry : this.surfaces(null, itemView, modelToButton, light);
                var features = worldFeatures != null ? worldFeatures : this.features(itemView, partialTick, sourceCamera, danceTime);
                for (int i = 0; i < this.parts.size(); i++) {
                    BlockPos pos = this.parts.get(i).offset;
                    pose.pushPose();
                    try {
                        pose.translate(pos.getX(), pos.getY(), pos.getZ());
                        var surface = surfaces.parts.get(i);
                        surface.submit(pose, collector);
                        if (!features.isEmpty()) {
                            features.get(i).submit(pose, collector, view, surface.light, overlay, outline);
                        }
                    } finally {
                        pose.popPose();
                    }
                }
            } finally {
                pose.popPose();
                ACTIVE.get().remove(block);
            }
        };
    }

    private List<Frame> features(MountedBlockView view, float partialTick, Vec3 camera, @Nullable Double danceTime) {
        if (!this.hasFeatures) {
            return List.of();
        }
        Supplier<List<Frame>> capture = () -> this.parts.stream().map(part -> part.capture(partialTick, camera, danceTime)).toList();
        return this.world == null ? capture.get() : this.world.withEnvironment(view, capture);
    }

    private MountedBlockView view(@Nullable BigRedButtonBlockEntity host, Direction facing, int light, Matrix4fc modelToButton) {
        var environment = host != null && host.getLevel() instanceof BlockAndTintGetter level ? level : null;
        return new MountedBlockView(this.world == null ? BlockAndTintGetter.EMPTY : this.world, this.sourceStates,
            environment, this.origin, facing, light, modelToButton);
    }

    private SurfaceCache surfaces(@Nullable BigRedButtonBlockEntity host, MountedBlockView view, Matrix4fc modelToButton, int light) {
        var client = Minecraft.getInstance();
        var settings = MeshSettings.current(client);
        boolean cacheWorld = host != null && host.getLevel() instanceof ClientLevel;
        long tick = cacheWorld ? host.getLevel().getGameTime() : 0;
        var previous = host == null || cacheWorld ? this.lastSurfaces : null;
        if (previous != null && previous.tick == tick && previous.transform.equals(modelToButton) && previous.light == light
            && settings.equals(previous.settings)) {
            return previous;
        }
        if (this.renderer == null || !settings.equals(this.rendererSettings)) {
            this.renderer = MountedWorldModel.renderer(settings.ambient, client.getBlockColors());
            this.rendererSettings = settings;
        }
        ModelBlockRenderer currentRenderer = this.renderer;
        var samples = this.world == null ? this.sample(currentRenderer, view, settings.cutoutLeaves)
            : this.world.withEnvironment(view, () -> this.sample(currentRenderer, view, settings.cutoutLeaves));
        var result = new SurfaceCache(tick, modelToButton, light, settings, samples);
        if (host == null || cacheWorld) {
            this.lastSurfaces = result;
        }
        return result;
    }

    private List<Surface> sample(ModelBlockRenderer renderer, MountedBlockView view, boolean cutoutLeaves) {
        return this.parts.stream().map(part -> part.surface(renderer, view, cutoutLeaves)).toList();
    }

    private record MeshSettings(boolean ambient, boolean cutoutLeaves, boolean enhancedLighting, boolean ambientPerPart) {
        static MeshSettings current(Minecraft client) {
            var config = NeoForgeClientConfig.INSTANCE;
            return new MeshSettings(client.options.ambientOcclusion().get(), client.options.cutoutLeaves().get(),
                config.enhancedLighting.getAsBoolean(), config.handleAmbientOcclusionPerPart.getAsBoolean());
        }
    }

    private record SurfaceCache(long tick, Matrix4fc transform, int light, MeshSettings settings, List<Surface> parts) {
    }

    private record Surface(@Nullable MountedWorldModel model, @Nullable WorldFluidRenderState fluid, int light) {
        void submit(PoseStack pose, SubmitNodeCollector collector) {
            if (this.model != null) {
                this.model.submitGeometry(pose, collector);
            }
            if (this.fluid != null) {
                this.fluid.submit(pose, collector);
            }
        }
    }

    private static final class Part {
        private static final Frame NO_FEATURES = (pose, collector, view, light, overlay, outline) -> {};
        private final BlockPos offset;
        private final BlockPos pos;
        private final BlockState state;
        private final @Nullable BlockStateModel model;
        private final boolean hasGeometry;
        private final @Nullable BlockEntity entity;
        private final @Nullable BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer;
        private final @Nullable ItemStackRenderState item;

        private Part(BlockAndTintGetter level, @Nullable PreviewLevel world, BlockPos offset, BlockPos pos, BlockState state,
                     MountedButtonBlock mounted, boolean chromatic, MountedModelFootprint footprint) {
            this.offset = offset;
            this.pos = pos;
            this.state = state;
            var client = Minecraft.getInstance();
            this.entity = level.getBlockEntity(pos);
            this.renderer = this.entity == null ? null : client.getBlockEntityRenderDispatcher().getRenderer(this.entity);
            var models = client.getModelManager();
            this.model = chromatic ? models.getStandaloneModel(BigRedButtonBlockEntityRenderer.chromaticModel(mounted))
                : state.getRenderShape() == RenderShape.MODEL ? models.getBlockStateModelSet().get(state) : null;
            this.hasGeometry = this.model != null && this.measure(level, this.model, footprint);
            if (world == null && state.hasBlockEntity() && !this.hasGeometry && offset.equals(BlockPos.ZERO)) {
                var item = new ItemStackRenderState();
                client.getItemModelResolver().updateForTopItem(item, mounted.material().create(), ItemDisplayContext.NONE, null, null, 0);
                this.item = item.isEmpty() ? null : item;
            } else {
                this.item = null;
            }
        }

        private boolean measure(BlockAndTintGetter level, BlockStateModel baked, MountedModelFootprint footprint) {
            var parts = new ArrayList<BlockStateModelPart>();
            baked.collectParts(level, this.pos, this.state, RandomSource.create(this.state.getSeed(this.pos)), parts);
            Vec3 offset = Vec3.atLowerCornerOf(this.offset).add(this.state.getOffset(this.pos));
            boolean visible = false;
            for (BlockStateModelPart part : parts) {
                for (BakedQuad quad : part.getQuads(null)) {
                    footprint.add(quad, offset);
                    visible = true;
                }
                for (Direction side : Direction.values()) {
                    if (Block.shouldRenderFace(level, this.pos, this.state, level.getBlockState(this.pos.relative(side)), side)) {
                        for (BakedQuad quad : part.getQuads(side)) {
                            footprint.add(quad, offset);
                            visible = true;
                        }
                    }
                }
            }
            return visible;
        }

        private Surface surface(ModelBlockRenderer renderer, MountedBlockView view, boolean cutoutLeaves) {
            var mesh = this.model == null ? null
                : MountedWorldModel.capture(renderer, view, this.pos, this.state, this.model, cutoutLeaves);
            var fluid = this.state.getFluidState().isEmpty() ? null : WorldFluidRenderState.extract(view, this.pos, this.state);
            return new Surface(mesh, fluid, LevelRenderer.getLightCoords(view, this.pos));
        }

        private boolean hasFeatures() {
            return this.renderer != null || this.item != null;
        }

        private Frame capture(float partialTick, Vec3 camera, @Nullable Double danceTime) {
            if (!this.hasFeatures()) {
                return NO_FEATURES;
            }
            var renderer = this.renderer;
            BlockEntityRenderState entityState = renderer == null ? null : renderer.createRenderState();
            BlockEntityRenderer<?, ?> modelRenderer = renderer;
            if (danceTime != null && this.entity instanceof SmartBlockPlacerBlockEntity placer
                && modelRenderer instanceof SmartBlockPlacerRenderer dancer && entityState instanceof SmartBlockPlacerRenderState arm) {
                dancer.extractDancingState(placer, arm, partialTick, camera, danceTime);
            } else if (this.entity != null && renderer != null && entityState != null) {
                renderer.extractRenderState(this.entity, entityState, partialTick, camera, null);
            }
            return (pose, collector, view, light, overlay, outline) -> {
                if (entityState != null && renderer != null) {
                    renderer.submit(entityState, pose, collector, view);
                }
                if (this.item != null) {
                    pose.pushPose();
                    try {
                        pose.translate(0.5, 0.5, 0.5);
                        this.item.submit(pose, collector, light, overlay, outline);
                    } finally {
                        pose.popPose();
                    }
                }
            };
        }
    }

    static final class PreviewLevel extends SandboxRenderLevel {
        private final Map<BlockPos, BlockState> states;
        private final Map<BlockPos, BlockEntity> entities = new HashMap<>();
        private @Nullable MountedBlockView environment;

        PreviewLevel(RegistryAccess registries, FeatureFlagSet features, Map<BlockPos, BlockState> states, MountedButtonBlock mounted) {
            super(registries, features);
            this.states = states;
            var stack = mounted.material().create();
            var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            states.forEach((pos, state) -> {
                if (state.getBlock() instanceof EntityBlock block) {
                    var entity = block.newBlockEntity(pos, state);
                    if (entity != null) {
                        entity.setLevel(this);
                        this.entities.put(pos, entity);
                    }
                }
            });
            for (BlockEntity entity : this.entities.values()) {
                if (data != null && data.type() == entity.getType()) {
                    data.loadInto(entity, this.registryAccess());
                }
                entity.applyComponentsFromItemStack(stack);
            }
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return this.entities.get(pos);
        }

        @Override
        public ModelData getModelData(BlockPos pos) {
            BlockEntity entity = this.entities.get(pos);
            return entity == null ? ModelData.EMPTY : entity.getModelData();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver color) {
            if (this.environment != null && this.environment.hasHost()) {
                return this.environment.getBlockTint(pos, color);
            }
            return color.getColor(this.getUncachedNoiseBiome(0, 0, 0).value(), pos.getX(), pos.getZ());
        }

        // 方块实体可能嵌套渲染另一个安装物，返回时需要恢复外层的采样位置。
        <T> T withEnvironment(MountedBlockView view, Supplier<T> extract) {
            var previous = this.environment;
            this.environment = view;
            try {
                return extract.get();
            } finally {
                this.environment = previous;
            }
        }

        @Override
        public Holder<Biome> getBiome(BlockPos pos) {
            var biome = this.environment == null ? null : this.environment.biomeAt(pos);
            return biome != null ? biome : super.getBiome(pos);
        }

        @Override
        public CardinalLighting cardinalLighting() {
            return this.environment != null ? this.environment.cardinalLighting() : super.cardinalLighting();
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return this.environment != null ? this.environment.getBrightness(layer, pos) : super.getBrightness(layer, pos);
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darkening) {
            return this.environment != null ? this.environment.getRawBrightness(pos, darkening) : super.getRawBrightness(pos, darkening);
        }

        @Override
        public long getGameTime() {
            Minecraft client = Minecraft.getInstance();
            return client == null || client.level == null ? super.getGameTime() : client.level.getGameTime();
        }

        // 本模组的访问转换器将 Level.getEntities 设为 public，沙盒的覆盖方法仍是 protected。
        @Override
        public LevelEntityGetter<Entity> getEntities() {
            return super.getEntities();
        }
    }
}
