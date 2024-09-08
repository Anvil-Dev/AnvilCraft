package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.depository.DepositoryHolder;
import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class CrabTrapBlockEntity extends BlockEntity implements DepositoryHolder {
    public CrabTrapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private final ItemDepository depository = new ItemDepository(9);

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", depository.serializeNbt(provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        depository.deserializeNbt(provider, tag.getCompound("Inventory"));
    }
}
