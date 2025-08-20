package dev.dubhe.anvilcraft.api.anvil;

import dev.dubhe.anvilcraft.api.event.AnvilBehaviorRegisterEvent;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

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
        AnvilEvent.OnLand event
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

    static @NotNull @Unmodifiable List<IAnvilBehavior> findMatching(BlockState state) {
        return BEHAVIORS.keySet().stream()
            .filter(it -> it.test(state))
            .map(BEHAVIORS::get)
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
