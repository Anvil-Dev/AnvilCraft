package dev.dubhe.anvilcraft.api.taslatower;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.HashMap;

public abstract class TeslaFilter {
    static final TeslaFilter EMPTY_FILTER = new TeslaFilter() {
        @Override
        public String getId() {
            return "";
        }

        @Override
        public boolean match(LivingEntity entity, String arg) {
            return false;
        }

        @Override
        public Component title() {
            return Component.translatable("screen.anvilcraft.tesla_tower.filter.unknown");
        }
    };
    private static final HashMap<String, TeslaFilter> FILTER_MAP = new HashMap<>();

    public static void register(TeslaFilter filter) {
        FILTER_MAP.put(filter.getId(), filter);
    }

    public static TeslaFilter getFilter(String id) {
        return FILTER_MAP.getOrDefault(id, EMPTY_FILTER);
    }

    public static Collection<TeslaFilter> all() {
        return FILTER_MAP.values();
    }

    public abstract String getId();

    public abstract boolean match(LivingEntity entity, String arg);

    public boolean needArg() {
        return false;
    }

    public abstract Component title();

    public String tooltip(String arg) {
        return "";
    }

    public static void init() {
        FILTER_MAP.clear();
        register(new IsPlayerFilter());
        register(new IsPlayerIdFilter());
        register(new IsPetFilter());
        register(new IsOnVehicleFilter());
        register(new IsFriendlyFilter());
        register(new IsEntityIdFilter());
        register(new IsBabyFriendlyFilter());
        register(new HasCustomNameFilter());
    }
}