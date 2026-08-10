package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resolves registered megastructures for a Celestial Forging Anvil context. */
public final class CelestialRefactorRegistry {
    private CelestialRefactorRegistry() {
    }

    public static List<CelestialRefactorOption> getOptions(
        @Nullable CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        if (body == null) return Collections.emptyList();
        Megastructure.Context context = new Megastructure.Context(body, amplified, resources);
        List<CelestialRefactorOption> options = new ArrayList<>();
        for (Megastructure megastructure : ModRegistries.MEGASTRUCTURE) {
            if (!megastructure.isAvailable(context)) continue;
            int ring = megastructure.ring(context);
            if (!CelestialRefactorRegistry.isRingSupported(ring, amplified)) continue;
            options.add(CelestialRefactorOption.resolve(megastructure, context));
        }
        return options;
    }

    public static @Nullable CelestialRefactorOption getOption(
        Identifier id,
        @Nullable CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        if (body == null) return null;
        Megastructure megastructure = CelestialRefactorRegistry.get(id);
        if (megastructure == null) return null;
        return CelestialRefactorOption.resolve(
            megastructure,
            new Megastructure.Context(body, amplified, resources)
        );
    }

    public static @Nullable Megastructure get(Identifier id) {
        return ModRegistries.MEGASTRUCTURE.get(id).map(Holder.Reference::value).orElse(null);
    }

    public static @Nullable Identifier findLegacyId(
        String name,
        int ring,
        @Nullable CelestialBodyData body,
        boolean amplified,
        @Nullable PlanetaryResourceSet resources
    ) {
        Megastructure.Context context = body == null ? null : new Megastructure.Context(body, amplified, resources);
        Identifier nameMatch = null;
        for (Megastructure megastructure : ModRegistries.MEGASTRUCTURE) {
            if (!megastructure.name().equals(name)) continue;
            nameMatch = megastructure.id();
            if (context != null && megastructure.ring(context) == ring) {
                return megastructure.id();
            }
        }
        return nameMatch;
    }

    private static boolean isRingSupported(int ring, boolean amplified) {
        return amplified ? ring >= 4 && ring <= 6 : ring >= 1 && ring <= 2;
    }
}
