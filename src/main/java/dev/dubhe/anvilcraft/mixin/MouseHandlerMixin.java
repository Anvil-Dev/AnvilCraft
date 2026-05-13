package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.api.injection.input.IMouseHandlerExtension;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import dev.dubhe.anvilcraft.mixin.accessor.MultiPlayerGameModeAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin implements IMouseHandlerExtension {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private boolean mouseGrabbed;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Shadow
    private boolean ignoreFirstMove;

    @Override
    public void anvilcraft$grabMouseWithScreen() {
        if (this.minecraft.isWindowActive() && !this.mouseGrabbed) {
            if (!Minecraft.ON_OSX) {
                KeyMapping.setAll();
            }
            this.mouseGrabbed = true;
            this.xpos = (double) this.minecraft.getWindow().getScreenWidth() / 2;
            this.ypos = (double) this.minecraft.getWindow().getScreenHeight() / 2;
            InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212995, this.xpos, this.ypos);
            this.minecraft.missTime = 10000;
            this.ignoreFirstMove = true;
        }
    }

    @Inject(method = "onPress", at = @At("TAIL"))
    private void handleResonator(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        HitResult hitResult = Minecraft.getInstance().hitResult;
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (level == null || player == null || hitResult == null || gameMode == null) return;
        if (action == 1 && hitResult instanceof BlockHitResult blockHitResult
            && level.getBlockState(blockHitResult.getBlockPos()).getDestroyProgress(player, level, blockHitResult.getBlockPos()) >= 1.0F
            && player.getMainHandItem().getItem() instanceof ResonatorItem
        ) {
            ((MultiPlayerGameModeAccessor) gameMode).setDestroyDelay(5);
        }
    }
}
