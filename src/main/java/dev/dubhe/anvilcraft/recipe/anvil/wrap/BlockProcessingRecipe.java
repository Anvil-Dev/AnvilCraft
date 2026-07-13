package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.state.IrradiatorType;
import dev.dubhe.anvilcraft.block.workstation.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.workstation.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// 序列装配配方中的"闪炼、时移、中子辐照"都属于方块处理，即用方块处理方块
public class BlockProcessingRecipe extends AbstractProcessRecipe<BlockProcessingRecipe> {

    public static final RecipeSerializer<BlockProcessingRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStatePredicate.CODEC
                .listOf()
                .fieldOf("inputs")
                .forGetter(BlockProcessingRecipe::getInputBlocks),
            ChanceBlockState.CODEC.codec()
                .fieldOf("result")
                .forGetter(BlockProcessingRecipe::getFirstResultBlock)
        ).apply(instance, BlockProcessingRecipe::new)),
        StreamCodec.composite(
            BlockStatePredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BlockProcessingRecipe::getInputBlocks,
            ChanceBlockState.STREAM_CODEC,
            BlockProcessingRecipe::getFirstResultBlock,
            BlockProcessingRecipe::new
        )
    );

    public BlockProcessingRecipe(
        List<BlockStatePredicate> inputs,
        ChanceBlockState result
    ) {
        super(
            new Property()
                .setBlockInputOffset(new Vec3i(0, -1, 0))
                .setInputBlocks(inputs)
                .setBlockOutputOffset(new Vec3i(0, -1, 0))
                .setResultBlocks(result)
        );
    }

    @Override
    public RecipeSerializer<BlockProcessingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<BlockProcessingRecipe> getType() {
        return ModRecipeTypes.BLOCK_PROCESSING.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractRecipeBuilder<BlockProcessingRecipe> {
        private final List<BlockStatePredicate> inputs = new ArrayList<>();
        private @Nullable ChanceBlockState result = null;

        public Builder input(Block input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            return this;
        }

        public Builder fakeSuperHeating(Block input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            this.inputs.add(BlockStatePredicate.builder()
                .of(ModBlocks.HEATER.get())
                .with(HeaterBlock.OVERLOAD, false)
                .or()
                .with(BurningHeaterBlock.LEVEL, 2)
                .build());
            return this;
        }

        public Builder fakeTimeWarp(Block input) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            this.inputs.add(BlockStatePredicate.builder()
                .of(ModBlocks.CORRUPTED_BEACON.get())
                .with(CorruptedBeaconBlock.LIT, true)
                .build());
            return this;
        }

        public Builder fakeNeutronIrradiation(Block input, IrradiatorType type) {
            this.inputs.add(BlockStatePredicate.builder().of(input).build());
            this.inputs.add(BlockStatePredicate.builder()
                .of(ModBlocks.NEUTRON_IRRADIATOR.get())
                .with(NeutronIrradiatorBlock.TYPE, type)
                .build());
            return this;
        }

        public Builder result(ChanceBlockState result) {
            this.result = result;
            return this;
        }

        public Builder result(Block result) {
            this.result = new ChanceBlockState(result.defaultBlockState(), 1.0f);
            return this;
        }

        @Override
        public BlockProcessingRecipe buildRecipe() {
            return new BlockProcessingRecipe(this.inputs, this.result);
        }

        @Override
        public void validate(Identifier id) {
            if (this.inputs.isEmpty()) {
                throw new IllegalArgumentException("Recipe inputs must not be empty, RecipeId: " + id);
            }
            if (this.result == null) {
                throw new IllegalArgumentException("Recipe result must not be null, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "block_processing";
        }

        @Override
        public ItemStackTemplate getResult() {
            return WrapUtils.getItem(this.result);
        }
    }
}
