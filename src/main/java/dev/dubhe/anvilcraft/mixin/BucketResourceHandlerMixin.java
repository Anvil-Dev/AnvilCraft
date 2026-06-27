package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.fluid.BucketResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketResourceHandler.class)
public class BucketResourceHandlerMixin {
    @Inject(
        method = "getResourceFrom("
                 + "Lnet/neoforged/neoforge/transfer/item/ItemResource;I)"
                 + "Lnet/neoforged/neoforge/transfer/fluid/FluidResource;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void powderSnow(ItemResource accessResource, int index, CallbackInfoReturnable<FluidResource> cir) {
        if (accessResource.getItem() == Items.POWDER_SNOW_BUCKET) {
            cir.setReturnValue(FluidResource.of(ModFluids.POWDER_SNOW.get()));
        } else if (accessResource.getItem() == Items.MILK_BUCKET) {
            cir.setReturnValue(FluidResource.of(ModFluids.MILK.get()));
        }
    }
}
