package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.network.HeliostatsIrradiationPacket;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

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
        if (vec31.hashCode() + vec32.hashCode() == this.surfaceVec3Hash) return this.surfaceVec3;
        if (level == null) return vec31;
        if (!level.getBlockState(this.irritatePos.north()).isAir()
            && !level.getBlockState(this.irritatePos.south()).isAir()
            && !level.getBlockState(this.irritatePos.east()).isAir()
            && !level.getBlockState(this.irritatePos.west()).isAir()) {
            return vec31.add(0, 0, 0);
        }
        Vec2 vec2 = new Vec2((float) (vec32.z - vec31.z), (float) (vec32.x - vec31.x));
        if (vec2.x == 0) return vec31.add(vec2.y > 0 ? 0.49F : -0.49F, 0, 0);
        if (vec2.y == 0) return vec31.add(0, 0, vec2.x > 0 ? 0.49F : -0.49F);
        float k = vec2.y / vec2.x;
        float x = vec2.x > 0 ? 0.49F : -0.49F;
        float y = vec2.y > 0 ? 0.49F : -0.49F;
        if (y / k < 0.5 && y / k > -0.5) {
            return vec31.add(y, 0, y / k);
        }
        if (k * x < 0.5 && k * x > -0.5) {
            return vec31.add(k * x, 0, x);
        }
        this.surfaceVec3Hash = vec31.hashCode() + vec32.hashCode();
        this.surfaceVec3 = vec31;
        return vec31;
    }

    /**
     * 设置照射坐标
     */
    public boolean setIrritatePos(BlockPos pos) {
        this.irritatePos = pos;
        this.setChanged();
        return this.validatePos(pos).isWorking();
    }

    private WorkResult validatePos(@Nullable BlockPos irritatePos) {
        this.normalVector3f = new Vector3f();
        if (level == null) return WorkResult.UNKNOWN;
        if (level.isClientSide() && Minecraft.getInstance().player == null) return WorkResult.UNKNOWN;
        if (irritatePos == null) return WorkResult.UNSPECIFIED_IRRADIATION_BLOCK;
        if (getBlockPos().getCenter().distanceTo(irritatePos.getCenter()) > 64) {
            return WorkResult.TOO_FAR;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    if (dy == 0) {
                        BlockPos pos = getBlockPos().offset(dx, dy, dz);
                        if (level.getBlockState(pos).is(ModBlocks.HELIOSTATS)) {
                            return WorkResult.ADJACENT_HELIOSTATS;
                        }
                    }
                    if (dy == 1) {
                        BlockPos pos = getBlockPos().offset(dx, dy, dz);
                        BlockState bs = level.getBlockState(pos);
                        if (!bs.isAir() && !(bs.getBlock() instanceof HalfTransparentBlock)) {
                            return WorkResult.OBSCURED;
                        }
                    }
                }
            }
        }
        if (level.isRainingAt(getBlockPos().above())
            || level.getBrightness(LightLayer.SKY, getBlockPos().above()) != 15
        ) {
            return WorkResult.NO_SUN;
        }
        Vec3 irritateVec3 =
            this.getSurfaceVec3(irritatePos.getCenter(), getBlockPos().getCenter());
        BlockHitResult blockHitResult = level.clip(new ClipContext(
            getBlockPos().getCenter().add(0F, 1.376F, 0F),
            irritateVec3,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            level.isClientSide()
                ? Objects.requireNonNull(Minecraft.getInstance().player)
                : AnvilCraftFakePlayers.anvilcraftBlockPlacer.getPlayer())
        );
        if (!blockHitResult.getBlockPos().equals(irritatePos)) {
            return WorkResult.OBSCURED;
        }
        double sunAngle = Util.getSunAngle(level, this.worldPosition.getBottomCenter());
        sunAngle = sunAngle <= Math.PI / 2 * 3 ? (sunAngle + Math.PI / 2) : (sunAngle - Math.PI / 2 * 3);
        if (sunAngle > Math.PI) return WorkResult.NO_SUN;
        Vector3f sunVector3f = new Vector3f((float) Math.cos(sunAngle), (float) Math.sin(sunAngle), 0).normalize();
        this.irritateVector3f = new Vector3f(
            (float) (irritateVec3.x - getBlockPos().getX()),
            (float) (irritateVec3.y - getBlockPos().getY()),
            (float) (irritateVec3.z - getBlockPos().getZ())
        ).normalize();
        this.normalVector3f = sunVector3f.add(this.irritateVector3f).div(2);
        if (this.normalVector3f.y < 0) {
            return WorkResult.NO_ROTATION_ANGLE;
        }
        return WorkResult.SUCCESS;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Ix", this.irritatePos.getX());
        output.putInt("Iy", this.irritatePos.getY());
        output.putInt("Iz", this.irritatePos.getZ());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (input.getInt("Ix").isEmpty()) return;
        int x = input.getIntOr("Ix", 0);
        int y = input.getIntOr("Iy", 0);
        int z = input.getIntOr("Iz", 0);
        this.irritatePos = new BlockPos(x, y, z);
    }

    /**
     * tick
     */
    public void tick() {
        if (level == null) return;
        if (level.getGameTime() % (AnvilCraft.CONFIG.heliostatsDetectionInterval + 1) != 0) return;
        if (this.irritatePos == null && level.isClientSide()) {
            ClientPacketDistributor.sendToServer(new HeliostatsIrradiationPacket(getBlockPos(), this.irritatePos));
        }
        this.workResult = this.validatePos(this.irritatePos);
        if (this.workResult.isWorking()) {
            HeaterManager.addProducer(getBlockPos(), Objects.requireNonNull(getLevel()), ModHeaterInfos.HELIOSTATS);
        } else {
            HeaterManager.removeProducer(getBlockPos(), Objects.requireNonNull(getLevel()), ModHeaterInfos.HELIOSTATS);
        }
    }

    public enum WorkResult {
        SUCCESS(""),
        NO_ROTATION_ANGLE("tooltip.anvilcraft.heliostats.no_rotation_angle"),
        NO_SUN("tooltip.anvilcraft.heliostats.no_sun"),
        OBSCURED("tooltip.anvilcraft.heliostats.obscured"),
        ADJACENT_HELIOSTATS("tooltip.anvilcraft.heliostats.adjacent_heliostats"),
        TOO_FAR("tooltip.anvilcraft.heliostats.too_far"),
        UNSPECIFIED_IRRADIATION_BLOCK("tooltip.anvilcraft.heliostats.unspecified_irradiation_block"),
        UNKNOWN("tooltip.anvilcraft.heliostats.unknown");

        private final String key;

        WorkResult(String key) {
            this.key = key;
        }

        public String getTranslateKey() {
            return this.key;
        }

        public boolean isWorking() {
            return this == SUCCESS;
        }
    }
}
