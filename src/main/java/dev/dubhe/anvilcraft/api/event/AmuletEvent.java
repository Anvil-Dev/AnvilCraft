package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/// 所有与本模组的 {@link dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet 护符} 有关的事件的基类。
@Getter
public abstract sealed class AmuletEvent extends Event
    permits AmuletEvent.Find,
    AmuletEvent.ProcessFound,
    AmuletEvent.ModifyRaffleProbability {
    private final AmuletManager manager;

    protected AmuletEvent(AmuletManager manager) {
        this.manager = manager;
    }

    /// 本事件会在 {@link AmuletManager#getAmuletsFromInventory(Player)} 内发出，<br>
    /// 并允许其它模组从非物品栈形式的源提供护符。
    /// <p>注意：您不需要在此事件中处理护符容器（物品栈形式的源）。请与 {@link ProcessFound} 中执行上述操作。</p>
    /// <p>本事件会在双端发出。</p>
    ///
    /// @see ProcessFound
    public static final class Find extends AmuletEvent {
        @Getter
        private final Player player;
        private final Consumer<ItemStack> collector;

        public Find(AmuletManager manager, Player player, Consumer<ItemStack> collector) {
            super(manager);
            this.player = player;
            this.collector = collector;
        }

        /// 向事件提供源物品栈
        ///
        /// @param source 源物品栈
        public void provide(ItemStack source) {
            this.collector.accept(source);
        }
    }

    /// 本事件会在 {@link AmuletManager#processFoundStack(ItemStack, List)} 内发出，<br>
    /// 并允许其它模组处理护符容器（物品栈形式的源）并提供。
    /// <p>取消该事件将阻止源及其可能包含的其它护符被加入最终结果。</p>
    /// <p>注意：您不应在此事件中从非物品栈形式的源提供护符。请与 {@link Find} 中执行上述操作。</p>
    /// <p>本事件会在双端发出。</p>
    ///
    /// @see Find
    @Getter
    public static final class ProcessFound extends AmuletEvent implements ICancellableEvent {
        private final ItemStack found;
        private final List<ItemStack> extracted = new ArrayList<>();

        public ProcessFound(AmuletManager manager, ItemStack found) {
            super(manager);
            this.found = found;
        }

        /// 向事件提供护符物品栈
        ///
        /// @param amulet 护符物品栈
        public void provide(ItemStack amulet) {
            this.extracted.add(amulet);
        }
    }

    /// 本事件会在 {@link AmuletManager#tryRaffle(ServerPlayer, DamageSource)} 中抽取失败时发出，<br>
    /// 并允许修改抽取概率。
    /// <p>抽取概率为 0-100 的整数，对应 0%-100%</p>
    /// <p>本事件会在逻辑服务端发出。</p>
    @Getter
    public static final class ModifyRaffleProbability extends AmuletEvent {
        private final ServerPlayer player;
        private final DamageSource source;
        private final Holder.Reference<IAmuletDefinition> def;
        @Setter
        private int probability;

        public ModifyRaffleProbability(
            AmuletManager manager,
            ServerPlayer player,
            DamageSource source,
            Holder.Reference<IAmuletDefinition> def,
            int probability
        ) {
            super(manager);
            this.player = player;
            this.source = source;
            this.def = def;
            this.probability = probability;
        }
    }
}
