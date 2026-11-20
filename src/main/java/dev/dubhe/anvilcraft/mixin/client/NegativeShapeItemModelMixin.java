package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.item.NegativeShapeItemModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockModel.class)
abstract class NegativeShapeItemModelMixin {
    @ModifyReturnValue(
        method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/model/BlockModel;"
            + "Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Z)"
            + "Lnet/minecraft/client/resources/model/BakedModel;",
        at = @At("RETURN")
    )
    private BakedModel anvilcraft$lightNegativeShape(BakedModel baked) {
        BlockModel model = (BlockModel) (Object) this;
        if (model.name.startsWith(AnvilCraft.MOD_ID + ":") && model.getElements().stream().anyMatch(element ->
            !element.shade && element.from.x > element.to.x && element.from.y > element.to.y && element.from.z > element.to.z)) {
            return new NegativeShapeItemModel(baked);
        }
        return baked;
    }
}
