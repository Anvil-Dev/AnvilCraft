package dev.dubhe.anvilcraft.api.amulet;

import dev.anvilcraft.lib.v2.util.CollectionUtil;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.amulet.DoNothingAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.WrappedOthersAmulet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AmuletManager {
    private static @Nullable SoftReference<AmuletManager> INSTANCE;

    public static AmuletManager get(HolderLookup.Provider registries) {
        if (AmuletManager.INSTANCE != null || AmuletManager.INSTANCE.get() == null) {
            AmuletManager.INSTANCE = new SoftReference<>(new AmuletManager(AmuletManager.extractDefinitions(registries)));
        }
        return AmuletManager.INSTANCE.get();
    }

    public static List<Holder.Reference<IAmuletDefinition>> extractDefinitions(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(ModRegistries.AMULET_DEF)
            .listElements()
            .toList();
    }

    private final List<Holder.Reference<IAmuletDefinition>> definitions;

    private AmuletManager(List<Holder.Reference<IAmuletDefinition>> definitions) {
        this.definitions = definitions;
    }

    public List<ItemStack> getAmuletsFromInventory(Player player) {
        List<ItemStack> founds = new ArrayList<>();
        NeoForge.EVENT_BUS.post(new AmuletEvent.Find(this, player, founds::add));
        List<ItemStack> amulets = new ArrayList<>();
        for (ItemStack found : founds) {
            this.processFoundStack(found, amulets);
        }
        return amulets;
    }

    private void processFoundStack(ItemStack found, List<ItemStack> amulets) {
        AmuletEvent.ProcessFound event = new AmuletEvent.ProcessFound(this, found);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }
        List<ItemStack> extracted = event.getExtracted();
        if (extracted.isEmpty()) {
            if (found.has(ModComponents.AMULET)) {
                amulets.add(found);
            }
            return;
        }
        for (ItemStack amulet : extracted) {
            if (!amulet.has(ModComponents.AMULET)) {
                continue;
            }
            amulets.add(amulet);
        }
    }

    public void tryRaffle(ServerPlayer player, DamageSource source) {
        Optional<IAmuletDefinition> defOp = this.getDefinitionMatchedDamage(player, source).map(Holder::value);
        if (defOp.isEmpty()) return;
        IAmuletDefinition def = defOp.get();
        ItemStack amulet = def.create();
        if (this.hasAmuletInInventory(player, amulet.getOrDefault(ModComponents.AMULET, DoNothingAmulet.INSTANCE))) return;

        RandomSource random = player.getRandom();
        int probability = Math.min(this.getRaffleProbability(player, source), 100);
        if (probability > random.nextIntBetweenInclusive(0, 100)) {
            player.getInventory().placeItemBackInInventory(amulet.copy());
            this.setRaffleProbability(player, source, 0);
        } else {
            AmuletEvent.ModifyRaffleProbability event = new AmuletEvent.ModifyRaffleProbability(this, player, source, probability + 10);
            NeoForge.EVENT_BUS.post(event);
            probability = event.getProbability();
            this.setRaffleProbability(player, source, Math.clamp(probability, 0, 100));
        }
    }

    public Optional<Holder<IAmuletDefinition>> getDefinitionMatchedDamage(ServerPlayer victim, DamageSource source) {
        for (Holder<IAmuletDefinition> def : this.definitions) {
            if (def.value().mayObtain(victim, source)) {
                return Optional.of(def);
            }
        }
        return Optional.empty();
    }

    public int getRaffleProbability(Player player, DamageSource source) {
        if (!(player instanceof ServerPlayer victim)) {
            return 0;
        }
        return this.getDefinitionMatchedDamage(victim, source)
            .map(holder -> this.getRaffleProbability(player, holder))
            .orElse(0);
    }

    public int getRaffleProbability(Player player, Holder<IAmuletDefinition> def) {
        if (this.hasAmuletInInventory(player, def)) {
            return 0;
        }
        return AmuletManager.getStoredRaffleProbability(player, def.value()) + 20;
    }

    public boolean hasAmuletInInventory(Player player, IAmulet amulet) {
        List<ItemStack> amulets = this.getAmuletsFromInventory(player);
        return CollectionUtil.anyMatch(amulets, stack -> stack.get(ModComponents.AMULET).canActAs(amulet));
    }

    public boolean hasAmuletInInventory(Player player, Holder<IAmuletDefinition> def) {
        ItemStack target = def.value().create();
        List<ItemStack> amulets = this.getAmuletsFromInventory(player);
        return CollectionUtil.anyMatch(amulets, stack -> ItemStack.isSameItem(stack, target));
    }

    public static int getStoredRaffleProbability(Player player, IAmuletDefinition type) {
        return player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY).getProbability(type);
    }

    public void setRaffleProbability(ServerPlayer player, DamageSource source, int probability) {
        Optional<Holder<IAmuletDefinition>> def = this.getDefinitionMatchedDamage(player, source);
        def.ifPresent(holder -> this.setRaffleProbability(player, holder, probability));
    }

    public void setRaffleProbability(ServerPlayer player, Holder<IAmuletDefinition> def, int probability) {
        AmuletRaffleProbability arp = player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY);
        if (!this.hasAmuletInInventory(player, def)) {
            arp.setProbability(def.value(), probability);
        } else {
            arp.setProbability(def.value(), 0);
        }
    }

    public void inventoryTick(ServerPlayer player) {
        List<ItemStack> all = new ArrayList<>();
        for (Holder<IAmuletDefinition> def : this.definitions) {
            all.add(def.value().create());
        }
        List<ItemStack> now = this.getAmuletsFromInventory(player);
        for (ItemStack stack : now) {
            IAmulet amulet = stack.get(ModComponents.AMULET);
            all.removeIf(other -> amulet.canActAs(other.get(ModComponents.AMULET)));
            stack.get(ModComponents.AMULET).inventoryTick(player, stack, true);
        }
        for (ItemStack stack : all) {
            IAmulet amulet = stack.get(ModComponents.AMULET);
            if (amulet instanceof WrappedOthersAmulet) return;
            amulet.inventoryTick(player, stack, false);
        }
    }

    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        return CollectionUtil.anyMatch(
            this.getAmuletsFromInventory(player),
            stack -> stack.get(ModComponents.AMULET).shouldImmune(player, source)
        );
    }
}
