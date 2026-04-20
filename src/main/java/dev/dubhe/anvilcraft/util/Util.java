package dev.dubhe.anvilcraft.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

    public static int comparingIntReversed(int x, int y) {
        return Integer.compare(y, x);
    }

    public static boolean findCaller(String caller) {
        return STACK_WALKER.walk(it -> it.anyMatch(frame -> frame.getMethodName().equals(caller)));
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMapCollector() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
