package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** A minecart carrying one 16-bucket fluid tank. */
public class FluidTankMinecartEntity extends AbstractMinecart implements IFluidHandlerHolder {
    public static final int CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    private static final String TAG_TANK = "Tank";

    private static final EntityDataAccessor<Integer> FLUID_ID = SynchedEntityData.defineId(
        FluidTankMinecartEntity.class,
        EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> FLUID_AMOUNT = SynchedEntityData.defineId(
        FluidTankMinecartEntity.class,
        EntityDataSerializers.INT
    );

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            FluidTankMinecartEntity.this.syncFluidData();
        }
    };
    private BlockPos fluidNetworkPos;

    public FluidTankMinecartEntity(EntityType<? extends FluidTankMinecartEntity> type, Level level) {
        super(type, level);
    }

    public FluidTankMinecartEntity(
        EntityType<? extends FluidTankMinecartEntity> type,
        Level level,
        double x,
        double y,
        double z
    ) {
        super(type, level, x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLUID_ID, -1).define(FLUID_AMOUNT, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level().isClientSide && (FLUID_ID.equals(key) || FLUID_AMOUNT.equals(key))) {
            this.syncClientTank();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved() || this.level().isClientSide) return;
        this.updateFluidNetworkRegistration();
    }

    private void updateFluidNetworkRegistration() {
        BlockPos currentPos = BlockPos.containing(this.getBoundingBox().getCenter());
        if (currentPos.equals(this.fluidNetworkPos)) return;
        if (this.fluidNetworkPos != null) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level(), this.fluidNetworkPos);
        }
        this.fluidNetworkPos = currentPos.immutable();
        FluidNetworkManager.INSTANCE.addContainer(this.level(), this.fluidNetworkPos);
    }

    private void unregisterFromFluidNetwork() {
        if (this.fluidNetworkPos == null || this.level().isClientSide) return;
        FluidNetworkManager.INSTANCE.removeContainer(this.level(), this.fluidNetworkPos);
        this.fluidNetworkPos = null;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.unregisterFromFluidNetwork();
        super.remove(reason);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult result = super.interact(player, hand);
        if (result.consumesAction()) return result;
        if (this.level().isClientSide) {
            if (FluidHandlerWrapper.tryInteractWithBottle(player, hand, this.tank, this.level(), this.blockPosition())) {
                return InteractionResult.SUCCESS;
            }
            return FluidUtil.interactWithFluidHandler(player, hand, this.tank)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
        }
        if (FluidHandlerWrapper.tryInteractWithBottle(player, hand, this.tank, this.level(), this.blockPosition())) {
            return InteractionResult.CONSUME;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, this.tank)
            ? InteractionResult.CONSUME
            : InteractionResult.PASS;
    }

    @Override
    protected void applyNaturalSlowdown() {
        // Keep the same fill-dependent friction as AbstractMinecartContainer.
        int signal = this.getComparatorLevel();
        float friction = 0.98F + (15 - signal) * 0.001F;
        if (this.isInWater()) friction *= 0.95F;
        this.setDeltaMovement(this.getDeltaMovement().multiply(friction, 0.0, friction));
    }

    @Override
    public int getComparatorLevel() {
        int amount = this.tank.getFluidAmount();
        return amount == 0
            ? 0
            : Mth.floor((float) amount * 14.0F / this.tank.getCapacity()) + 1;
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        // Use a non-powered, non-ridable vanilla type for generic minecart hooks.
        return AbstractMinecart.Type.CHEST;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.FLUID_TANK.get().defaultBlockState();
    }

    @Override
    protected Item getDropItem() {
        return ModItems.FLUID_TANK_MINECART.get();
    }

    @Override
    public ItemStack getPickResult() {
        return this.createItemStack(false);
    }

    @Override
    protected void destroy(DamageSource source) {
        this.kill();
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(this.createDropStack());
        }
    }

    public ItemStack createDropStack() {
        return this.createItemStack(true);
    }

    private ItemStack createItemStack(boolean includeFluid) {
        ItemStack stack = new ItemStack(ModItems.FLUID_TANK_MINECART.get());
        CompoundTag tankData = new CompoundTag();
        net.minecraft.world.level.block.entity.BlockEntity.addEntityType(
            tankData,
            ModBlockEntities.FLUID_TANK.get()
        );
        if (includeFluid && !this.tank.isEmpty()) {
            tankData.put(TAG_TANK, this.tank.writeToNBT(this.registryAccess(), new CompoundTag()));
        }
        net.minecraft.world.item.BlockItem.setBlockEntityData(
            stack,
            ModBlockEntities.FLUID_TANK.get(),
            tankData
        );
        if (this.getCustomName() != null) {
            stack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        }
        return stack;
    }

    public void loadTankFromItem(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) return;
        CompoundTag tag = data.copyTag();
        if (tag.contains(TAG_TANK, CompoundTag.TAG_COMPOUND)) {
            this.tank.readFromNBT(this.registryAccess(), tag.getCompound(TAG_TANK));
            this.clampTank();
            this.syncFluidData();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(TAG_TANK, this.tank.writeToNBT(this.registryAccess(), new CompoundTag()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.tank.readFromNBT(this.registryAccess(), tag.getCompound(TAG_TANK));
        this.clampTank();
        this.syncFluidData();
    }

    private void clampTank() {
        if (this.tank.getFluidAmount() > this.tank.getCapacity()) {
            this.tank.getFluid().setAmount(this.tank.getCapacity());
        }
    }

    private void syncFluidData() {
        if (this.level().isClientSide) return;
        FluidStack fluid = this.tank.getFluid();
        this.entityData.set(FLUID_ID, fluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluid.getFluid()));
        this.entityData.set(FLUID_AMOUNT, fluid.getAmount());
    }

    private void syncClientTank() {
        int id = this.entityData.get(FLUID_ID);
        int amount = this.entityData.get(FLUID_AMOUNT);
        Fluid fluid = id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(id);
        this.tank.setFluid(fluid == null || fluid == Fluids.EMPTY || amount <= 0
            ? FluidStack.EMPTY
            : new FluidStack(fluid, Math.min(amount, this.tank.getCapacity())));
    }

    public FluidStack getSyncedFluid() {
        if (!this.level().isClientSide) return this.tank.getFluid();
        int id = this.entityData.get(FLUID_ID);
        int amount = this.entityData.get(FLUID_AMOUNT);
        Fluid fluid = id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(id);
        return fluid == null || fluid == Fluids.EMPTY || amount <= 0
            ? FluidStack.EMPTY
            : new FluidStack(fluid, Math.min(amount, this.tank.getCapacity()));
    }

    @Override
    public FluidTank getFluidHandler() {
        return this.tank;
    }

    public int getFluidAmount() {
        return this.tank.getFluidAmount();
    }

    public int getCapacity() {
        return this.tank.getCapacity();
    }
}
