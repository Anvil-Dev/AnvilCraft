package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.server.Services;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Util {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static String generateUniqueRecipeSuffix() {
        return "_generated_" + Util.generateRandomString(8, true, false);
    }

    public static String generateRandomString(int len) {
        return Util.generateRandomString(len, true, true);
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

    public static int comparingIntReversed(int x, int y) {
        return Integer.compare(y, x);
    }

    public static boolean findCaller(String caller) {
        return Util.STACK_WALKER.walk(it -> it.anyMatch(frame -> frame.getMethodName().equals(caller)));
    }

    public static InteractionResult sidedSuccess(Level level) {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @SuppressWarnings("Convert2Lambda")
    public static Services getServices(Level level) {
        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
            AtomicReference<@Nullable Services> ref = new AtomicReference<>();
            DistExecutor.run(
                Dist.CLIENT, () -> new Runnable() {
                    @Override
                    public void run() {
                        ref.set(Minecraft.getInstance().services());
                    }
                }
            );
            return Objects.requireNonNull(ref.get(), "Client services were not initialized");
        } else {
            return Objects.requireNonNull(level.getServer(), "Server services are unavailable").services();
        }
    }

    public static float getSunAngle(Level level, Vec3 pos) {
        return level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, pos);
    }
}
