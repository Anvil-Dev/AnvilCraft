package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CauldronFluidContent.class)
public class CauldronFluidContentMixin {
    @WrapOperation(
        method = "init",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;LAVA_CAULDRON:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static Block useOurLavaCauldron(Operation<Block> original) {
        return ModBlocks.LAVA_CAULDRON.get();
    }
}
