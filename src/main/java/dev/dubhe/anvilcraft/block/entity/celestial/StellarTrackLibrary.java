package dev.dubhe.anvilcraft.block.entity.celestial;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** 精确质量档位的轨道库。一次性校验并发布完整资源集，运行中演化使用自己的快照。 */
public final class StellarTrackLibrary {
    public static final String SOLAR_TRACK_ID = "mass_49";
    public static final Codec<List<StellarTrack>> TRACKS_CODEC = StellarTrack.CODEC.listOf();
    public static final List<CelestialBodyClass> EVOLVABLE_SURFACE_CLASSES = List.of(
        CelestialBodyClass.M_MAIN, CelestialBodyClass.K_MAIN, CelestialBodyClass.G_MAIN,
        CelestialBodyClass.F_MAIN, CelestialBodyClass.A_MAIN, CelestialBodyClass.B_MAIN, CelestialBodyClass.O_MAIN,
        CelestialBodyClass.M_GIANT, CelestialBodyClass.K_GIANT, CelestialBodyClass.G_GIANT,
        CelestialBodyClass.F_GIANT, CelestialBodyClass.A_GIANT, CelestialBodyClass.B_GIANT, CelestialBodyClass.O_GIANT,
        CelestialBodyClass.M_SUPERGIANT, CelestialBodyClass.K_SUPERGIANT, CelestialBodyClass.G_SUPERGIANT,
        CelestialBodyClass.F_SUPERGIANT, CelestialBodyClass.A_SUPERGIANT, CelestialBodyClass.B_SUPERGIANT,
        CelestialBodyClass.O_SUPERGIANT
    );
    private static final Library BUNDLED = readBundled();
    private static volatile Library library = BUNDLED;

    private StellarTrackLibrary() {
    }

    private record Library(Map<String, StellarTrack> tracks, Map<String, StellarEventProfile> profiles) {
        private Library {
            tracks = Map.copyOf(tracks);
            profiles = Map.copyOf(profiles);
        }
    }

    public static List<StellarTrack> tracks() {
        return library.tracks().values().stream()
            .sorted(java.util.Comparator.comparingInt(track -> track.definition().massAnvils())).toList();
    }

    public static Map<String, StellarEventProfile> eventProfiles() {
        return library.profiles();
    }

    @Nullable
    public static StellarTrack track(String id) {
        return library.tracks().get(id);
    }

    public static StellarTrack forMass(int initialMassAnvils) {
        StellarTrack track = library.tracks().get("mass_" + initialMassAnvils);
        if (track == null) throw new IllegalArgumentException("Missing stellar mass track: " + initialMassAnvils);
        return track;
    }

    @Nullable
    public static StellarTrack select(int initialMass, CelestialBodyClass surfaceClass, long seed) {
        return select(initialMass, surfaceClass, false, seed);
    }

    @Nullable
    public static StellarTrack select(int initialMass, CelestialBodyClass surfaceClass, boolean specialRedDwarf, long seed) {
        if (!canStart(initialMass, surfaceClass, specialRedDwarf)) return null;
        return forMass(initialMass);
    }

    public static boolean canStart(int mass, CelestialBodyClass surfaceClass, boolean specialRedDwarf) {
        if (specialRedDwarf) return false;
        if (mass == 41) return surfaceClass == CelestialBodyClass.BROWN_DWARF;
        return mass >= 42 && mass <= 64 && EVOLVABLE_SURFACE_CLASSES.contains(surfaceClass);
    }

    public static int startingPhaseIndex(StellarTrack track, CelestialBodyClass surfaceClass) {
        String nodeId = track.definition().startingNodes().get(surfaceClass.name());
        if (nodeId == null) throw new IllegalArgumentException("Missing stellar entry: " + track.trackId() + "/" + surfaceClass);
        return track.nodeIndex(nodeId);
    }

    public static int variant(long seed, int salt) {
        long value = seed ^ (0x9E3779B97F4A7C15L * (salt + 1L));
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (int) value;
    }

    public static StellarEventProfile eventProfile(String id) {
        StellarEventProfile profile = library.profiles().get(id);
        if (profile != null) return profile;
        throw new IllegalArgumentException("Unknown stellar event profile: " + id);
    }

    /** 解码整批资源时先检查全部引用，再替换单个不可变库引用。 */
    private static Library decode(JsonElement tracksJson, JsonElement profilesJson) {
        List<StellarTrack> tracks = TRACKS_CODEC.parse(JsonOps.INSTANCE, tracksJson).getOrThrow();
        List<StellarEventProfile> profiles = StellarEventProfile.CODEC.listOf().parse(JsonOps.INSTANCE, profilesJson).getOrThrow();
        Map<String, StellarEventProfile> profileMap = new LinkedHashMap<>();
        for (StellarEventProfile profile : profiles) {
            profile.validate();
            if (profileMap.put(profile.profileId(), profile) != null) {
                throw new IllegalArgumentException("Duplicate stellar event profile: " + profile.profileId());
            }
        }
        Map<String, StellarTrack> trackMap = validate(tracks, profileMap);
        return new Library(trackMap, profileMap);
    }

    private static Map<String, StellarTrack> validate(List<StellarTrack> tracks, Map<String, StellarEventProfile> profiles) {
        Map<String, StellarTrack> result = new LinkedHashMap<>();
        for (StellarTrack track : tracks) {
            track.validate();
            if (result.put(track.trackId(), track) != null) {
                throw new IllegalArgumentException("Invalid or duplicate discrete stellar track: " + track.trackId());
            }
            for (PhaseNode node : track.phaseNodes()) {
                if (node.hasEventProfile() && !profiles.containsKey(node.eventProfileId())) {
                    throw new IllegalArgumentException("Unknown stellar event: " + node.eventProfileId());
                }
            }
            if (!track.terminalProfile().isEmpty() && track.phaseNodes().stream()
                .noneMatch(node -> node.eventProfileId().equals(track.terminalProfile()))) {
                throw new IllegalArgumentException("Unreachable terminal event: " + track.trackId());
            }
            List<CelestialBodyClass> classes = track.definition().massAnvils() == 41
                ? List.of(CelestialBodyClass.BROWN_DWARF) : EVOLVABLE_SURFACE_CLASSES;
            for (CelestialBodyClass cls : classes) startingPhaseIndex(track, cls);
        }
        for (int mass = 41; mass <= 64; mass++) {
            if (!result.containsKey("mass_" + mass)) throw new IllegalArgumentException("Missing stellar mass track: " + mass);
        }
        if (result.size() != 24) throw new IllegalArgumentException("Unexpected stellar mass track");
        return result;
    }

    public static void replaceTracks(List<StellarTrack> tracks) {
        Library current = library;
        library = new Library(validate(tracks, current.profiles()), current.profiles());
    }

    public static boolean replaceTracksFromJson(JsonElement json) {
        try {
            replaceTracks(TRACKS_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            return true;
        } catch (RuntimeException exception) {
            LogUtils.getLogger().warn("Invalid stellar tracks; retaining previous library: {}", exception.getMessage());
            return false;
        }
    }

    public static String validateTracks() {
        try {
            Library current = library;
            validate(List.copyOf(current.tracks().values()), current.profiles());
            return "";
        } catch (RuntimeException exception) {
            return exception.toString();
        }
    }

    public static void validateCoverage(JsonElement coverage, List<StellarTrack> tracks) {
        Map<String, StellarTrack> byId = new LinkedHashMap<>();
        for (StellarTrack track : tracks) byId.put(track.trackId(), track);
        var rows = coverage.getAsJsonObject().getAsJsonArray("tracks");
        if (rows.size() != byId.size()) throw new IllegalArgumentException("Stellar coverage size mismatch");
        for (JsonElement row : rows) {
            var entry = row.getAsJsonObject();
            String id = entry.get("trackId").getAsString();
            StellarTrack track = byId.remove(id);
            if (track == null || track.definition().massAnvils() != entry.get("massAnvils").getAsInt()) {
                throw new IllegalArgumentException("Invalid stellar coverage row: " + id);
            }
            Map<String, String> starts = Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .parse(JsonOps.INSTANCE, entry.get("startingNodes")).getOrThrow();
            if (!starts.equals(track.definition().startingNodes())) {
                throw new IllegalArgumentException("Stellar entry coverage mismatch: " + id);
            }
        }
    }

    private static JsonElement bundledJson(String path) throws IOException {
        var stream = StellarTrackLibrary.class.getClassLoader().getResourceAsStream("data/anvilcraft/" + path);
        if (stream == null) throw new IOException("Missing bundled stellar resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static Library readBundled() {
        try {
            Library result = decode(bundledJson("stellar_tracks.json"), bundledJson("stellar_event_profiles.json"));
            validateCoverage(bundledJson("stellar_track_coverage.json"), List.copyOf(result.tracks().values()));
            return result;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Invalid bundled stellar resources", exception);
        }
    }

    public static void loadBundledResources() {
        library = BUNDLED;
    }

    public static void resetDefaults() {
        library = BUNDLED;
    }

    private static JsonElement resourceJson(ResourceManager manager, String path) throws IOException {
        var resource = manager.getResource(ResourceLocation.fromNamespaceAndPath("anvilcraft", path));
        if (resource.isEmpty()) return bundledJson(path);
        try (var reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader);
        }
    }

    public static void reload(ResourceManager manager) {
        try {
            Library next = decode(resourceJson(manager, "stellar_tracks.json"), resourceJson(manager, "stellar_event_profiles.json"));
            validateCoverage(resourceJson(manager, "stellar_track_coverage.json"), List.copyOf(next.tracks().values()));
            library = next;
        } catch (IOException | RuntimeException exception) {
            LogUtils.getLogger().warn("Invalid stellar resources; retaining previous library", exception);
        }
    }
}
