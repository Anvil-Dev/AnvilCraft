package dev.dubhe.anvilcraft.api.heat;

import com.mojang.datafixers.util.Pair;
import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.heatable.GlowingBlock;
import dev.dubhe.anvilcraft.block.heatable.HeatedBlock;
import dev.dubhe.anvilcraft.block.heatable.IncandescentBlock;
import dev.dubhe.anvilcraft.block.heatable.OverheatedBlock;
import dev.dubhe.anvilcraft.block.heatable.RedhotBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.TriPredicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class HeatRecorder {
    private static final Map<HeatableBlockEntry, Identifier> ENTRY_TO_ID = new HashMap<>();
    private static final Map<Identifier, List<HeatableBlockEntry>> ENTRIES = new HashMap<>();
    static final Set<HeaterInfo<?>> PRODUCER_INFOS = new HashSet<>();

    public static RegisterHelper registerHeatables(Identifier id) {
        return new RegisterHelper(id);
    }

    public static <T> HeaterInfo<T> registerProducerInfo(HeaterInfo<T> info) {
        HeatRecorder.PRODUCER_INFOS.add(info);
        return info;
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public static class RegisterHelper {
        private final Identifier id;

        public RegisterHelper(Identifier id) {
            this.id = id;
        }

        public RegisterHelper normal(Block normal) {
            return this.customTier(HeatTier.NORMAL, normal);
        }

        public RegisterHelper normal(Supplier<? extends Block> normal) {
            return this.customTier(HeatTier.NORMAL, normal.get());
        }

        public RegisterHelper normal(Block normal, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.NORMAL, normal, predicate);
        }

        public RegisterHelper normal(Supplier<? extends Block> normal, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.NORMAL, normal.get(), predicate);
        }

        public RegisterHelper heated(HeatedBlock heated) {
            return this.customTier(HeatTier.HEATED, heated);
        }

        public RegisterHelper heated(Supplier<? extends HeatedBlock> heated) {
            return this.customTier(HeatTier.HEATED, heated.get());
        }

        public RegisterHelper heated(HeatedBlock heated, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.HEATED, heated, predicate);
        }

        public RegisterHelper heated(Supplier<? extends HeatedBlock> heated, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.HEATED, heated.get(), predicate);
        }

        public RegisterHelper redhot(RedhotBlock redhot) {
            return this.customTier(HeatTier.REDHOT, redhot);
        }

        public RegisterHelper redhot(Supplier<? extends RedhotBlock> redhot) {
            return this.customTier(HeatTier.REDHOT, redhot.get());
        }

        public RegisterHelper redhot(RedhotBlock redhot, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.REDHOT, redhot, predicate);
        }

        public RegisterHelper redhot(Supplier<? extends RedhotBlock> redhot, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.REDHOT, redhot.get(), predicate);
        }

        public RegisterHelper glowing(GlowingBlock glowing) {
            return this.customTier(HeatTier.GLOWING, glowing);
        }

        public RegisterHelper glowing(Supplier<? extends GlowingBlock> glowing) {
            return this.customTier(HeatTier.GLOWING, glowing.get());
        }

        public RegisterHelper glowing(GlowingBlock glowing, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.GLOWING, glowing, predicate);
        }

        public RegisterHelper glowing(Supplier<? extends GlowingBlock> glowing, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.GLOWING, glowing.get(), predicate);
        }

        public RegisterHelper incandescent(IncandescentBlock incandescent) {
            return this.customTier(HeatTier.INCANDESCENT, incandescent);
        }

        public RegisterHelper incandescent(Supplier<? extends IncandescentBlock> incandescent) {
            return this.customTier(HeatTier.INCANDESCENT, incandescent.get());
        }

        public RegisterHelper incandescent(IncandescentBlock incandescent, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.INCANDESCENT, incandescent, predicate);
        }

        public RegisterHelper incandescent(
            Supplier<? extends IncandescentBlock> incandescent, TriPredicate<Level, BlockPos, BlockState> predicate
        ) {
            return this.customTier(HeatTier.INCANDESCENT, incandescent.get(), predicate);
        }

        public RegisterHelper overheated(OverheatedBlock incandescent) {
            return this.customTier(HeatTier.OVERHEATED, incandescent);
        }

        public RegisterHelper overheated(Supplier<? extends OverheatedBlock> incandescent) {
            return this.customTier(HeatTier.OVERHEATED, incandescent.get());
        }

        public RegisterHelper overheated(OverheatedBlock incandescent, TriPredicate<Level, BlockPos, BlockState> predicate) {
            return this.customTier(HeatTier.OVERHEATED, incandescent, predicate);
        }

        public RegisterHelper overheated(
            Supplier<? extends OverheatedBlock> incandescent, TriPredicate<Level, BlockPos, BlockState> predicate
        ) {
            return this.customTier(HeatTier.OVERHEATED, incandescent.get(), predicate);
        }

        public RegisterHelper customTier(HeatTier tier, Block heatable) {
            HeatableBlockEntry entry = HeatableBlockEntry.simple(tier, heatable);
            HeatRecorder.ENTRY_TO_ID.put(entry, this.id);
            HeatRecorder.ENTRIES.computeIfAbsent(this.id, o -> new ArrayList<>()).add(entry);
            HeatRecorder.ENTRIES.get(this.id).sort(HeatableBlockEntry::compareTo);
            return this;
        }

        public RegisterHelper customTier(HeatTier tier, Block heatable, TriPredicate<Level, BlockPos, BlockState> predicate) {
            HeatableBlockEntry entry = HeatableBlockEntry.predicate(tier, heatable, predicate);
            HeatRecorder.ENTRY_TO_ID.put(entry, this.id);
            HeatRecorder.ENTRIES.computeIfAbsent(this.id, o -> new ArrayList<>()).add(entry);
            HeatRecorder.ENTRIES.get(this.id).sort(HeatableBlockEntry::compareTo);
            return this;
        }
    }

    public static Optional<HeatableBlockEntry> getEntry(Identifier id, HeatTier tier) {
        for (HeatableBlockEntry entry : HeatRecorder.ENTRIES.get(id)) {
            if (entry.getTier().equals(tier)) return Optional.of(entry);
        }
        return Optional.empty();
    }

    public static Optional<HeatableBlockEntry> getEntry(Identifier id, Level level, BlockPos pos, BlockState state) {
        for (HeatableBlockEntry entry : HeatRecorder.ENTRIES.get(id)) {
            if (entry.isValidBlock(level, pos, state)) return Optional.of(entry);
        }
        return Optional.empty();
    }

    public static Optional<HeatableBlockEntry> getEntry(HeatTier tier) {
        return HeatRecorder.getIdAndEntry(tier).getSecond();
    }

    public static Optional<HeatableBlockEntry> getEntry(Level level, BlockPos pos, BlockState state) {
        return HeatRecorder.getIdAndEntry(level, pos, state).getSecond();
    }

    public static Optional<HeatableBlockEntry> getEntry(Level level, BlockPos pos, BlockState prevState, HeatTier tier) {
        return HeatRecorder.getId(level, pos, prevState)
            .flatMap(id -> Optional.of(HeatRecorder.ENTRIES.get(id).get(tier.ordinal())));
    }

    private static Pair<Optional<Identifier>, Optional<HeatableBlockEntry>> getIdAndEntry(HeatTier tier) {
        for (List<HeatableBlockEntry> entries : HeatRecorder.ENTRIES.values()) {
            for (HeatableBlockEntry entry : entries) {
                if (entry.getTier().equals(tier)) {
                    return new Pair<>(Optional.ofNullable(HeatRecorder.ENTRY_TO_ID.get(entry)), Optional.of(entry));
                }
            }
        }
        return new Pair<>(Optional.empty(), Optional.empty());
    }

    private static Pair<Optional<Identifier>, Optional<HeatableBlockEntry>> getIdAndEntry(
        Level level, BlockPos pos, BlockState state
    ) {
        for (List<HeatableBlockEntry> entries : HeatRecorder.ENTRIES.values()) {
            for (HeatableBlockEntry entry : entries) {
                if (entry.isValidBlock(level, pos, state)) {
                    return new Pair<>(Optional.ofNullable(HeatRecorder.ENTRY_TO_ID.get(entry)), Optional.of(entry));
                }
            }
        }
        return new Pair<>(Optional.empty(), Optional.empty());
    }

    public static Optional<Identifier> getId(HeatTier tier) {
        return HeatRecorder.getIdAndEntry(tier).getFirst();
    }

    public static Optional<Identifier> getId(Level level, BlockPos pos, BlockState state) {
        return HeatRecorder.getIdAndEntry(level, pos, state).getFirst();
    }

    public static Optional<HeatTier> getTier(Level level, BlockPos pos, BlockState state) {
        return HeatRecorder.getEntry(level, pos, state).map(HeatableBlockEntry::getTier);
    }

    public static Optional<Block> getHeatableBlock(Level level, BlockPos pos, BlockState prevState, HeatTier tier) {
        return HeatRecorder.getEntry(level, pos, prevState, tier).map(HeatableBlockEntry::getDefaultBlock);
    }

    public static Optional<Block> getHeatableBlock(Identifier id, HeatTier tier) {
        return HeatRecorder.getEntry(id, tier).map(HeatableBlockEntry::getDefaultBlock);
    }

    public static Optional<HeatableBlockEntry> getPrevTierEntry(Level level, BlockPos pos, BlockState state) {
        Pair<Optional<Identifier>, Optional<HeatableBlockEntry>> pair = HeatRecorder.getIdAndEntry(level, pos, state);
        Optional<Identifier> idOp = pair.getFirst();
        Optional<HeatableBlockEntry> entryOp = pair.getSecond();
        if (idOp.isEmpty() || entryOp.isEmpty()) return Optional.empty();
        List<HeatableBlockEntry> entries = HeatRecorder.ENTRIES.get(idOp.get());
        return ListUtil.safelyGet(entries, entries.indexOf(entryOp.get()) - 1);
    }

    public static Optional<HeatTier> getPrevTier(Level level, BlockPos pos, BlockState state) {
        return HeatRecorder.getPrevTierEntry(level, pos, state).map(HeatableBlockEntry::getTier);
    }

    public static Optional<Block> getPrevTierHeatableBlock(Level level, BlockPos pos, BlockState state) {
        return HeatRecorder.getPrevTierEntry(level, pos, state).map(HeatableBlockEntry::getDefaultBlock);
    }

    public static Optional<HeatableBlockEntry> getNextTierEntry(Level level, BlockPos pos, BlockState state) {
        Pair<Optional<Identifier>, Optional<HeatableBlockEntry>> pair = HeatRecorder.getIdAndEntry(level, pos, state);
        Optional<Identifier> idOp = pair.getFirst();
        Optional<HeatableBlockEntry> entryOp = pair.getSecond();
        if (idOp.isEmpty() || entryOp.isEmpty()) return Optional.empty();
        List<HeatableBlockEntry> entries = HeatRecorder.ENTRIES.get(idOp.get());
        return ListUtil.safelyGet(entries, entries.indexOf(entryOp.get()) + 1);
    }

    public static Optional<HeatTier> getNextTier(Level level, BlockPos pos, BlockState state) {
        return HeatRecorder.getNextTierEntry(level, pos, state).map(HeatableBlockEntry::getTier);
    }

    public static Optional<Block> getNextTierHeatableBlock(Level level, BlockPos pos, BlockState state) {
        return HeatRecorder.getNextTierEntry(level, pos, state).map(HeatableBlockEntry::getDefaultBlock);
    }

    static {
        HeatRecorder.registerHeatables(AnvilCraft.of("netherite"))
            .normal(Blocks.NETHERITE_BLOCK, (level, pos, state) -> state.is(Tags.Blocks.STORAGE_BLOCKS_NETHERITE))
            .heated(ModBlocks.HEATED_NETHERITE_BLOCK)
            .redhot(ModBlocks.REDHOT_NETHERITE_BLOCK)
            .glowing(ModBlocks.GLOWING_NETHERITE_BLOCK)
            .incandescent(ModBlocks.INCANDESCENT_NETHERITE_BLOCK);
        HeatRecorder.registerHeatables(AnvilCraft.of("tungsten"))
            .normal(ModBlocks.TUNGSTEN_BLOCK, (level, pos, state) -> state.is(ModBlockTags.STORAGE_BLOCKS_TUNGSTEN))
            .heated(ModBlocks.HEATED_TUNGSTEN_BLOCK)
            .redhot(ModBlocks.REDHOT_TUNGSTEN_BLOCK)
            .glowing(ModBlocks.GLOWING_TUNGSTEN_BLOCK)
            .incandescent(ModBlocks.INCANDESCENT_TUNGSTEN_BLOCK);
        HeatRecorder.registerHeatables(AnvilCraft.of("ember_metal"))
            .normal(ModBlocks.EMBER_METAL_BLOCK)
            .overheated(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK);
    }
}
