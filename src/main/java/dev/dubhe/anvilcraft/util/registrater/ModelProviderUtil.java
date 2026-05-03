package dev.dubhe.anvilcraft.util.registrater;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumItemModelGenerator;
import dev.anvilcraft.lib.v2.util.nullness.NonNullBiConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelProviderUtil {
    /**
     * 用于流体的BlockState生成器
     */
    public static <L extends LiquidBlock> NonNullBiConsumer<DataGenContext<Block, L>, RegistrumBlockModelGenerator> liquid() {
        return (ctx, generator) -> generator.create(
            ctx.get(),
            generator.getBuilder()
                .texture(TextureSlot.PARTICLE, generator.modLoc("block/" + ctx.getName()), false)
                .build(ctx.get())
        );
    }

    /**
     * 用于流体的ItemModel生成器
     */
    public static NonNullBiConsumer<DataGenContext<Item, BucketItem>, RegistrumItemModelGenerator> bucket() {
        return (ctx, generator) -> generator.itemModelOutput.accept(
            ctx.get(),
            new DynamicFluidContainerModel.Unbaked(
                new DynamicFluidContainerModel.Textures(
                    Optional.empty(),
                    Optional.of(new Material(ModelLocationUtils.decorateItemModelLocation("bucket"))),
                    Optional.of(new Material(ModelLocationUtils.decorateItemModelLocation("neoforge:mask/bucket_fluid_drip"))),
                    Optional.empty()
                ),
                ctx.get().content,
                false,
                true,
                true
            )
        );
    }
}
