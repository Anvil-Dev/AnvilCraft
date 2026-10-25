package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.Window;
import dev.dubhe.anvilcraft.api.injection.IGuiGraphicsExtractorExtension;
import dev.dubhe.anvilcraft.client.renderer.item.ExtraItemDisplayRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements IGuiGraphicsExtractorExtension {
    @Unique
    private @Nullable List<Runnable> anvilcraft$tooltips;

    @Unique
    private static int ANVILCRAFT$RECURSION = 0;
    @Unique
    private static final int ANVILCRAFT$MAX_RECURSION = 3;

    @Shadow
    protected abstract void item(@Nullable LivingEntity owner, @Nullable Level level, ItemStack itemStack, int x, int y, int seed);

    @Shadow
    @Final
    private Matrix3x2fStack pose;

    @Shadow
    public abstract void nextStratum();

    @Override
    public void anvilcraft$deferTooltip(Runnable tooltip) {
        if (this.anvilcraft$tooltips == null) this.anvilcraft$tooltips = new ArrayList<>();
        Matrix3x2f tooltipPose = new Matrix3x2f(this.pose);
        this.anvilcraft$tooltips.add(() -> {
            this.pose.pushMatrix();
            this.pose.set(tooltipPose);
            try {
                tooltip.run();
            } finally {
                this.pose.popMatrix();
            }
        });
    }

    @WrapMethod(method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/"
        + "tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V")
    private void anvilcraft$deferStandardTooltip(
        Font font, List<ClientTooltipComponent> lines, int x, int y, ClientTooltipPositioner positioner,
        @Nullable Identifier style, ItemStack stack, Operation<Void> original
    ) {
        this.anvilcraft$deferTooltip(() -> original.call(font, lines, x, y, positioner, style, stack));
    }

    // GameRenderer applies the cursor after extracting all GUI layers, including overlays and toasts.
    @Inject(method = "applyCursor", at = @At("HEAD"))
    private void anvilcraft$extractTooltips(Window window, CallbackInfo ci) {
        if (this.anvilcraft$tooltips == null) return;
        for (int index = 0; index < this.anvilcraft$tooltips.size(); index++) {
            this.nextStratum();
            this.anvilcraft$tooltips.get(index).run();
        }
        this.anvilcraft$tooltips.clear();
    }

    @Inject(
        method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
        at = @At(
            value = "RETURN"
        )
    )
    private void renderExtra(LivingEntity owner, Level level, ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        ExtraItemDisplayRenderer.renderGuiExtra(
            this.pose,
            this::item,
            owner,
            level,
            itemStack,
            x,
            y,
            seed,
            GuiGraphicsExtractorMixin.ANVILCRAFT$RECURSION,
            GuiGraphicsExtractorMixin.ANVILCRAFT$MAX_RECURSION,
            i -> GuiGraphicsExtractorMixin.ANVILCRAFT$RECURSION = i
        );
    }
}
