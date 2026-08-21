package dev.dubhe.anvilcraft.util.registrater;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumItemModelProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelProviderUtil {
    /**
     * 用于流体的BlockState生成器
     */
    public static void liquid(DataGenContext<Block, ? extends LiquidBlock> ctx, RegistrumBlockstateProvider provider) {
        provider.simpleBlock(
            ctx.get(),
            provider.models().getBuilder(ctx.getName()).texture("particle", provider.modLoc("block/" + ctx.getName()))
        );
    }

    /**
     * Used for gaseous fluid buckets; the fluid is rendered upside-down in the bucket.
     */
    public static void bucketGassy(DataGenContext<Item, ? extends BucketItem> ctx, RegistrumItemModelProvider provider) {
        provider.withExistingParent(
            ctx.getName(),
            ResourceLocation.parse("neoforge:item/bucket_drip")
        ).customLoader((builder, helper) -> DynamicFluidContainerModelBuilder.begin(builder, helper)
            .fluid(ctx.get().content)
            .flipGas(true));
    }

    public static void bucket(DataGenContext<Item, ? extends BucketItem> ctx, RegistrumItemModelProvider provider) {
        provider.withExistingParent(
            ctx.getName(),
            ResourceLocation.parse("neoforge:item/bucket_drip")
        ).customLoader((builder, helper) -> DynamicFluidContainerModelBuilder.begin(builder, helper).fluid(ctx.get().content));
    }
}
