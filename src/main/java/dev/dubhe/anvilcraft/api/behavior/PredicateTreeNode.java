package dev.dubhe.anvilcraft.api.behavior;

import java.util.HashSet;
import java.util.function.Predicate;

public class PredicateTreeNode<T> extends SetTreeNode<T> {
    private final Predicate<ExecutionContext<T>> predicate;

    public PredicateTreeNode(Predicate<ExecutionContext<T>> predicate) {
        super(HashSet::new);
        this.predicate = predicate;
    }

    @Override
    public boolean matches(ExecutionContext<T> context) {
        return predicate.test(context);
    }
}
