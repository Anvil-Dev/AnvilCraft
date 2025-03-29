package dev.dubhe.anvilcraft.api.behavior;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface TreeNode<T> {

    boolean matches(ExecutionContext<T> context);

    Collection<TreeNode<T>> getChildren();

    default void run(ExecutionContext<T> context) {
    }

    static <T> PredicateTreeNode<T> predicate(Predicate<ExecutionContext<T>> predicate) {
        return new PredicateTreeNode<>(predicate);
    }

    static <T> PredicateExecutableTreeNode<T> predicatedExecutable(Predicate<ExecutionContext<T>> predicate) {
        return new PredicateExecutableTreeNode<>(predicate);
    }

    static <T> ExecutableTreeNode<T> executes(Consumer<ExecutionContext<T>> consumer) {
        return new ExecutableTreeNode<>(consumer);
    }

    @SafeVarargs
    static <T> SetTreeNode<T> multiple(TreeNode<T>... children) {
        return new SetTreeNode<>(() -> new HashSet<>(List.of(children)));
    }


}
