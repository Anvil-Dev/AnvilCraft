package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@Setter
@Getter
public class WipBlockEntity extends BlockEntity {

    protected int stepCount = 0;
    protected BlockState initialBlock = Blocks.AIR.defaultBlockState();
    protected @Nullable ResourceLocation recipeId = null;

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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StepCount", stepCount);
        tag.put("InitialBlock", NbtUtils.writeBlockState(initialBlock));
        if (recipeId != null) {
            tag.putString("Recipe", recipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stepCount = tag.getInt("StepCount");
        initialBlock = NbtUtils.readBlockState(
            registries.lookupOrThrow(Registries.BLOCK),
            tag.getCompound("InitialBlock")
        );
        recipeId = ResourceLocation.parse(tag.getString("Recipe"));
        // ResourceLocation.parse()自带空值处理，bySeparator会在切不出两片的时候返回withDefaultNamespace
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag data = super.getUpdateTag(registries);
        data.put("InitialBlock", NbtUtils.writeBlockState(initialBlock));
        return data;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
