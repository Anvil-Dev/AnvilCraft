package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.item.ExtraItemDisplayRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    @Unique
    private static int ANVILCRAFT$RECURSION = 0;
    @Unique
    private static final int ANVILCRAFT$MAX_RECURSION = 3;

    @Final
    @Shadow
    private PoseStack pose;

    @Shadow
    protected abstract void item(@Nullable LivingEntity owner, @Nullable Level level, ItemStack itemStack, int x, int y, int seed);

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
            ANVILCRAFT$RECURSION,
            ANVILCRAFT$MAX_RECURSION,
            i -> ANVILCRAFT$RECURSION = i
        );
    }
}
