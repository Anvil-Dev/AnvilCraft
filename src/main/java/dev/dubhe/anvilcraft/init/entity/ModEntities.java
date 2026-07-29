package dev.dubhe.anvilcraft.init.entity;

import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.AscendingBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.CauldronOutletRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.FluidTankMinecartRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.IonocraftRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.MagnetizedNodeEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.RailgunAnvilRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.SlidingBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.SpectralBlockRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.SpectralProjectileRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.ThrownHeavyHalberdRenderer;
import dev.dubhe.anvilcraft.client.renderer.entity.WeaponBeamRenderer;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.entity.FallingSpectralBlockEntity;
import dev.dubhe.anvilcraft.entity.FloatingBlockEntity;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.entity.IonocraftEntity;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.entity.RailgunAnvilEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.entity.SpectralProjectileEntity;
import dev.dubhe.anvilcraft.entity.StandableFallingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableLevitatingBlockEntity;
import dev.dubhe.anvilcraft.entity.ThrownEmberMetalHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownFrostMetalHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownTranscendenceHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.WeaponBeamEntity;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityEntry<? extends RailgunAnvilEntity> RAILGUN_ANVIL = AnvilCraft.REGISTRUM
        .<RailgunAnvilEntity>entity("railgun_anvil", RailgunAnvilEntity::new, MobCategory.MISC)
        .properties(builder -> builder.sized(0.98F, 0.98F).clientTrackingRange(80).updateInterval(1).noLootTable())
        .renderer(() -> RailgunAnvilRenderer::new)
        .register();

    public static final EntityEntry<? extends WeaponBeamEntity> WEAPON_BEAM = AnvilCraft.REGISTRUM
        .<WeaponBeamEntity>entity("weapon_beam", WeaponBeamEntity::new, MobCategory.MISC)
        .properties(builder -> builder.sized(0.01F, 0.01F).clientTrackingRange(80).updateInterval(1).noLootTable())
        .renderer(() -> WeaponBeamRenderer::new)
        .register();

    public static final EntityEntry<? extends AnimateAscendingBlockEntity> ASCENDING_BLOCK_ENTITY = AnvilCraft.REGISTRUM
        .entity("animate_ascending_block", AnimateAscendingBlockEntity::new, MobCategory.MISC)
        .properties(EntityType.Builder::noLootTable)
        .renderer(() -> AscendingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends FallingGiantAnvilEntity> FALLING_GIANT_ANVIL = AnvilCraft.REGISTRUM
        .entity("falling_giant_anvil", FallingGiantAnvilEntity::new, MobCategory.MISC)
        .properties(EntityType.Builder::noLootTable)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends FallingSpectralBlockEntity> FALLING_SPECTRAL_BLOCK = AnvilCraft.REGISTRUM
        .entity("falling_spectral_block", FallingSpectralBlockEntity::new, MobCategory.MISC)
        .properties(builder -> builder.sized(0.98F, 0.98F).noLootTable())
        .renderer(() -> SpectralBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends FloatingBlockEntity> FLOATING_BLOCK = AnvilCraft.REGISTRUM
        .entity("floating_block", FloatingBlockEntity::new, MobCategory.MISC)
        .properties(EntityType.Builder::noLootTable)
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends LevitatingBlockEntity> LEVITATING_BLOCK = AnvilCraft.REGISTRUM
        .entity("levitating_block", LevitatingBlockEntity::new, MobCategory.MISC)
        .properties(builder -> builder.sized(0.98F, 0.98F).noLootTable())
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends StandableFallingBlockEntity> STANDABLE_FALLING_BLOCK = AnvilCraft.REGISTRUM
        .entity("standable_falling_block", StandableFallingBlockEntity::new, MobCategory.MISC)
        .properties(builder -> builder.sized(0.98F, 0.98F).noLootTable())
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends StandableLevitatingBlockEntity> STANDABLE_LEVITATING_BLOCK = AnvilCraft.REGISTRUM
        .entity("standable_levitating_block", StandableLevitatingBlockEntity::new, MobCategory.MISC)
        .properties(builder -> builder.sized(0.98F, 0.98F).noLootTable())
        .renderer(() -> FallingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends IonocraftEntity> IONOCRAFT = AnvilCraft.REGISTRUM
        .<IonocraftEntity>entity("ionocraft", IonocraftEntity::new, MobCategory.MISC)
        .properties(it -> it.sized(0.75F, 0.75F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
            .noLootTable()
        )
        .renderer(() -> IonocraftRenderer::new)
        .register();

    public static final EntityEntry<? extends ThrownFrostMetalHeavyHalberdEntity> THROWN_FROST_METAL_HEAVY_HALBERD = AnvilCraft.REGISTRUM
        .entity(
            "thrown_frost_metal_heavy_halberd",
            (ThrownHeavyHalberdEntity.Factory<ThrownFrostMetalHeavyHalberdEntity>) ThrownFrostMetalHeavyHalberdEntity::new,
            MobCategory.MISC
        )
        .properties(it -> it.sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20)
            .noLootTable()
        )
        .renderer(() -> ThrownHeavyHalberdRenderer::new)
        .register();

    public static final EntityEntry<? extends ThrownEmberMetalHeavyHalberdEntity> THROWN_EMBER_METAL_HEAVY_HALBERD = AnvilCraft.REGISTRUM
        .entity(
            "thrown_ember_metal_heavy_halberd",
            (ThrownHeavyHalberdEntity.Factory<ThrownEmberMetalHeavyHalberdEntity>) ThrownEmberMetalHeavyHalberdEntity::new,
            MobCategory.MISC
        )
        .properties(it -> it.sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20)
            .noLootTable()
        )
        .renderer(() -> ThrownHeavyHalberdRenderer::new)
        .register();

    public static final EntityEntry<? extends ThrownTranscendenceHeavyHalberdEntity> THROWN_TRANSCENDENCE_HEAVY_HALBERD =
        AnvilCraft.REGISTRUM
            .entity(
                "thrown_transcendence_heavy_halberd",
                (ThrownHeavyHalberdEntity.Factory<ThrownTranscendenceHeavyHalberdEntity>) ThrownTranscendenceHeavyHalberdEntity::new,
                MobCategory.MISC
            )
            .properties(it -> it.sized(0.5F, 0.5F)
                .eyeHeight(0.13F)
                .clientTrackingRange(4)
                .updateInterval(20)
                .noLootTable()
            )
            .renderer(() -> ThrownHeavyHalberdRenderer::new)
            .register();

    public static final EntityEntry<? extends SlidingBlockEntity> SLIDING_BLOCK = AnvilCraft.REGISTRUM
        .<SlidingBlockEntity>entity("sliding_block", SlidingBlockEntity::new, MobCategory.MISC)
        .properties(EntityType.Builder::noLootTable)
        .renderer(() -> SlidingBlockRenderer::new)
        .register();

    public static final EntityEntry<? extends MagnetizedNodeEntity> MAGNETIZED_NODE = AnvilCraft.REGISTRUM
        .<MagnetizedNodeEntity>entity("magnetized_node", MagnetizedNodeEntity::new, MobCategory.MISC)
        .properties(it -> it.eyeHeight(0F).noLootTable())
        .renderer(() -> MagnetizedNodeEntityRenderer::new)
        .register();

    public static final EntityEntry<? extends CauldronOutletEntity> CAULDRON_OUTLET = AnvilCraft.REGISTRUM
        .<CauldronOutletEntity>entity("cauldron_outlet", CauldronOutletEntity::new, MobCategory.MISC)
        .properties(it -> it.eyeHeight(0F).noLootTable())
        .renderer(() -> CauldronOutletRenderer::new)
        .register();

    public static final EntityEntry<? extends SpectralProjectileEntity> SPECTRAL_PROJECTILE = AnvilCraft.REGISTRUM
        .<SpectralProjectileEntity>entity("spectral_projectile", SpectralProjectileEntity::new, MobCategory.MISC)
        .properties(it -> it.sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(4)
            .updateInterval(20)
            .noLootTable()
        )
        .renderer(() -> SpectralProjectileRenderer::new)
        .tag(EntityTypeTags.ARROWS)
        .register();

    public static final EntityEntry<? extends FluidTankMinecartEntity> FLUID_TANK_MINECART = AnvilCraft.REGISTRUM
        .<FluidTankMinecartEntity>entity("fluid_tank_minecart", FluidTankMinecartEntity::new, MobCategory.MISC)
        .properties(it -> it
            .sized(1.0F, 0.7F)
            .passengerAttachments(0.1875F)
            .clientTrackingRange(8)
            .updateInterval(3)
            .noLootTable())
        .renderer(() -> FluidTankMinecartRenderer::new)
        .register();

    public static void register() {
        // intentionally empty
    }
}
