package dev.dubhe.anvilcraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

public class PlasmaJetsParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    protected PlasmaJetsParticle(
        ClientLevel level,
        double x,
        double y,
        double z,
        double auxX,
        double auxY,
        double auxZ,
        SpriteSet sprites
    ) {
        super(level, x, y, z, sprites.first());
        this.gravity = 0.2F;
        this.friction = 0.9F;
        this.sprites = sprites;
        this.xd = auxX + (Math.random() * 2.0 - 1.0) * 0.05F;
        this.yd = auxY + (Math.random() * 2.0 - 1.0) * 0.05F;
        this.zd = auxZ + (Math.random() * 2.0 - 1.0) * 0.05F;
        this.rCol = 1;
        this.gCol = 1;
        this.bCol = 1;
        this.quadSize = 0.1F * (this.random.nextFloat() * this.random.nextFloat() * 2.0F + 1.0F);
        this.lifetime = (int) (16.0 / ((double) this.random.nextFloat() * 0.8 + 0.2)) + 2;
        this.setSpriteFromAge(sprites);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setAlpha(1);
        this.setSpriteFromAge(this.sprites);
        this.setColorFromAge(this.age, this.lifetime);
    }

    protected void setColorFromAge(int age, int maxAge) {
        float oneThird = maxAge / 3.0F;
        float r = 1;
        float g = 1;
        float b = 1;
        if (age < oneThird) {
            b -= age / oneThird;
        } else if (age < oneThird * 2) {
            g -= (age - oneThird) / oneThird;
            b = 0;
        } else if (age <= maxAge) {
            r -= (age - oneThird * 2) / oneThird;
            g = 0;
            b = 0;
        }
        this.setColor(r, g, b);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
            SimpleParticleType options,
            ClientLevel level,
            double x,
            double y,
            double z,
            double auxX,
            double auxY,
            double auxZ,
            RandomSource random
        ) {
            return new PlasmaJetsParticle(level, x, y, z, auxX, auxY, auxZ, this.sprites);
        }
    }
}
