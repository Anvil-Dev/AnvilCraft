package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import net.minecraft.client.Minecraft;

/**
 * Client-side helper for SpacetimeSupercomputerBlock.
 * Used via DistExecutor to avoid loading client classes on dedicated server.
 */
public class SpacetimeSupercomputerClientHelper {
    private SpacetimeSupercomputerClientHelper() {}

    public static void openScreen(SpacetimeSupercomputerBlockEntity entity) {
        Minecraft.getInstance().setScreen(new SpacetimeSupercomputerScreen(entity));
    }
}
