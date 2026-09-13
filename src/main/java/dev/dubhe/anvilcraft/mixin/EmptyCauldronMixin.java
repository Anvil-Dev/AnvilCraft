package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.block.IEmptyCauldron;
import net.minecraft.world.level.block.CauldronBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CauldronBlock.class)
abstract class EmptyCauldronMixin implements IEmptyCauldron {
}
