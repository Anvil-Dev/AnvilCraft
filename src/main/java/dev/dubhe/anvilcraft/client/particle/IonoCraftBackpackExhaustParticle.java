package dev.dubhe.anvilcraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * 飘升机背包引擎白烟粒子 — 模拟喷气背包排气烟雾效果。
 */
public class IonoCraftBackpackExhaustParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    protected IonoCraftBackpackExhaustParticle(
        ClientLevel level, double x, double y, double z,
        double speedX, double speedY, double speedZ, SpriteSet sprites
    ) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.gravity = -0.02F;       // 轻微上升
        this.friction = 0.95F;       // 少量空气阻力
        this.xd = speedX + (Math.random() * 2.0 - 1.0) * 0.05;
        this.yd = speedY + (Math.random() * 2.0 - 1.0) * 0.05;
        this.zd = speedZ + (Math.random() * 2.0 - 1.0) * 0.05;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.quadSize = 0.15F * (this.random.nextFloat() * 0.5F + 0.5F);
        this.lifetime = (int) (20.0 / ((double) this.random.nextFloat() * 0.5 + 0.5));
        this.setSpriteFromAge(sprites);
        this.alpha = 0.6F;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        // 渐隐
        float progress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.6F * (1.0F - progress);
        // 粒子逐渐变大
        this.quadSize += 0.002F;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
            SimpleParticleType type, ClientLevel level,
            double x, double y, double z,
            double speedX, double speedY, double speedZ,
            RandomSource random
        ) {
            return new IonoCraftBackpackExhaustParticle(
                level, x, y, z, speedX, speedY, speedZ, this.sprites
            );
        }
    }
}
