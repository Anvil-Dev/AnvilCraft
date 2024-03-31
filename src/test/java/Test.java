import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Test {
    public static void main(String[] args) {
        Map<String, Comparable<?>> states = new HashMap<>() {{
            this.put("a", 1);
            this.put("b", 1.0);
            this.put("c", true);
            this.put("d", "abc");
        }};
        StringBuilder builder = new StringBuilder("[");
        Iterator<Map.Entry<String, Comparable<?>>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Comparable<?>> entry = iterator.next();
            builder.append("%s=%s".formatted(entry.getKey(), entry.getValue()));
            if (iterator.hasNext()) builder.append(", ");
        }
        builder.append("]");
        String string = builder.toString();
        System.out.println(string);
        Map<String, Comparable<?>> map = new HashMap<>();
        String[] strings = string.substring(1, string.length() - 1).split(", ");
        for (String str : strings) {
            String[] strings1 = str.split("=");
            if (strings1.length != 2) continue;
            String key = strings1[0];
            String valueStr = strings1[1];
            Object object = valueStr;
            try {
                object = Long.valueOf(valueStr);
            } catch (NumberFormatException ignored) {
                try {
                    object = Double.valueOf(valueStr);
                } catch (NumberFormatException _ignored) {
                    if ("true".equals(valueStr)) object = true;
                    else if ("false".equals(valueStr)) object = false;
                }
            }
            if (object instanceof Comparable<?> value) map.put(key, value);
        }
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(map));
    }
}
