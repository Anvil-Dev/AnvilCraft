package dev.dubhe.anvilcraft.util;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
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
    public static void liquid(DataGenContext<Block, LiquidBlock> ctx, RegistrateBlockstateProvider provider) {
        provider.simpleBlock(
            ctx.get(),
            provider.models().getBuilder(ctx.getName()).texture("particle", provider.modLoc("block/" + ctx.getName()))
        );
    }

    public static void bucket(DataGenContext<Item, ? extends BucketItem> ctx, RegistrateItemModelProvider provider) {
        provider.withExistingParent(
                ctx.getName(),
                ResourceLocation.parse("neoforge:item/bucket_drip"))
            .texture("cover", ResourceLocation.parse("neoforge:item/mask/bucket_fluid_cover_drip"))
            .customLoader((builder, helper) -> DynamicFluidContainerModelBuilder.begin(builder, helper)
                .fluid(ctx.get().content)
                .coverIsMask(true)
            );
    }
}
