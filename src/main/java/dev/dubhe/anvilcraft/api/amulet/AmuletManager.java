package dev.dubhe.anvilcraft.api.amulet;

import dev.anvilcraft.lib.v2.util.CollectionUtil;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.WrappedOthersAmulet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AmuletManager {
    private static final List<BiConsumer<Player, List<ItemStack>>> AMULET_FINDERS = new ArrayList<>();
    private final List<Holder<IAmuletDefinition>> definitions;

    static {
        AmuletManager.registerFinders(
            (player, holders) -> AmuletManager.processFoundStack(player.getWeaponItem(), holders),
            (player, holders) -> AmuletManager.processFoundStack(player.getOffhandItem(), holders)
        );
    }

    private AmuletManager(List<Holder<IAmuletDefinition>> definitions) {
        this.definitions = definitions;
    }

    public static AmuletManager get(HolderLookup.Provider registries) {
        return new AmuletManager(AmuletManager.extractDefinitions(registries));
    }

    public static List<Holder<IAmuletDefinition>> extractDefinitions(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(ModRegistries.AMULET_DEF)
            .listElements()
            .<Holder<IAmuletDefinition>>map(Function.identity())
            .toList();
    }

    @SafeVarargs
    public static void registerFinders(BiConsumer<Player, List<ItemStack>>... typeFinders) {
        Collections.addAll(AmuletManager.AMULET_FINDERS, typeFinders);
    }

    public static void processFoundStack(ItemStack found, List<ItemStack> holders) {
        if (found.is(ModItems.AMULET_BOX)) {
            BoxContents contents = found.get(ModComponents.BOX_CONTENTS);
            if (contents == null) return;
            for (ItemStack stack : contents.amulets()) {
                if (stack.has(ModComponents.AMULET)) {
                    holders.add(stack.copy());
                }
            }
        } else if (found.has(ModComponents.AMULET)) {
            holders.add(found);
        }
    }

    public List<ItemStack> getAmuletsFromInventory(Player player) {
        List<ItemStack> amulets = new ArrayList<>();
        for (BiConsumer<Player, List<ItemStack>> finder : AmuletManager.AMULET_FINDERS) {
            finder.accept(player, amulets);
        }
        amulets.removeIf(stack -> !stack.has(ModComponents.AMULET));
        return amulets;
    }

    public void startRaffle(ServerPlayer player, DamageSource source) {
        Optional<IAmuletDefinition> defOp = this.getDefinitionMatchedDamage(player, source).map(Holder::value);
        if (defOp.isEmpty()) return;
        IAmuletDefinition def = defOp.get();
        ItemStack amulet = def.create();
        if (!InventoryUtil.getFirstItem(player.getInventory(), amulet.getItem()).isEmpty()) return;
        if (!InventoryUtil.getItemInCompat(player, stack -> ItemStack.isSameItem(stack, amulet)).isEmpty()) return;

        RandomSource random = player.getRandom();
        int probability = Math.min(this.getRaffleProbability(player, source), 100);
        if (probability > random.nextIntBetweenInclusive(0, 100)) {
            player.getInventory().placeItemBackInInventory(amulet.copy());
            this.setRaffleProbability(player, source, 0);
        } else {
            this.setRaffleProbability(player, source, Math.min(probability + 10, 100));
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

    public boolean shouldIgnoreDamage(ServerPlayer player, DamageSource source) {
        return CollectionUtil.anyMatch(
            this.getAmuletsFromInventory(player),
            stack -> stack.get(ModComponents.AMULET).shouldImmune(player, source)
        );
    }
}
