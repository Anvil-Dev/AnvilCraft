package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import dev.dubhe.anvilcraft.block.HeliostatsBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class HeliostatsBlockEntity extends BlockEntity {
    private BlockPos irritatePos;

    public HeliostatsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 设置照射坐标
     */
    public boolean trySetIrritatePos(BlockPos pos) {
        if (!validatePos(pos)) return false;
        irritatePos = pos;
        this.setChanged();
        HeatedBlockRecorder.INSTANCE.addOrIncrease(irritatePos);
        return true;
    }

    private boolean validatePos(BlockPos pos) {
        Direction direction = getBlockState().getValue(HeliostatsBlock.FACING);
        BlockPos blockPos = getBlockPos();
        if (pos.getY() > blockPos.getY() + 16 || pos.getY() < blockPos.getY()) return false;
        switch (direction) {
            case WEST -> { //-x
                if (pos.getX() > blockPos.getX() || pos.getX() < (blockPos.getX() - 16)
                        || pos.getZ() < (blockPos.getZ() - 16) || pos.getZ() > (blockPos.getZ() + 16)
                ) return false;
            }
            case EAST -> { //+x
                if (pos.getX() < blockPos.getX() || pos.getX() > (blockPos.getX() + 16)
                        || pos.getZ() < (blockPos.getZ() - 16) || pos.getZ() > (blockPos.getZ() + 16)
                ) return false;
            }
            case NORTH -> { //-z
                if (pos.getZ() < (blockPos.getZ() - 16) || pos.getZ() > blockPos.getX()
                        || pos.getX() < (blockPos.getX() - 16) || pos.getX() > (blockPos.getX() + 16)
                ) return false;
            }
            case SOUTH -> { //+z
                if (pos.getZ() > (blockPos.getZ() + 16) || pos.getZ() < blockPos.getZ()
                        || pos.getX() < (blockPos.getX() - 16) || pos.getX() > (blockPos.getX() + 16)
                ) return false;
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        if (irritatePos == null) return;
        tag.putInt("Ix", irritatePos.getX());
        tag.putInt("Iy", irritatePos.getY());
        tag.putInt("Iz", irritatePos.getZ());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        if (!tag.contains("Ix")) return;
        int x = tag.getInt("Ix");
        int y = tag.getInt("Iy");
        int z = tag.getInt("Iz");
        irritatePos = new BlockPos(x, y, z);
    }

    /**
     *
     */
    public void tick(Level level, BlockPos pos) {
        if (!validatePos(pos)) {
            if (irritatePos != null) {
                HeatedBlockRecorder.INSTANCE.remove(irritatePos);
                irritatePos = null;
            }
        }
    }

    /**
     *
     */
    public void notifyRemoved() {
        if (irritatePos != null) {
            HeatedBlockRecorder.INSTANCE.remove(irritatePos);
        }
    }
}
