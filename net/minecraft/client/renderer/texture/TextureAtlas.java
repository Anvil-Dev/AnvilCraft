package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureAtlas extends AbstractTexture implements TickableTexture, Dumpable {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Deprecated
    public static final Identifier LOCATION_BLOCKS = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
    @Deprecated
    public static final Identifier LOCATION_ITEMS = Identifier.withDefaultNamespace("textures/atlas/items.png");
    @Deprecated
    public static final Identifier LOCATION_PARTICLES = Identifier.withDefaultNamespace("textures/atlas/particles.png");
    private List<TextureAtlasSprite> sprites = List.of();
    private List<SpriteContents.AnimationState> animatedTexturesStates = List.of();
    private Map<Identifier, TextureAtlasSprite> texturesByName = Map.of();
    private @Nullable TextureAtlasSprite missingSprite;
    private final Identifier location;
    private final int maxSupportedTextureSize;
    private int width;
    private int height;
    private int maxMipLevel;
    private int mipLevelCount;
    private GpuTextureView[] mipViews = new GpuTextureView[0];
    private @Nullable GpuBuffer spriteUbos;

    public TextureAtlas(Identifier location) {
        this.location = location;
        this.maxSupportedTextureSize = RenderSystem.getDevice().getMaxTextureSize();
    }

    private void createTexture(int newWidth, int newHeight, int newMipLevel) {
        LOGGER.info("Created: {}x{}x{} {}-atlas", newWidth, newHeight, newMipLevel, this.location);
        GpuDevice device = RenderSystem.getDevice();
        this.close();
        this.texture = device.createTexture(this.location::toString, 15, TextureFormat.RGBA8, newWidth, newHeight, 1, newMipLevel + 1);
        this.textureView = device.createTextureView(this.texture);
        this.width = newWidth;
        this.height = newHeight;
        this.maxMipLevel = newMipLevel;
        this.mipLevelCount = newMipLevel + 1;
        this.mipViews = new GpuTextureView[this.mipLevelCount];

        for (int level = 0; level <= this.maxMipLevel; level++) {
            this.mipViews[level] = device.createTextureView(this.texture, level, 1);
        }
    }

    public void upload(SpriteLoader.Preparations preparations) {
        this.createTexture(preparations.width(), preparations.height(), preparations.mipLevel());
        this.clearTextureData();
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        this.texturesByName = Map.copyOf(preparations.regions());
        this.missingSprite = this.texturesByName.get(MissingTextureAtlasSprite.getLocation());
        if (this.missingSprite == null) {
            throw new IllegalStateException("Atlas '" + this.location + "' (" + this.texturesByName.size() + " sprites) has no missing texture sprite");
        } else {
            List<TextureAtlasSprite> sprites = new ArrayList<>();
            List<SpriteContents.AnimationState> animationStates = new ArrayList<>();
            int animatedSpriteCount = (int)preparations.regions().values().stream().filter(TextureAtlasSprite::isAnimated).count();
            int spriteUboSize = Mth.roundToward(SpriteContents.UBO_SIZE, RenderSystem.getDevice().getUniformOffsetAlignment());
            int uboBlockSize = spriteUboSize * this.mipLevelCount;
            ByteBuffer spriteUboBuffer = MemoryUtil.memAlloc(animatedSpriteCount * uboBlockSize);
            int animationIndex = 0;

            for (TextureAtlasSprite sprite : preparations.regions().values()) {
                if (sprite.isAnimated()) {
                    sprite.uploadSpriteUbo(spriteUboBuffer, animationIndex * uboBlockSize, this.maxMipLevel, this.width, this.height, spriteUboSize);
                    animationIndex++;
                }
            }

            GpuBuffer spriteUbos = animationIndex > 0
                ? RenderSystem.getDevice().createBuffer(() -> this.location + " sprite UBOs", 128, spriteUboBuffer)
                : null;
            animationIndex = 0;

            for (TextureAtlasSprite spritex : preparations.regions().values()) {
                sprites.add(spritex);
                if (spritex.isAnimated() && spriteUbos != null) {
                    SpriteContents.AnimationState animationState = spritex.createAnimationState(
                        spriteUbos.slice(animationIndex * uboBlockSize, uboBlockSize), spriteUboSize
                    );
                    animationIndex++;
                    if (animationState != null) {
                        animationStates.add(animationState);
                    }
                }
            }

            this.spriteUbos = spriteUbos;
            this.sprites = sprites;
            this.animatedTexturesStates = List.copyOf(animationStates);
            this.uploadInitialContents();
            if (SharedConstants.DEBUG_DUMP_TEXTURE_ATLAS) {
                Path dumpDir = TextureUtil.getDebugTexturePath();

                try {
                    Files.createDirectories(dumpDir);
                    this.dumpContents(this.location, dumpDir);
                } catch (Exception var13) {
                    LOGGER.warn("Failed to dump atlas contents to {}", dumpDir);
                }
            }

            net.neoforged.neoforge.client.ClientHooks.onTextureAtlasStitched(this);
        }
    }

    private void uploadInitialContents() {
        GpuDevice device = RenderSystem.getDevice();
        int spriteUboSize = Mth.roundToward(SpriteContents.UBO_SIZE, RenderSystem.getDevice().getUniformOffsetAlignment());
        int uboBlockSize = spriteUboSize * this.mipLevelCount;
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
        List<TextureAtlasSprite> staticSprites = this.sprites.stream().filter(s -> !s.isAnimated()).toList();
        List<GpuTextureView[]> scratchTextures = new ArrayList<>();
        ByteBuffer buffer = MemoryUtil.memAlloc(staticSprites.size() * uboBlockSize);

        for (int i = 0; i < staticSprites.size(); i++) {
            TextureAtlasSprite sprite = staticSprites.get(i);
            sprite.uploadSpriteUbo(buffer, i * uboBlockSize, this.maxMipLevel, this.width, this.height, spriteUboSize);
            GpuTexture scratchTexture = device.createTexture(
                () -> sprite.contents().name().toString(), 5, TextureFormat.RGBA8, sprite.contents().width(), sprite.contents().height(), 1, this.mipLevelCount
            );
            GpuTextureView[] views = new GpuTextureView[this.mipLevelCount];

            for (int level = 0; level <= this.maxMipLevel; level++) {
                sprite.uploadFirstFrame(scratchTexture, level);
                views[level] = device.createTextureView(scratchTexture);
            }

            scratchTextures.add(views);
        }

        try (GpuBuffer ubo = device.createBuffer(() -> "SpriteAnimationInfo", 128, buffer)) {
            for (int level = 0; level < this.mipLevelCount; level++) {
                try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Animate " + this.location, this.mipViews[level], OptionalInt.empty())) {
                    renderPass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);

                    for (int i = 0; i < staticSprites.size(); i++) {
                        renderPass.bindTexture("Sprite", scratchTextures.get(i)[level], sampler);
                        renderPass.setUniform("SpriteAnimationInfo", ubo.slice(i * uboBlockSize + level * spriteUboSize, SpriteContents.UBO_SIZE));
                        renderPass.draw(0, 6);
                    }
                }
            }
        }

        for (GpuTextureView[] views : scratchTextures) {
            for (GpuTextureView view : views) {
                view.close();
                view.texture().close();
            }
        }

        MemoryUtil.memFree(buffer);
        this.uploadAnimationFrames();
    }

    @Override
    public void dumpContents(Identifier selfId, Path dir) throws IOException {
        String outputId = selfId.toDebugFileName();
        TextureUtil.writeAsPNG(dir, outputId, this.getTexture(), this.maxMipLevel, argb -> argb);
        dumpSpriteNames(dir, outputId, this.texturesByName);
    }

    private static void dumpSpriteNames(Path dir, String outputId, Map<Identifier, TextureAtlasSprite> regions) {
        Path outputPath = dir.resolve(outputId + ".txt");

        try (Writer output = Files.newBufferedWriter(outputPath)) {
            for (Entry<Identifier, TextureAtlasSprite> e : regions.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
                TextureAtlasSprite value = e.getValue();
                output.write(
                    String.format(
                        Locale.ROOT,
                        "%s\tx=%d\ty=%d\tw=%d\th=%d%n",
                        e.getKey(),
                        value.getX(),
                        value.getY(),
                        value.contents().width(),
                        value.contents().height()
                    )
                );
            }
        } catch (IOException var10) {
            LOGGER.warn("Failed to write file {}", outputPath, var10);
        }
    }

    public void cycleAnimationFrames() {
        if (this.texture != null) {
            for (SpriteContents.AnimationState animationState : this.animatedTexturesStates) {
                animationState.tick();
            }

            this.uploadAnimationFrames();
        }
    }

    private void uploadAnimationFrames() {
        if (this.animatedTexturesStates.stream().anyMatch(SpriteContents.AnimationState::needsToDraw)) {
            for (int level = 0; level <= this.maxMipLevel; level++) {
                try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Animate " + this.location, this.mipViews[level], OptionalInt.empty())) {
                    for (SpriteContents.AnimationState animationState : this.animatedTexturesStates) {
                        if (animationState.needsToDraw()) {
                            animationState.drawToAtlas(renderPass, animationState.getDrawUbo(level));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        this.cycleAnimationFrames();
    }

    public TextureAtlasSprite getSprite(Identifier location) {
        TextureAtlasSprite result = this.texturesByName.getOrDefault(location, this.missingSprite);
        if (result == null) {
            throw new IllegalStateException("Tried to lookup sprite, but atlas is not initialized");
        } else {
            return result;
        }
    }

    public TextureAtlasSprite missingSprite() {
        return Objects.requireNonNull(this.missingSprite, "Atlas not initialized");
    }

    public void clearTextureData() {
        this.sprites.forEach(TextureAtlasSprite::close);
        this.sprites = List.of();
        this.animatedTexturesStates = List.of();
        this.texturesByName = Map.of();
        this.missingSprite = null;
    }

    @Override
    public void close() {
        super.close();

        for (GpuTextureView view : this.mipViews) {
            view.close();
        }

        for (SpriteContents.AnimationState animationState : this.animatedTexturesStates) {
            animationState.close();
        }

        if (this.spriteUbos != null) {
            this.spriteUbos.close();
            this.spriteUbos = null;
        }
    }

    public Identifier location() {
        return this.location;
    }

    public int maxSupportedTextureSize() {
        return this.maxSupportedTextureSize;
    }

    int getWidth() {
        return this.width;
    }

    int getHeight() {
        return this.height;
    }

    public Map<Identifier, TextureAtlasSprite> getTextures() {
        return java.util.Collections.unmodifiableMap(texturesByName);
    }
}
