package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.IItemSubmitExtension;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SubmitNodeStorage.ItemSubmit.class)
public class ItemSubmitMixin implements IItemSubmitExtension {
    @Unique
    private boolean anvilcraft$halfTransparent;

    @Override
    public void anvilcraft$setHalfTransparent(boolean halfTransparent) {
        this.anvilcraft$halfTransparent = halfTransparent;
    }

    @Override
    public boolean anvilcraft$isHalfTransparent() {
        return this.anvilcraft$halfTransparent;
    }
}
