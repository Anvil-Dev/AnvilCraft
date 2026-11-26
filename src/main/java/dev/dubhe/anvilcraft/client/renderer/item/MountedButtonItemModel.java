package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.MountedBlockProjection;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public record MountedButtonItemModel(ModelRenderProperties properties, Matrix4fc transform)
    implements ItemModel, SpecialModelRenderer<MountedBlockProjection.Frame> {
    @SubscribeEvent
    public static void register(RegisterItemModelsEvent event) {
        event.register(AnvilCraft.of("mounted_button"), Unbaked.CODEC);
    }

    @Override
    public void update(
        ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context,
        @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed
    ) {
        MountedButtonBlock block = MountedButtonBlock.get(stack);
        if (block == null) {
            return;
        }
        var frame = this.extractArgument(stack);
        if (frame == null) {
            return;
        }
        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(block);
        output.setAnimated();
        var layer = output.newLayer();
        layer.setupSpecialModel(this, frame);
        layer.setLocalTransform(this.transform);
        layer.setExtents(() -> corners(MountedBlockProjection.fit(block).bounds()));
        this.properties.applyToLayer(layer, context);
    }

    @Override
    public MountedBlockProjection.@Nullable Frame extractArgument(ItemStack stack) {
        var block = MountedButtonBlock.get(stack);
        if (block == null) {
            return null;
        }
        var client = Minecraft.getInstance();
        return MountedBlockProjection.extract(block, client.getDeltaTracker().getGameTimeDeltaPartialTick(false), Vec3.ZERO);
    }

    @Override
    public void submit(
        MountedBlockProjection.@Nullable Frame frame, PoseStack pose, SubmitNodeCollector collector,
        int light, int overlay, boolean foil, int outline
    ) {
        if (frame != null) {
            frame.submit(pose, collector, Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState,
                light, overlay, outline);
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (Vector3fc corner : corners(new AABB(0, 0, 0, 1, 1, 1))) {
            output.accept(corner);
        }
    }

    private static Vector3fc[] corners(AABB bounds) {
        var corners = new Vector3fc[8];
        for (int index = 0; index < corners.length; index++) {
            corners[index] = new Vector3f((float) ((index & 1) == 0 ? bounds.minX : bounds.maxX),
                (float) ((index & 2) == 0 ? bounds.minY : bounds.maxY), (float) ((index & 4) == 0 ? bounds.minZ : bounds.maxZ));
        }
        return corners;
    }

    public record Unbaked(Identifier base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = Identifier.CODEC.fieldOf("base").xmap(Unbaked::new, Unbaked::base);

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            var baker = context.blockModelBaker();
            var model = baker.getModel(this.base);
            var properties = ModelRenderProperties.fromResolvedModel(baker, model, model.getTopTextureSlots());
            return new MountedButtonItemModel(properties, transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
