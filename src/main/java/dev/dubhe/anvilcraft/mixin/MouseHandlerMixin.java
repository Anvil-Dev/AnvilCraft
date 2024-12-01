package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.api.input.IMouseHandlerExtension;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin implements IMouseHandlerExtension {

    @Shadow @Final private Minecraft minecraft;

    @Shadow private boolean mouseGrabbed;

    @Shadow private double xpos;

    @Shadow private double ypos;

    @Shadow private boolean ignoreFirstMove;

    @Override
    public void anvilCraft$grabMouseWithScreen() {
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
}
