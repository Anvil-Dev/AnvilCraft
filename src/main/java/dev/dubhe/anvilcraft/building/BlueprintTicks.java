package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickPriority;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class BlueprintTicks {
    public static final String KEY = "anvilcraft:scheduled_ticks";

    private BlueprintTicks() {
    }

    public record Entry(BlockPos pos, ResourceLocation type, int delay, int priority, boolean fluid, long order) {
        public Entry(BlockPos pos, ResourceLocation type, int delay, int priority, boolean fluid) {
            this(pos, type, delay, priority, fluid, 0);
        }

        public Entry at(BlockPos target) {
            return new Entry(target, this.type, this.delay, this.priority, this.fluid, this.order);
        }
    }

    public static List<Entry> read(CompoundTag tag) {
        ListTag saved = tag.getList(KEY, Tag.TAG_COMPOUND);
        if (saved.size() > 32768) throw new IllegalArgumentException("Too many scheduled blueprint ticks");
        List<Entry> ticks = new ArrayList<>();
        for (Tag value : saved) {
            CompoundTag entry = (CompoundTag) value;
            BlockPos pos = NbtUtils.readBlockPos(entry, "pos").orElseThrow();
            ResourceLocation type = ResourceLocation.parse(entry.getString("type"));
            boolean fluid = entry.getBoolean("fluid");
            if (!(fluid ? BuiltInRegistries.FLUID.containsKey(type) : BuiltInRegistries.BLOCK.containsKey(type))) {
                throw new IllegalArgumentException("Unknown scheduled tick type " + type);
            }
            ticks.add(new Entry(pos, type, Math.max(0, entry.getInt("delay")), entry.getInt("priority"), fluid, entry.getLong("order")));
        }
        return ticks;
    }

    public static void write(CompoundTag tag, List<Entry> ticks) {
        ListTag entries = new ListTag();
        for (Entry tick : ticks) {
            CompoundTag entry = new CompoundTag();
            entry.put("pos", NbtUtils.writeBlockPos(tick.pos()));
            entry.putString("type", tick.type().toString());
            entry.putInt("delay", tick.delay());
            entry.putInt("priority", tick.priority());
            entry.putBoolean("fluid", tick.fluid());
            entry.putLong("order", tick.order());
            entries.add(entry);
        }
        tag.put(KEY, entries);
    }

    public static List<Entry> capture(ServerLevel level, BoundingBox bounds) {
        List<Entry> ticks = new ArrayList<>();
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
            for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                var chunk = level.getChunkAt(new BlockPos(x << 4, 0, z << 4));
                ((LevelChunkTicks<Block>) chunk.getBlockTicks()).getAll()
                    .filter(tick -> bounds.isInside(tick.pos())).forEach(tick -> ticks.add(new Entry(
                    tick.pos(), BuiltInRegistries.BLOCK.getKey(tick.type()),
                    (int) Math.clamp(tick.triggerTick() - level.getGameTime(), 0, Integer.MAX_VALUE),
                    tick.priority().getValue(), false, tick.subTickOrder())));
                ((LevelChunkTicks<Fluid>) chunk.getFluidTicks()).getAll()
                    .filter(tick -> bounds.isInside(tick.pos())).forEach(tick -> ticks.add(new Entry(
                    tick.pos(), BuiltInRegistries.FLUID.getKey(tick.type()),
                    (int) Math.clamp(tick.triggerTick() - level.getGameTime(), 0, Integer.MAX_VALUE),
                    tick.priority().getValue(), true, tick.subTickOrder())));
            }
        }
        return ticks;
    }

    public static void restore(ServerLevel level, List<Entry> ticks, Set<BlockPos> placed) {
        for (Entry tick : ticks.stream().sorted(Comparator.comparingLong(Entry::order)).toList()) {
            if (!placed.contains(tick.pos())) continue;
            if (tick.fluid()) {
                var fluid = BuiltInRegistries.FLUID.get(tick.type());
                if (level.getFluidState(tick.pos()).getType() == fluid) {
                    level.scheduleTick(tick.pos(), fluid, tick.delay(), TickPriority.byValue(tick.priority()));
                }
            } else {
                var block = BuiltInRegistries.BLOCK.get(tick.type());
                if (level.getBlockState(tick.pos()).is(block)) {
                    level.scheduleTick(tick.pos(), block, tick.delay(), TickPriority.byValue(tick.priority()));
                }
            }
        }
    }
}
