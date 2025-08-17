package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

import java.util.function.Predicate;

public class AnvilBehaviorRegisterEvent extends Event {
    private final BlockBehaviorRegister blockBehaviorRegister;
    private final StateBehaviorRegister stateBehaviorRegister;

    public AnvilBehaviorRegisterEvent(BlockBehaviorRegister blockBehaviorRegister, StateBehaviorRegister stateBehaviorRegister) {
        this.blockBehaviorRegister = blockBehaviorRegister;
        this.stateBehaviorRegister = stateBehaviorRegister;
    }

    public void registerBehavior(Block matchingBlock, IAnvilBehavior behavior) {
        blockBehaviorRegister.register(matchingBlock, behavior);
    }

    public void registerBehavior(Predicate<BlockState> pred, IAnvilBehavior behavior) {
        stateBehaviorRegister.register(pred, behavior);
    }

    public interface BlockBehaviorRegister {
        void register(Block matchingBlock, IAnvilBehavior behavior);
    }

    public interface StateBehaviorRegister {
        void register(Predicate<BlockState> pred, IAnvilBehavior behavior);
    }
}
