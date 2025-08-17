package dev.dubhe.anvilcraft.api.amulet.fromto;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface ImmuneDamage extends BiPredicate<ServerPlayer, DamageSource> {
    ImmuneDamage NEVER = (player, source) -> false;

    boolean shouldImmuneDamage(ServerPlayer player, DamageSource source);

    @Override
    default boolean test(ServerPlayer player, DamageSource source) {
        return this.shouldImmuneDamage(player, source);
    }

    @Override
    default ImmuneDamage and(BiPredicate<? super ServerPlayer, ? super DamageSource> other) {
        return new Multiple(this).and(other);
    }

    @Override
    default ImmuneDamage negate() {
        return new Negate(this);
    }

    @Override
    default ImmuneDamage or(BiPredicate<? super ServerPlayer, ? super DamageSource> other) {
        return new Multiple(this).or(other);
    }

    class Negate implements ImmuneDamage {
        protected final ImmuneDamage self;
        private boolean isNegate = true;

        protected Negate(ImmuneDamage self) {
            this.self = self;
        }

        @Override
        public boolean shouldImmuneDamage(ServerPlayer player, DamageSource source) {
            return this.self.shouldImmuneDamage(player, source) ^ this.isNegate;
        }

        @Override
        public @NotNull ImmuneDamage negate() {
            this.isNegate = !this.isNegate;
            return this;
        }
    }

    class Multiple implements ImmuneDamage {
        private ImmuneDamage first;
        private final List<Sub> subs = new ArrayList<>();

        Multiple(ImmuneDamage first) {
            this.first = first;
        }

        @Override
        public boolean shouldImmuneDamage(ServerPlayer player, DamageSource source) {
            boolean result = this.first.shouldImmuneDamage(player, source);
            for (Sub sub : this.subs) {
                result = sub.shouldImmuneDamage(player, source, result);
            }
            return result;
        }

        @Override
        public @NotNull ImmuneDamage and(@NotNull BiPredicate<? super ServerPlayer, ? super DamageSource> other) {
            this.subs.add(new And(other));
            return this;
        }

        @Override
        public @NotNull ImmuneDamage negate() {
            this.first = this.first.negate();
            this.subs.getLast().negate();
            return this;
        }

        @Override
        public @NotNull ImmuneDamage or(@NotNull BiPredicate<? super ServerPlayer, ? super DamageSource> other) {
            this.subs.add(new Or(other));
            return this;
        }

        private abstract static class Sub {
            protected final BiPredicate<? super ServerPlayer, ? super DamageSource> self;
            protected boolean isNegate = true;

            protected Sub(BiPredicate<? super ServerPlayer, ? super DamageSource> self) {
                this.self = self;
            }

            abstract boolean shouldImmuneDamage(ServerPlayer player, DamageSource source, boolean otherResult);

            void negate() {
                this.isNegate = !this.isNegate;
            }
        }

        private static class And extends Sub {
            private And(BiPredicate<? super ServerPlayer, ? super DamageSource> self) {
                super(self);
            }

            @Override
            public boolean shouldImmuneDamage(ServerPlayer player, DamageSource source, boolean otherResult) {
                return (otherResult && this.self.test(player, source)) ^ this.isNegate;
            }
        }

        private static class Or extends Sub {
            private Or(BiPredicate<? super ServerPlayer, ? super DamageSource> self) {
                super(self);
            }

            @Override
            public boolean shouldImmuneDamage(ServerPlayer player, DamageSource source, boolean otherResult) {
                return (otherResult || this.self.test(player, source)) ^ this.isNegate;
            }
        }
    }
}
