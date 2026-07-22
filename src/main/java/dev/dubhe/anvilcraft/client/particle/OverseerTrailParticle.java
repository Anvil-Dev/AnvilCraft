package dev.dubhe.anvilcraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class OverseerTrailParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    protected OverseerTrailParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double speedX,
        double speedY,
        double speedZ,
        SpriteSet sprites
    ) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.xd = speedX;
        this.yd = speedY;
        this.zd = speedZ;
        this.friction = 0.82F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.rCol = 0.55F + this.random.nextFloat() * 0.2F;
        this.gCol = 0.08F + this.random.nextFloat() * 0.1F;
        this.bCol = 0.9F;
        this.alpha = 0.75F;
        this.quadSize = 0.045F + this.random.nextFloat() * 0.035F;
        this.lifetime = 8 + this.random.nextInt(5);
        this.setSpriteFromAge(sprites);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        float progress = (float) this.age / this.lifetime;
        this.alpha = 0.75F * (1.0F - progress);
        this.quadSize *= 0.96F;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
            SimpleParticleType type,
            ClientLevel level,
            double x,
            double y,
            double z,
            double speedX,
            double speedY,
            double speedZ,
            RandomSource random
        ) {
            return new OverseerTrailParticle(level, x, y, z, speedX, speedY, speedZ, this.sprites);
        }
    }
}
