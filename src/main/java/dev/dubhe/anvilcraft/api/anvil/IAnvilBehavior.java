package dev.dubhe.anvilcraft.api.anvil;

import dev.dubhe.anvilcraft.api.anvil.impl.BoilingBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.BulgingBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.CementStainingBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ConcreteBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.CookingBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.HitBeeNestBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.HitCrabTrapBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.HitSpawnerBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ItemCompressBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ItemCrushBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ItemMeshBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ItemStampingBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.MassInjectBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.RedstoneEMPBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.ResetVaultBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.SuperHeatingBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.TimeWarpBehavior;
import dev.dubhe.anvilcraft.api.anvil.impl.UnpackBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@FunctionalInterface
public interface IAnvilBehavior {
    Map<Predicate<BlockState>, IAnvilBehavior> BEHAVIORS = new LinkedHashMap<>();

    boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    );

    default int priority() {
        return 100;
    }

    static void registerBehavior(Block matchingBlock, IAnvilBehavior behavior) {
        BEHAVIORS.put(it -> it.is(matchingBlock), behavior);
    }

    static void registerBehavior(Predicate<BlockState> pred, IAnvilBehavior behavior) {
        BEHAVIORS.put(pred, behavior);
    }

    static List<IAnvilBehavior> findMatching(BlockState state) {
        return BEHAVIORS.keySet().stream()
            .filter(it -> it.test(state))
            .map(BEHAVIORS::get)
            .toList();
    }

    static void register() {
        registerBehavior(Blocks.REDSTONE_BLOCK, new RedstoneEMPBehavior());
        registerBehavior(
            state -> state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST),
            new HitBeeNestBehavior()
        );
        registerBehavior(Blocks.SPAWNER, new HitSpawnerBehavior());
        registerBehavior(ModBlocks.CRAB_TRAP.get(), new HitCrabTrapBehavior());

        registerBehavior(
            state -> state.is(Blocks.IRON_TRAPDOOR)
                && state.getValue(TrapDoorBlock.HALF) == Half.TOP
                && !state.getValue(TrapDoorBlock.OPEN),
            new UnpackBehavior()
        );
        registerBehavior(state -> state.getBlock() instanceof CementCauldronBlock, new CementStainingBehavior());
        registerBehavior(state -> state.getBlock() instanceof CementCauldronBlock, new ConcreteBehavior());
        registerBehavior(state -> state.getBlock() instanceof AbstractCauldronBlock, new BulgingBehavior());
        registerBehavior(state -> state.getBlock() instanceof AbstractCauldronBlock, new TimeWarpBehavior());
        registerBehavior(Blocks.CAULDRON, new SuperHeatingBehavior());
        registerBehavior(Blocks.CAULDRON, new CookingBehavior());
        registerBehavior(Blocks.CAULDRON, new ItemCompressBehavior());
        registerBehavior(Blocks.WATER_CAULDRON, new BoilingBehavior());
        registerBehavior(ModBlocks.STAMPING_PLATFORM.get(), new ItemStampingBehavior());
        registerBehavior(ModBlocks.CRUSHING_TABLE.get(), new ItemCrushBehavior());
        registerBehavior(ModBlocks.SPACE_OVERCOMPRESSOR.get(), new MassInjectBehavior());
        registerBehavior(Blocks.SCAFFOLDING, new ItemMeshBehavior());
        registerBehavior(state -> state.is(ModBlockTags.STORAGE_BLOCKS_LEAD), new ResetVaultBehavior());
    }
}
