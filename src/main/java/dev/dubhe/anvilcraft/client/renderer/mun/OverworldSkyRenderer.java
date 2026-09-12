package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.OverworldSkyMode;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath.Vector;
import dev.dubhe.anvilcraft.worldgen.OverworldSkyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.io.IOException;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class OverworldSkyRenderer {
    private static final ResourceLocation MOON_TEXTURE = AnvilCraft.of("textures/block/celestial_body/planet_atmosphereless.png");
    private static final ResourceLocation SUN_TEXTURE = AnvilCraft.of("block/celestial_body/star");
    private static @Nullable ShaderInstance shader;
    private static int atmosphereTexture = -1;
    private static int atmosphereWidth;
    private static int atmosphereHeight;

    private OverworldSkyRenderer() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        shader = null;
        if (atmosphereTexture != -1) TextureUtil.releaseTextureId(atmosphereTexture);
        atmosphereTexture = -1;
        atmosphereWidth = 0;
        atmosphereHeight = 0;
        try {
            ShaderInstance loaded = new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("overworld_sky"),
                DefaultVertexFormat.POSITION);
            if (GlStateManager.glGetProgrami(loaded.getId(), GL20C.GL_LINK_STATUS) == GL20C.GL_FALSE) {
                loaded.close();
                return;
            }
            event.registerShader(loaded, instance -> shader = instance);
        } catch (IOException | RuntimeException exception) {
            AnvilCraft.LOGGER.warn("Special Overworld sky unavailable; using vanilla sun and moon.", exception);
        }
    }

    public static boolean enabled() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && Level.OVERWORLD.equals(level.dimension()) && shader != null
            && AnvilCraft.CLIENT_CONFIG.overworldSkyMode == OverworldSkyMode.SPECIAL;
    }

    /** 保存原版天空与晨昏渐变；纹理复用，只在窗口尺寸变化时重新分配。 */
    public static void captureAtmosphere() {
        if (!enabled()) return;
        var target = Minecraft.getInstance().getMainRenderTarget();
        try (MunRenderScope ignored = MunRenderScope.resources()) {
            if (atmosphereTexture == -1) atmosphereTexture = TextureUtil.generateTextureId();
            if (atmosphereWidth != target.width || atmosphereHeight != target.height) {
                TextureUtil.prepareImage(atmosphereTexture, target.width, target.height);
                atmosphereWidth = target.width;
                atmosphereHeight = target.height;
            }
            GlStateManager._bindTexture(atmosphereTexture);
            GlStateManager._texParameter(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
            GlStateManager._texParameter(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
            int previous = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
            try {
                GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING));
                GL11C.glCopyTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, 0, 0, atmosphereWidth, atmosphereHeight);
            } finally {
                GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previous);
            }
        }
    }

    public static void render(Matrix4f view, Matrix4f projection, float partialTick) {
        if (!enabled()) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ShaderInstance skyShader = shader;
        if (level == null || skyShader == null) return;
        OverworldSkyState state = OverworldSkyState.at(level.getDayTime(), MunClientSky.partialDayTime(level, partialTick),
            level.getTimeOfDay(partialTick));
        skyShader.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());
        skyShader.safeGetUniform("InverseView").set(new Matrix4f(view).invert());
        setVector(skyShader, "SunDirection", state.sun());
        setVector(skyShader, "MoonCenter", state.moon());
        Vector x = state.moonNormal(new Vector(1, 0, 0));
        Vector y = state.moonNormal(new Vector(0, 1, 0));
        Vector z = state.moonNormal(new Vector(0, 0, 1));
        skyShader.safeGetUniform("MoonRotation").set(new Matrix4f(
            (float) x.x(), (float) x.y(), (float) x.z(), 0,
            (float) y.x(), (float) y.y(), (float) y.z(), 0,
            (float) z.x(), (float) z.y(), (float) z.z(), 0, 0, 0, 0, 1
        ));
        TextureAtlasSprite sun = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(SUN_TEXTURE);
        skyShader.safeGetUniform("SunUvBounds").set(sun.getU0(), sun.getV0(), sun.getU1(), sun.getV1());
        skyShader.safeGetUniform("Visibility").set(1 - level.getRainLevel(partialTick));
        try (MunRenderScope ignored = MunRenderScope.celestialSky()) {
            RenderSystem.setShaderTexture(0, MOON_TEXTURE);
            RenderSystem.setShaderTexture(1, TextureAtlas.LOCATION_BLOCKS);
            RenderSystem.setShaderTexture(2, atmosphereTexture);
            RenderSystem.setShader(() -> skyShader);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            MunSkyRenderer.drawScreenQuad();
        }
    }

    private static void setVector(ShaderInstance target, String name, Vector value) {
        target.safeGetUniform(name).set((float) value.x(), (float) value.y(), (float) value.z());
    }
}
