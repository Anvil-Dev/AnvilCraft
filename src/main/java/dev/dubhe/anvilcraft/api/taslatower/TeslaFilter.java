package dev.dubhe.anvilcraft.api.taslatower;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.HashMap;

public abstract class TeslaFilter {
    static TeslaFilter emptyFilter = new TeslaFilter() {
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
        return FILTER_MAP.getOrDefault(id, emptyFilter);
    }
    public static Collection<TeslaFilter> all() { return FILTER_MAP.values(); }
    abstract public String getId();
    abstract public boolean match(LivingEntity entity, String arg);
    public boolean needArg() { return false; }
    abstract public Component title();
    public String tooltip(String arg) { return ""; }

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