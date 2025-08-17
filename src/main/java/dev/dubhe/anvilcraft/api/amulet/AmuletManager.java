package dev.dubhe.anvilcraft.api.amulet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.api.item.property.BoxContents;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.item.amulet.AmuletItem;
import dev.dubhe.anvilcraft.util.CollectionUtil;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class AmuletManager {
    public static final AmuletManager INSTANCE = new AmuletManager();
    private final Set<Supplier<AmuletItem>> amuletItems = Sets.newConcurrentHashSet();
    private final List<BiConsumer<Player, List<ItemStack>>> amuletFinders = new ArrayList<>();

    private AmuletManager() {
        this.registerAmulets(
            ModItems.EMERALD_AMULET::get,
            ModItems.TOPAZ_AMULET::get,
            ModItems.RUBY_AMULET::get,
            ModItems.SAPPHIRE_AMULET::get,
            ModItems.ANVIL_AMULET::get,
            ModItems.COMRADE_AMULET::get,
            ModItems.FEATHER_AMULET::get,
            ModItems.CAT_AMULET::get,
            ModItems.DOG_AMULET::get,
            ModItems.SILENCE_AMULET::get,
            ModItems.ABNORMAL_AMULET::get,
            ModItems.GEM_AMULET::get,
            ModItems.NATURE_AMULET::get
        );
        this.registerFinders(
            (player, holders) -> processFoundStack(player.getWeaponItem(), holders),
            (player, holders) -> processFoundStack(player.getOffhandItem(), holders)
        );
    }

    @SafeVarargs
    public final void registerAmulets(Supplier<AmuletItem>... amuletItems) {
        Collections.addAll(this.amuletItems, amuletItems);
    }

    @SafeVarargs
    public final void registerFinders(BiConsumer<Player, List<ItemStack>>... typeFinders) {
        Collections.addAll(this.amuletFinders, typeFinders);
    }

    public static void processFoundStack(ItemStack found, List<ItemStack> holders) {
        if (found.is(ModItems.AMULET_BOX)) {
            BoxContents contents = found.get(ModComponents.BOX_CONTENTS);
            if (contents == null) return;
            for (ItemStack stack : contents.getAmulets()) {
                if (stack.getItem() instanceof AmuletItem) {
                    holders.add(stack.copy());
                }
            }
        } else if (found.getItem() instanceof AmuletItem) {
            holders.add(found);
        }
    }

    public HashMultimap<Holder<AmuletType>, ItemStack> getAmuletsFromInventory(Player player) {
        List<ItemStack> amuletItems = new ArrayList<>();
        for (BiConsumer<Player, List<ItemStack>> amuletFinder : this.amuletFinders) {
            amuletFinder.accept(player, amuletItems);
        }
        return CollectionUtil.newMultimap(
            HashMultimap.create(), amuletItems, stack -> ((AmuletItem) stack.getItem()).getType()
        );
    }

    public List<Holder<AmuletType>> getTypesFromInventory(Player player) {
        return List.copyOf(this.getAmuletsFromInventory(player).keySet());
    }

    public Optional<Holder<AmuletType>> getTypeMatchedDamage(ServerPlayer player, DamageSource source, HolderLookup.Provider registryAccess) {
        Optional<HolderLookup.RegistryLookup<AmuletType>> lookupOptional = registryAccess.lookup(ModRegistries.AMULET_TYPE_KEY);
        return lookupOptional.flatMap(lookup -> lookup.listElements()
            .filter(reference -> reference.value().canObtain(player, source))
            .findFirst());
    }

    public void startRaffle(ServerPlayer player, DamageSource source, boolean isConsumedInBox) {
        Optional<AmuletType> typeOp = this.getTypeMatchedDamage(player, source, player.registryAccess()).map(Holder::value);
        if (typeOp.isEmpty()) return;
        AmuletType type = typeOp.get();
        ItemStack amulet = type.amulet().get();
        if (!InventoryUtil.getFirstItem(player.getInventory(), amulet.getItem()).isEmpty()) return;
        if (!InventoryUtil.getItemInCompat(player, stack -> ItemStack.isSameItem(stack, amulet)).isEmpty()) return;

        RandomSource random = player.getRandom();
        int probability = Math.min(this.getRaffleProbability(player, source, isConsumedInBox), 100);
        if (probability > random.nextIntBetweenInclusive(0, 100)) {
            player.getInventory().placeItemBackInInventory(amulet.copy());
            this.setRaffleProbability(player, source, 0);
        } else {
            this.setRaffleProbability(player, source, Math.min(probability + (isConsumedInBox ? 10 : 5), 100));
        }
    }

    public static int getStoredRaffleProbability(Player player, AmuletType type) {
        return player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY).getProbability(type);
    }

    public int getRaffleProbability(Player player, DamageSource source, boolean isConsumedInBox) {
        Optional<Holder<AmuletType>> type = Optional.empty();
        if (player instanceof ServerPlayer serverPlayer) {
            type = this.getTypeMatchedDamage(serverPlayer, source, serverPlayer.registryAccess());
        }
        return type.map(holder -> this.getRaffleProbability(player, holder, isConsumedInBox)).orElse(0);
    }

    public int getRaffleProbability(Player player, Holder<AmuletType> type, boolean isConsumedInBox) {
        if (!this.hasAmuletInInventory(player, type)) {
            return getStoredRaffleProbability(player, type.value()) + (isConsumedInBox ? 20 : 5);
        } else {
            return 0;
        }
    }

    public void setRaffleProbability(ServerPlayer player, DamageSource source, int probability) {
        Optional<Holder<AmuletType>> typeHolder = this.getTypeMatchedDamage(player, source, player.registryAccess());
        typeHolder.ifPresent(holder -> this.setRaffleProbability(player, holder, probability));
    }

    public void setRaffleProbability(ServerPlayer player, Holder<AmuletType> type, int probability) {
        AmuletRaffleProbability arp = player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY);
        if (!this.hasAmuletInInventory(player, type)) {
            arp.setProbability(type.value(), probability);
        } else {
            arp.setProbability(type.value(), 0);
        }
    }

    public boolean hasAmuletInInventory(Player player, ItemLike itemLike) {
        List<Holder<AmuletType>> holders = this.getTypesFromInventory(player);
        return !holders.isEmpty() && CollectionUtil.anyMatch(holders, holder -> holder.value().matches(itemLike));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasAmuletInInventory(Player player, Holder<AmuletType> type) {
        List<Holder<AmuletType>> holders = this.getTypesFromInventory(player);
        return !holders.isEmpty() && CollectionUtil.anyMatch(holders, holder -> holder.equals(type));
    }

    public void inventoryTick(ServerPlayer player) {
        HashMultimap<Holder<AmuletType>, ItemStack> amulets = this.getAmuletsFromInventory(player);
        for (Supplier<AmuletItem> amuletGetter : this.amuletItems) {
            AmuletItem amuletItem = amuletGetter.get();
            Holder<AmuletType> type = amuletItem.getType();
            if (amulets.containsKey(type)) {
                for (ItemStack amulet : amulets.get(type)) {
                    type.value().inventoryTick(player, amulet, true);
                }
            } else {
                type.value().inventoryTick(player, type.value().amulet().get(), false);
            }
        }
    }

    public boolean shouldIgnoreDamage(ServerPlayer player, DamageSource source) {
        List<Holder<AmuletType>> holders = this.getTypesFromInventory(player);
        return !holders.isEmpty() && CollectionUtil.anyMatch(holders, holder -> holder.value().shouldImmuneDamage(player, source));
    }
}
