package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.GatewayGuiProjection;
import dev.dubhe.anvilcraft.client.support.ScaledGuiItemAtlases;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphicsExtractor.class)
abstract class ScaledGuiItemExtractionMixin {
    @WrapOperation(
        method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;III)V",
        at = @At(value = "NEW", target = "net/minecraft/client/renderer/state/gui/GuiItemRenderState")
    )
    private GuiItemRenderState anvilcraft$markOwnedIcon(
        Matrix3x2f pose, TrackingItemStackRenderState state, int x, int y, @Nullable ScreenRectangle scissor,
        Operation<GuiItemRenderState> original, @Local(argsOnly = true) ItemStack stack
    ) {
        var screen = Minecraft.getInstance().screen;
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(AnvilCraft.MOD_ID)
            || screen != null && screen.getClass().getName().startsWith("dev.dubhe.anvilcraft.")) {
            state.appendModelIdentityElement(ScaledGuiItemAtlases.OWNED_ITEM);
        }
        GatewayGuiProjection.mark(state, pose, x, y);
        return original.call(pose, state, x, y, scissor);
    }
}
