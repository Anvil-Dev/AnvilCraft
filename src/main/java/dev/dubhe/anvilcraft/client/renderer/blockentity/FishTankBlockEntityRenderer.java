package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.client.event.ClientTickRecorder;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.mixin.accessor.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FishTankBlockEntityRenderer implements BlockEntityRenderer<FishTankBlockEntity> {
    public static final float FISH_SCALE = 0.5F;
    private static final ModelResourceLocation FIRE = ModelResourceLocation.standalone(AnvilCraft.of("block/fire_cauldron_fire4"));
    private final RandomSource random = RandomSource.create();
    private final RandomSource fishRandom = RandomSource.create();
    private final BlockRenderDispatcher dispatcher;

    private final Map<Long, FishCacheEntry> fishCache = new HashMap<>();

    public FishTankBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.dispatcher = ctx.getBlockRenderDispatcher();
    }

    private static class FishCacheEntry {
        List<TropicalFish> cachedFishes;
        int dataHash;
        
        FishCacheEntry(List<TropicalFish> cachedFishes, int dataHash) {
            this.cachedFishes = cachedFishes;
            this.dataHash = dataHash;
        }
    }

    @Override
    public void render(
        FishTankBlockEntity tank,
        float partialTick,
        PoseStack pose,
        MultiBufferSource source,
        int light,
        int overlay
    ) {
        FluidTank fluid = tank.getFluidHandler();
        float minY = TANK_W;
        float maxY = TANK_W;
        float fill = 0;
        if (!fluid.isEmpty()) {
            fill = Math.min((float) fluid.getFluidAmount() / fluid.getCapacity(), 1);

            // Top and bottom positions of the fluid inside the tank
            float height = 1 - 2 * TANK_W;

            maxY = minY + height * fill;
        }

        ItemStackHandler handler = tank.getItemHandler();
        this.random.setSeed(ItemHandlerUtil.hash(handler));
        Level level = tank.getLevel();
        if (level == null) return;
        FishTankBlockEntityRenderer.drawItemsInTank(
            level,
            ItemHandlerUtil.getNonEmptyItemsFromHandler(handler),
            fill,
            Minecraft.getInstance().getItemRenderer(),
            pose,
            source,
            this.random,
            light,
            overlay
        );

        this.drawTropicalFishInTank(tank, partialTick, pose, source, light);
        FishTankBlockEntityRenderer.drawFluidInTank(pose, source, light, fluid, minY, maxY);
        if (tank.isIgnited()) {
            pose.pushPose();
            pose.translate(0, maxY - (1 - TANK_W), 0);
            PoseStack.Pose last = pose.last();
            BakedModel fire = this.dispatcher.getBlockModelShaper().getModelManager().getModel(FishTankBlockEntityRenderer.FIRE);
            this.dispatcher.getModelRenderer().renderModel(
                last,
                source.getBuffer(RenderType.CUTOUT),
                null,
                fire,
                1,
                1,
                1,
                LightTexture.FULL_BRIGHT,
                overlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
            pose.popPose();
        }
    }

    private static final float TANK_W = 1 / 16F + 0.001F; // avoiding Z-fighting

    // Thanks for Create Mod, logics in this method are mostly from it.
    private static void drawItemsInTank(
        Level level,
        List<ItemStack> items,
        float fill,
        ItemRenderer renderer,
        PoseStack pose,
        MultiBufferSource source,
        RandomSource random,
        int light,
        int overlay
    ) {
        if (items.isEmpty()) return;
        final float randomOffsetDeg = random.nextIntBetweenInclusive(0, 50) - 25;

        pose.pushPose();
        pose.translate(0.5F, TANK_W, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(randomOffsetDeg));

        int itemCount = items.size();
        float y = Mth.clamp(fill - TANK_W - 1 / 8F, TANK_W, 1 - TANK_W - 1 / 8F);
        float partAngleDeg = 360F / itemCount;
        Vec3 vec = itemCount == 1 ? new Vec3(0, y, 0) : new Vec3(0.125, y, 0);
        for (ItemStack stack : items) {
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
                renderer.renderStatic(
                    stack,
                    ItemDisplayContext.GROUND,
                    light,
                    overlay,
                    pose,
                    source,
                    level,
                    0
                );

                pose.popPose();
            }
            pose.popPose();

            itemCount--;
        }
        pose.popPose();
        if (source instanceof MultiBufferSource.BufferSource buffer) buffer.endBatch();
    }

    private static void drawFluidInTank(PoseStack pose, MultiBufferSource source, int light, FluidTank fluid, float minY, float maxY) {
        if (fluid.isEmpty()) return;
        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid.getFluid(),
            TANK_W,
            minY,
            TANK_W,
            1 - TANK_W,
            maxY,
            1 - TANK_W,
            source,
            pose,
            light,
            true,
            false
        );
        if (source instanceof MultiBufferSource.BufferSource buffer) buffer.endBatch();
    }

    private void drawTropicalFishInTank(
        FishTankBlockEntity tank,
        float partialTick,
        PoseStack pose,
        MultiBufferSource source,
        int light
    ) {
        Level level = tank.getLevel();
        if (level == null) return;
        if (tank.isEmptyOfFish()) return;

        List<CompoundTag> fishData = tank.getTropicalFishData();
        int newDataHash = computeFishDataHash(fishData);
        long cacheKey = tank.getBlockPos().asLong();

        // Get or create cache entry
        FishCacheEntry cacheEntry = this.fishCache.get(cacheKey);
        List<TropicalFish> cachedFishes;

        // Rebuild cache if it doesn't exist or data has changed
        if (cacheEntry == null || cacheEntry.dataHash != newDataHash) {
            cachedFishes = createTropicalFishEntities(level, fishData);
            this.fishCache.put(cacheKey, new FishCacheEntry(cachedFishes, newDataHash));
        } else {
            cachedFishes = cacheEntry.cachedFishes;
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        this.fishRandom.setSeed(cachedFishes.hashCode() + tank.getBlockPos().hashCode());
        float ticks = ClientTickRecorder.getTicks() + partialTick + this.fishRandom.nextInt(1297361);
        float height = 1 - 2 * TANK_W;
        int count = cachedFishes.size();

        for (int i = 0; i < count; i++) {
            TropicalFish fish = cachedFishes.get(i);
            fish.tickCount = (int) ticks;

            float speed = 0.05F;
            float angle = ticks * speed + (Mth.TWO_PI / count) * i;
            float radius = 0.22F;
            float x = 0.5F + Mth.cos(angle) * radius;
            float z = 0.5F + Mth.sin(angle) * radius;

            float y = TANK_W + height * (0.5F + Mth.sin(ticks * 0.07F + i) * 0.07F + Mth.sin(ticks * 0.19F + i) * 0.19F);

            float yawDeg = -(angle * Mth.RAD_TO_DEG);

            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
            pose.scale(FISH_SCALE, FISH_SCALE, FISH_SCALE);
            dispatcher.render(fish, 0, 0, 0, yawDeg, partialTick, pose, source, light);
            pose.popPose();
        }
    }

    /**
     * Creates TropicalFish entities from fish data NBT tags
     */
    private static List<TropicalFish> createTropicalFishEntities(Level level, List<CompoundTag> fishData) {
        List<TropicalFish> fishes = new ArrayList<>();
        for (CompoundTag fishDatum : fishData) {
            TropicalFish fish = EntityType.TROPICAL_FISH.create(level);
            if (fish == null) continue;

            CompoundTag data = fishDatum.copy();
            fish.loadFromBucketTag(data);
            fish.fromBucket();
            fish.setNoAi(true);
            fish.setSilent(true);
            EntityAccessor accessor = Util.cast(fish);
            accessor.setWasTouchingWater(true);

            fishes.add(fish);
        }
        return fishes;
    }

    /**
     * Computes a hash of the fish data to detect changes
     */
    private static int computeFishDataHash(List<CompoundTag> fishData) {
        if (fishData.isEmpty()) return 0;
        int hash = fishData.size();
        for (CompoundTag tag : fishData) {
            hash = hash * 31 + tag.hashCode();
        }
        return hash;
    }
}
