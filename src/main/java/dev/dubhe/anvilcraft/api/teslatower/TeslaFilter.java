package dev.dubhe.anvilcraft.api.teslatower;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.HashMap;

public abstract class TeslaFilter {
    public static final StreamCodec<ByteBuf, TeslaFilter> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
        TeslaFilter::getFilter,
        TeslaFilter::getId
    );
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
        TeslaFilter.FILTER_MAP.put(filter.getId(), filter);
    }

    public static TeslaFilter getFilter(String id) {
        return TeslaFilter.FILTER_MAP.getOrDefault(id, TeslaFilter.emptyFilter);
    }

    public static Collection<TeslaFilter> all() {
        return TeslaFilter.FILTER_MAP.values();
    }

    public abstract String getId();

    public abstract boolean match(LivingEntity entity, String arg);

    public boolean needArg() {
        return false;
    }

    public abstract Component title();

    public Component getTitle(String arg) {
        return this.title();
    }

    public String tooltip(String arg) {
        return "";
    }

    public static void init() {
        TeslaFilter.FILTER_MAP.clear();
        TeslaFilter.register(new IsPlayerFilter());
        TeslaFilter.register(new IsPlayerIdFilter());
        TeslaFilter.register(new IsPetFilter());
        TeslaFilter.register(new IsOnVehicleFilter());
        TeslaFilter.register(new IsFriendlyFilter());
        TeslaFilter.register(new IsHostileFilter());
        TeslaFilter.register(new IsNeutralFilter());
        TeslaFilter.register(new IsEntityIdFilter());
        TeslaFilter.register(new IsBabyFriendlyFilter());
        TeslaFilter.register(new HasCustomNameFilter());
    }
}
