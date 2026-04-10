package dev.dubhe.anvilcraft.util;

import lombok.Getter;

import java.util.List;

@Getter
public class SegmentedActuator {
    @Getter
    public static class Task {
        private final int tick;
        private final Runnable runnable;

        private int count = 0;

        public Task(int tick, Runnable runnable) {
            this.tick = tick;
            this.runnable = runnable;
        }

        public Result execute() {
            this.runnable.run();
            this.count++;
            if (this.count >= this.tick) {
                return Result.NEXT;
            }
            return Result.CURRENT;
        }

        public void reset() {
            this.count = 0;
        }

        public enum Result {
            CURRENT,
            NEXT
        }
    }

    private final List<Task> tasks;
    private int index;
    private Runnable finallyRunnable = () -> {};

    public SegmentedActuator(Runnable finallyRunnable, Task... tasks) {
        this(tasks);
        this.finallyRunnable = finallyRunnable;
    }

    public SegmentedActuator(Task... tasks) {
        this.tasks = List.of(tasks);
        this.index = 0;
    }

    public void execute() {
        if (this.index >= this.tasks.size()) {
            this.reset();
            this.finallyRunnable.run();
            return;
        }
        Task task = tasks.get(this.index);
        Task.Result execute = task.execute();
        if (execute == Task.Result.NEXT) {
            this.index++;
        }
    }

    public void reset() {
        this.index = 0;
        for (Task task : tasks) {
            task.reset();
        }
    }
}
