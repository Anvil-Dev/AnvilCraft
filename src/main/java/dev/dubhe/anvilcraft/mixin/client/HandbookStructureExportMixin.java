package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent;
import dev.dubhe.anvilcraft.client.markdown.HandbookStructureExportClient;
import net.minecraft.client.Minecraft;
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
        MDNBTStructureComponent.renderExportButton(context);
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
