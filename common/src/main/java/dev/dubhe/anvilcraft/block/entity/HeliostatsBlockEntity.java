package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer;
import dev.dubhe.anvilcraft.network.HeliostatsIrradiationPack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class HeliostatsBlockEntity extends BlockEntity {
    private BlockPos irritatePos;
    @Getter
    @Setter
    private Vector3f normalVector3f = new Vector3f().normalize();
    @Getter
    @Setter
    private Vector3f irritateVector3f = new Vector3f().normalize();
    @Getter
    @Setter
    private float irritateDistance = 0;

    public HeliostatsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 设置照射坐标
     */
    public boolean trySetIrritatePos(BlockPos pos) {
        if (getBlockPos().getCenter().distanceTo(pos.getCenter()) > 16) return false;
        irritatePos = pos;
        this.setChanged();
        HeatedBlockRecorder.getInstance(getLevel()).addOrIncrease(irritatePos, this);
        return true;
    }

    private boolean validatePos(BlockPos irritatePos) {
        if (level == null) return false;
        if (getBlockPos().getCenter().distanceTo(irritatePos.getCenter()) > 16) {
            normalVector3f = new Vector3f();
            new HeliostatsIrradiationPack(getBlockPos(), normalVector3f, irritateVector3f, irritateDistance)
                    .broadcast();
            return false;
        }
        double sunAngle = level.getSunAngle(1);
        sunAngle = sunAngle <= Math.PI / 2 * 3 ? sunAngle + Math.PI / 2 : sunAngle - Math.PI / 2 * 3;
        if (sunAngle > Math.PI) {
            normalVector3f = new Vector3f();
            new HeliostatsIrradiationPack(getBlockPos(), normalVector3f, irritateVector3f, irritateDistance)
                    .broadcast();
            return false;
        }
        Vector3f sunVector3f = new Vector3f((float) Math.cos(sunAngle), (float) Math.sin(sunAngle), 0).normalize();
        irritateVector3f = new Vector3f(
                irritatePos.getX() - getBlockPos().getX(),
                irritatePos.getY() - getBlockPos().getY(),
                irritatePos.getZ() - getBlockPos().getZ()
        ).normalize();
        irritateDistance = (float) (getBlockPos().getCenter().distanceTo(irritatePos.getCenter()) - 0.5);
        Vector3f normalVector3f = sunVector3f.add(irritateVector3f).div(2);
        if (normalVector3f.y < 0) {
            normalVector3f = new Vector3f();
            new HeliostatsIrradiationPack(getBlockPos(), normalVector3f, irritateVector3f, irritateDistance)
                    .broadcast();
            return false;
        }
        new HeliostatsIrradiationPack(getBlockPos(), normalVector3f, irritateVector3f, irritateDistance).broadcast();
        return level.clip(
                new ClipContext(
                        getBlockPos().getCenter().add(0f, 0.54f, 0f),
                        irritatePos.getCenter(),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        AnvilCraftBlockPlacer.anvilCraftBlockPlacer.getPlayer())
        ).getBlockPos().equals(irritatePos);
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
        if (irritatePos == null) return;
        ServerLevel level1 = (ServerLevel) level;
        int currentBrightness = level.getBrightness(LightLayer.SKY, pos.above());
        if (!(level1.isRainingAt(pos.above()) || level1.isThundering() || !validatePos(irritatePos))
                && currentBrightness == 15) {
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
