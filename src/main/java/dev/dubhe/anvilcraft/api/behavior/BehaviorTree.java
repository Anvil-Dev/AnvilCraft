package dev.dubhe.anvilcraft.api.behavior;

import java.util.concurrent.atomic.AtomicBoolean;

public class BehaviorTree<T> {
    private final TreeNode<T> root;

    public BehaviorTree(TreeNode<T> root) {
        this.root = root;
    }

    public boolean run(T context) {
        AtomicBoolean matches = new AtomicBoolean(false);
        walkNode(new ExecutionContext<>(context), root, matches);
        return matches.get();
    }

    public void walkNode(ExecutionContext<T> context, TreeNode<T> node, AtomicBoolean returns) {
        if (node.matches(context)) {
            returns.set(true);
            for (TreeNode<T> child : node.getChildren()) {
                walkNode(context, child, returns);
            }
            node.run(context);
        }
    }
}
