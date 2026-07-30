package dev.dubhe.anvilcraft.block.entity.heatable;

import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.heatable.HeatableBlock;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

public abstract class HeatableBlockEntity extends BlockEntity {
    protected static final StreamCodec<ByteBuf, BlockPos> POS_STREAM_CODEC = ByteBufCodecs.VAR_LONG
        .map(BlockPos::of, BlockPos::asLong);
    protected static final int MAX_DURATION = 1200 * 20;
    @Getter
    protected int duration = 0;

    protected HeatableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /// 增加1秒
    public void addDuration(int second) {
        this.addDurationInTick(second * 20);
    }

    public void addDurationInTick(int tick) {
        this.setDuration(Math.clamp(this.duration + tick, -1, MAX_DURATION));
    }

    public void setDuration(int duration) {
        this.duration = duration;
        this.setChanged();
    }

    @RemoteCallable(validator = Validator.class)
    public static int getDuration(@CallableParam(clazz = HeatableBlockEntity.class, field = "POS_STREAM_CODEC") BlockPos pos) {
        ServerLevel level = Validator.LEVEL.get();
        Validator.LEVEL.remove();
        if (level == null) return -1;
        return Util.castSafely(level.getBlockEntity(pos), HeatableBlockEntity.class)
            .map(HeatableBlockEntity::getDuration)
            .orElse(-1);
    }

    public int getSignal() {
        if (this.duration == MAX_DURATION) return 15;
        if (this.duration == 0) return 0;
        return (int) Math.ceil((double) this.duration / MAX_DURATION * 14);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("duration", this.duration);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.duration = input.getIntOr("duration", 0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        HeaterManager.addHeatableBlock(this.getBlockPos(), Objects.requireNonNull(this.getLevel()));
    }

    public static void tick(Level level, BlockPos pos) {
        HeaterManager.addHeatableBlock(pos, level);
    }

    public Optional<BlockState> getPrevTier(Level level, BlockPos pos) {
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof HeatableBlock heatable)) return Optional.empty();
        return heatable.getPrevTier(level, pos, state);
    }

    private static class Validator implements IRemoteCallableValidator {
        private static final ThreadLocal<@Nullable ServerLevel> LEVEL = new ThreadLocal<>();

        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            if (ctx.flow() != PacketFlow.SERVERBOUND) return false;
            Validator.LEVEL.set(Util.cast(ctx.player().level()));
            return true;
        }
    }
}
