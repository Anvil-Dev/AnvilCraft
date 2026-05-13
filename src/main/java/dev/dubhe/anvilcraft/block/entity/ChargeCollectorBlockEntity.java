package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.ChargeCollectorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.network.ChargeCollectorIncomingChargePacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class ChargeCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange {
    private static final double MAX_POWER_PER_INCOMING = 128;
    public static final int INPUT_COOLDOWN = 2;
    public static final int OUTPUT_COOLDOWN = 10;

    private int inputCooldownCount = 2;
    private final List<Integer> charges = new LinkedList<>() {
        {
            for (int i = 0; i < 10; i++) {
                this.add(0);
            }
        }

        @Override
        public boolean add(Integer integer) {
            if (this.size() > 10) this.removeFirst();
            return super.add(integer);
        }
    };
    private int outputCooldownCount = 10;
    private double chargeCount = 0;
    private PowerGrid grid = null;
    private int power = 0;
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inputCooldownCount = input.getIntOr("InputCooldownCount", 2);
        this.outputCooldownCount = input.getIntOr("OutputCooldownCount", 10);
        this.chargeCount = input.getDoubleOr("ChargeCount", 0);
        this.power = input.getIntOr("Power", 0);
        int[] charges = input.getIntArray("Charges").orElse(new int[0]);
        for (int i : charges) {
            this.charges.add(i);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("InputCooldownCount", this.inputCooldownCount);
        output.putInt("OutputCooldownCount", this.outputCooldownCount);
        output.putDouble("ChargeCount", this.chargeCount);
        output.putInt("Power", this.power);
        output.putIntArray("Charges", this.charges.stream().mapToInt(i -> i).toArray());
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

    /**
     * 向集电器添加电荷
     *
     * @param num 添加至收集器的电荷数
     * @return 溢出的电荷数(即未被添加至收集器的电荷数)
     */
    public double incomingCharge(double num, BlockPos srcPos) {
        double overflow = num - (MAX_POWER_PER_INCOMING - this.chargeCount);
        if (overflow < 0) {
            overflow = 0;
        }
        double acceptableChargeCount = num - overflow;
        PacketDistributor.sendToPlayersTrackingChunk(
            (ServerLevel) this.level,
            ChunkPos.containing(worldPosition),
            new ChargeCollectorIncomingChargePacket(
                srcPos,
                this.worldPosition,
                acceptableChargeCount
            )
        );
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
