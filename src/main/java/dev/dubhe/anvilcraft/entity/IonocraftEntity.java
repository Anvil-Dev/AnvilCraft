package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static net.minecraft.world.entity.vehicle.Boat.canVehicleCollide;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class IonocraftEntity extends VehicleEntity {
    public static final DynamicPowerComponent.PowerConsumption CONSUMPTION = new DynamicPowerComponent.PowerConsumption(16);
    private final DynamicPowerComponent component;

    public IonocraftEntity(Level level, Vec3 pos) {
        super(ModEntities.IONOCRAFT.get(), level);
        this.setPos(pos);
        this.xo = pos.x;
        this.yo = pos.y;
        this.zo = pos.z;
        component = new DynamicPowerComponent(this, this::getPowerSupplyingBoundingBox);
        component.getPowerConsumptions().add(CONSUMPTION);
    }

    public IonocraftEntity(EntityType<IonocraftEntity> type, Level level) {
        super(type, level);
        component = new DynamicPowerComponent(this, this::getPowerSupplyingBoundingBox);
        component.getPowerConsumptions().add(CONSUMPTION);
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
        if (!level().isClientSide) {
            PowerGrid powerGrid = PowerGrid.findPowerGridContains(level(), this.getPowerSupplyingBoundingBox()).orElse(null);
            PowerGrid findSmaller = PowerGrid.findPowerGridContains(level(), this.getBoundingBox()).orElse(null);
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
        } else {
            clientCompute();
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        super.tick();
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F), EntitySelector.pushableBy(this));
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (!entity.hasPassenger(this)) {
                    this.push(entity);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private Optional<SimplePowerGrid> clientFindPowerGridContains(AABB aabb) {
        Collection<SimplePowerGrid> powerGrids = PowerGridSupport.getGridMap().values();
        for (SimplePowerGrid it : powerGrids) {
            if (it.collideFast(aabb)) {
                return Optional.of(it);
            }
        }
        return Optional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    private void clientCompute() {
        SimplePowerGrid powerGrid = clientFindPowerGridContains(this.getPowerSupplyingBoundingBox()).orElse(null);
        SimplePowerGrid findSmaller = clientFindPowerGridContains(this.getBoundingBox()).orElse(null);
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
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
        super.move(type, motion);
        if (this.getDeltaMovement().y == 0) return;
        List<Entity> list = this.level().getEntities(
            this,
            this.getBoundingBox().expandTowards(0, 1F, 0),
            EntitySelector.pushableBy(this)
        );
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (entity instanceof IonocraftEntity) continue;
                entity.setDeltaMovement(
                    entity.getDeltaMovement().x,
                    entity.getDeltaMovement().y > 0 ? motion.y : entity.getDeltaMovement().y + motion.y * 2.5,
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
    protected void readAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.component.switchTo(null);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return canVehicleCollide(this, entity);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }
}
