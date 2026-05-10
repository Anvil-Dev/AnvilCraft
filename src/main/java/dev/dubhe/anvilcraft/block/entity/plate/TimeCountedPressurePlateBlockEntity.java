package dev.dubhe.anvilcraft.block.entity.plate;

import dev.dubhe.anvilcraft.block.plate.TimeCountedPressurePlateBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TimeCountedPressurePlateBlockEntity extends BlockEntity {
    private int needTick;
    private int tick = 0;

    protected TimeCountedPressurePlateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int needTick) {
        super(type, pos, blockState);
        this.needTick = needTick;
    }

    public TimeCountedPressurePlateBlockEntity(BlockPos pos, BlockState blockState, int needTick) {
        this(ModBlockEntities.TIME_COUNTED_PRESSURE_PLATE.get(), pos, blockState, needTick);
    }

    public static TimeCountedPressurePlateBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new TimeCountedPressurePlateBlockEntity(type, pos, blockState, 10);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("tick", this.tick);
        output.putInt("NeedTick", this.needTick);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tick = input.getIntOr("tick", 0);
        this.needTick = input.getIntOr("NeedTick", 0);
    }

    public int getSignalStrength() {
        return Math.clamp(tick / (needTick == 0 ? 1 : needTick), 0, 15);
    }

    public void tick(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TimeCountedPressurePlateBlock plate) {
            List<LivingEntity> entities = level.getEntities(EntityTypeTest.forClass(LivingEntity.class), new AABB(pos), entity -> true);
            if (!entities.isEmpty()) {
                if (tick < plate.needTick * 15) {
                    tick++;
                }
            } else if (tick > 0) {
                tick--;
            }
        }
    }
}
