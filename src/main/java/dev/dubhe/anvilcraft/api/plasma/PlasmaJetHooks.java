package dev.dubhe.anvilcraft.api.plasma;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import dev.dubhe.anvilcraft.api.heat.HeaterInfo;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 等离子喷流附属扩展点。燃料查询与生成检查在服务器线程调用；粒子仅在客户端调用。
 */
public final class PlasmaJetHooks {
    private static final MapCodec<PlasmaJetsBlockEntity> ADDON_WRITER = new MapCodec<>() {
        @Override
        public <T> RecordBuilder<T> encode(PlasmaJetsBlockEntity jet, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            CompoundTag tag = jet.getAddonData();
            if (!BEHAVIORS.isEmpty()) {
                if (!(ops instanceof RegistryOps<?> registryOps)
                    || !(registryOps.lookupProvider instanceof RegistryOps.HolderLookupAdapter adapter)) {
                    return prefix.withErrorsFrom(DataResult.error(() -> "Plasma addon serialization requires registry context"));
                }
                PlasmaJetHooks.save(jet, tag, adapter.lookupProvider);
            }
            if (!tag.isEmpty()) prefix.add("addon", CompoundTag.CODEC.encodeStart(ops, tag));
            return prefix;
        }

        @Override
        public <T> DataResult<PlasmaJetsBlockEntity> decode(DynamicOps<T> ops, MapLike<T> input) {
            return DataResult.error(() -> "Plasma addon writer cannot decode entities");
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("addon"));
        }
    };
    private static final List<Predicate<BlockState>> PASS_THROUGH = new CopyOnWriteArrayList<>();
    private static final List<PlasmaJetFuelHandler> FUELS = new CopyOnWriteArrayList<>();
    private static final List<PlasmaJetBehavior> BEHAVIORS = new CopyOnWriteArrayList<>();

    private PlasmaJetHooks() {
    }

    public static void registerPassThrough(Predicate<BlockState> predicate) {
        PASS_THROUGH.add(predicate);
    }

    public static void registerFuel(PlasmaJetFuelHandler handler) {
        FUELS.add(handler);
    }

    public static void registerBehavior(PlasmaJetBehavior behavior) {
        BEHAVIORS.add(behavior);
    }

    public static boolean isPassThrough(BlockState state) {
        for (Predicate<BlockState> predicate : PASS_THROUGH) {
            if (predicate.test(state)) return true;
        }
        return false;
    }

    public static boolean isIgnitedFuel(Level level, BlockPos pos) {
        for (PlasmaJetFuelHandler handler : FUELS) {
            TriState state = handler.isIgnitedFuel(level, pos);
            if (state == TriState.TRUE) return true;
            if (state == TriState.FALSE) return false;
        }
        return false;
    }

    public static @Nullable Boolean isValidBaseOverride(Level level, BlockPos pos) {
        for (PlasmaJetFuelHandler handler : FUELS) {
            TriState state = handler.isValidBase(level, pos);
            if (state == TriState.TRUE) return true;
            if (state == TriState.FALSE) return false;
        }
        return null;
    }

    public static OptionalInt tryConsumeOnceOverride(Level level, BlockPos pos, boolean simulate) {
        for (PlasmaJetFuelHandler handler : FUELS) {
            OptionalInt extraDuration = handler.tryConsumeOnce(level, pos, simulate);
            if (extraDuration.isPresent()) return extraDuration;
        }
        return OptionalInt.empty();
    }

    public static @Nullable Integer fuelAmount(Level level, BlockPos pos) {
        for (PlasmaJetFuelHandler handler : FUELS) {
            Integer amount = handler.fuelAmount(level, pos);
            if (amount != null) return amount;
        }
        return null;
    }

    public static void onServerTickHead(PlasmaJetsBlockEntity jet, ServerLevel level) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            behavior.onServerTickHead(jet, level);
        }
    }

    public static void onServerTickTail(PlasmaJetsBlockEntity jet, ServerLevel level) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            behavior.onServerTickTail(jet, level);
        }
    }

    public static boolean shouldStopRaising(PlasmaJetsBlockEntity jet) {
        if (jet.getLevel() != null && isPassThrough(jet.getLevel().getBlockState(jet.getBlockPos().above()))) {
            return true;
        }
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            if (behavior.shouldStopRaising(jet)) return true;
        }
        return false;
    }

    public static void afterRaise(PlasmaJetsBlockEntity from, PlasmaJetsBlockEntity to) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            behavior.afterRaise(from, to);
        }
    }

    public static boolean keepInitialJet(PlasmaJetsBlockEntity jet, Level level) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            if (behavior.keepInitialJet(jet, level)) return true;
        }
        return false;
    }

    public static HeaterInfo<?> heatInfo(PlasmaJetsBlockEntity jet, HeaterInfo<?> ordinary) {
        HeaterInfo<?> result = ordinary;
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            result = behavior.heatInfo(jet, result);
        }
        return result;
    }

    public static float modifyDamage(PlasmaJetsBlockEntity jet, float ordinary) {
        float result = ordinary;
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            result = behavior.modifyDamage(jet, result);
        }
        return result;
    }

    public static boolean refreshDuration(PlasmaJetsBlockEntity jet, Level level) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            if (behavior.refreshDuration(jet, level)) return true;
        }
        return false;
    }

    public static ParticleOptions particle(PlasmaJetsBlockEntity jet, ParticleOptions ordinary) {
        ParticleOptions result = ordinary;
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            result = behavior.particle(jet, result);
        }
        return result;
    }

    public static void extraParticles(PlasmaJetsBlockEntity jet, ClientLevel level) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            behavior.extraParticles(jet, level);
        }
    }

    public static void onRemoved(PlasmaJetsBlockEntity jet) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            behavior.onRemoved(jet);
        }
    }

    public static void save(PlasmaJetsBlockEntity jet, CompoundTag tag, HolderLookup.Provider registries) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            behavior.save(jet, tag, registries);
        }
    }

    public static void saveToValue(PlasmaJetsBlockEntity jet, ValueOutput output) {
        output.store(ADDON_WRITER, jet);
    }

    public static void load(PlasmaJetsBlockEntity jet, CompoundTag tag, HolderLookup.Provider registries) {
        for (PlasmaJetBehavior behavior : BEHAVIORS) {
            behavior.load(jet, tag, registries);
        }
    }
}
