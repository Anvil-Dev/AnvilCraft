package dev.dubhe.anvilcraft.api.anvil;

import dev.dubhe.anvilcraft.api.anvil.impl.HitBeeNestBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.HitCrabTrapBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.HitSpawnerBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ItemCompressBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ItemCrushBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ItemMeshBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ItemStampingBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.RedstoneEMPBehavior;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@FunctionalInterface
public interface AnvilBehavior {
    Map<Predicate<BlockState>, AnvilBehavior> BEHAVIORS = new HashMap<>();

    void handle(
            Level level,
            BlockPos hitBlockPos,
            BlockState hitBlockState,
            float fallDistance,
            AnvilFallOnLandEvent event);

    static void registerBehavior(Block matchingBlock, AnvilBehavior behavior) {
        BEHAVIORS.put(it -> it.is(matchingBlock), behavior);
    }

    static void registerBehavior(Predicate<BlockState> pred, AnvilBehavior behavior) {
        BEHAVIORS.put(pred, behavior);
    }

    static Optional<AnvilBehavior> findMatching(BlockState state) {
        return BEHAVIORS.keySet().stream().filter(it -> it.test(state)).findFirst().map(BEHAVIORS::get);
    }

    static void register() {
        registerBehavior(ModBlocks.CRAB_TRAP.get(), new HitCrabTrapBehavior());
        registerBehavior(
                state -> state.is(Blocks.IRON_TRAPDOOR)
                        && state.getValue(TrapDoorBlock.HALF) == Half.TOP
                        && !state.getValue(TrapDoorBlock.OPEN),
                new ItemCrushBehavior());
        registerBehavior(Blocks.CAULDRON, new ItemCompressBehavior());
        registerBehavior(ModBlocks.STAMPING_PLATFORM.get(), new ItemStampingBehavior());
        registerBehavior(Blocks.SCAFFOLDING, new ItemMeshBehavior());
        registerBehavior(Blocks.REDSTONE_BLOCK, new RedstoneEMPBehavior());
        registerBehavior(
                state -> state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST), new HitBeeNestBehavior());
        registerBehavior(Blocks.SPAWNER, new HitSpawnerBehavior());
    }
}
