package dev.dubhe.anvilcraft.mixin.client;

import dev.dubhe.anvilcraft.client.support.ProcessingModelShape;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CuboidItemModelWrapper.class)
abstract class ProcessingModelShapeMixin implements ProcessingModelShape {
    @Unique
    private boolean anvilcraft$threeDimensional;

    @Override
    public boolean anvilcraft$isThreeDimensional() {
        return this.anvilcraft$threeDimensional;
    }

    @Override
    public void anvilcraft$setThreeDimensional(boolean value) {
        this.anvilcraft$threeDimensional = value;
    }
}
