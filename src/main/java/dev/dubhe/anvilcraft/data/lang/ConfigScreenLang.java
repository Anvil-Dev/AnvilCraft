package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.config.AnvilCraftServerConfig;

public class ConfigScreenLang {
    /// 初始化配置语言
    ///
    /// @param provider 提供器
    public static void init(RegistrumLangProvider provider) {
        ConfigScreenLang.addOverrides(provider);
        ConfigData.readConfigClass(provider, AnvilCraftServerConfig.class);
        ConfigData.readConfigClass(provider, AnvilCraftClientConfig.class);
    }

    @SuppressWarnings("checkstyle:LineLength")
    private static void addOverrides(RegistrumLangProvider provider) {
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.anvil_collision_craft_speed", "Anvil Collision Explosion Speed Threshold");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.anvil_collision_craft_speed.tooltip",
            "Minimum collision speed at which anvils explode instead of merely stopping (blocks/tick)"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.render_bloom_effect", "Render Power Transmission Line Bloom");
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.ground_heave_particles_enabled", "Show Ground Heave Particles");
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.ground_heave_particle_chance", "Ground Heave Particle Spawn Chance");
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.load_monitor", "Load Monitor Cooldown");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.load_monitor.tooltip",
            "Working interval of the Load Monitor in seconds"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.gravitational_lens", "Render Gravitational Lensing Effects");
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.gravitational_lens.button", "Expand Submenu");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.tooltip",
            "Renders gravitational lensing effects for black holes and white holes"
        );
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.render_black_hole_lensing",
            "Render Gravitational Lensing Effects"
        );
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.render_black_hole_lensing.tooltip",
            "Enables the gravitational lensing post-processing effect near black holes and white holes"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.gravitational_lens.max_hole_count", "Maximum Render Count");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.max_hole_count.tooltip",
            "Maximum number of black hole and white hole lensing effects to render (2-256). Higher values render more effects; lower values improve performance."
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.gravitational_lens.lens_strength", "Gravitational Lensing Strength");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.lens_strength.tooltip",
            "Strength of the lensing distortion around black holes and white holes (higher values bend light more strongly; default: 0.002)"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.gravitational_lens.event_horizon_radius", "Event Horizon Radius");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.event_horizon_radius.tooltip",
            "Core event horizon radius of black holes and white holes in screen UV units (default: 0.083)"
        );
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.lens_perspective_scale",
            "Gravitational Lensing Perspective Scale"
        );
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.lens_perspective_scale.tooltip",
            "Reference distance for perspective scaling. At this distance the effect matches the configured size; it appears larger at shorter distances."
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.gravitational_lens.lens_direction", "Gravitational Lensing Direction");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.gravitational_lens.lens_direction.tooltip",
            "Positive values create a convex lens that pulls toward the center; negative values create a concave lens that pushes outward. The absolute value determines the curvature."
        );
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.iono_craft_backpack_exhaust_particles_enabled",
            "Show Ionocraft Backpack Exhaust Particles"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.iono_craft_backpack_hud", "Ionocraft Backpack HUD");
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.iono_craft_backpack_hud.button", "Expand Submenu");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.iono_craft_backpack_hud.tooltip",
            "HUD shown while the Ionocraft Backpack is equipped, including its remaining power"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.iono_craft_backpack_hud.enabled", "Enable Ionocraft Backpack HUD");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.iono_craft_backpack_hud.enabled.tooltip",
            "Shows the Ionocraft Backpack's current power on the HUD when enabled"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.iono_craft_backpack_hud.hud_scale", "HUD Scale");
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.iono_craft_backpack_hud.hud_scale.tooltip", "HUD scale multiplier");
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.iono_craft_backpack_hud.hud_x", "HUD X Coordinate");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.iono_craft_backpack_hud.hud_x.tooltip",
            "HUD X coordinate (0 at the top-left of the game window; the window width at the bottom-right)"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.iono_craft_backpack_hud.hud_y", "HUD Y Coordinate");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.iono_craft_backpack_hud.hud_y.tooltip",
            "HUD Y coordinate (0 at the top-left of the game window; the window height at the bottom-right)"
        );
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.iono_craft_backpack_hud.capacitor_count_enabled",
            "Show Capacitor Count"
        );
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.iono_craft_backpack_hud.capacitor_count_enabled.tooltip",
            "Shows the current number of Capacitors and Super Capacitors in the inventory on the HUD when enabled"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.anvil_hammer_radial_menu_scale", "Anvil Hammer Radial Menu Scale");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.anvil_hammer_radial_menu_scale.tooltip",
            "Adjusts the scale of the Anvil Hammer radial menu"
        );
        ConfigScreenLang.addOverride(provider, "anvilcraft.configuration.laser_ore_cluster_max_size", "Laser Gun Mining Chain Limit");
        ConfigScreenLang.addOverride(
            provider,
            "anvilcraft.configuration.laser_ore_cluster_max_size.tooltip",
            "Maximum ore vein size searched while mining with a laser gun; ore beyond this limit is not chain-mined (default: 64)"
        );
    }

    private static void addOverride(RegistrumLangProvider provider, String key, String value) {
        provider.add(key, value);
        ConfigData.ADDED.add(key);
    }
}
