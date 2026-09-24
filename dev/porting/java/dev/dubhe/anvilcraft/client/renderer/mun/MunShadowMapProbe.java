package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunShadowMapProbe {
    private static boolean complete;

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        complete = false;
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        var client = Minecraft.getInstance();
        if (complete || !Boolean.getBoolean("anvilcraft.portMunShadowResources") || client.level == null || client.player == null
            || client.getOverlay() != null
            || !client.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.initialized) return;
        var level = client.level;
        var chunk = ChunkPos.containing(client.player.blockPosition());
        if (level.getChunkSource().getChunk(chunk.x(), chunk.z(), ChunkStatus.FULL, false) == null) return;
        complete = true;
        check(MunShadowMesh.fullCube(ModBlocks.LUNAR_ROCK.getDefaultState(), level, BlockPos.ZERO),
            "Weighted lunar rock models lost full-cube merging");
        check(MunShadowMesh.fullCube(ModBlocks.LUNAR_SOIL.getDefaultState(), level, BlockPos.ZERO),
            "Lunar soil models lost full-cube merging");
        var base = new BlockPos(chunk.getMinBlockX() + 4, level.getMaxY() - 8, chunk.getMinBlockZ() + 4);
        var anchor = new Vec3(base.getX() + 2, 650, base.getZ() + 2);
        var solar = new MunSolarLighting();
        solar.update(18000, 0, anchor);
        var profile = profile(128_000);
        Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        var cow = Objects.requireNonNull(EntityType.COW.create(level, EntitySpawnReason.LOAD));
        boolean added = false;
        final int renderDistance = client.options.renderDistance().get();
        try (var ignored = new MunShadowGlScope(); var map = new MunShadowMap()) {
            client.options.renderDistance().set(2);
            for (int x = 0; x < 4; x++) {
                for (int z = 0; z < 4; z++) put(level, previous, base.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            }
            put(level, previous, base.offset(5, 1, 0), Blocks.RED_STAINED_GLASS.defaultBlockState());
            put(level, previous, base.offset(5, 1, 2), Blocks.WATER.defaultBlockState());
            settle(map, level, anchor, solar, profile, () -> !(Boolean) field(map, "meshesPending"));
            check(map.count() == 3 && chunks(map).containsKey(chunk.pack()), "Three cascades or the anchor chunk are missing");
            check((Integer) field(map, "cacheVertices") <= profile.cacheVertices(), "Global mesh cache exceeded its budget");
            final Object original = Objects.requireNonNull(chunks(map).get(chunk.pack()));
            int staticId = map.staticTextureId(0);
            int transmissionId = map.translucentTextureId(0);
            Snapshot depth = texture(staticId, false);
            check(depth.occupied > 0, "Static terrain did not write shadow depth");
            check(texture(transmissionId, true).occupied > 0, "Glass/fluid transmission was not rendered");
            check(map.textureId(0) == staticId, "An empty dynamic scene selected the wrong target");
            long revision = map.geometryRevision();
            map.prepare(level, anchor, anchor, solar, 0, profile);
            check(map.geometryRevision() == revision && chunks(map).get(chunk.pack()) == original,
                "Unchanged terrain was rebuilt");
            check(texture(staticId, false).equals(depth), "Unchanged static depth changed");
            level.setBlock(base, Blocks.DEEPSLATE.defaultBlockState(), 2);
            map.blockChanged(level, base, Blocks.STONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState());
            map.prepare(level, anchor, anchor, solar, 0, profile);
            check(chunks(map).get(chunk.pack()) == original, "Equivalent full cubes invalidated geometry");
            level.setBlock(base, Blocks.STONE_SLAB.defaultBlockState(), 2);
            map.blockChanged(level, base, Blocks.DEEPSLATE.defaultBlockState(), Blocks.STONE_SLAB.defaultBlockState());
            settle(map, level, anchor, solar, profile, () -> chunks(map).get(chunk.pack()) != original);
            check(map.geometryRevision() > revision, "Non-cube block change did not rebuild its chunk");
            int id = -3_000_000;
            while (level.getEntity(id) != null) id--;
            cow.setId(id);
            cow.setPos(anchor);
            cow.setOldPosAndRot();
            level.addEntity(cow);
            added = true;
            Snapshot staticBeforeEntity = texture(staticId, false);
            map.prepare(level, anchor, anchor, solar, 0, profile);
            check(map.textureId(0) != staticId, "Dynamic depth was not composited separately");
            check(texture(staticId, false).equals(staticBeforeEntity), "Dynamic draw overwrote static depth");
            check(!texture(map.textureId(0), false).equals(staticBeforeEntity), "Actual entity did not affect combined depth");
            level.removeEntity(cow.getId(), Entity.RemovalReason.DISCARDED);
            added = false;
            map.prepare(level, anchor, anchor, solar, 0, profile);
            check(map.textureId(0) == staticId, "Removed entity left stale dynamic shadows");
            Vec3 moved = anchor.add(10000, 0, 10000);
            solar.update(18000, 0, moved);
            map.prepare(level, moved, moved, solar, 0, profile);
            check(chunks(map).isEmpty() && (Integer) field(map, "cacheVertices") == 0, "Moving the window retained old chunks");
            check(texture(map.staticTextureId(0), false).occupied == 0, "Moved window retained old depth");
            check(texture(map.translucentTextureId(0), true).occupied == 0, "Moved window retained old transmission");
            solar.update(18000, 0, anchor);
            var deniedProfile = profile(0);
            settle(map, level, anchor, solar, deniedProfile, () -> ((Set<?>) field(map, "denied")).contains(chunk.pack()));
            check(!chunks(map).containsKey(chunk.pack()) && (Integer) field(map, "cacheVertices") == 0,
                "Over-budget chunks were admitted");
            int currentDepth = map.staticTextureId(0);
            int currentTransmission = map.translucentTextureId(0);
            map.close();
            map.close();
            check(map.count() == 0 && !GL11C.glIsTexture(currentDepth) && !GL11C.glIsTexture(currentTransmission),
                "Cascade textures survived close");
        } finally {
            client.options.renderDistance().set(renderDistance);
            if (added) level.removeEntity(cow.getId(), Entity.RemovalReason.DISCARDED);
            previous.forEach((pos, state) -> level.setBlock(pos, state, 2));
        }
        AnvilCraft.LOGGER.info("PORT_MUN_SHADOW_MAP_PASSED: three cascades, static/transmission/dynamic depth, "
            + "invalidation, budgets and disposal");
    }

    private static void put(ClientLevel level, Map<BlockPos, BlockState> previous, BlockPos pos, BlockState state) {
        previous.putIfAbsent(pos, level.getBlockState(pos));
        check(level.setBlock(pos, state, 2), "Could not place shadow fixture at " + pos);
    }

    private static void settle(MunShadowMap map, ClientLevel level, Vec3 anchor, MunSolarLighting solar,
                               MunLightingProfile profile, BooleanSupplier done) {
        for (int attempt = 0; attempt < 1000; attempt++) {
            map.prepare(level, anchor, anchor, solar, 0, profile);
            if (done.getAsBoolean()) {
                AnvilCraft.LOGGER.info("PORT_MUN_SHADOW_CACHE_SETTLED: calls={}, chunks={}, vertices={}",
                    attempt + 1, chunks(map).size(), field(map, "cacheVertices"));
                return;
            }
        }
        throw new IllegalStateException("Shadow cache did not settle: chunks=" + chunks(map).size()
            + ", vertices=" + field(map, "cacheVertices") + ", denied=" + ((Set<?>) field(map, "denied")).size());
    }

    private static MunLightingProfile profile(int cacheVertices) {
        return new MunLightingProfile(3, 128, 32, 32, 32768, cacheVertices,
            49152, 64, 64, 2_000_000, true, true, 2, 12, 0.9F, 0.65F, 0.065F, 0.30F);
    }

    private static Map<?, ?> chunks(MunShadowMap map) {
        return (Map<?, ?>) field(map, "chunks");
    }

    private static Object field(Object owner, String name) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return Objects.requireNonNull(field.get(owner));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Snapshot texture(int texture, boolean integer) {
        try (var ignored = new MunShadowGlScope()) {
            GlStateManager._bindTexture(texture);
            final int size = GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_WIDTH);
            final int rowLength = GL11C.glGetInteger(GL11C.GL_PACK_ROW_LENGTH);
            final int packBuffer = GL11C.glGetInteger(GL21C.GL_PIXEL_PACK_BUFFER_BINDING);
            GlStateManager._pixelStore(GL11C.GL_PACK_ROW_LENGTH, 0);
            GlStateManager._glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, 0);
            var data = MemoryUtil.memAlloc(size * size * (integer ? 8 : 4));
            try {
                GL11C.glGetTexImage(GL11C.GL_TEXTURE_2D, 0, integer ? GL30C.GL_RG_INTEGER : GL11C.GL_DEPTH_COMPONENT,
                    integer ? GL11C.GL_UNSIGNED_INT : GL11C.GL_FLOAT, data);
                int occupied = 0;
                for (int i = 0; i < size * size; i++) {
                    if (integer ? data.getInt(i * 8) != 0xFFFFFF : data.getFloat(i * 4) < 0.99999F) occupied++;
                }
                return new Snapshot(data.hashCode(), occupied);
            } finally {
                MemoryUtil.memFree(data);
                GlStateManager._pixelStore(GL11C.GL_PACK_ROW_LENGTH, rowLength);
                GlStateManager._glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, packBuffer);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record Snapshot(int hash, int occupied) {
    }
}
