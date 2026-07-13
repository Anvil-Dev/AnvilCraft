package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

@Setter
@Getter
public class WipBlockEntity extends BlockEntity {

    protected int stepCount = 0;
    protected BlockState initialBlock = Blocks.AIR.defaultBlockState();
    protected @Nullable Identifier recipeId = null;

    public WipBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static WipBlockEntity createInstance(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new WipBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("StepCount", this.stepCount);
        output.store("InitialBlock", BlockState.CODEC, this.initialBlock);
        if (this.recipeId != null) {
            output.putString("Recipe", this.recipeId.toString());
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stepCount = input.getIntOr("StepCount", 0);
        this.initialBlock = input.read("InitialBlock", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
        String recipeStr = input.getStringOr("Recipe", "");
        this.recipeId = recipeStr.isEmpty() ? null : Identifier.parse(recipeStr);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(
            new ProblemReporter.Collector(this.problemPath()), registries);
        output.store("InitialBlock", BlockState.CODEC, this.initialBlock);
        if (this.recipeId != null) {
            output.putString("Recipe", this.recipeId.toString());
        }
        return output.buildResult();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
