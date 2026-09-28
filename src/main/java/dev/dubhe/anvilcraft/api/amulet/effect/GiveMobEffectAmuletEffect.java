package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.predicate.InWaterOrBreathingPredicate;
import dev.dubhe.anvilcraft.predicate.NotInLavaPredicate;
import dev.dubhe.anvilcraft.predicate.NotInWaterPredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/// 在满足给定谓词时给予佩戴者药水效果，没有谓词则一直给予。<br>
/// 效果时长由 {@link Entry#duration} 依传入值算出，并由 {@link MinMaxBounds.Ints 时长上下界} 限制。
public record GiveMobEffectAmuletEffect(
    Optional<EntityPredicate> predicate,
    List<Entry> effects
) implements IAmuletEffect {
    /// 求值 {@link Entry#duration} 时按名字绑上的传入值：既有药水效果的剩余时长，也就是表达式里的 {@code $(current)}。
    public static final String VAR_CURRENT = "current";
    /// 求值 {@link Entry#duration} 时按名字绑上的传入值：本次要给予的效果自身时长，也就是表达式里的 {@code $(extra)}。
    public static final String VAR_EXTRA = "extra";
    /// {@link Entry#duration} 缺省时使用的表达式：在既有剩余时长上再叠一份效果自身时长，即 {@code $(current)+$(extra)}。
    public static final IExpression DEFAULT_DURATION = LibBuiltInFunctions.ADD.call(
        IExpression.ref(VAR_CURRENT),
        IExpression.ref(VAR_EXTRA)
    );
    /// 条件刷新用的时长表达式：只取效果自身时长，把剩余时长重置回该值，即 {@code $(extra)}。
    public static final IExpression REFRESH_DURATION = IExpression.ref(VAR_EXTRA);
    /// 条件刷新给予的效果时长：每 tick 重置回该值，保证不再闪烁。
    public static final int REFRESH_TICKS = 205;

    /// 佩戴者不在水里时给予，时长按上下界累加
    public static GiveMobEffectAmuletEffect notInWater(MobEffectInstance effect, MinMaxBounds.Ints bounds) {
        return new GiveMobEffectAmuletEffect(
            Optional.of(GiveMobEffectAmuletEffect.subPredicate(new NotInWaterPredicate())),
            List.of(Entry.of(effect, bounds))
        );
    }

    /// 佩戴者不在岩浆里时给予，时长按上下界累加
    public static GiveMobEffectAmuletEffect notInLava(MobEffectInstance effect, MinMaxBounds.Ints bounds) {
        return new GiveMobEffectAmuletEffect(
            Optional.of(GiveMobEffectAmuletEffect.subPredicate(new NotInLavaPredicate())),
            List.of(Entry.of(effect, bounds))
        );
    }

    /// 无条件持续刷新该效果
    public static GiveMobEffectAmuletEffect always(Holder<MobEffect> effect, int amplifier) {
        return new GiveMobEffectAmuletEffect(
            Optional.empty(),
            List.of(Entry.refresh(GiveMobEffectAmuletEffect.refreshInstance(effect, amplifier)))
        );
    }

    /// 佩戴者着火时持续刷新该效果
    public static GiveMobEffectAmuletEffect onFire(Holder<MobEffect> effect, int amplifier) {
        return GiveMobEffectAmuletEffect.refresh(
            GiveMobEffectAmuletEffect.flags(flags -> flags.setOnFire(true)),
            effect,
            amplifier
        );
    }

    /// 佩戴者没着火时持续刷新该效果
    public static GiveMobEffectAmuletEffect notOnFire(Holder<MobEffect> effect, int amplifier) {
        return GiveMobEffectAmuletEffect.refresh(
            GiveMobEffectAmuletEffect.flags(flags -> flags.setOnFire(false)),
            effect,
            amplifier
        );
    }

    /// 佩戴者潜行时持续刷新该效果
    public static GiveMobEffectAmuletEffect sneaking(Holder<MobEffect> effect, int amplifier) {
        return GiveMobEffectAmuletEffect.refresh(
            GiveMobEffectAmuletEffect.flags(flags -> flags.setCrouching(true)),
            effect,
            amplifier
        );
    }

    /// 佩戴者没潜行时持续刷新该效果
    public static GiveMobEffectAmuletEffect notSneaking(Holder<MobEffect> effect, int amplifier) {
        return GiveMobEffectAmuletEffect.refresh(
            GiveMobEffectAmuletEffect.flags(flags -> flags.setCrouching(false)),
            effect,
            amplifier
        );
    }

    /// 佩戴者在水里或戴着能呼吸的头盔时持续刷新该效果
    public static GiveMobEffectAmuletEffect inWaterOrBreathing(Holder<MobEffect> effect, int amplifier) {
        return GiveMobEffectAmuletEffect.refresh(
            GiveMobEffectAmuletEffect.subPredicate(new InWaterOrBreathingPredicate()),
            effect,
            amplifier
        );
    }

    /// 在给定谓词成立时持续刷新该效果
    public static GiveMobEffectAmuletEffect refresh(EntityPredicate predicate, Holder<MobEffect> effect, int amplifier) {
        return new GiveMobEffectAmuletEffect(
            Optional.of(predicate),
            List.of(Entry.refresh(GiveMobEffectAmuletEffect.refreshInstance(effect, amplifier)))
        );
    }

    private static MobEffectInstance refreshInstance(Holder<MobEffect> effect, int amplifier) {
        return new MobEffectInstance(effect, GiveMobEffectAmuletEffect.REFRESH_TICKS, amplifier, false, false, true);
    }

    private static EntityPredicate flags(Consumer<EntityFlagsPredicate.Builder> consumer) {
        EntityFlagsPredicate.Builder flags = EntityFlagsPredicate.Builder.flags();
        consumer.accept(flags);
        return EntityPredicate.Builder.entity().flags(flags).build();
    }

    private static EntityPredicate subPredicate(EntitySubPredicate subPredicate) {
        return EntityPredicate.Builder.entity().subPredicate(subPredicate).build();
    }

    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        if (!(entity.level() instanceof ServerLevel level) || !ctx.getOrDefault(ModAmuletEffectContextKeys.ENABLED, false)) {
            return;
        }
        if (this.predicate.isPresent() && !this.predicate.get().matches(level, entity.position(), entity)) {
            return;
        }
        for (Entry entry : this.effects) {
            MobEffectInstance effect = entry.effect();
            Optional<MinMaxBounds.Ints> boundsOp = entry.bounds();
            Holder<MobEffect> type = effect.getEffect();
            MobEffectInstance exist = entity.getEffect(type);
            if (exist == null) {
                entity.addEffect(new MobEffectInstance(
                    type,
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
                ));
                exist = entity.getEffect(type);
                if (exist != null) {
                    exist.getCures().clear();
                    exist.getCures().addAll(effect.getCures());
                }
            } else if (boundsOp.isEmpty()) {
                exist.update(new MobEffectInstance(
                    type,
                    entry.duration().evaluateInt(GiveMobEffectAmuletEffect.durationInputs(exist.getDuration(), effect.getDuration())),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
                ));
                exist.getCures().clear();
                exist.getCures().addAll(effect.getCures());
                if (entity instanceof ServerPlayer player) {
                    player.connection.send(new ClientboundUpdateMobEffectPacket(entity.getId(), exist, false));
                }
            } else if (boundsOp.get().matches(exist.getDuration())) {
                MinMaxBounds.Ints bounds = boundsOp.get();
                exist.update(new MobEffectInstance(
                    type,
                    Math.clamp(
                        entry.duration().evaluateInt(GiveMobEffectAmuletEffect.durationInputs(exist.getDuration(), effect.getDuration())),
                        bounds.min().orElse(0),
                        bounds.max().orElse(Integer.MAX_VALUE)
                    ),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
                ));
                exist.getCures().clear();
                exist.getCures().addAll(effect.getCures());
                if (entity instanceof ServerPlayer player) {
                    player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), exist, false));
                }
            }
        }
    }

    /// 求值 {@link Entry#duration} 的传入值：第 0 个传入值与 {@link #VAR_CURRENT} 都是既有剩余时长，{@link #VAR_EXTRA} 是效果自身时长。
    private static Arguments durationInputs(int current, int extra) {
        return Arguments.of(current).withAll(
            List.of(VAR_CURRENT, VAR_EXTRA),
            List.of(new Arguments.Value.Single(current), new Arguments.Value.Single(extra))
        );
    }

    /// 一条要给予的药水效果
    ///
    /// @param effect   效果自身，其中的时长是 {@link #duration} 里的 {@code $(extra)}
    /// @param bounds   既有剩余时长的上下界，不满足时不叠加时长
    /// @param duration 剩余时长的计算方式，见 {@link GiveMobEffectAmuletEffect#DEFAULT_DURATION}
    public record Entry(MobEffectInstance effect, Optional<MinMaxBounds.Ints> bounds, IExpression duration) {
        /// 时长按上下界累加
        public static Entry of(MobEffectInstance effect, MinMaxBounds.Ints bounds) {
            return new Entry(effect, Optional.of(bounds), GiveMobEffectAmuletEffect.DEFAULT_DURATION);
        }

        /// 每 tick 把剩余时长重置回效果自身时长
        public static Entry refresh(MobEffectInstance effect) {
            return new Entry(effect, Optional.empty(), GiveMobEffectAmuletEffect.REFRESH_DURATION);
        }
    }
}
