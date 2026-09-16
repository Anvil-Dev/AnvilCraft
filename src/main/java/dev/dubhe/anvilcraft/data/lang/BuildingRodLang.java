package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public final class BuildingRodLang {
    private BuildingRodLang() {
    }

    public static void init(RegistrumLangProvider provider) {
        provider.add("message.anvilcraft.building_rod.component_mismatch",
            "Some blocks have mismatched components; right-click again within 3 seconds to confirm placement");
        provider.add("message.anvilcraft.building_rod.missing_book", "Missing materials; carry a book to receive a material list");
        provider.add("message.anvilcraft.building_rod.nothing_to_undo", "No unchanged placement to undo");
        provider.add("message.anvilcraft.building_rod.undone", "Last placement undone");
        provider.add("item.anvilcraft.building_rod.fluids",
            "Buckets can fill areas and waterlog blocks; renewable fluids cost 2 B per fill, supplied by inventory or linked storage");
        provider.add("screen.anvilcraft.building_rod.traditional.hint",
            "Ctrl+scroll switches tools  Alt+scroll adjusts  Right click executes  Scroll changes hotbar");
        provider.add("screen.anvilcraft.building_rod.traditional.status",
            "%1$s | %2$s | Rotation %3$s | Mirror %4$s | Layer %5$s | %6$s");
        provider.add("screen.anvilcraft.building_rod.traditional.layer_all", "All");
        provider.add("screen.anvilcraft.building_rod.traditional.anchor_locked", "Anchor locked");
        provider.add("screen.anvilcraft.building_rod.traditional.anchor_following", "Following crosshair");
        provider.add("screen.anvilcraft.building_rod.tool.cancel", "Cancel placement");
        provider.add("screen.anvilcraft.building_rod.distance",
            "Hold [Ctrl] to keep the projection distance; use Up/Down to adjust | %s Lock blueprint");
        provider.add("item.anvilcraft.building_rod.summary", "Place blocks and blueprints in bulk.");
        provider.add("message.anvilcraft.building_rod.placed", "Blueprint placed");
        provider.add("screen.anvilcraft.building_rod.left_click", "Left Click");
        provider.add(
            "screen.anvilcraft.building_rod.suspended",
            "Projection locked | Hold the building rod and %s to unlock"
        );
        provider.add(
            "item.anvilcraft.building_rod.desc",
            "Grants crab claw reach while carried, including in pockets; hold in either hand to build with +15 block reach"
        );
        provider.add(
            "item.anvilcraft.building_rod.energy",
            "%s / 8,000,000 FE · 100 FE/block"
        );
        provider.add(
            "item.anvilcraft.building_rod.controls",
            "Hold use and drag to fill a box (up to 4,000 blocks); hold Shift to build the available part of a blueprint"
        );
        provider.add(
            "item.anvilcraft.building_rod.import",
            "Import blueprint files through the Structure Scanner (16×16×16 maximum)"
        );
        provider.add(
            "message.anvilcraft.building_rod.no_energy",
            "Not enough power; carry a charged capacitor"
        );
        provider.add(
            "message.anvilcraft.building_rod.too_many",
            "Cannot place more than 4,000 blocks at once"
        );
        provider.add(
            "message.anvilcraft.building_rod.blocked",
            "Placement area is obstructed or protected"
        );
        provider.add(
            "message.anvilcraft.building_rod.missing_blocks",
            "Not enough materials or fluid"
        );
        provider.add(
            "message.anvilcraft.building_rod.unsupported",
            "This blueprint contains materials that cannot be supplied"
        );
        provider.add(
            "message.anvilcraft.building_rod.invalid_structure",
            "Invalid blueprint or larger than 16×16×16"
        );
        provider.add(
            "screen.anvilcraft.building_rod.optimized",
            "%s Place | %s Cancel | %s %s %s %s %s %s Move | %s %s Rotate | %s Mirror"
        );
        provider.add(
            "screen.anvilcraft.building_rod.traditional",
            "%s + Scroll: select tool | %s + Scroll: adjust | %s + Scroll: layers | Use: execute · %s | %s Cancel"
        );
        provider.add(
            "screen.anvilcraft.building_rod.tool.move",
            "Move anchor"
        );
        provider.add(
            "screen.anvilcraft.building_rod.tool.rotate",
            "Rotate"
        );
        provider.add(
            "screen.anvilcraft.building_rod.tool.flip",
            "Mirror"
        );
        provider.add(
            "screen.anvilcraft.building_rod.tool.layer_down",
            "Layer down"
        );
        provider.add(
            "screen.anvilcraft.building_rod.tool.layer_up",
            "Layer up"
        );
        provider.add(
            "screen.anvilcraft.building_rod.tool.confirm",
            "Confirm placement"
        );
        provider.add(
            "key.anvilcraft.building_rod_forward",
            "Building Rod: Move forward"
        );
        provider.add(
            "key.anvilcraft.building_rod_back",
            "Building Rod: Move backward"
        );
        provider.add(
            "key.anvilcraft.building_rod_left",
            "Building Rod: Move left"
        );
        provider.add(
            "key.anvilcraft.building_rod_right",
            "Building Rod: Move right"
        );
        provider.add(
            "key.anvilcraft.building_rod_up",
            "Building Rod: Move up"
        );
        provider.add(
            "key.anvilcraft.building_rod_down",
            "Building Rod: Move down"
        );
        provider.add(
            "key.anvilcraft.building_rod_clockwise",
            "Building Rod: Rotate clockwise"
        );
        provider.add(
            "key.anvilcraft.building_rod_counterclockwise",
            "Building Rod: Rotate counterclockwise"
        );
        provider.add(
            "key.anvilcraft.building_rod_mirror",
            "Building Rod: Mirror"
        );
        provider.add(
            "key.anvilcraft.building_rod_tool",
            "Building Rod: Select tool (with scroll)"
        );
        provider.add(
            "key.anvilcraft.building_rod_adjust",
            "Building Rod: Adjust tool (with scroll)"
        );
        provider.add(
            "key.anvilcraft.building_rod_layer",
            "Building Rod: View layer (with scroll)"
        );
    }
}
