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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class HeliostatsBlockEntity extends BlockEntity {
    private BlockPos irritatePos;
    private Vec3 irritateVec3;
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

    private Vec3 getSurfaceVec3(Vec3 vec31, Vec3 vec32) {
        Vec2 vec22 = new Vec2((float) (vec32.z - vec31.z), (float) (vec32.x - vec31.x));
        float k = vec22.x / vec22.y;
        float x = vec22.x > 0 ? 0.49f : -0.49f;
        float y = vec22.y > 0 ? 0.49f : -0.49f;
        if (y / k <= 0.49 && y / k >= -0.49) return vec31.add(y / k, 0, x);
        if (k * x <= 0.49 && k * x >= -0.49) return vec31.add(k * x, 0, y);
        return vec31;
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
        if (getBlockPos().getCenter().distanceTo(irritatePos.getCenter()) > 16
                || level.isRainingAt(getBlockPos().above())
                || level.isThundering()) {
            normalVector3f = new Vector3f();
            new HeliostatsIrradiationPack(getBlockPos(), normalVector3f, irritateVector3f, irritateDistance)
                    .broadcast();
            return false;
        }
        irritateVec3 = getSurfaceVec3(irritatePos.getCenter(), getBlockPos().getCenter());
        BlockHitResult blockHitResult = level.clip(new ClipContext(
                getBlockPos().getCenter().add(0f, 0.34f, 0f),
                irritateVec3,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                AnvilCraftBlockPlacer.anvilCraftBlockPlacer.getPlayer())
        );
        if (!blockHitResult.getBlockPos().equals(irritatePos)) {
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
                (float) (irritateVec3.x - getBlockPos().getX()),
                (float) (irritateVec3.y - getBlockPos().getY()),
                (float) (irritateVec3.z - getBlockPos().getZ())
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
        if (irritatePos == null) return;
        ServerLevel level1 = (ServerLevel) level;
        int currentBrightness = level.getBrightness(LightLayer.SKY, pos.above());
        if (validatePos(irritatePos) && currentBrightness == 15) {
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
