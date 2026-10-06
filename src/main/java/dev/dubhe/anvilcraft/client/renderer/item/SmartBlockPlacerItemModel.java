package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.power.consumer.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.SmartBlockPlacerRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.SmartBlockPlacerRenderState;
import dev.dubhe.anvilcraft.client.selection.SelectionModel;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class SmartBlockPlacerItemModel implements ItemModel {
    private static final Map<LivingEntity, DanceAnimation> ANIMATIONS = new WeakHashMap<>();
    private final ItemModel ordinaryModel;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Renderer renderer = new Renderer();

    private SmartBlockPlacerItemModel(ItemModel ordinaryModel, ModelRenderProperties properties, Matrix4fc transformation) {
        this.ordinaryModel = ordinaryModel;
        this.properties = properties;
        this.transformation = transformation;
    }

    @SubscribeEvent
    public static void register(RegisterItemModelsEvent event) {
        event.register(AnvilCraft.of("smart_block_placer"), Unbaked.CODEC);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ANIMATIONS.clear();
            return;
        }
        ANIMATIONS.keySet().removeIf(wearer -> wearer.level() != minecraft.level || wearer.isRemoved()
            || !wearer.getItemBySlot(EquipmentSlot.HEAD).is(ModBlocks.SMART_BLOCK_PLACER.asItem()));
        if (minecraft.isPaused()) return;
        for (var player : minecraft.level.players()) {
            if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModBlocks.SMART_BLOCK_PLACER.asItem())) continue;
            ANIMATIONS.computeIfAbsent(player, wearer -> new DanceAnimation()).tick(hasPower(player));
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ANIMATIONS.clear();
    }

    private static boolean hasPower(LivingEntity wearer) {
        return wearer.getData(ModDataAttachments.IN_POWER_GRID) && !wearer.getData(ModDataAttachments.POWER_GRID_OVERLOADED);
    }

    @Override
    public void update(
        ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context,
        @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed
    ) {
        if (context != ItemDisplayContext.HEAD) {
            this.ordinaryModel.update(output, stack, resolver, context, level, owner, seed);
            return;
        }
        LivingEntity wearer = owner == null ? null : owner.asLivingEntity();
        boolean powered = wearer != null && hasPower(wearer);
        DanceAnimation animation = wearer == null ? null : ANIMATIONS.get(wearer);
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.isPaused() ? 1 : minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double renderTick = animation == null ? 0 : animation.getRenderTime(partialTick, powered);
        Argument argument = new Argument(powered, renderTick);
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(argument);
        output.setAnimated();
        var layer = output.newLayer();
        layer.setLocalTransform(this.transformation);
        layer.setExtents(Renderer::extents);
        layer.setupSpecialModel(this.renderer, argument);
        this.properties.applyToLayer(layer, context);
    }

    private static class DanceAnimation {
        private long ticks;
        private boolean playing;

        void tick(boolean powered) {
            this.playing = powered;
            if (powered) this.ticks++;
        }

        double getRenderTime(float partialTick, boolean powered) {
            return this.ticks - (this.playing && powered ? 1 - partialTick : 0);
        }
    }

    private record Argument(boolean powered, double renderTick) {
    }

    private static class Renderer implements SpecialModelRenderer<Argument> {
        private final SmartBlockPlacerBlockEntity preview = new SmartBlockPlacerBlockEntity(
            BlockPos.ZERO, ModBlocks.SMART_BLOCK_PLACER.getDefaultState()
        );
        private final Map<SelectionModel, BlockModelRenderState> models = new HashMap<>();

        @Override
        public Argument extractArgument(ItemStack stack) {
            return new Argument(false, 0);
        }

        @Override
        public void submit(
            @Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector,
            int light, int overlay, boolean foil, int outlineColor
        ) {
            if (argument == null) return;
            pose.pushPose();
            pose.translate(0, 0.9, 0);
            BlockState state = this.preview.getBlockState().setValue(SmartBlockPlacerBlock.OVERLOAD, !argument.powered());
            this.model(new SelectionModel.State(state)).submit(pose, collector, light, overlay, outlineColor);
            var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .<SmartBlockPlacerBlockEntity, SmartBlockPlacerRenderState>getRenderer(this.preview);
            if (renderer instanceof SmartBlockPlacerRenderer placerRenderer) {
                placerRenderer.collectDancingModels(argument.renderTick(), pose,
                    (model, modelPose) -> this.model(model).submitModel(
                        ModRenderTypes.CUTOUT_BLOCK, modelPose, collector, light, overlay, outlineColor));
            }
            pose.popPose();
        }

        private BlockModelRenderState model(SelectionModel key) {
            return this.models.computeIfAbsent(key, model -> {
                var manager = Minecraft.getInstance().getModelManager();
                BlockState state = model instanceof SelectionModel.State shell ? shell.state() : this.preview.getBlockState();
                BlockStateModel baked = model instanceof SelectionModel.Standalone standalone
                    ? (BlockStateModel) manager.getStandaloneModel(standalone.key()) : manager.getBlockStateModelSet().get(state);
                BlockModelRenderState result = new BlockModelRenderState();
                if (baked != null) {
                    baked.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RandomSource.create(42),
                        result.setupModel(new Matrix4f(), false));
                }
                return result;
            });
        }

        private static Vector3fc[] extents() {
            return new Vector3fc[]{
                new Vector3f(-1, 0.9f, -1), new Vector3f(-1, 0.9f, 2),
                new Vector3f(-1, 4, -1), new Vector3f(-1, 4, 2),
                new Vector3f(2, 0.9f, -1), new Vector3f(2, 0.9f, 2),
                new Vector3f(2, 4, -1), new Vector3f(2, 4, 2)
            };
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            for (Vector3fc extent : extents()) output.accept(extent);
        }
    }

    public record Unbaked(Identifier model) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = Identifier.CODEC.fieldOf("model").xmap(Unbaked::new, Unbaked::model);

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            var baker = context.blockModelBaker();
            var resolved = baker.getModel(this.model);
            return new SmartBlockPlacerItemModel(
                ItemModelUtils.plainModel(this.model).bake(context, transformation),
                ModelRenderProperties.fromResolvedModel(baker, resolved, resolved.getTopTextureSlots()), transformation
            );
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
