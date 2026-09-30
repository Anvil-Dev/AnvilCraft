package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent;
import dev.dubhe.anvilcraft.client.markdown.HandbookStructureExportClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MDNBTStructureComponent.class, remap = false)
abstract class HandbookStructureExportMixin {
    @Shadow @Final private MDNBTStructureComponent.StructureTarget target;
    @Shadow private @Nullable StructureTemplate structureTemplateCache;

    @Inject(method = "renderButton", at = @At("HEAD"))
    private void anvilcraft$exportButton(MDRenderContext context, CallbackInfo ci) {
        var graphics = context.graphics();
        int x = context.maxX() - 21;
        boolean hover = context.mouseX() >= x && context.mouseX() < x + 16 && context.mouseY() >= 25 && context.mouseY() < 41;
        graphics.fill(x, 25, x + 16, 41, hover ? -1146443094 : -2008791996);
        graphics.fill(x + 7, 28, x + 9, 34, -1);
        graphics.fill(x + 5, 32, x + 11, 34, -1);
        graphics.fill(x + 6, 34, x + 10, 35, -1);
        graphics.fill(x + 7, 35, x + 9, 36, -1);
        graphics.fill(x + 4, 37, x + 12, 39, -1);
        if (hover) context.addTooltip(Component.translatable("tooltip.ageratum.structure_export"));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$exportClick(
        Minecraft minecraft, double x, double y, int button, int maxX, CallbackInfoReturnable<Boolean> cir
    ) {
        if (button == 0 && this.structureTemplateCache != null && x >= maxX - 21 && x < maxX - 5 && y >= 25 && y < 41) {
            HandbookStructureExportClient.export(this.target.location(), this.structureTemplateCache);
            cir.setReturnValue(true);
        }
    }
}
