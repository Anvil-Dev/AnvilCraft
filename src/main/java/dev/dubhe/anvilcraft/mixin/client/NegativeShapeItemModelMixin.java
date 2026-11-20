package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CuboidItemModelWrapper.Unbaked.class)
abstract class NegativeShapeItemModelMixin {
    @Shadow
    public abstract Identifier model();

    @ModifyExpressionValue(method = "bake", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/resources/model/ResolvedModel;bakeTopGeometry(Lnet/minecraft/client/resources/model/sprite/TextureSlots;"
            + "Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;)"
            + "Lnet/minecraft/client/resources/model/geometry/QuadCollection;"))
    private QuadCollection anvilcraft$lightNegativeShape(QuadCollection quads, @Local ResolvedModel model) {
        // 负形外壳的三个轴均反向，同一模型的晶体表面也可通过 shade 关闭方向光。
        if (!AnvilCraft.MOD_ID.equals(this.model().getNamespace())
            || !(model.getTopGeometry() instanceof UnbakedCuboidGeometry geometry)
            || geometry.elements().stream().noneMatch(element -> !element.shade()
                && element.from().x() > element.to().x()
                && element.from().y() > element.to().y()
                && element.from().z() > element.to().z())) {
            return quads;
        }

        QuadCollection.Builder result = new QuadCollection.Builder();
        MutableQuad unshaded = new MutableQuad();
        for (BakedQuad quad : quads.getAll()) {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            if (!material.shade()) {
                Identifier atlas = material.sprite().atlasLocation();
                RenderType renderType = material.layer().translucent()
                    ? NeoForgeRenderTypes.getItemTranslucentUnlit(atlas)
                    : NeoForgeRenderTypes.getItemCutoutUnlit(atlas);
                quad = unshaded.setFrom(quad).setSprite(material.sprite(), material.layer(), renderType).toBakedQuad();
            }
            result.addUnculledFace(quad);
        }
        return result.build();
    }
}
