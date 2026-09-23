package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

import java.util.List;

final class BlueprintRuntimeData {
    private static final List<String> FIELDS = List.of(
        "Cooldown", "TransferCooldown", "TimeLeft", "TimeTotalCache", "StartupCoolDown", "cd", "CooldownTicks",
        "PoweredBefore", "OutputSignal", "Powered", "crafting_ticks_remaining", "triggered", "ticks_since_song_started",
        "Rolling", "PreviousFaces", "Faces", "Output", "RollStart", "phase", "progress", "currentPlacementIndex"
    );

    private BlueprintRuntimeData() {
    }

    static CompoundTag take(BlockEntity entity, CompoundTag source) {
        CompoundTag runtime = new CompoundTag();
        for (String key : FIELDS) move(source, runtime, key);
        if (entity instanceof AbstractFurnaceBlockEntity) {
            for (String key : List.of("BurnTime", "CookTime", "CookTimeTotal",
                "lit_time_remaining", "lit_total_time", "cooking_time_spent", "cooking_total_time")) {
                move(source, runtime, key);
            }
        }
        if (source.get("ExtraData") instanceof CompoundTag) {
            CompoundTag extra = source.getCompoundOrEmpty("ExtraData");
            CompoundTag kept = new CompoundTag();
            for (String key : List.of("State", "Inputting", "PhaseStartGameTime", "PhaseDuration",
                "RemainingWaitingTime", "RemainingSignalDuration")) {
                move(extra, kept, key);
            }
            if (!kept.isEmpty()) runtime.put("ExtraData", kept);
        }
        if (entity instanceof PistonMovingBlockEntity) {
            for (String key : List.of("blockState", "facing", "progress", "extending", "source")) move(source, runtime, key);
        }
        return runtime;
    }

    private static void move(CompoundTag source, CompoundTag target, String key) {
        Tag value = source.get(key);
        if (value != null) target.put(key, value.copy());
        source.remove(key);
    }

    static void rebase(CompoundTag config, long capturedAt, long now) {
        for (String key : List.of("PhaseStartGameTime", "RollStart")) {
            if (config.get(key) instanceof NumericTag && config.getLongOr(key, 0L) >= 0) {
                config.putLong(key, capturedAt > 0 ? now + config.getLongOr(key, 0L) - capturedAt : now);
            }
        }
        if (config.get("ExtraData") instanceof CompoundTag) rebase(config.getCompoundOrEmpty("ExtraData"), capturedAt, now);
    }

    static void restoreClock(CompoundTag config, StructureSnapshot snapshot, BlockPos local, long now) {
        rebase(config, snapshot.capturedAt(), now);
        CompoundTag extra = config.getCompoundOrEmpty("ExtraData");
        if (extra.isEmpty()) return;
        int remaining = Math.max(extra.getIntOr("RemainingWaitingTime", 0), extra.getIntOr("RemainingSignalDuration", 0));
        if (snapshot.capturedAt() == 0) {
            for (var tick : snapshot.ticks()) {
                if (!tick.fluid() && tick.pos().equals(local)) remaining = Math.max(1, tick.delay());
            }
        }
        if (remaining > 0 && (snapshot.capturedAt() == 0 || !extra.contains("PhaseStartGameTime"))) {
            int duration = Math.max(remaining, extra.getIntOr("PhaseDuration", 0));
            long start = now - (duration - remaining);
            extra.putLong("PhaseStartGameTime", Math.max(0, start));
            extra.putInt("PhaseDuration", start >= 0 ? duration : remaining);
        }
    }
}
