package com.etema.ragnarmmo.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProvokeEmoteParticle extends TextureSheetParticle {

    protected ProvokeEmoteParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet) {
        super(level, x, y, z);
        this.setSpriteFromAge(spriteSet);
        this.lifetime = 40; // 2 seconds
        this.gravity = 0.0F; // No gravity
        this.quadSize = 0.5F; // Size of the emote
        this.hasPhysics = false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // Just float slightly upwards or stay still
        this.yo = this.y;
        this.y += 0.01;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ProvokeEmoteParticle particle = new ProvokeEmoteParticle(level, x, y, z, spriteSet);
            particle.pickSprite(spriteSet);
            return particle;
        }
    }
}
