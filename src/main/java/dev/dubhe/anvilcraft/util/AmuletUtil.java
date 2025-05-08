package dev.dubhe.anvilcraft.util;

import com.google.common.collect.Sets;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.ModDamageTypes;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.amulet.AbstractAmuletItem;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

@ParametersAreNonnullByDefault
public class AmuletUtil {
    public static BiPredicate<Player, Type> hasAmuletInInventory =
        (player, type) -> InventoryUtil.hasItem(player.getInventory(), type.getEntry().asItem());

    private static final Set<Type> types = Sets.newHashSet(
        new Type(
            "emerald", (sources, source) ->
            DamageSourceUtil.isEntityMatchTypes(source, EntityType.IRON_GOLEM, EntityType.PILLAGER),
            ModItems.EMERALD_AMULET
        ),
        new Type(
            "topaz", (sources, source) ->
            DamageSourceUtil.isMatchTypes(source, sources, DamageTypes.LIGHTNING_BOLT),
            ModItems.TOPAZ_AMULET
        ),
        new Type(
            "ruby", (sources, source) ->
            DamageSourceUtil.isMatchTypes(
                source, sources,
                DamageTypes.ON_FIRE, DamageTypes.CAMPFIRE, DamageTypes.LAVA, DamageTypes.HOT_FLOOR, ModDamageTypes.LASER
            ),
            ModItems.RUBY_AMULET
        ),
        new Type(
            "sapphire", (sources, source) ->
            DamageSourceUtil.isMatchTypes(source, sources, DamageTypes.DROWN, DamageTypes.DRY_OUT)
                || DamageSourceUtil.isEntityMatchTypes(source, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN),
            ModItems.SAPPHIRE_AMULET
        ),
        new Type(
            "anvil", (sources, source) ->
            DamageSourceUtil.isMatchTypes(source, sources, DamageTypes.FALLING_ANVIL)
                || (source.type().equals(sources.damageTypes.get(DamageTypes.FALLING_BLOCK))
                && source.getEntity() instanceof FallingGiantAnvilEntity)
                || Optional.ofNullable(source.getWeaponItem())
                .filter(item -> item.is(ModItemTags.ANVIL_HAMMER))
                .isPresent(),
            ModItems.ANVIL_AMULET
        ),
        new Type(
            "comrade", (sources, source) -> {
            if (source.getEntity() instanceof Player murder && source.getDirectEntity() instanceof Player victim) {
                return Optional.ofNullable(victim.getTeam())
                    .map(team -> team.getPlayers().contains(murder.getScoreboardName()))
                    .orElse(true);
            }

            return false;
        },
            ModItems.COMRADE_AMULET
        ),
        new Type(
            "feather", (sources, source) ->
            DamageSourceUtil.isMatchTypes(source, sources, DamageTypes.FALL),
            ModItems.FEATHER_AMULET
        ),
        new Type(
            "cat", (sources, source) ->
            DamageSourceUtil.isEntityMatchTypes(source, EntityType.CREEPER, EntityType.PHANTOM),
            ModItems.CAT_AMULET
        ),
        new Type(
            "dog", (sources, source) ->
            DamageSourceUtil.isEntityMatchTypes(
                source,
                EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.BOGGED
            ),
            ModItems.DOG_AMULET
        ),
        new Type(
            "silence", (sources, source) ->
            DamageSourceUtil.isEntityMatchTypes(source, EntityType.WARDEN),
            ModItems.SILENCE_AMULET
        )
    );

    public record Type(
        @Getter String typeId,
        BiPredicate<DamageSources, DamageSource> predicate,
        @Getter ItemEntry<? extends AbstractAmuletItem> entry
    ) {
        public boolean isValid(DamageSources sources, DamageSource source) {
            try {
                return this.predicate.test(sources, source);
            } catch (Throwable ignored) {
            }

            return false;
        }

        public boolean isValid(ItemEntry<? extends AbstractAmuletItem> entry) {
            try {
                return this.entry.equals(entry);
            } catch (Throwable ignored) {
            }

            return false;
        }
    }

    public static void registerCustomType(Type type) {
        types.add(type);
    }

    public static @Nullable Type getType(Player player, DamageSource source) {
        DamageSources sources = player.damageSources();
        for (Type type : types) {
            if (type.isValid(sources, source)) {
                return type;
            }
        }

        return null;
    }

    public static @Nullable Type getType(ItemEntry<? extends AbstractAmuletItem> entry) {
        for (Type type : types) {
            if (type.isValid(entry)) {
                return type;
            }
        }

        return null;
    }

    public static int getRaffleProbability(Player player, DamageSource source, boolean isConsumedInBox) {
        Type type = getType(player, source);
        if (type != null) {
            return getRaffleProbability(player, type, isConsumedInBox);
        }

        return 0;
    }

    public static int getRaffleProbability(Player player, Type type, boolean isConsumedInBox) {
        if (!hasAmuletInInventory(player, type)) {
            return getStoredRaffleProbability(player, type) + (isConsumedInBox ? 20 : 5);
        } else {
            return 0;
        }
    }

    public static int getStoredRaffleProbability(Player player, Type type) {
        return player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY).getInt(type.getTypeId());
    }

    public static void setRaffleProbability(Player player, DamageSource source, NonNullUnaryOperator<Integer> modifier) {
        Type type = getType(player, source);
        if (type != null) {
            setRaffleProbability(player, type, modifier);
        }
    }

    public static void setRaffleProbability(Player player, Type type, NonNullUnaryOperator<Integer> modifier) {
        CompoundTag root = player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY);
        if (!hasAmuletInInventory(player, type)) {
            root.putInt(type.getTypeId(), modifier.apply(root.getInt(type.getTypeId())));
        } else {
            root.putInt(type.getTypeId(), 0);
        }
    }

    public static boolean shouldIgnoreDamage(Player player, DamageSource source) {
        Type type = getType(player, source);
        if (type != null) {
            return hasAmuletInInventory(player, type.getEntry());
        } else {
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasAmuletInInventory(Player player, ItemEntry<? extends AbstractAmuletItem> entry) {
        try {
            return entry.isIn(
                InventoryUtil.getFirstItem(player.getInventory(), stack -> stack.getItem() instanceof AbstractAmuletItem)
            ) || InventoryUtil.hasItemInCompat(player, entry::isIn);
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasAmuletInInventory(Player player, Type type) {
        try {
            return hasAmuletInInventory.test(player, type);
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    public static ItemStack getEffectiveAmulet(Player player, ItemEntry<? extends AbstractAmuletItem> entry) {
        try {
            ItemStack stack = InventoryUtil.getFirstItem(player.getInventory(), stack1 -> stack1.getItem() instanceof AbstractAmuletItem);
            if (entry.isIn(stack)) {
                return stack;
            } else {
                return InventoryUtil.getItemInCompat(player, entry::isIn);
            }
        } catch (NullPointerException ignored) {
            return ItemStack.EMPTY;
        }
    }

    public static void startRaffle(ServerPlayer player, DamageSource source, boolean isConsumedInBox) {
        RandomSource random = player.getRandom();
        int raffleProbability = Math.min(getRaffleProbability(player, source, isConsumedInBox), 100);

        if (raffleProbability > random.nextIntBetweenInclusive(0, 100)) {
            Type type = getType(player, source);
            if (type != null) {
                InventoryUtil.addToInventory(player.getInventory(), type.getEntry().asStack());
            }

            AmuletUtil.setRaffleProbability(player, source, value -> 20);
        } else {
            AmuletUtil.setRaffleProbability(
                player, source,
                value -> Math.min(value + (isConsumedInBox ? 10 : 5), 100)
            );
        }
    }
}
