package dev.dubhe.anvilcraft.api.anvil;

import dev.dubhe.anvilcraft.api.event.AnvilBehaviorRegisterEvent;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Unmodifiable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@FunctionalInterface
public interface IAnvilBehavior {
    Map<Predicate<BlockState>, IAnvilBehavior> BEHAVIORS = new LinkedHashMap<>();

    boolean handle(
        ServerLevel level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        double fallDistance,
        AnvilEvent.OnLand event
    );

    default int priority() {
        return 100;
    }

    static void registerBehavior(Block matchingBlock, IAnvilBehavior behavior) {
        IAnvilBehavior.BEHAVIORS.put(it -> it.is(matchingBlock), behavior);
    }

    static void registerBehavior(Predicate<BlockState> pred, IAnvilBehavior behavior) {
        IAnvilBehavior.BEHAVIORS.put(pred, behavior);
    }

    static @Unmodifiable List<IAnvilBehavior> findMatching(BlockState state) {
        return IAnvilBehavior.BEHAVIORS.keySet().stream()
            .filter(it -> it.test(state))
            .map(IAnvilBehavior.BEHAVIORS::get)
            .toList();
    }

    static void register() {
        AnvilBehaviorRegisterEvent event = new AnvilBehaviorRegisterEvent(
            IAnvilBehavior::registerBehavior,
            IAnvilBehavior::registerBehavior
        );
        NeoForge.EVENT_BUS.post(event);
    }
}
