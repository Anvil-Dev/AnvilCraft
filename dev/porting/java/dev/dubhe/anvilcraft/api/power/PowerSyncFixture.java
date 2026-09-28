package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.network.PowerGridSyncChunkPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class PowerSyncFixture {
    public static PowerGrid create(Level level, int count) {
        PowerGrid grid = new PowerGrid(level);
        reset(grid, count);
        return grid;
    }

    public static void reset(PowerGrid grid, int count) {
        grid.components.clear();
        grid.producers.clear();
        for (int i = 0; i < count; i++) {
            var producer = new Producer(grid.getLevel(), new BlockPos(100000 + i, 160, 100000), 100 + i, i == 0);
            grid.components.add(producer);
            grid.producers.add(producer);
        }
        set(grid, "pos", new BlockPos(100000, 160, 100000));
        set(grid, "generate", 12345);
        set(grid, "consume", 2345);
        set(grid, "hasInfinitePower", true);
    }

    public static PowerGridSyncChunkPacket[] chunks(PowerGrid grid) {
        try {
            var method = PowerGridSyncChunkPacket.class.getDeclaredMethod("chunks", PowerGrid.class);
            method.setAccessible(true);
            return (PowerGridSyncChunkPacket[]) method.invoke(null, grid);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void set(PowerGrid grid, String name, Object value) {
        try {
            var field = PowerGrid.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(grid, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Producer(Level level, BlockPos pos, int power, boolean infinite) implements IPowerProducer {
        @Override
        public Level getCurrentLevel() {
            return this.level;
        }

        @Override
        public BlockPos getPos() {
            return this.pos;
        }

        @Override
        public int getOutputPower() {
            return this.power;
        }

        @Override
        public boolean isInfinitePower() {
            return this.infinite;
        }

        @Override
        public void setGrid(@Nullable PowerGrid grid) {
        }

        @Override
        public @Nullable PowerGrid getGrid() {
            return null;
        }
    }
}
