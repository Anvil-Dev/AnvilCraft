package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CropBlock.class)
public interface CropBlockAccessor {
    @Invoker
    ItemLike invokeGetBaseSeedId();

    @Invoker
    IntegerProperty invokeGetAgeProperty();
}
