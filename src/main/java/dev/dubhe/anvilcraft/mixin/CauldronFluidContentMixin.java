package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.cauldron.Layered4LevelCauldronBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CauldronFluidContent.class)
public class CauldronFluidContentMixin {
    @WrapOperation(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/RegisterCauldronFluidContentEvent;register("
                     + "Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/material/Fluid;I"
                     + "Lnet/minecraft/world/level/block/state/properties/IntegerProperty;)V",
            ordinal = 2
        )
    )
    private static void registerLayeredLavaCauldron(
        RegisterCauldronFluidContentEvent event,
        Block block,
        Fluid fluid,
        int totalAmount,
        @Nullable IntegerProperty levelProperty,
        Operation<Void> original
    ) {
        original.call(event, ModBlocks.LAVA_CAULDRON.get(), fluid, totalAmount, Layered4LevelCauldronBlock.LEVEL);
    }
}
