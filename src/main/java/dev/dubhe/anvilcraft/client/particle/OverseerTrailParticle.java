package dev.dubhe.anvilcraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class OverseerTrailParticle extends TextureSheetParticle {
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
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = speedX;
        this.yd = speedY;
        this.zd = speedZ;
        this.friction = 0.82f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.rCol = 0.55f + this.random.nextFloat() * 0.2f;
        this.gCol = 0.08f + this.random.nextFloat() * 0.1f;
        this.bCol = 0.9f;
        this.alpha = 0.75f;
        this.quadSize = 0.045f + this.random.nextFloat() * 0.035f;
        this.lifetime = 8 + this.random.nextInt(5);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        float progress = (float) this.age / this.lifetime;
        this.alpha = 0.75f * (1.0f - progress);
        this.quadSize *= 0.96f;
    }

    @OnlyIn(Dist.CLIENT)
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
            double speedZ
        ) {
            return new OverseerTrailParticle(level, x, y, z, speedX, speedY, speedZ, this.sprites);
        }
    }
}
