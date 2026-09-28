package dev.dubhe.anvilcraft.client.selection;

import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsBlockEntityRenderer;
import net.minecraft.client.Minecraft;

public final class RuinsSelection {
    private RuinsSelection() {
    }

    public static void prepare(RuinsBlockEntity ruins) {
        Object renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(ruins);
        if (renderer instanceof RuinsBlockEntityRenderer display) display.prepareDisplay(ruins);
    }
}
