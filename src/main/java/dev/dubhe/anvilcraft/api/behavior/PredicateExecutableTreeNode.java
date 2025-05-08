package dev.dubhe.anvilcraft.api.behavior;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PredicateExecutableTreeNode<T> extends SetTreeNode<T> {
    private final Predicate<ExecutionContext<T>> predicate;
    private Consumer<ExecutionContext<T>> executes = PredicateExecutableTreeNode::doNothing;

    public PredicateExecutableTreeNode(Predicate<ExecutionContext<T>> predicate) {
        super(HashSet::new);
        this.predicate = predicate;
    }

    @Override
    public void run(ExecutionContext<T> context) {
        this.executes.accept(context);
    }

    @Override
    public boolean matches(ExecutionContext<T> context) {
        return predicate.test(context);
    }

    @Override
    public PredicateExecutableTreeNode<T> executes(Consumer<ExecutionContext<T>> consumer) {
        this.executes = consumer;
        return this;
    }

    private static <T> void doNothing(ExecutionContext<T> t) {
    }
}
