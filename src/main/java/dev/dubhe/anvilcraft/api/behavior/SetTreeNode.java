package dev.dubhe.anvilcraft.api.behavior;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SetTreeNode<T> implements TreeNode<T> {

    private final Set<TreeNode<T>> childrens;

    public SetTreeNode(Supplier<Set<TreeNode<T>>> factory) {
        this.childrens = factory.get();
    }

    @Override
    public boolean matches(ExecutionContext<T> context) {
        return true;
    }

    public SetTreeNode<T> then(TreeNode<T> node) {
        this.childrens.add(node);
        return this;
    }

    @Override
    public Collection<TreeNode<T>> getChildren() {
        return childrens;
    }

    public SetTreeNode<T> executes(Consumer<ExecutionContext<T>> consumer) {
        return then(TreeNode.executes(consumer));
    }
}
