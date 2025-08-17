package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.AscendingBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.IonocraftRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.MagnetizedNodeEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.SlidingBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.SpectralBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.ThrownHeavyHalberdRenderer;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.entity.FallingSpectralBlockEntity;
import dev.dubhe.anvilcraft.entity.FloatingBlockEntity;
import dev.dubhe.anvilcraft.entity.IonocraftEntity;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableFallingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableLevitatingBlockEntity;
import dev.dubhe.anvilcraft.entity.ThrownEmberMetalHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownFrostMetalHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownTranscendenceHeavyHalberdEntity;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityEntry<? extends AnimateAscendingBlockEntity> ASCENDING_BLOCK_ENTITY = AnvilCraft.REGISTRATE
        .entity("animate_ascending_block", AnimateAscendingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> AscendingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends FallingGiantAnvilEntity> FALLING_GIANT_ANVIL = AnvilCraft.REGISTRATE
        .entity("falling_giant_anvil", FallingGiantAnvilEntity::new, MobCategory.MISC)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends FallingSpectralBlockEntity> FALLING_SPECTRAL_BLOCK = AnvilCraft.REGISTRATE
        .entity("falling_spectral_block", FallingSpectralBlockEntity::new, MobCategory.MISC)
        .properties(builder -> builder.sized(0.98f, 0.98f))
        .renderer(() -> SpectralBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends FloatingBlockEntity> FLOATING_BLOCK = AnvilCraft.REGISTRATE
        .entity("floating_block", FloatingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends LevitatingBlockEntity> LEVITATING_BLOCK = AnvilCraft.REGISTRATE
        .entity("levitating_block", LevitatingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends StandableFallingBlockEntity> STANDABLE_FALLING_BLOCK = AnvilCraft.REGISTRATE
        .entity("standable_falling_block", StandableFallingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends StandableLevitatingBlockEntity> STANDABLE_LEVITATING_BLOCK = AnvilCraft.REGISTRATE
        .entity("standable_levitating_block", StandableLevitatingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends IonocraftEntity> IONOCRAFT = AnvilCraft.REGISTRATE
        .<IonocraftEntity>entity("ionocraft", IonocraftEntity::new, MobCategory.MISC)
        .properties(it -> it.sized(0.75f, 0.75f)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
        ).renderer(() -> IonocraftRenderer::new)
        .register();

    public static final EntityEntry<? extends ThrownFrostMetalHeavyHalberdEntity> THROWN_FROST_METAL_HEAVY_HALBERD = AnvilCraft.REGISTRATE
        .<ThrownFrostMetalHeavyHalberdEntity>entity("thrown_frost_metal_heavy_halberd", ThrownFrostMetalHeavyHalberdEntity::new, MobCategory.MISC)
        .properties(it -> it.sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20))
        .renderer(() -> ThrownHeavyHalberdRenderer::new)
        .register();

    public static final EntityEntry<? extends ThrownEmberMetalHeavyHalberdEntity> THROWN_EMBER_METAL_HEAVY_HALBERD = AnvilCraft.REGISTRATE
        .<ThrownEmberMetalHeavyHalberdEntity>entity("thrown_ember_metal_heavy_halberd", ThrownEmberMetalHeavyHalberdEntity::new, MobCategory.MISC)
        .properties(it -> it.sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20))
        .renderer(() -> ThrownHeavyHalberdRenderer::new)
        .register();

    public static final EntityEntry<? extends ThrownTranscendenceHeavyHalberdEntity> THROWN_TRANSCENDENCE_HEAVY_HALBERD = AnvilCraft.REGISTRATE
        .<ThrownTranscendenceHeavyHalberdEntity>entity(
            "thrown_transcendence_heavy_halberd", ThrownTranscendenceHeavyHalberdEntity::new, MobCategory.MISC)
        .properties(it -> it.sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20))
        .renderer(() -> ThrownHeavyHalberdRenderer::new)
        .register();

    public static final EntityEntry<? extends SlidingBlockEntity> SLIDING_BLOCK = AnvilCraft.REGISTRATE
        .<SlidingBlockEntity>entity("sliding_block", SlidingBlockEntity::new, MobCategory.MISC)
        .renderer(() -> SlidingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends MagnetizedNodeEntity> MAGNETIZED_NODE = AnvilCraft.REGISTRATE
        .<MagnetizedNodeEntity>entity("magnetized_node", MagnetizedNodeEntity::new, MobCategory.MISC)
        .properties(it -> it.eyeHeight(0f))
        .renderer(() -> MagnetizedNodeEntityRenderer::new)
        .register();

    public static void register() {
        // intentionally empty
    }
}
