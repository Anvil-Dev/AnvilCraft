package dev.dubhe.anvilcraft.util.function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface SafePredicate<T> extends Predicate<T> {
    @Override
    default SafePredicate<T> and(Predicate<? super T> other) {
        return new Multiple<>(this).and(other);
    }

    @Override
    default SafePredicate<T> negate() {
        return new Negate<>(this);
    }

    @Override
    default SafePredicate<T> or(Predicate<? super T> other) {
        return new Multiple<>(this).or(other);
    }

    class Negate<T> implements SafePredicate<T> {
        protected final SafePredicate<T> self;
        private boolean isNegate = true;

        protected Negate(SafePredicate<T> self) {
            this.self = self;
        }

        @Override
        public boolean test(T t) {
            return this.self.test(t) ^ this.isNegate;
        }

        @Override
        public SafePredicate<T> negate() {
            this.isNegate = !this.isNegate;
            return this;
        }
    }

    class Multiple<T> implements SafePredicate<T> {
        private SafePredicate<T> first;
        private final List<Sub<? super T>> subs = new ArrayList<>();

        Multiple(SafePredicate<T> first) {
            this.first = first;
        }

        @Override
        public boolean test(T t) {
            boolean result = this.first.test(t);
            for (Sub<? super T> sub : this.subs) {
                result = sub.test(t, result);
            }
            return result;
        }

        @Override
        public SafePredicate<T> and(Predicate<? super T> other) {
            this.subs.add(new Multiple.And<>(other));
            return this;
        }

        @Override
        public SafePredicate<T> negate() {
            this.first = this.first.negate();
            this.subs.getLast().negate();
            return this;
        }

        @Override
        public SafePredicate<T> or(Predicate<? super T> other) {
            this.subs.add(new Multiple.Or<>(other));
            return this;
        }

        private abstract static class Sub<T> {
            protected final Predicate<T> self;
            protected boolean isNegate = true;

            protected Sub(Predicate<T> self) {
                this.self = self;
            }

            abstract boolean test(T t, boolean otherResult);

            void negate() {
                this.isNegate = !this.isNegate;
            }
        }

        private static class And<T> extends Multiple.Sub<T> {
            private And(Predicate<T> self) {
                super(self);
            }

            @Override
            public boolean test(T t, boolean otherResult) {
                return (otherResult && this.self.test(t)) ^ this.isNegate;
            }
        }

        private static class Or<T> extends Multiple.Sub<T> {
            private Or(Predicate<T> self) {
                super(self);
            }

            @Override
            public boolean test(T t, boolean otherResult) {
                return (otherResult || this.self.test(t)) ^ this.isNegate;
            }
        }
    }
}
