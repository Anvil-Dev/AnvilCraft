package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer;
import dev.dubhe.anvilcraft.network.HeliostatsIrradiationPack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;

public class HeliostatsBlockEntity extends BlockEntity {
    @Getter
    private BlockPos irritatePos;
    @Getter
    @Setter
    private Vector3f normalVector3f = new Vector3f().normalize();
    @Getter
    @Setter
    private Vector3f irritateVector3f = new Vector3f().normalize();
    @Getter
    @Setter
    private WorkResult workResult = WorkResult.SUCCESS;
    private int surfaceVec3Hash = 0;
    private Vec3 surfaceVec3 = new Vec3(0, 0, 0);

    public HeliostatsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private Vec3 getSurfaceVec3(Vec3 vec31, Vec3 vec32) {
        if (vec31.hashCode() + vec32.hashCode() == surfaceVec3Hash) return surfaceVec3;
        if (level == null) return vec31;
        if (!level.getBlockState(irritatePos.north()).isAir()
                && !level.getBlockState(irritatePos.south()).isAir()
                && !level.getBlockState(irritatePos.east()).isAir()
                && !level.getBlockState(irritatePos.west()).isAir()) return vec31.add(0, 0, 0);
        Vec2 vec2 = new Vec2((float) (vec32.z - vec31.z), (float) (vec32.x - vec31.x));
        if (vec2.x == 0) return vec31.add(vec2.y > 0 ? 0.49f : -0.49f, 0, 0);
        if (vec2.y == 0) return vec31.add(0, 0, vec2.x > 0 ? 0.49f : -0.49f);
        float k = vec2.y / vec2.x;
        float x = vec2.x > 0 ? 0.49f : -0.49f;
        float y = vec2.y > 0 ? 0.49f : -0.49f;
        if (y / k < 0.5 && y / k > -0.5) return vec31.add(y, 0, y / k);
        if (k * x < 0.5 && k * x > -0.5) return vec31.add(k * x, 0, x);
        surfaceVec3Hash = vec31.hashCode() + vec32.hashCode();
        surfaceVec3 = vec31;
        return vec31;
    }

    /**
     * 设置照射坐标
     */
    public boolean setIrritatePos(BlockPos pos) {
        irritatePos = pos;
        this.setChanged();
        return validatePos(pos).isWork();
    }

    private WorkResult validatePos(BlockPos irritatePos) {
        normalVector3f = new Vector3f();
        if (level == null) return WorkResult.UNKNOWN;
        if (level.isClientSide && Minecraft.getInstance().player == null)
            return WorkResult.UNKNOWN;
        if (irritatePos == null) return WorkResult.UNSPECIFIED_IRRADIATION_BLOCK;
        if (getBlockPos().getCenter().distanceTo(irritatePos.getCenter()) > 16)
            return WorkResult.TOO_FAR;
        if (level.isRainingAt(getBlockPos().above())
                || level.getBrightness(LightLayer.SKY, getBlockPos().above()) != 15)
            return WorkResult.NO_SUN;
        Vec3 irritateVec3 = getSurfaceVec3(irritatePos.getCenter(), getBlockPos().getCenter());
        BlockHitResult blockHitResult = level.clip(new ClipContext(
                getBlockPos().getCenter().add(0f, 0.34f, 0f),
                irritateVec3,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                level.isClientSide
                        ? Objects.requireNonNull(Minecraft.getInstance().player)
                        : AnvilCraftBlockPlacer.anvilCraftBlockPlacer.getPlayer()
                )
        );
        if (!blockHitResult.getBlockPos().equals(irritatePos))
            return WorkResult.OBSCURED;
        double sunAngle = level.getSunAngle(1);
        sunAngle = sunAngle <= Math.PI / 2 * 3 ? sunAngle + Math.PI / 2 : sunAngle - Math.PI / 2 * 3;
        if (sunAngle > Math.PI) return WorkResult.NO_SUN;
        Vector3f sunVector3f = new Vector3f((float) Math.cos(sunAngle), (float) Math.sin(sunAngle), 0).normalize();
        irritateVector3f = new Vector3f(
                (float) (irritateVec3.x - getBlockPos().getX()),
                (float) (irritateVec3.y - getBlockPos().getY()),
                (float) (irritateVec3.z - getBlockPos().getZ())
        ).normalize();
        normalVector3f = sunVector3f.add(irritateVector3f).div(2);
        if (normalVector3f.y < 0) return WorkResult.NO_ROTATION_ANGLE;
        return WorkResult.SUCCESS;
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
     * tick
     */
    public void tick() {
        if (level == null) return;
        if (level.getGameTime() % (AnvilCraft.config.heliostatsDetectionInterval + 1) != 0) return;
        if (irritatePos == null && level.isClientSide)
             new HeliostatsIrradiationPack(getBlockPos(), irritatePos).send();
        workResult = validatePos(irritatePos);
        if (workResult.isWork()) {
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

    public enum WorkResult {

        SUCCESS(""),
        NO_ROTATION_ANGLE("tooltip.anvilcraft.heliostats.no_rotation_angle"),
        NO_SUN("tooltip.anvilcraft.heliostats.no_sun"),
        OBSCURED("tooltip.anvilcraft.heliostats.obscured"),
        TOO_FAR("tooltip.anvilcraft.heliostats.too_far"),
        UNSPECIFIED_IRRADIATION_BLOCK("tooltip.anvilcraft.heliostats.unspecified_irradiation_block"),
        UNKNOWN("tooltip.anvilcraft.heliostats.unknown");

        private final String key;

        WorkResult(String key) {
            this.key = key;
        }

        public boolean equals(WorkResult workResult) {
            return this.key.equals(workResult.key);
        }

        public String getTranslateKey() {
            return this.key;
        }

        public boolean isWork() {
            return this.equals(SUCCESS);
        }
    }
}
