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
public interface Obtain extends BiPredicate<ServerPlayer, DamageSource> {
    Obtain NEVER = (player, source) -> false;

    boolean canObtain(ServerPlayer player, DamageSource source);

    @Override
    default boolean test(ServerPlayer player, DamageSource source) {
        return this.canObtain(player, source);
    }

    @Override
    default Obtain and(BiPredicate<? super ServerPlayer, ? super DamageSource> other) {
        return new Multiple(this).and(other);
    }

    @Override
    default Obtain negate() {
        return new Negate(this);
    }

    @Override
    default Obtain or(BiPredicate<? super ServerPlayer, ? super DamageSource> other) {
        return new Multiple(this).or(other);
    }

    class Negate implements Obtain {
        protected final Obtain self;
        private boolean isNegate = true;

        protected Negate(Obtain self) {
            this.self = self;
        }

        @Override
        public boolean canObtain(ServerPlayer player, DamageSource source) {
            return this.self.canObtain(player, source) ^ this.isNegate;
        }

        @Override
        public @NotNull Obtain negate() {
            this.isNegate = !this.isNegate;
            return this;
        }
    }

    class Multiple implements Obtain {
        private Obtain first;
        private final List<Sub> subs = new ArrayList<>();

        Multiple(Obtain first) {
            this.first = first;
        }

        @Override
        public boolean canObtain(ServerPlayer player, DamageSource source) {
            boolean result = this.first.canObtain(player, source);
            for (Sub sub : this.subs) {
                result = sub.canObtain(player, source, result);
            }
            return result;
        }

        @Override
        public @NotNull Obtain and(@NotNull BiPredicate<? super ServerPlayer, ? super DamageSource> other) {
            this.subs.add(new And(other));
            return this;
        }

        @Override
        public @NotNull Obtain negate() {
            this.subs.getLast().negate();
            return this;
        }

        @Override
        public @NotNull Obtain or(@NotNull BiPredicate<? super ServerPlayer, ? super DamageSource> other) {
            this.subs.add(new Or(other));
            return this;
        }

        private abstract static class Sub {
            protected final BiPredicate<? super ServerPlayer, ? super DamageSource> self;
            protected boolean isNegate = false;

            protected Sub(BiPredicate<? super ServerPlayer, ? super DamageSource> self) {
                this.self = self;
            }

            abstract boolean canObtain(ServerPlayer player, DamageSource source, boolean otherResult);

            void negate() {
                this.isNegate = !this.isNegate;
            }
        }

        private static class And extends Sub {
            private And(BiPredicate<? super ServerPlayer, ? super DamageSource> self) {
                super(self);
            }

            @Override
            public boolean canObtain(ServerPlayer player, DamageSource source, boolean otherResult) {
                return (otherResult && this.self.test(player, source)) ^ this.isNegate;
            }
        }

        private static class Or extends Sub {
            private Or(BiPredicate<? super ServerPlayer, ? super DamageSource> self) {
                super(self);
            }

            @Override
            public boolean canObtain(ServerPlayer player, DamageSource source, boolean otherResult) {
                return (otherResult || this.self.test(player, source)) ^ this.isNegate;
            }
        }
    }
}
