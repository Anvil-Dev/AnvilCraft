package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.ChargeCollectorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.network.ChargeCollectorIncomingChargePacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class ChargeCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange {
    private static final double MAX_POWER_PER_INCOMING = 128;
    private static final int CHARGE_HISTORY_SIZE = 10;
    public static final int INPUT_COOLDOWN = 2;
    public static final int OUTPUT_COOLDOWN = 10;

    private int inputCooldownCount = 2;
    private final List<Integer> charges = new LinkedList<>() {
        {
            for (int i = 0; i < CHARGE_HISTORY_SIZE; i++) {
                this.add(0);
            }
        }

        @Override
        public boolean add(Integer integer) {
            if (this.size() >= CHARGE_HISTORY_SIZE) this.removeFirst();
            return super.add(integer);
        }
    };
    private int outputCooldownCount = 10;
    private double chargeCount = 0;
    private PowerGrid grid = null;
    private int power = 0;
    private long lastIncomingParticleTick = Long.MIN_VALUE;
    @Getter
    private int time = 0;
    @Getter
    private float rotation = 0;

    public static ChargeCollectorBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new ChargeCollectorBlockEntity(type, pos, blockState);
    }

    public ChargeCollectorBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CHARGE_COLLECTOR.get(), pos, blockState);
    }

    private ChargeCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getRange() {
        return 2;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public int getOutputPower() {
        return this.power;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inputCooldownCount = tag.getInt("InputCooldownCount");
        this.outputCooldownCount = tag.getInt("OutputCooldownCount");
        this.chargeCount = tag.getDouble("ChargeCount");
        this.power = tag.getInt("Power");
        int[] charges = tag.getIntArray("Charges");
        this.charges.clear();
        for (int i : charges) {
            this.charges.add(i);
        }
        while (this.charges.size() < CHARGE_HISTORY_SIZE) {
            this.charges.add(0);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("InputCooldownCount", this.inputCooldownCount);
        tag.putInt("OutputCooldownCount", this.outputCooldownCount);
        tag.putDouble("ChargeCount", this.chargeCount);
        tag.putInt("Power", this.power);
        tag.putIntArray("Charges", this.charges);
    }

    @Override
    public void gridTick() {
        if (level == null || level.isClientSide()) return;
        if (this.inputCooldownCount-- <= 1) {
            this.inputCooldownCount = INPUT_COOLDOWN;
            this.charges.add((int) Math.floor(this.chargeCount));
            this.chargeCount = 0;
            this.time++;
        }
        if (this.outputCooldownCount-- <= 1) {
            this.outputCooldownCount = OUTPUT_COOLDOWN;
            final int oldPower = this.power;
            this.power = 0;
            for (Integer charge : this.charges) {
                this.power += charge;
            }
            this.power = this.power / this.charges.size();
            if (this.power > 0 && this.getBlockState().getBlock() instanceof ChargeCollectorBlock chargeCollector) {
                chargeCollector.activate(this.level, this.getBlockPos(), this.getBlockState());
            }
            if (this.power != oldPower && this.grid != null) this.grid.markChanged();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.level != null) ChargeCollectorManager.removeChargeCollector(this.level, this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null) ChargeCollectorManager.removeChargeCollector(this.level, this);
    }

    /**
     * 向集电器添加电荷
     *
     * @param num 添加至收集器的电荷数
     * @return 溢出的电荷数(即未被添加至收集器的电荷数)
     */
    public double incomingCharge(double num, BlockPos srcPos) {
        if (!(num > 0)) return num;
        double available = Math.max(0, MAX_POWER_PER_INCOMING - this.chargeCount);
        double acceptableChargeCount = Math.min(num, available);
        double overflow = num - acceptableChargeCount;
        if (acceptableChargeCount <= 0) return overflow;
        if (this.level instanceof ServerLevel serverLevel) {
            long gameTime = serverLevel.getGameTime();
            if (this.lastIncomingParticleTick != gameTime) {
                this.lastIncomingParticleTick = gameTime;
                PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    new ChunkPos(this.worldPosition),
                    new ChargeCollectorIncomingChargePacket(
                        srcPos,
                        this.worldPosition,
                        acceptableChargeCount
                    )
                );
            }
        }
        this.chargeCount += acceptableChargeCount;
        return overflow;
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(this.getBlockPos().getCenter(), 5, 5, 5);
    }

    public void clientTick() {
        this.rotation += (float) (Math.log(this.getServerPower() + 1) * 2.5);
    }
}
