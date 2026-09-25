package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.client.support.GatewayGuiProjection;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiItemAtlas.class)
abstract class GatewayItemAtlasMixin {
    @Shadow @Final private int textureSize;
    @Shadow @Final private int slotTextureSize;

    @WrapMethod(method = "drawToSlot")
    private void anvilcraft$projectGateway(int x, int y, boolean clear, ItemStackRenderState state, Operation<Void> original) {
        GatewayGuiProjection.withAtlas(this.textureSize, this.slotTextureSize, x, y, () -> original.call(x, y, clear, state));
    }
}
