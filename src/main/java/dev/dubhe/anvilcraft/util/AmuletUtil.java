package dev.dubhe.anvilcraft.util;

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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiPredicate;

@ParametersAreNonnullByDefault
public class AmuletUtil {
    public static @Nullable Type getType(Player player, DamageSource source) {
        DamageSources sources = player.damageSources();
        for (Type type : Type.values()) {
            if (type.isValid(sources, source)) {
                return type;
            }
        }

        return null;
    }

    public static int getRaffleProbability(Player player, DamageSource source, boolean isConsumeAmuletBox) {
        DamageSources sources = player.damageSources();
        for (Type type : Type.values()) {
            if (type.isValid(sources, source)) {
                return getRaffleProbability(player, type, isConsumeAmuletBox);
            }
        }

        return 0;
    }

    public static int getRaffleProbability(Player player, Type type, boolean isConsumeAmuletBox) {
        if (!hasAmuletInInventory(player, type)) {
            return getStoredRaffleProbability(player, type) + (isConsumeAmuletBox ? 20 : 5);
        } else {
            return 0;
        }
    }

    public static int getStoredRaffleProbability(Player player, Type type) {
        return player.getData(ModDataAttachments.AMULET_RAFFLE_PROBABILITY).getInt(type.getTypeId());
    }

    public static void setRaffleProbability(Player player, DamageSource source, NonNullUnaryOperator<Integer> modifier) {
        DamageSources sources = player.damageSources();
        for (Type type : Type.values()) {
            if (type.isValid(sources, source)) {
                setRaffleProbability(player, type, modifier);
            }
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasAmuletInInventory(Player player, Type type) {
        return player.getInventory().hasAnyOf(Collections.singleton(type.getEntry().asItem()));
    }

    public static void startRaffle(ServerPlayer player, DamageSource source, boolean isConsumeAmuletBox) {
        RandomSource random = player.getRandom();
        int raffleProbability = Math.min(getRaffleProbability(player, source, isConsumeAmuletBox), 50);

        if (raffleProbability > random.nextIntBetweenInclusive(0, 100)) {
            AmuletUtil.setRaffleProbability(player, source, value -> 0);

            Type type = getType(player, source);
            if (type != null) {
                player.getInventory().add(type.getEntry().asStack());
            }
        } else {
            AmuletUtil.setRaffleProbability(player, source, value -> Math.min(value + 5, 50));
        }
    }

    public enum Type {
        EMERALD(
            "emerald", (sources, source) ->
            DamageSourceUtil.isEntityMatchTypes(source, EntityType.IRON_GOLEM, EntityType.PILLAGER),
            ModItems.EMERALD_AMULET
        ),
        TOPAZ(
            "topaz", (sources, source) ->
            DamageSourceUtil.isMatchTypes(source, sources, DamageTypes.LIGHTNING_BOLT),
            ModItems.TOPAZ_AMULET
        ),
        RUBY(
            "ruby", (sources, source) ->
            DamageSourceUtil.isMatchTypes(
                source, sources,
                DamageTypes.ON_FIRE, DamageTypes.CAMPFIRE, DamageTypes.LAVA, DamageTypes.HOT_FLOOR, ModDamageTypes.LASER
            ),
            ModItems.RUBY_AMULET
        ),
        SAPPHIRE(
            "sapphire", (sources, source) ->
            DamageSourceUtil.isMatchTypes(source, sources, DamageTypes.DROWN, DamageTypes.DRY_OUT)
                || DamageSourceUtil.isEntityMatchTypes(source, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN),
            ModItems.SAPPHIRE_AMULET
        ),
        ANVIL(
            "anvil", (sources, source) ->
            DamageSourceUtil.isMatchTypes(source, sources, DamageTypes.FALLING_ANVIL)
                || (source.type().equals(sources.damageTypes.get(DamageTypes.FALLING_BLOCK)) && source.getEntity() instanceof FallingGiantAnvilEntity)
                || Optional.ofNullable(source.getWeaponItem())
                .filter(item -> item.is(ModItemTags.ANVIL_HAMMER))
                .isPresent(),
            ModItems.ANVIL_AMULET
        ),
        //COGWHEEL(
        //    "cogwheel", (sources, source) ->
        //    source.type().equals(sources.damageTypes.get(AllDamageTypes.CRUSH))
        //    || source.type().equals(sources.damageTypes.get(AllDamageTypes.CUCKOO_SURPRISE))
        //    || source.type().equals(sources.damageTypes.get(AllDamageTypes.FAN_FIRE))
        //    || source.type().equals(sources.damageTypes.get(AllDamageTypes.FAN_LAVA))
        //    || source.type().equals(sources.damageTypes.get(AllDamageTypes.DRILL))
        //    || source.type().equals(sources.damageTypes.get(AllDamageTypes.ROLLER))
        //    || source.type().equals(sources.damageTypes.get(AllDamageTypes.SAW))
        //    || source.type().equals(sources.damageTypes.get(AllDamageTypes.POTATO_CANNON))
        //    || source.type().equals(sources.damageTypes.get(AllDamageTypes.RUN_OVER)),
        //    ModItems.COGWHEEL_AMULET
        //),
        COMRADE(
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
        FEATHER(
            "feather", (sources, source) ->
            DamageSourceUtil.isMatchTypes(source, sources, DamageTypes.FALL),
            ModItems.FEATHER_AMULET
        ),
        CAT(
            "cat", (sources, source) ->
            DamageSourceUtil.isEntityMatchTypes(source, EntityType.CREEPER, EntityType.PHANTOM),
            ModItems.CAT_AMULET
        ),
        DOG(
            "dog", (sources, source) ->
            DamageSourceUtil.isEntityMatchTypes(
                source,
                EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.BOGGED
            ),
            ModItems.DOG_AMULET
        ),
        SILENCE(
            "silence", (sources, source) ->
            DamageSourceUtil.isEntityMatchTypes(source, EntityType.WARDEN),
            ModItems.SILENCE_AMULET
        ),
        ;

        @Getter
        private final String typeId;
        private final BiPredicate<DamageSources, DamageSource> predicate;
        @Getter
        private final ItemEntry<? extends AbstractAmuletItem> entry;

        Type(String typeId, BiPredicate<DamageSources, DamageSource> predicate, ItemEntry<? extends AbstractAmuletItem> entry) {
            this.typeId = typeId;
            this.predicate = predicate;
            this.entry = entry;
        }

        public boolean isValid(DamageSources sources, DamageSource source) {
            try {
                return this.predicate.test(sources, source);
            } catch (Throwable ignored) {

            }

            return false;
        }
    }
}
