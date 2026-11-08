package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidContainerLookup;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Set;
import java.util.stream.Collectors;

/// 装载一个 16 桶流体储罐的矿车
public class FluidTankMinecartEntity extends AbstractMinecart implements IFluidResourceHandlerHolder {
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

    private final FluidStackResourceHandler tank = new FluidStackResourceHandler(FluidTankMinecartEntity.CAPACITY);
    private Set<BlockPos> fluidNetworkPositions = Set.of();
    private FluidStack lastSyncedFluid = FluidStack.EMPTY;

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
        builder.define(FluidTankMinecartEntity.FLUID_ID, -1).define(FluidTankMinecartEntity.FLUID_AMOUNT, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved() || this.level().isClientSide()) return;
        this.syncFluidData();
        this.updateFluidNetworkRegistration();
    }

    /// 矿车覆盖的单元改变时更新登记，跨格矿车保留每个管道入口
    private void updateFluidNetworkRegistration() {
        var box = this.getBoundingBox();
        Set<BlockPos> positions = BlockPos.betweenClosedStream(
            Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
            Mth.floor(Math.nextDown(box.maxX)), Mth.floor(Math.nextDown(box.maxY)), Mth.floor(Math.nextDown(box.maxZ)))
            .map(BlockPos::immutable).collect(Collectors.toSet());
        if (positions.equals(this.fluidNetworkPositions)) {
            return;
        }
        for (BlockPos pos : this.fluidNetworkPositions) {
            if (!positions.contains(pos)) {
                this.unregisterPosition(pos);
            }
        }
        for (BlockPos pos : positions) {
            if (!this.fluidNetworkPositions.contains(pos)) {
                FluidNetworkManager.INSTANCE.addContainer(this.level(), pos);
            }
        }
        this.fluidNetworkPositions = positions;
    }

    private void unregisterPosition(BlockPos pos) {
        if (!this.level().isLoaded(pos)) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level(), pos);
            return;
        }
        boolean occupied = FluidContainerLookup.findAll(this.level(), pos, null).stream().anyMatch(result -> result.entity() != this);
        for (Direction side : Direction.values()) {
            if (occupied) {
                break;
            }
            occupied = FluidContainerLookup.findAll(this.level(), pos, side).stream().anyMatch(result -> result.entity() != this);
        }
        if (occupied) {
            FluidNetworkManager.INSTANCE.addContainer(this.level(), pos);
        } else {
            FluidNetworkManager.INSTANCE.removeContainer(this.level(), pos);
        }
    }

    private void unregisterFromFluidNetwork() {
        if (this.level().isClientSide()) {
            return;
        }
        for (BlockPos pos : this.fluidNetworkPositions) {
            this.unregisterPosition(pos);
        }
        this.fluidNetworkPositions = Set.of();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.unregisterFromFluidNetwork();
        super.remove(reason);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = super.interact(player, hand, location);
        if (result.consumesAction()) return result;
        boolean success;
        try (Transaction transaction = Transaction.openRoot()) {
            success = FluidUtil.interactWithFluidHandler(player, hand, this.blockPosition(), this.tank, transaction);
            if (success) transaction.commit();
        }
        if (!success) return InteractionResult.PASS;
        this.syncFluidData();
        return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    protected Vec3 applyNaturalSlowdown(Vec3 movement) {
        // 与原版容器矿车一致：装载越多，摩擦越大
        int signal = this.getComparatorLevel();
        float friction = 0.98F + (15 - signal) * 0.001F;
        Vec3 newMovement = movement.multiply(friction, 0.0, friction);
        return this.isInWater() ? newMovement.scale(0.95F) : newMovement;
    }

    @Override
    public boolean isRideable() {
        return false;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.FLUID_TANK.getDefaultState();
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
    protected void destroy(ServerLevel level, DamageSource source) {
        this.kill(level);
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            this.spawnAtLocation(level, this.createDropStack());
        }
    }

    public ItemStack createDropStack() {
        return this.createItemStack(true);
    }

    private ItemStack createItemStack(boolean includeFluid) {
        ItemStack stack = new ItemStack(ModItems.FLUID_TANK_MINECART.get());
        FluidStack fluid = this.tank.getStack();
        if (includeFluid && !fluid.isEmpty()) {
            TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                this.registryAccess()
            );
            this.tank.serialize(output.child(FluidTankMinecartEntity.TAG_TANK));
            BlockItem.setBlockEntityData(stack, ModBlockEntities.FLUID_TANK.get(), output);
        }
        if (this.getCustomName() != null) {
            stack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        }
        return stack;
    }

    /// 从矿车物品的方块实体数据里恢复罐内流体
    public void loadTankFromItem(ItemStack stack) {
        TypedEntityData<?> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return;
        data.copyTagWithoutId().getCompound(FluidTankMinecartEntity.TAG_TANK).ifPresent(tankTag -> {
            this.loadTank(tankTag);
            this.syncFluidData();
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.tank.serialize(output.child(FluidTankMinecartEntity.TAG_TANK));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.tank.deserialize(input.childOrEmpty(FluidTankMinecartEntity.TAG_TANK));
        this.syncFluidData();
    }

    private void loadTank(CompoundTag tag) {
        this.tank.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, this.registryAccess(), tag));
    }

    /// 罐内流体变化时同步一份轻量的流体 id + 数量给客户端，用于渲染
    private void syncFluidData() {
        if (this.level().isClientSide()) return;
        FluidStack fluid = this.tank.getStack();
        if (FluidStack.matches(fluid, this.lastSyncedFluid)) return;
        this.lastSyncedFluid = fluid.copy();
        this.entityData.set(FluidTankMinecartEntity.FLUID_ID, fluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluid.getFluid()));
        this.entityData.set(FluidTankMinecartEntity.FLUID_AMOUNT, fluid.getAmount());
    }

    /// 按罐内存量给出的比较器强度，同时用于摩擦计算
    public int getComparatorLevel() {
        int amount = this.tank.getStack().getAmount();
        return amount == 0 ? 0 : Mth.floor(amount * 14.0F / FluidTankMinecartEntity.CAPACITY) + 1;
    }

    /// 获取用于渲染的流体内容；客户端读取同步数据，服务端直接读罐
    public FluidStack getSyncedFluid() {
        if (!this.level().isClientSide()) return this.tank.getStack();
        int id = this.entityData.get(FluidTankMinecartEntity.FLUID_ID);
        int amount = this.entityData.get(FluidTankMinecartEntity.FLUID_AMOUNT);
        if (id < 0 || amount <= 0) return FluidStack.EMPTY;
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        if (fluid == Fluids.EMPTY) return FluidStack.EMPTY;
        return new FluidStack(fluid, Math.min(amount, FluidTankMinecartEntity.CAPACITY));
    }

    @Override
    public FluidStackResourceHandler getFluidHandler() {
        return this.tank;
    }

    public int getFluidAmount() {
        return this.getSyncedFluid().getAmount();
    }

    public int getCapacity() {
        return FluidTankMinecartEntity.CAPACITY;
    }
}
