package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.config.AnvilCraftServerConfig;

public class ConfigScreenLang {
    /**
     * 初始化配置语言
     *
     * @param provider 提供器
     */
    public static void init(RegistrumLangProvider provider) {
        addOverrides(provider);
        ConfigData.readConfigClass(provider, AnvilCraftServerConfig.class);
        ConfigData.readConfigClass(provider, AnvilCraftClientConfig.class);
    }

    @SuppressWarnings("checkstyle:LineLength")
    private static void addOverrides(RegistrumLangProvider provider) {
        addOverride(provider, "anvilcraft.configuration.anvil_collision_craft_speed", "Anvil Collision Explosion Speed Threshold");
        addOverride(
            provider,
            "anvilcraft.configuration.anvil_collision_craft_speed.tooltip",
            "Minimum collision speed at which anvils explode instead of merely stopping (blocks/tick)"
        );
        addOverride(provider, "anvilcraft.configuration.render_bloom_effect", "Render Power Transmission Line Bloom");
        addOverride(provider, "anvilcraft.configuration.ground_heave_particles_enabled", "Show Ground Heave Particles");
        addOverride(provider, "anvilcraft.configuration.ground_heave_particle_chance", "Ground Heave Particle Spawn Chance");
        addOverride(provider, "anvilcraft.configuration.load_monitor", "Load Monitor Cooldown");
        addOverride(
            provider,
            "anvilcraft.configuration.load_monitor.tooltip",
            "Working interval of the Load Monitor in seconds"
        );

        addOverride(provider, "anvilcraft.configuration.gravitational_lens", "Render Gravitational Lensing Effects");
        addOverride(provider, "anvilcraft.configuration.gravitational_lens.button", "Expand Submenu");
        addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.tooltip",
            "Renders gravitational lensing effects for black holes and white holes"
        );
        addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.render_black_hole_lensing",
            "Render Gravitational Lensing Effects"
        );
        addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.render_black_hole_lensing.tooltip",
            "Enables the gravitational lensing post-processing effect near black holes and white holes"
        );
        addOverride(provider, "anvilcraft.configuration.gravitational_lens.max_hole_count", "Maximum Render Count");
        addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.max_hole_count.tooltip",
            "Maximum number of black hole and white hole lensing effects to render (2-256). Higher values render more effects; lower values improve performance."
        );
        addOverride(provider, "anvilcraft.configuration.gravitational_lens.lens_strength", "Gravitational Lensing Strength");
        addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.lens_strength.tooltip",
            "Strength of the lensing distortion around black holes and white holes (higher values bend light more strongly; default: 0.002)"
        );
        addOverride(provider, "anvilcraft.configuration.gravitational_lens.event_horizon_radius", "Event Horizon Radius");
        addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.event_horizon_radius.tooltip",
            "Core event horizon radius of black holes and white holes in screen UV units (default: 0.083)"
        );
        addOverride(provider, "anvilcraft.configuration.gravitational_lens.lens_perspective_scale", "Gravitational Lensing Perspective Scale");
        addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.lens_perspective_scale.tooltip",
            "Reference distance for perspective scaling. At this distance the effect matches the configured size; it appears larger at shorter distances."
        );
        addOverride(provider, "anvilcraft.configuration.gravitational_lens.lens_direction", "Gravitational Lensing Direction");
        addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.lens_direction.tooltip",
            "Positive values create a convex lens that pulls toward the center; negative values create a concave lens that pushes outward. The absolute value determines the curvature."
        );

        addOverride(
            provider,
            "anvilcraft.configuration.ionocraft_backpack_exhaust_particles_enabled",
            "Show Ionocraft Backpack Exhaust Particles"
        );
        addOverride(provider, "anvilcraft.configuration.ionocraft_backpack_hud", "Ionocraft Backpack HUD");
        addOverride(provider, "anvilcraft.configuration.ionocraft_backpack_hud.button", "Expand Submenu");
        addOverride(
            provider,
            "anvilcraft.configuration.ionocraft_backpack_hud.tooltip",
            "HUD shown while the Ionocraft Backpack is equipped, including its remaining power"
        );
        addOverride(provider, "anvilcraft.configuration.ionocraft_backpack_hud.enabled", "Enable Ionocraft Backpack HUD");
        addOverride(
            provider,
            "anvilcraft.configuration.ionocraft_backpack_hud.enabled.tooltip",
            "Shows the Ionocraft Backpack's current power on the HUD when enabled"
        );
        addOverride(provider, "anvilcraft.configuration.ionocraft_backpack_hud.hud_scale", "HUD Scale");
        addOverride(provider, "anvilcraft.configuration.ionocraft_backpack_hud.hud_scale.tooltip", "HUD scale multiplier");
        addOverride(provider, "anvilcraft.configuration.ionocraft_backpack_hud.hud_x", "HUD X Coordinate");
        addOverride(
            provider,
            "anvilcraft.configuration.ionocraft_backpack_hud.hud_x.tooltip",
            "HUD X coordinate (0 at the top-left of the game window; the window width at the bottom-right)"
        );
        addOverride(provider, "anvilcraft.configuration.ionocraft_backpack_hud.hud_y", "HUD Y Coordinate");
        addOverride(
            provider,
            "anvilcraft.configuration.ionocraft_backpack_hud.hud_y.tooltip",
            "HUD Y coordinate (0 at the top-left of the game window; the window height at the bottom-right)"
        );
        addOverride(provider, "anvilcraft.configuration.ionocraft_backpack_hud.capacitor_count_enabled", "Show Capacitor Count");
        addOverride(
            provider,
            "anvilcraft.configuration.ionocraft_backpack_hud.capacitor_count_enabled.tooltip",
            "Shows the current number of Capacitors and Super Capacitors in the inventory on the HUD when enabled"
        );
        addOverride(provider, "anvilcraft.configuration.anvil_hammer_radial_menu_scale", "Anvil Hammer Radial Menu Scale");
        addOverride(
            provider,
            "anvilcraft.configuration.anvil_hammer_radial_menu_scale.tooltip",
            "Adjusts the scale of the Anvil Hammer radial menu"
        );
        addOverride(provider, "anvilcraft.configuration.laser_ore_cluster_max_size", "Laser Gun Mining Chain Limit");
        addOverride(
            provider,
            "anvilcraft.configuration.laser_ore_cluster_max_size.tooltip",
            "Maximum ore vein size searched while mining with a laser gun; ore beyond this limit is not chain-mined (default: 64)"
        );
        addOverride(
            provider,
            "anvilcraft.configuration.use_legacy_creative_tab",
            "Use Legacy Creative Inventory"
        );
        addOverride(
            provider,
            "anvilcraft.configuration.use_legacy_creative_tab.tooltip",
            "Uses the flat legacy creative inventory layout instead of the sectioned layout with banners (requires restart)"
        );
        addOverride(
            provider,
            "anvilcraft.configuration.creative_variant_picker_enabled",
            "Fold Colored Item Variants"
        );
        addOverride(
            provider,
            "anvilcraft.configuration.creative_variant_picker_enabled.tooltip",
            "Folds 16-color families into a single representative item and enables the right-click variant picker; applies to both creative inventory layouts (requires restart)"
        );
    }

    private static void addOverride(RegistrumLangProvider provider, String key, String value) {
        provider.add(key, value);
        // Prevent the reflected config reader from generating the same key again.
        ConfigData.ADDED.add(key);
    }
}
