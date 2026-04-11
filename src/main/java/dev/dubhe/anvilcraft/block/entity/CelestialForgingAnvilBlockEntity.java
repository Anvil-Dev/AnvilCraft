package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CelestialForgingAnvilBlockEntity extends BlockEntity {
    @Getter
    private int preRotation = 0;
    @Getter
    private int rotation = 0;
    private int checkAmplifierCooldown = 0;

    @Getter
    private boolean isAmplify = false;

    private final BlockPos[] positions;

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.positions = new BlockPos[] {
            getBlockPos().offset(2, 0, 2),
            getBlockPos().offset(-2, 0, 2),
            getBlockPos().offset(-2, 0, -2),
            getBlockPos().offset(2, 0, -2)
        };
    }

    public void tick(Level level) {
        if (this.rotation == 360) {
            this.rotation = 0;
        }
        this.preRotation = this.rotation;
        this.rotation += 3;

        if (this.checkAmplifierCooldown >= 20) {
            this.checkAmplifierCooldown = 0;

            boolean isES = false;
            boolean isWS = false;
            boolean isWN = false;
            boolean isEN = false;

            for (BlockPos blockPos : this.positions) {
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.is(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER)) {
                    DirectionCube232PartHalf half = blockState.getValue(CelestialForgingAnvilAmplifierBlock.HALF);
                    switch (blockState.getValue(CelestialForgingAnvilAmplifierBlock.FACING)) {
                        case SOUTH -> isES = half == DirectionCube232PartHalf.BOTTOM_WS;
                        case WEST -> isWS = half == DirectionCube232PartHalf.BOTTOM_S;
                        case NORTH -> isWN = half == DirectionCube232PartHalf.BOTTOM_PART;
                        case EAST -> isEN = half == DirectionCube232PartHalf.BOTTOM_W;
                        default -> {}
                    }
                }
            }
            this.isAmplify = isES && isWS && isWN && isEN;
        }
        this.checkAmplifierCooldown++;
    }
}
