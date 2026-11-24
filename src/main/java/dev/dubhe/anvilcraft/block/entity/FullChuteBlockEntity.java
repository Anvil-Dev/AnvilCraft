package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.chutePlates.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.chutePlates.load(input);
    }

    @Override
    public void tick() {
        this.chutePlates.clearPortPlates();
        if (!this.isRemoved()) {
            super.tick();
        }
    }
}
