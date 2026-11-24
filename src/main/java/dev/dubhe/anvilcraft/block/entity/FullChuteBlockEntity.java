package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 完整溜槽在原有机器库存和菜单数据之外保存盖板。 */
public abstract class FullChuteBlockEntity extends BaseChuteBlockEntity implements ChutePlates.Holder {
    private final ChutePlates chutePlates = new ChutePlates(this);

    protected FullChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ChutePlates getChutePlates() {
        return this.chutePlates;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.chutePlates.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.chutePlates.load(tag);
    }

    @Override
    public void tick() {
        this.chutePlates.clearPortPlates();
        if (!this.isRemoved()) {
            super.tick();
        }
    }
}
