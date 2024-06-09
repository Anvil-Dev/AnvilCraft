package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.InductionLightBlock;
import net.minecraft.world.level.block.NyliumBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public class InductionLightBlockEntity extends BlockEntity implements IPowerConsumer {

    private PowerGrid grid;

    private int cooldown = 0;
    private int rangeSize = 5;

    public InductionLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static InductionLightBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new InductionLightBlockEntity(type, pos, blockState);
    }

    public void tick(Level level1) {
        flushState(level1, getBlockPos());
        this.cooldown = cooldown > 0 ? cooldown - 1 : 0;
        if (this.cooldown > 0
            || this.getBlockState().getValue(InductionLightBlock.POWERED)
            || this.getBlockState().getValue(InductionLightBlock.OVERLOAD)) {
            return;
        }
        boolean done = false;
        for (int i = -rangeSize / 2; i <= rangeSize / 2; i++) {
            for (int j = -rangeSize / 2; j <= rangeSize / 2; j++) {
                for (int k = -rangeSize / 2; k <= rangeSize / 2; k++) {
                    BlockPos pos = getPos().offset(i, j, k);
                    BlockState state = level1.getBlockState(pos);
                    if(state.getBlock() instanceof BonemealableBlock growable
                        && !growable.getClass().equals(GrassBlock.class)
                        && !growable.getClass().equals(NyliumBlock.class)
                    ) {
                        growable.performBonemeal(
                            (ServerLevel) level1,
                            level1.getRandom(), pos, state);
                        done = true;
                    }
                }
            }
        }
        if (done) {
            this.cooldown = AnvilCraft.config.inductionLightBlockRipeningCooldown;
        }
    }

    @Override
    public int getInputPower() {
        if (level == null) return 1;
        return getBlockState().getValue(InductionLightBlock.POWERED) ? 0 : 1;
    }

    @Override
    public Level getCurrentLevel() {
        return level;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return getBlockPos();
    }
}
