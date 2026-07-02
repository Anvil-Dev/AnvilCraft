package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.mixin.accessor.MinecraftServerAccessor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Util {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static String generateUniqueRecipeSuffix() {
        return "_generated_" + generateRandomString(8, true, false);
    }

    public static String generateRandomString(int len) {
        return generateRandomString(len, true, true);
    }

    public static String generateRandomString(int len, boolean hasInteger, boolean hasUpperLetter) {
        String ch = "abcdefghijklmnopqrstuvwxyz" + (hasUpperLetter ? "ABCDEFGHIGKLMNOPQRSTUVWXYZ" : "")
            + (hasInteger ? "0123456789" : "");
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < len; i++) {
            Random random = new Random(System.nanoTime());
            int num = random.nextInt(ch.length() - 1);
            stringBuffer.append(ch.charAt(num));
        }
        return stringBuffer.toString();
    }

    public static @Nullable GameProfileCache clientCache = null;

    @SuppressWarnings("Convert2Lambda")
    public static GameProfileCache findProfileCache(Level level) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            AtomicReference<GameProfileCache> ref = new AtomicReference<>();
            DistExecutor.run(Dist.CLIENT, () -> new Runnable() {
                @Override
                public void run() {
                    if (Util.clientCache == null) {
                        return;
                    }
                    ref.set(Util.clientCache);
                }
            });
            return ref.get();
        } else {
            return ((MinecraftServerAccessor) Objects.requireNonNull(level.getServer())).getServices().profileCache();
        }
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMapCollector() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
