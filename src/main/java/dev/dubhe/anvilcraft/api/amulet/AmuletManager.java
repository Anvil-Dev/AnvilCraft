package dev.dubhe.anvilcraft.api.amulet;

import dev.anvilcraft.lib.v2.util.CollectionUtil;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.api.event.AmuletEvent;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
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
import java.util.Comparator;
import java.util.List;

public class AmuletManager {
    private static @Nullable SoftReference<AmuletManager> INSTANCE;

    public static AmuletManager get(HolderLookup.Provider registries) {
        SoftReference<AmuletManager> reference = AmuletManager.INSTANCE;
        AmuletManager manager = reference == null ? null : reference.get();
        if (manager == null) {
            manager = new AmuletManager(AmuletManager.extractDefinitions(registries));
            AmuletManager.INSTANCE = new SoftReference<>(manager);
        }
        return manager;
    }

    public static List<Holder.Reference<IAmuletDefinition>> extractDefinitions(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(ModRegistryKeys.AMULET_DEF)
            .listElements()
            .toList();
    }

    public static void clear() {
        AmuletManager.INSTANCE = null;
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
        Holder.Reference<IAmuletDefinition> trying = null;
        ItemStack amulet = null;

        RandomSource random = player.getRandom();
        List<Holder.Reference<IAmuletDefinition>> defs = this.getDefinitionMatchedDamage(player, source);
        if (defs.isEmpty()) {
            return;
        }

        List<Holder.Reference<IAmuletDefinition>> shuffled = new ArrayList<>(defs);
        shuffled.sort(Comparator.comparingInt(_ -> random.nextInt()));
        for (Holder.Reference<IAmuletDefinition> def : shuffled) {
            amulet = def.value().create();
            if (!this.hasAmuletInInventory(player, amulet.getOrDefault(ModComponents.AMULET, DoNothingAmulet.INSTANCE))) {
                trying = def;
                break;
            }
        }

        if (trying == null) {
            return;
        }

        int probability = Math.min(this.getRaffleProbability(player, trying), 100);
        if (random.nextInt(100) < probability) {
            player.getInventory().placeItemBackInInventory(amulet.copy());
            this.setRaffleProbability(player, trying, 0);
        } else {
            probability = NeoForge.EVENT_BUS.post(new AmuletEvent.ModifyRaffleProbability(
                this,
                player,
                source,
                trying,
                probability + 10
            )).getProbability();
            this.setRaffleProbability(player, trying, Math.clamp(probability, 0, 100));
        }
    }

    public List<Holder.Reference<IAmuletDefinition>> getDefinitionMatchedDamage(ServerPlayer victim, DamageSource source) {
        List<Holder.Reference<IAmuletDefinition>> results = new ArrayList<>();
        for (Holder.Reference<IAmuletDefinition> def : this.definitions) {
            if (def.value().mayObtain(victim, source)) {
                results.add(def);
            }
        }
        return results;
    }

    public int getRaffleProbability(Player player, Holder<IAmuletDefinition> def) {
        if (this.hasAmuletInInventory(player, def)) {
            return 0;
        }
        return AmuletManager.getStoredRaffleProbability(player, def);
    }

    public static int getStoredRaffleProbability(Player player, Holder<IAmuletDefinition> def) {
        return player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY).getProbability(def);
    }

    public boolean hasAmuletInInventory(Player player, IAmulet amulet) {
        List<ItemStack> amulets = this.getAmuletsFromInventory(player);
        return CollectionUtil.anyMatch(
            amulets,
            stack -> stack.getOrDefault(ModComponents.AMULET, DoNothingAmulet.INSTANCE).canActAs(amulet)
        );
    }

    public boolean hasAmuletInInventory(Player player, Holder<IAmuletDefinition> def) {
        ItemStack target = def.value().create();
        List<ItemStack> amulets = this.getAmuletsFromInventory(player);
        return CollectionUtil.anyMatch(amulets, stack -> ItemStack.isSameItem(stack, target));
    }

    public void setRaffleProbability(ServerPlayer player, Holder<IAmuletDefinition> def, int probability) {
        AmuletRaffleProbability arp = player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY);
        if (!this.hasAmuletInInventory(player, def)) {
            arp.setProbability(def, probability);
        } else {
            arp.setProbability(def, 0);
        }
    }

    public void inventoryTick(ServerPlayer player) {
        List<ItemStack> all = new ArrayList<>();
        for (Holder<IAmuletDefinition> def : this.definitions) {
            all.add(def.value().create());
        }
        List<ItemStack> now = this.getAmuletsFromInventory(player);
        for (ItemStack stack : now) {
            IAmulet amulet = stack.getOrDefault(ModComponents.AMULET, DoNothingAmulet.INSTANCE);
            all.removeIf(other -> amulet.canActAs(
                other.getOrDefault(ModComponents.AMULET, DoNothingAmulet.INSTANCE)
            ));
            amulet.inventoryTick(player, stack, true);
        }
        for (ItemStack stack : all) {
            IAmulet amulet = stack.getOrDefault(ModComponents.AMULET, DoNothingAmulet.INSTANCE);
            if (amulet instanceof WrappedOthersAmulet) return;
            amulet.inventoryTick(player, stack, false);
        }
    }

    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        return CollectionUtil.anyMatch(
            this.getAmuletsFromInventory(player),
            stack -> stack.getOrDefault(ModComponents.AMULET, DoNothingAmulet.INSTANCE).shouldImmune(player, source)
        );
    }
}
