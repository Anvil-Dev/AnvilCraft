package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class WeaponBeamEntity extends Entity {
    public static final int CORRUPTED = 0;
    public static final int TESLA = 1;
    public static final int LASER = 2;
    private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(
        WeaponBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(
        WeaponBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(
        WeaponBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(
        WeaponBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STRENGTH = SynchedEntityData.defineId(
        WeaponBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
        WeaponBeamEntity.class, EntityDataSerializers.INT);

    public WeaponBeamEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static WeaponBeamEntity create(Level level, Vec3 start, Vec3 end, int style) {
        return create(level, start, end, style, 1, null);
    }

    public static WeaponBeamEntity create(Level level, Vec3 start, Vec3 end, int style, int strength) {
        return create(level, start, end, style, strength, null);
    }

    public static WeaponBeamEntity create(
        Level level,
        Vec3 start,
        Vec3 end,
        int style,
        int strength,
        @Nullable Entity owner
    ) {
        WeaponBeamEntity beam = new WeaponBeamEntity(ModEntities.WEAPON_BEAM.get(), level);
        beam.refresh(start, end, style, strength, owner);
        return beam;
    }

    public static void showContinuous(
        Level level,
        Vec3 start,
        Vec3 end,
        int style,
        int strength,
        Entity owner
    ) {
        List<WeaponBeamEntity> beams = level.getEntitiesOfClass(
            WeaponBeamEntity.class,
            owner.getBoundingBox().expandTowards(owner.getDeltaMovement().scale(-1.0)).inflate(3.0),
            beam -> beam.getStyle() == style && beam.getOwnerId() == owner.getId()
        );
        if (beams.isEmpty()) {
            level.addFreshEntity(create(level, start, end, style, strength, owner));
            return;
        }

        WeaponBeamEntity beam = beams.getFirst();
        beam.refresh(start, end, style, strength, owner);
        for (int i = 1; i < beams.size(); i++) beams.get(i).discard();
    }

    private void refresh(Vec3 start, Vec3 end, int style, int strength, @Nullable Entity owner) {
        this.tickCount = 0;
        this.setPos(start);
        Vec3 offset = end.subtract(start);
        this.entityData.set(END_X, (float) offset.x);
        this.entityData.set(END_Y, (float) offset.y);
        this.entityData.set(END_Z, (float) offset.z);
        this.entityData.set(STYLE, style);
        this.entityData.set(STRENGTH, strength);
        this.entityData.set(OWNER_ID, owner == null ? -1 : owner.getId());
    }

    public Vec3 getEndOffset() {
        return new Vec3(this.entityData.get(END_X), this.entityData.get(END_Y), this.entityData.get(END_Z));
    }

    public int getStyle() {
        return this.entityData.get(STYLE);
    }

    public int getStrength() {
        return this.entityData.get(STRENGTH);
    }

    public @Nullable Entity getOwner() {
        int ownerId = this.getOwnerId();
        return ownerId < 0 ? null : this.level().getEntity(ownerId);
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER_ID);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.getStyle() != TESLA) return;
        if (this.tickCount > (this.getStyle() == TESLA ? 5 : 2)) this.discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(END_X, 0.0F)
            .define(END_Y, 0.0F)
            .define(END_Z, 0.0F)
            .define(STYLE, CORRUPTED)
            .define(STRENGTH, 1)
            .define(OWNER_ID, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }
}
