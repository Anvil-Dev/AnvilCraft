package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.api.amulet.type.FourToOneAmuletType;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.item.abnormal.IAbnormal;
import dev.dubhe.anvilcraft.item.abnormal.ICursed;
import dev.dubhe.anvilcraft.item.abnormal.ILevitation;
import dev.dubhe.anvilcraft.item.abnormal.IRadiation;
import dev.dubhe.anvilcraft.item.abnormal.ISuperHeavy;
import dev.dubhe.anvilcraft.item.amulet.ComradeAmuletItem;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.scores.Team;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.dubhe.anvilcraft.init.ModDataAttachments.DISCOUNT_RATE;

public class ModAmuletTypes {
    private static final DeferredRegister<AmuletType> REGISTER = DeferredRegister.create(ModRegistries.AMULET_TYPE_KEY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<AmuletType, ? extends AmuletType> EMERALD = register(
        "emerald",
        type -> AmuletType.builder()
            .obtainByMurder(type)
            .inventoryTick((player, amulet, isEnabled) -> {
                if (isEnabled) {
                    player.setData(DISCOUNT_RATE, 0.3f);
                } else {
                    player.removeData(DISCOUNT_RATE);
                }
            })
            .amulet(ModItems.EMERALD_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> TOPAZ = register(
        "topaz",
        type -> AmuletType.builder()
            .obtainByDamage(type)
            .immuneDamageFromObtain()
            .amulet(ModItems.TOPAZ_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> RUBY = register(
        "ruby",
        type -> AmuletType.builder()
            .obtainByDamage(type)
            .inventoryTick((player, amulet, isEnabled) -> {
                if (!isEnabled || player.isInLava()) return;
                MobEffectInstance effect = player.getEffect(MobEffects.FIRE_RESISTANCE);
                if (effect == null) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.FIRE_RESISTANCE,
                        2, 0, false, false
                    ));
                } else if (effect.getDuration() < 3600) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.FIRE_RESISTANCE,
                        effect.getDuration() + 2, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible()
                    ));
                }
            })
            .amulet(ModItems.RUBY_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> SAPPHIRE = register(
        "sapphire",
        type -> AmuletType.builder()
            .obtainByDamageOr(type)
            .obtainByMurderOr(type)
            .inventoryTick((player, amulet, isEnabled) -> {
                if (!isEnabled || player.isInWater()) return;
                MobEffectInstance effect = player.getEffect(MobEffects.CONDUIT_POWER);
                if (effect == null) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.CONDUIT_POWER,
                        2, 0, false, false
                    ));
                } else if (effect.getDuration() < 3600) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.CONDUIT_POWER,
                        effect.getDuration() + 2, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible()
                    ));
                }
            })
            .amulet(ModItems.SAPPHIRE_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> ANVIL = register(
        "anvil",
        type -> AmuletType.builder()
            .obtain((player, source) -> {
                if (source.typeHolder().is(DamageTypes.FALLING_ANVIL)) return true;
                if (Optional.ofNullable(source.getEntity())
                    .flatMap(entity -> Util.castSafely(entity, FallingBlockEntity.class))
                    .or(() -> Optional.ofNullable(source.getDirectEntity())
                        .flatMap(entity -> Util.castSafely(entity, FallingBlockEntity.class)))
                    .map(fbe -> fbe.getBlockState().is(BlockTags.ANVIL))
                    .orElse(false)
                ) return true;
                if (Optional.ofNullable(source.getEntity())
                    .map(entity -> Util.instanceOfAny(entity, FallingGiantAnvilEntity.class))
                    .or(() -> Optional.ofNullable(source.getDirectEntity())
                        .map(entity -> Util.instanceOfAny(entity, FallingGiantAnvilEntity.class)))
                    .orElse(false)
                ) return true;
                return Optional.ofNullable(source.getWeaponItem())
                    .map(stack -> stack.is(ModItemTags.ANVIL_HAMMER))
                    .orElse(false);
            })
            .immuneDamageFromObtain()
            .amulet(ModItems.ANVIL_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> COMRADE = register(
        "comrade",
        type -> AmuletType.builder()
            .obtain((victim, source) -> {
                ServerPlayer murder = Util.castSafely(source.getEntity(), ServerPlayer.class).orElse(null);
                if (murder == null) return false;
                Team victimTeam = victim.getTeam();
                Team murderTeam = murder.getTeam();
                return victimTeam == null ? murderTeam == null : victimTeam.isAlliedTo(murderTeam);
            })
            .inventoryTick(ComradeAmuletItem::inventoryTick)
            .immuneDamage(ComradeAmuletItem::shouldImmuneDamage)
            .amulet(ModItems.COMRADE_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> FEATHER = register(
        "feather",
        type -> AmuletType.builder()
            .obtainByDamage(type)
            .immuneDamageFromObtain()
            .amulet(ModItems.FEATHER_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> CAT = register(
        "cat",
        type -> AmuletType.builder()
            .obtainByMurder(type)
            .inventoryTick((player, amulet, isEnabled) -> {
                CompoundTag root = player.getData(ModDataAttachments.SCARE_ENTITIES);
                root.putBoolean("creepers", isEnabled);
                root.putBoolean("phantoms", isEnabled);
                player.setData(ModDataAttachments.SCARE_ENTITIES, root);
            })
            .amulet(ModItems.CAT_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> DOG = register(
        "dog",
        type -> AmuletType.builder()
            .obtainByMurder(type)
            .inventoryTick((player, amulet, isEnabled) -> {
                CompoundTag root = player.getData(ModDataAttachments.SCARE_ENTITIES);
                root.putBoolean("skeletons", isEnabled);
                player.setData(ModDataAttachments.SCARE_ENTITIES, root);
            })
            .amulet(ModItems.DOG_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> SILENCE = register(
        "silence",
        type -> AmuletType.builder()
            .obtainByMurder(type)
            .amulet(ModItems.SILENCE_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> ABNORMAL = register(
        "abnormal",
        type -> AmuletType.builder()
            .obtainByDamage(type)
            .obtain((player, source) -> IAbnormal.getAbnormalCount(player, ICursed.class) > 0
                    && IAbnormal.getAbnormalCount(player, ILevitation.class) >= 64
                    && IAbnormal.getAbnormalCount(player, ISuperHeavy.class) > 0
                    && IAbnormal.getAbnormalCount(player, IRadiation.class) >= 1152)
            .amulet(ModItems.ABNORMAL_AMULET)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> GEM = registerFour(
        "gem",
        () -> FourToOneAmuletType.of(
            ModItems.GEM_AMULET::asStack,
            ModAmuletTypes.SAPPHIRE, ModAmuletTypes.RUBY, ModAmuletTypes.TOPAZ, ModAmuletTypes.EMERALD)
    );
    public static final DeferredHolder<AmuletType, ? extends AmuletType> NATURE = registerFour(
        "nature",
        () -> FourToOneAmuletType.of(
            ModItems.NATURE_AMULET::asStack,
            ModAmuletTypes.SILENCE, ModAmuletTypes.FEATHER, ModAmuletTypes.CAT, ModAmuletTypes.DOG)
    );

    private static DeferredHolder<AmuletType, ? extends AmuletType> register(String typeId, Function<String, AmuletType.Builder> builder) {
        return REGISTER.register(typeId, builder.apply(typeId)::build);
    }

    private static DeferredHolder<AmuletType, ? extends FourToOneAmuletType> registerFour(
        String typeId, Supplier<? extends FourToOneAmuletType> getter
    ) {
        return REGISTER.register(typeId, getter);
    }

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
