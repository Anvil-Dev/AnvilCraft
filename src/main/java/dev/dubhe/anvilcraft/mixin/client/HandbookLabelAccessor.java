package dev.dubhe.anvilcraft.mixin.client;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "dev.anvilcraft.resource.ageratum.client.gui.GuideScreen$LabelEntry", remap = false)
public interface HandbookLabelAccessor {
    @Accessor("level")
    int anvilcraft$level();

    @Accessor("location")
    @Nullable Identifier anvilcraft$location();

    @Accessor("clickable")
    boolean anvilcraft$clickable();
}
