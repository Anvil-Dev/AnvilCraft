package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyCategory;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;

/**
 * 锁定第 N 种新的天体类型时触发，count 为该判据要求累计的天体类别数。
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CelestialVisitTrigger extends SimpleCriterionTrigger<CelestialVisitTrigger.TriggerInstance> {
    private static final String VISITS = "anvilcraft:celestial_visited";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().contains(VISITS)) {
            visitedCategories(player);
        }
    }

    public void visit(ServerPlayer player, CelestialBodyCategory category) {
        ListTag visited = visitedCategories(player);
        StringTag name = StringTag.valueOf(category.name());
        if (!visited.contains(name)) {
            visited.add(name);
        }
        this.trigger(player, visited.size());
    }

    private static ListTag visitedCategories(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        // NeoForge 重建玩家时仅复制 PlayerPersisted，列表与逗号分隔文本均可能来自旧存档。
        CompoundTag persisted = root.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        ListTag visited = persisted.getListOrEmpty(VISITS);
        Tag legacy = root.get(VISITS);
        if (legacy instanceof ListTag list) {
            for (Tag entry : list) {
                if (entry instanceof StringTag && !visited.contains(entry)) {
                    visited.add(entry);
                }
            }
        } else if (legacy instanceof StringTag csv) {
            for (String value : csv.value().split(",")) {
                StringTag name = StringTag.valueOf(value);
                if (!value.isEmpty() && !visited.contains(name)) {
                    visited.add(name);
                }
            }
        }
        root.remove(VISITS);
        persisted.put(VISITS, visited);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        return visited;
    }

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int count) {
        this.trigger(player, (instance) -> instance.matches(count));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<Integer> count
    ) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            Codec.INT.optionalFieldOf("count").forGetter(TriggerInstance::count)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> visited() {
            return ModCriterionTriggers.CELESTIAL_VISIT.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.empty())
            );
        }

        public static Criterion<TriggerInstance> visited(int count) {
            return ModCriterionTriggers.CELESTIAL_VISIT.get().createCriterion(
                new TriggerInstance(Optional.empty(), Optional.of(count))
            );
        }

        public boolean matches(int count) {
            return this.count.isEmpty() || count >= this.count.get();
        }
    }
}
