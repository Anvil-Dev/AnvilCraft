package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class IonocraftEntity extends VehicleEntity {
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION = new DynamicPowerComponent.PowerConsumption(2);
    private static final double ASCENT_GAP = 0.01;
    private static final int ASCENT_DELAY_TICKS = 4;
    private final DynamicPowerComponent component;
    private int ascentDelay;

    public IonocraftEntity(Level level, Vec3 pos) {
        super(ModEntities.IONOCRAFT.get(), level);
        this.setPos(pos);
        this.xo = pos.x;
        this.yo = pos.y;
        this.zo = pos.z;
        this.component = new DynamicPowerComponent(this, this::getPowerSupplyingBoundingBox);
        this.component.getPowerConsumptions().add(IonocraftEntity.CONSUMPTION);
    }

    public IonocraftEntity(EntityType<IonocraftEntity> type, Level level) {
        super(type, level);
        this.component = new DynamicPowerComponent(this, this::getPowerSupplyingBoundingBox);
        this.component.getPowerConsumptions().add(IonocraftEntity.CONSUMPTION);
    }

    public AABB getPowerSupplyingBoundingBox() {
        return this.getBoundingBox().inflate(0.5);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return ModItems.IONOCRAFT.asStack();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void tick() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.8, 0.8));
        this.updateAscentDelay();
        if (!this.level().isClientSide()) {
            PowerGrid powerGrid = PowerGrid.findPowerGridContains(this.level(), this.getPowerSupplyingBoundingBox()).orElse(null);
            PowerGrid findSmaller = PowerGrid.findPowerGridContains(this.level(), this.getBoundingBox()).orElse(null);
            this.component.switchTo(powerGrid);
            if (findSmaller == null && powerGrid != null) {
                if (!(this.component.getPowerGrid() != null && this.component.getPowerGrid().isWorking())) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01, 0));
                }
            } else {
                if (this.component.getPowerGrid() != null && this.component.getPowerGrid().isWorking()) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.04, 0));
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01, 0));
                }
            }
            if (powerGrid != null) {
                this.limitGridAscent(powerGrid.getComponents().stream().map(IPowerComponent::getShape));
            }
        } else {
            this.clientCompute();
        }
        if (this.ascentDelay > 0 && this.getDeltaMovement().y > 0) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, 0, motion.z);
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        super.tick();
        List<Entity> list = this.level().getEntities(
            this,
            this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F),
            EntitySelector.pushableBy(this)
        );
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (!entity.hasPassenger(this)) {
                    this.push(entity);
                }
            }
        }
    }

    private void updateAscentDelay() {
        if (this.ascentDelay == 0) {
            return;
        }
        AABB box = this.getBoundingBox();
        double ascent = getBlockAscent(box, ASCENT_GAP,
            this.level().getBlockCollisions(this, box.expandTowards(0, ASCENT_GAP * 2, 0)));
        this.ascentDelay = ascent > 1.0E-7 ? this.ascentDelay - 1 : ASCENT_DELAY_TICKS;
    }

    private Optional<SimplePowerGrid> clientFindPowerGridContains(AABB aabb) {
        Collection<SimplePowerGrid> powerGrids = PowerGridSupport.getGridMap().values();
        for (SimplePowerGrid it : powerGrids) {
            if (it.collideFast(aabb)) {
                return Optional.of(it);
            }
        }
        return Optional.empty();
    }

    private void clientCompute() {
        SimplePowerGrid powerGrid = this.clientFindPowerGridContains(this.getPowerSupplyingBoundingBox()).orElse(null);
        SimplePowerGrid findSmaller = this.clientFindPowerGridContains(this.getBoundingBox()).orElse(null);
        if (findSmaller == null && powerGrid != null) {
            if (powerGrid.isOverloaded()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01, 0));
            }
        } else {
            if (powerGrid != null && !powerGrid.isOverloaded()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.04, 0));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01, 0));
            }
        }
        if (powerGrid != null) {
            this.limitGridAscent(powerGrid.getPowerComponentInfoList().stream().map(PowerComponentInfo::boundingBox));
        }
    }

    private void limitGridAscent(Stream<AABB> ranges) {
        Vec3 motion = this.getDeltaMovement();
        if (motion.y > 0) {
            AABB box = this.getBoundingBox().expandTowards(motion.x, 0, motion.z);
            this.setDeltaMovement(motion.x, getGridAscent(box, motion.y, ranges), motion.z);
        }
    }

    static double getGridAscent(AABB box, double ascent, Stream<AABB> ranges) {
        AABB probe = new AABB(box.minX, box.maxY, box.minZ, box.maxX, box.maxY + ascent + ASCENT_GAP, box.maxZ);
        VoxelShape uncovered = Shapes.create(probe);
        for (AABB range : ranges.filter(probe::intersects).toList()) {
            uncovered = Shapes.join(uncovered, Shapes.create(range), BooleanOp.ONLY_FIRST);
            if (uncovered.isEmpty()) {
                return ascent;
            }
        }
        return Math.max(0, Math.min(ascent, uncovered.min(Direction.Axis.Y) - box.maxY - ASCENT_GAP));
    }

    static double getBlockAscent(AABB box, double ascent, Iterable<VoxelShape> collisions) {
        return Math.max(0, Shapes.collide(Direction.Axis.Y, box, collisions, ascent + ASCENT_GAP) - ASCENT_GAP);
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
        if (motion.y > 0) {
            AABB box = this.getBoundingBox().expandTowards(motion.x, 0, motion.z);
            double ascent = getBlockAscent(box, motion.y,
                this.level().getBlockCollisions(this, box.expandTowards(0, motion.y + ASCENT_GAP, 0)));
            if (ascent < motion.y - 1.0E-7) {
                this.ascentDelay = ASCENT_DELAY_TICKS;
                motion = new Vec3(motion.x, ascent, motion.z);
                Vec3 velocity = this.getDeltaMovement();
                this.setDeltaMovement(velocity.x, Math.min(velocity.y, ascent), velocity.z);
            }
        }
        double previousY = this.getY();
        super.move(type, motion);
        double movedY = this.getY() - previousY;
        if (movedY == 0) {
            return;
        }
        List<Entity> list = this.level().getEntities(
            this,
            this.getBoundingBox().expandTowards(0, 1F, 0),
            EntitySelector.pushableBy(this)
        );
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (entity instanceof IonocraftEntity) {
                    continue;
                }
                entity.setDeltaMovement(
                    entity.getDeltaMovement().x,
                    entity.getDeltaMovement().y > 0 ? movedY : entity.getDeltaMovement().y + movedY * 2.5,
                    entity.getDeltaMovement().z
                );
            }
        }
    }

    @Override
    protected Item getDropItem() {
        return ModItems.IONOCRAFT.asItem();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput compound) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput compound) {

    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.component.switchTo(null);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return AbstractBoat.canVehicleCollide(this, entity);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }
}
