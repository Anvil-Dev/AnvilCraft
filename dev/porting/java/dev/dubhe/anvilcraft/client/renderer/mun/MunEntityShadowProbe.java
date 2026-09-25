package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunEntityShadowProbe {
    private static boolean complete;

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        complete = false;
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        var client = Minecraft.getInstance();
        if (complete || !Boolean.getBoolean("anvilcraft.portMunShadowResources") || client.level == null
            || !client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.initialized) return;
        complete = true;
        var level = client.level;
        var origin = new Vec3(0, 650, 0);
        var profile = MunLightingProfile.of(MunLightingQuality.STANDARD);
        List<Entity> owned = new ArrayList<>();
        try (var ignored = new MunShadowGlScope(); var shadows = new MunEntityShadows()) {
            var cow = Objects.requireNonNull(EntityType.COW.create(level, EntitySpawnReason.LOAD));
            var invisible = Objects.requireNonNull(EntityType.COW.create(level, EntitySpawnReason.LOAD));
            var far = Objects.requireNonNull(EntityType.COW.create(level, EntitySpawnReason.LOAD));
            var item = new ItemEntity(level, 1.5, 650, 0, new ItemStack(Items.STONE));
            var falling = Objects.requireNonNull(EntityType.FALLING_BLOCK.create(level, EntitySpawnReason.LOAD));
            invisible.setInvisible(true);
            int id = -2_000_000;
            for (var entity : List.of(cow, invisible, far, item, falling)) {
                while (level.getEntity(id) != null) id--;
                entity.setId(id--);
                entity.setPos(entity == far ? 200 : entity == item ? 1.5 : entity == falling ? 3 : 0, 650, 0);
                entity.setOldPosAndRot();
                level.addEntity(entity);
                owned.add(entity);
            }
            var chest = new ChestBlockEntity(new BlockPos(2, 650, 0), Blocks.CHEST.defaultBlockState());
            chest.setLevel(level);
            shadows.refresh(level, origin, origin, 0, profile, List.of(chest));
            var first = buffers(shadows);
            check(first.size() >= 3 && vertices(shadows) > 36, "Actual cow/chest/item/falling-block models were not captured");
            final int firstHash = hash(first);
            cow.yBodyRotO = 0;
            cow.yBodyRot = 90;
            cow.yHeadRotO = 0;
            cow.yHeadRot = 90;
            shadows.refresh(level, origin, origin, 1, profile, List.of(chest));
            var second = buffers(shadows);
            check(first.equals(second), "Dynamic GPU handles were reallocated each frame");
            check(firstHash != hash(second), "Entity animation was replaced by static geometry");
            draw(shadows, origin);
            shadows.refresh(level, origin, origin, 1, limited(profile, 12, 1), List.of(chest));
            check(vertices(shadows) > 0 && vertices(shadows) <= 12, "Dynamic vertex budget was exceeded");
            owned.forEach(entity -> entity.setInvisible(true));
            shadows.refresh(level, origin, origin, 1, profile, List.of());
            check(shadows.isEmpty(), "Invisible or distant entities cast shadows");
            var generator = new CreativeGeneratorBlockEntity(new BlockPos(2, 650, 0), ModBlocks.CREATIVE_GENERATOR.getDefaultState());
            generator.setLevel(level);
            shadows.refresh(level, origin, origin, 0, profile, List.of(generator));
            check(vertices(shadows) > 0, "AnvilCraft generator custom geometry was not captured");
            final int generatorHash = hash(buffers(shadows));
            shadows.refresh(level, origin, origin, 1, profile, List.of(generator));
            check(generatorHash != hash(buffers(shadows)), "Generator animation was not captured");
            var allBuffers = buffers(shadows);
            for (int frame = 0; frame < 61; frame++) shadows.refresh(level, origin, origin, 1, profile, List.of());
            check(buffers(shadows).isEmpty(), "Idle dynamic buffers were not evicted");
            for (int buffer : allBuffers.values()) check(!GL15C.glIsBuffer(buffer), "Evicted GPU buffer leaked");
            var features = field(shadows, "features");
            var outlines = (net.minecraft.client.renderer.OutlineBufferSource) field(features, "outlines");
            shadows.close();
            check(outlines.outlineBufferSource.sharedBuffer.pointer == 0, "Unused outline storage leaked");
            shadows.close();
        } finally {
            for (Entity entity : owned) level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        }
        AnvilCraft.LOGGER.info("PORT_MUN_ENTITY_SHADOWS_PASSED: cow, chest, item, falling block, generator, "
            + "budgets, invisibility and idle disposal");
    }

    private static void draw(MunEntityShadows shadows, Vec3 origin) {
        try (var target = new MunShadowTarget(64)) {
            target.bind();
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GL11C.GL_LEQUAL);
            GlStateManager._disableCull();
            GlStateManager._disableBlend();
            var solar = new MunSolarLighting();
            solar.update(18000, 0, origin);
            var projection = new MunShadowProjection();
            projection.update(solar.project(origin.add(1, 0, 0)), origin, 8, 64);
            var program = new MunShadowProgram(false);
            program.apply(solar, 8, 64, false);
            program.origin(origin, projection.offset(origin));
            shadows.draw();
            final int rowLength = GL11C.glGetInteger(GL11C.GL_PACK_ROW_LENGTH);
            final int packBuffer = GL11C.glGetInteger(org.lwjgl.opengl.GL21C.GL_PIXEL_PACK_BUFFER_BINDING);
            GlStateManager._pixelStore(GL11C.GL_PACK_ROW_LENGTH, 0);
            GlStateManager._glBindBuffer(org.lwjgl.opengl.GL21C.GL_PIXEL_PACK_BUFFER, 0);
            var depths = MemoryUtil.memAllocFloat(64 * 64);
            try {
                GL11C.glReadPixels(0, 0, 64, 64, GL11C.GL_DEPTH_COMPONENT, GL11C.GL_FLOAT, depths);
                int occupied = 0;
                for (int i = 0; i < depths.capacity(); i++) {
                    if (depths.get(i) < 0.999F) occupied++;
                }
                check(occupied > 20 && occupied < 1500, "Entity silhouette failed to render: " + occupied);
                AnvilCraft.LOGGER.info("PORT_MUN_ENTITY_SHADOW_PIXELS: {}", occupied);
            } finally {
                MemoryUtil.memFree(depths);
                GlStateManager._pixelStore(GL11C.GL_PACK_ROW_LENGTH, rowLength);
                GlStateManager._glBindBuffer(org.lwjgl.opengl.GL21C.GL_PIXEL_PACK_BUFFER, packBuffer);
            }
        }
    }

    private static MunLightingProfile limited(MunLightingProfile p, int vertices, int entities) {
        return new MunLightingProfile(p.cascades(), p.resolution(), p.distance(), p.cacheRadius(), p.chunkVertices(), p.cacheVertices(),
            vertices, entities, p.dynamicDistance(), p.buildBudgetNanos(), p.entityShadows(), p.translucentShadows(),
            p.effectDownsample(), p.aoSamples(), p.aoRadius(), p.aoStrength(), p.glareStrength(), p.ambientFloor());
    }

    private static Map<Object, Integer> buffers(MunEntityShadows shadows) {
        Map<Object, Integer> result = new HashMap<>();
        var buffers = (Map<?, ?>) field(shadows, "buffers");
        for (var entry : buffers.entrySet()) {
            var buffer = nullableField(entry.getValue(), "buffer");
            if (buffer != null) result.put(entry.getKey(), (Integer) field(buffer, "buffer"));
        }
        return result;
    }

    private static int vertices(MunEntityShadows shadows) {
        int count = 0;
        for (var mesh : (List<?>) field(shadows, "meshes")) count += (Integer) field(field(mesh, "buffer"), "count");
        return count;
    }

    private static int hash(Map<Object, Integer> buffers) {
        int result = 0;
        for (int buffer : buffers.values()) {
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
            int size = GL15C.glGetBufferParameteri(GL15C.GL_ARRAY_BUFFER, GL15C.GL_BUFFER_SIZE);
            var data = MemoryUtil.memAlloc(size);
            try {
                GL15C.glGetBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, data);
                result += data.hashCode();
            } finally {
                MemoryUtil.memFree(data);
            }
        }
        return result;
    }

    private static Object field(Object owner, String name) {
        return Objects.requireNonNull(nullableField(owner, name));
    }

    @javax.annotation.Nullable
    private static Object nullableField(Object owner, String name) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
