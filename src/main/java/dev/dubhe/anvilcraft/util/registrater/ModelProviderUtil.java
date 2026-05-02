package dev.dubhe.anvilcraft.util.registrater;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumItemModelGenerator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelProviderUtil {
    /**
     * 用于流体的BlockState生成器
     */
    public static void liquid(DataGenContext<Block, ? extends LiquidBlock> ctx, RegistrumBlockModelGenerator generator) {
        generator.create(
            ctx.get(),
            generator.getBuilder().texture(TextureSlot.PARTICLE, generator.modLoc("block/" + ctx.getName()), false).build(ctx.get())
        );
    }

    /**
     * 用于流体的ItemModel生成器
     */
    public static void bucket(DataGenContext<Item, ? extends BucketItem> ctx, RegistrumItemModelProvider provider) {
        provider.withExistingParent(
            ctx.getName(),
            Identifier.parse("neoforge:item/bucket_drip")
        ).customLoader((builder, helper) -> DynamicFluidContainerModelBuilder.begin(builder, helper).fluid(ctx.get().content));
    }
}
