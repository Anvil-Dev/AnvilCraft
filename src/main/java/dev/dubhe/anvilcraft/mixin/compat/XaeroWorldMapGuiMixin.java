package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.client.overseer.OverseerMapData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

@Pseudo
@Mixin(value = GuiMap.class, remap = false)
public abstract class XaeroWorldMapGuiMixin implements IRightClickableElement {
    @Shadow
    private int rightClickX;
    @Shadow
    private int rightClickZ;
    @Shadow
    private @Nullable ResourceKey<Level> rightClickDim;

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void anvilcraft$addOverseerToggle(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        boolean visible = OverseerMapData.isVisible();
        if (visible && (this.rightClickDim == null
            || OverseerMapData.flags(this.rightClickDim, this.rightClickX >> 4, this.rightClickZ >> 4) < 0)) {
            return;
        }
        ArrayList<RightClickOption> options = cir.getReturnValue();
        String key = visible ? "map.anvilcraft.overseer.hide" : "map.anvilcraft.overseer.show";
        options.add(new RightClickOption(key, options.size(), this) {
            @Override
            public void onAction(Screen screen) {
                OverseerMapData.setVisible(!visible);
            }
        });
    }
}
