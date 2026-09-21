package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
            for (String key : List.of("BurnTime", "CookTime", "CookTimeTotal")) move(source, runtime, key);
        }
        if (source.contains("ExtraData", Tag.TAG_COMPOUND)) {
            CompoundTag extra = source.getCompound("ExtraData");
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
            if (config.contains(key, Tag.TAG_ANY_NUMERIC) && config.getLong(key) >= 0) {
                config.putLong(key, capturedAt > 0 ? now + config.getLong(key) - capturedAt : now);
            }
        }
        if (config.contains("ExtraData", Tag.TAG_COMPOUND)) rebase(config.getCompound("ExtraData"), capturedAt, now);
    }

    static void restoreClock(CompoundTag config, StructureSnapshot snapshot, BlockPos local, long now) {
        rebase(config, snapshot.capturedAt(), now);
        CompoundTag extra = config.getCompound("ExtraData");
        if (extra.isEmpty()) return;
        int remaining = Math.max(extra.getInt("RemainingWaitingTime"), extra.getInt("RemainingSignalDuration"));
        if (snapshot.capturedAt() == 0) {
            for (var tick : snapshot.ticks()) {
                if (!tick.fluid() && tick.pos().equals(local)) remaining = Math.max(1, tick.delay());
            }
        }
        if (remaining > 0 && (snapshot.capturedAt() == 0 || !extra.contains("PhaseStartGameTime"))) {
            int duration = Math.max(remaining, extra.getInt("PhaseDuration"));
            long start = now - (duration - remaining);
            extra.putLong("PhaseStartGameTime", Math.max(0, start));
            extra.putInt("PhaseDuration", start >= 0 ? duration : remaining);
        }
    }
}
