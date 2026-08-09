package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.util.ClientTickRecorder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FishTankRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.mixin.accessor.EntityAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FishTankRenderer extends BaseFluidHandlerHolderRenderer<FishTankBlockEntity, FishTankRenderState> {
    public static final StandaloneModelKey<BlockStateModel> FIRE = new StandaloneModelKey<>(
        () -> "AnvilCraft: Fish Tank Fire Model"
    );
    private static final float TANK_W = 1 / 16F + 0.001F; // avoiding Z-fighting
    private final RandomSource random = RandomSource.create();
    private final RandomSource fishRandom = RandomSource.create();
    private final ItemModelResolver resolver;
    private final EntityRenderDispatcher renderer;

    private final Map<Long, FishCacheEntry> fishCache = new HashMap<>();

    public FishTankRenderer(BlockEntityRendererProvider.Context ctx) {
        this.resolver = ctx.itemModelResolver();
        this.renderer = ctx.entityRenderer();
    }

    @Override
    public FishTankRenderState createRenderState() {
        return new FishTankRenderState();
    }

    @Override
    protected void updateTankW(
        FishTankBlockEntity be,
        FishTankRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setTankW(FishTankRenderer.TANK_W);
    }

    @Override
    public void extractRenderState(
        FishTankBlockEntity be,
        FishTankRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setIgnited(be.isIgnited());
        for (ItemStack stack : ItemHandlerUtil.getNonEmptyItemsFromHandler(be.getItemHandler())) {
            state.getStacks().add(Pair.of(stack, FeatureRendererSupport.initialize(stack, this.resolver)));
        }
        state.setFire(FeatureRendererSupport.initialize(FishTankRenderer.FIRE, be));
        // seed workaround
        state.setSeed(System.identityHashCode(be));

        Level level = be.getLevel();
        if (level == null) return;
        if (be.isEmptyOfFish()) return;

        List<FishTankBlockEntity.TropicalFishData> fishData = be.getFishes();
        int newDataHash = FishTankRenderer.computeFishDataHash(fishData);
        long cacheKey = be.getBlockPos().asLong();

        // Get or create cache entry
        FishCacheEntry cacheEntry = this.fishCache.get(cacheKey);
        List<TropicalFish> cachedFishes;

        // Rebuild cache if it doesn't exist or data has changed
        if (cacheEntry == null || cacheEntry.dataHash != newDataHash) {
            cachedFishes = FishTankRenderer.createTropicalFishEntities(level, fishData);
            this.fishCache.put(cacheKey, new FishCacheEntry(cachedFishes, newDataHash));
        } else {
            cachedFishes = cacheEntry.cachedFishes;
        }

        state.setTicks(ClientTickRecorder.getTicks() + partialTicks);
        for (TropicalFish fish : cachedFishes) {
            fish.tickCount = (int) state.getTicks();
            EntityRenderState entityState = this.renderer.extractEntity(fish, partialTicks);
            entityState.lightCoords = LightCoordsUtil.FULL_SKY;
            state.getFishes().add(entityState);
        }
        this.fishRandom.setSeed(cachedFishes.hashCode() + be.getBlockPos().hashCode());
    }

    @Override
    public void submit(FishTankRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        if (state.isIgnited()) {
            pose.pushPose();
            if (state.getFill() != 0) {
                pose.translate(0, (state.getMaxY() - state.getMinY()) * (state.getFill() - 1), 0);
            } else {
                pose.translate(0, FishTankRenderer.TANK_W - 1, 0);
            }
            state.getFire().submit(
                pose,
                collector,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0
            );
            pose.popPose();
        }
        if (!state.getStacks().isEmpty()) {
            FishTankRenderer.submitItemsInTank(
                state.getStacks(),
                pose,
                collector,
                this.random,
                state.getFill(),
                state.lightCoords,
                state.getSeed()
            );
        }
        FishTankRenderer.submitFishesInTank(state, this.renderer, pose, collector, camera);
    }

    // Thanks for Create Mod, logics in this method are mostly from it.
    private static void submitItemsInTank(
        List<Pair<ItemStack, ItemClusterRenderState>> items,
        PoseStack pose,
        SubmitNodeCollector collector,
        RandomSource random,
        float fill,
        int light,
        long seed
    ) {
        random.setSeed(seed);
        final float randomOffsetDeg = random.nextIntBetweenInclusive(0, 50) - 25;

        pose.pushPose();
        pose.translate(0.5F, FishTankRenderer.TANK_W, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(randomOffsetDeg));

        int itemCount = items.size();
        float y = Mth.clamp(fill - FishTankRenderer.TANK_W - 1 / 8F, FishTankRenderer.TANK_W, 1 - FishTankRenderer.TANK_W - 1 / 8F);
        float partAngleDeg = 360F / itemCount;
        Vec3 vec = itemCount == 1 ? new Vec3(0, y, 0) : new Vec3(0.125, y, 0);
        for (Pair<ItemStack, ItemClusterRenderState> entry : items) {
            final ItemStack stack = entry.getFirst();
            final ItemClusterRenderState cluster = entry.getSecond();
            pose.pushPose();

            if (fill > 0) {
                pose.translate(
                    0,
                    (Mth.sin(ClientTickRecorder.getTicks() / 12F + partAngleDeg * itemCount) + 1.5F) * 1 / 32F,
                    0
                );
            }

            float angle = Mth.DEG_TO_RAD * (partAngleDeg * itemCount);
            double sin = Mth.sin(angle);
            double cos = Mth.cos(angle);
            pose.translate(vec.x * cos + vec.z * sin, vec.y, vec.z * cos - vec.x * sin);
            pose.mulPose(
                new Quaternionf()
                    .rotateY(Mth.DEG_TO_RAD * (partAngleDeg * itemCount + 35))
                    .rotateX(Mth.DEG_TO_RAD * 65)
            );
            for (int i = 0; i <= stack.getCount() / 8; i++) {
                pose.pushPose();

                float radius = 1 / 16F;
                pose.translate(
                    0 + (random.nextFloat() - 0.5F) * 2 * radius,
                    0 + (random.nextFloat() - 0.5F) * 2 * radius,
                    0 + (random.nextFloat() - 0.5F) * 2 * radius
                );
                cluster.item.submit(
                    pose,
                    collector,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    0
                );
                pose.popPose();
            }
            pose.popPose();

            itemCount--;
        }
        pose.popPose();
    }

    private static void submitFishesInTank(
        FishTankRenderState state,
        EntityRenderDispatcher renderer,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        List<EntityRenderState> fishes = state.getFishes();
        float height = 1 - 2 * FishTankRenderer.TANK_W;
        int count = fishes.size();

        for (int i = 0; i < count; i++) {
            int ticks = (int) state.getTicks();

            float speed = 0.05F;
            float angle = ticks * speed + (Mth.TWO_PI / count) * i;
            float radius = 0.22F;
            float x = 0.5F + Mth.cos(angle) * radius;
            float z = 0.5F + Mth.sin(angle) * radius;

            float y = FishTankRenderer.TANK_W + height * (0.5F + Mth.sin(ticks * 0.07F + i) * 0.07F + Mth.sin(ticks * 0.19F + i) * 0.19F);

            float yawDeg = -(angle * Mth.RAD_TO_DEG);

            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
            pose.scale(0.5F, 0.5F, 0.5F);
            renderer.submit(fishes.get(i), camera, 0, 0, 0, pose, collector);
            pose.popPose();
        }
    }

    /// Creates TropicalFish entities from fish data NBT tags
    private static List<TropicalFish> createTropicalFishEntities(Level level, List<FishTankBlockEntity.TropicalFishData> fishData) {
        List<TropicalFish> fishes = new ArrayList<>();
        for (FishTankBlockEntity.TropicalFishData fishDatum : fishData) {
            TropicalFish fish = EntityType.TROPICAL_FISH.create(level, EntitySpawnReason.BUCKET);
            if (fish == null) continue;

            ItemStack bucket = fishDatum.toBucket();
            fish.applyComponentsFromItemStack(bucket);
            fish.fromBucket();
            fish.setNoAi(true);
            fish.setSilent(true);
            EntityAccessor accessor = Util.cast(fish);
            accessor.setWasTouchingWater(true);

            fishes.add(fish);
        }
        return fishes;
    }

    /// Computes a hash of the fish data to detect changes
    private static int computeFishDataHash(List<FishTankBlockEntity.TropicalFishData> fishData) {
        if (fishData.isEmpty()) return 0;
        int hash = fishData.size();
        for (FishTankBlockEntity.TropicalFishData tag : fishData) {
            hash = hash * 31 + tag.hashCode();
        }
        return hash;
    }

    private static class FishCacheEntry {
        List<TropicalFish> cachedFishes;
        int dataHash;

        FishCacheEntry(List<TropicalFish> cachedFishes, int dataHash) {
            this.cachedFishes = cachedFishes;
            this.dataHash = dataHash;
        }
    }
}
