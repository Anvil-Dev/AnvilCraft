package dev.dubhe.anvilcraft.api.behavior;

import java.util.HashSet;
import java.util.function.Consumer;

public class ExecutableTreeNode<T> extends SetTreeNode<T> {

    private final Consumer<ExecutionContext<T>> executes;

    public ExecutableTreeNode(Consumer<ExecutionContext<T>> executes) {
        super(HashSet::new);
        this.executes = executes;
    }

    @Override
    public boolean matches(ExecutionContext<T> context) {
        return true;
    }

    @Override
    public void run(ExecutionContext<T> context) {
        executes.accept(context);
    }
}
