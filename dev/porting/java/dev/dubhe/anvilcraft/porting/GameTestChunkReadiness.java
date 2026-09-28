package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GameTestTicker;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/** Bound setup waits, which occur before the individual GameTest timeout starts. */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class GameTestChunkReadiness {
    private static final Map<GameTestInfo, Long> WAITING = new IdentityHashMap<>();

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (!(event.getServer() instanceof GameTestServer)) return;
        long now = System.nanoTime();
        try {
            var field = GameTestTicker.class.getDeclaredField("testInfos");
            field.setAccessible(true);
            Collection<?> active = (Collection<?>) field.get(GameTestTicker.SINGLETON);
            WAITING.keySet().retainAll(active);
            for (Object value : active) {
                var test = (GameTestInfo) value;
                if (test.isDone() || test.hasStarted() || test.getTestBlockPos() == null) {
                    WAITING.remove(test);
                    continue;
                }
                var level = event.getServer().overworld();
                var pending = test.getTestInstanceBlockEntity().getStructureBoundingBox().intersectingChunks()
                    .filter(chunk -> !level.areEntitiesActuallyLoadedAndTicking(chunk)).toList();
                if (pending.isEmpty()) {
                    WAITING.remove(test);
                    continue;
                }
                long since = WAITING.computeIfAbsent(test, ignored -> now);
                if (now - since > 30_000_000_000L) {
                    throw new IllegalStateException("GameTest setup remained unticked at " + test.getTestBlockPos() + ": " + pending);
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
