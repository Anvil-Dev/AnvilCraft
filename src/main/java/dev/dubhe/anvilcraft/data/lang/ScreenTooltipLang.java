package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

/**
 * 物品栏上方的提示的翻译键
 */
public class ScreenTooltipLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("screen.anvilcraft.tooltip.cfa_interface", "It must be placed tightly against the side of the Celestial Forging Anvil bottom");
        provider.add("screen.anvilcraft.tooltip.cfa_amplifier", "It must be placed diagonally on the Celestial Forging Anvil");

        provider.add("screen.anvilcraft.tooltip.trading_station.break_failed", "Please do not break someone else's Trading Station! Hold Shift to forcibly break it");
        provider.add("screen.anvilcraft.tooltip.crate.break_requires_shift", "The crate contains too many items! Hold Shift to break it");
        provider.add("screen.anvilcraft.tooltip.crate.hammer_break_denied", "The crate contains too many items! The Anvil Hammer cannot remove it");
        provider.add("screen.anvilcraft.tooltip.fluid_tank.break_confirm", "This tank contains infinite fluid. Mine it again while holding Ctrl+Shift+Alt to confirm removal");
        provider.add("screen.anvilcraft.tooltip.fluid_tank.break_modifiers", "Hold Ctrl+Shift+Alt while mining to remove this infinite-fluid tank");
        provider.add("screen.anvilcraft.tooltip.fluid_tank.tool_break_failed", "Infinite-fluid tanks cannot be removed with an Anvil Hammer or Dragon Rod");

        provider.add("screen.anvilcraft.range_no_overlap", "The working ranges of two identical blocks must not overlap");
        provider.add("screen.anvilcraft.range_overlap", "No other identical block is allowed within the %1$s×%1$s range.");

        provider.add("tooltip.anvilcraft.large_crate.0", "Inject 1x Space Overcompressor and");
        provider.add("tooltip.anvilcraft.large_crate.1", "6x Netherite Blocks into the top of");
        provider.add("tooltip.anvilcraft.large_crate.2", "a Large Crate to upgrade it to Shulker Container");
        provider.add("tooltip.anvilcraft.large_crate.3", "This process is irreversible");
        provider.add("tooltip.anvilcraft.shulker_container.0", "Inject more Space Overcompressors into");
        provider.add("tooltip.anvilcraft.shulker_container.1", "Shulker Container to increase its capacity");
        provider.add("tooltip.anvilcraft.shulker_container.2", "This process is irreversible");
        provider.add("tooltip.anvilcraft.shulker_container.3", "Each injected Space Overcompressor doubles");
        provider.add("tooltip.anvilcraft.shulker_container.4", "the type limit and the space per type, up to");
        provider.add("tooltip.anvilcraft.shulker_container.5", "16384 types x 16384 stacks per type (4 upgrades)");
        provider.add("tooltip.anvilcraft.shulker_container.6", "Currently injected times: %s");
        provider.add("tooltip.anvilcraft.shulker_container.6.waiting", "Waiting for syncing");
        provider.add("tooltip.anvilcraft.shulker_container.hyperdimension.0", "Inject 1x Singularity Crystal and");
        provider.add("tooltip.anvilcraft.shulker_container.hyperdimension.1", "16x Hypercube into the top of a Shulker Container");
        provider.add("tooltip.anvilcraft.shulker_container.hyperdimension.2", "to upgrade it into a Hyperdimension Storage Station");
        provider.add("tooltip.anvilcraft.shulker_container.hyperdimension.3", "This process is irreversible");
    }
}
