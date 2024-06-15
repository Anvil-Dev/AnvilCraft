package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import dev.dubhe.anvilcraft.block.HeliostatsBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
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
        HeatedBlockRecorder.getInstance(getLevel()).addOrIncrease(irritatePos, this);
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
        if (level.isClientSide) return;
        if (!validatePos(pos)) {
            if (irritatePos != null) {
                HeatedBlockRecorder.getInstance(level).remove(irritatePos, this);
                irritatePos = null;
            }
        }
        if (irritatePos == null) return;
        ServerLevel level1 = (ServerLevel) level;
        int currentBrightness = level.getBrightness(LightLayer.SKY, pos.above());
        if (!(level1.isRainingAt(pos.above()) || level1.isThundering())
                && currentBrightness == 15
                && level.getDayTime() > 0 && level.getDayTime() <= 13000) {
            HeatedBlockRecorder.getInstance(getLevel()).addOrIncrease(irritatePos, this);
        } else {
            HeatedBlockRecorder.getInstance(getLevel()).remove(irritatePos, this);
        }
    }

    /**
     *
     */
    public void notifyRemoved() {
        if (irritatePos != null) {
            HeatedBlockRecorder.getInstance(getLevel()).remove(irritatePos, this);
        }
    }
}
