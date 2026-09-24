package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Data-driven travel rules for a special celestial body.
 *
 * <p>The absence of this value means that the body cannot be reached through a
 * Celestial Forging Anvil portal.  Keeping the value optional lets datapacks
 * opt a planet in without adding a second, easily desynchronised boolean.</p>
 */
public record CelestialTravelData(
    Identifier dimension,
    CoordinateRule coordinateRule,
    ReturnRule returnRule
) {
    public static final Identifier OVERWORLD_LIKE_DIMENSION =
        Identifier.fromNamespaceAndPath("anvilcraft", "overworld_like");
    public static final MapCodecHolder CODECS = new MapCodecHolder();

    /** The coordinate used when a rule omits its optional position fields. */
    public record CoordinateRule(Type type, double scale, int x, int y, int z, int radius) {
        public static final CoordinateRule DEFAULT = new CoordinateRule(Type.FIXED, 1.0, 0, 64, 0, 8);

        private static final Codec<CoordinateRule> OBJECT_CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Type.CODEC.optionalFieldOf("type", Type.FIXED).forGetter(CoordinateRule::type),
            Codec.DOUBLE.optionalFieldOf("scale", 1.0).forGetter(CoordinateRule::scale),
            Codec.INT.optionalFieldOf("x", 0).forGetter(CoordinateRule::x),
            Codec.INT.optionalFieldOf("y", 64).forGetter(CoordinateRule::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(CoordinateRule::z),
            Codec.INT.optionalFieldOf("radius", 8).forGetter(CoordinateRule::radius)
        ).apply(ins, CoordinateRule::new));

        /** Accept both the documented object form and a concise string rule in datapacks. */
        public static final Codec<CoordinateRule> CODEC = Codec.either(OBJECT_CODEC, Type.CODEC).xmap(
            either -> either.map(rule -> rule, type -> new CoordinateRule(type, 1.0, 0, 64, 0, 8)),
            Either::left
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, CoordinateRule> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                rule -> rule.type().getSerializedName(),
                ByteBufCodecs.DOUBLE,
                CoordinateRule::scale,
                ByteBufCodecs.INT,
                CoordinateRule::x,
                ByteBufCodecs.INT,
                CoordinateRule::y,
                ByteBufCodecs.INT,
                CoordinateRule::z,
                ByteBufCodecs.INT,
                CoordinateRule::radius,
                (type, scale, x, y, z, radius) -> new CoordinateRule(
                    Type.fromName(type), scale, x, y, z, radius
                )
            );

        public CoordinateRule {
            scale = Double.isFinite(scale) && scale > 0.0 ? scale : 1.0;
            radius = Math.max(0, radius);
        }

        @Getter
        public enum Type {
            SAME("same"),
            /** Keeps the source height as well, for destinations without any terrain to land on. */
            SAME_3D("same_3d"),
            SCALED("scaled"),
            FIXED("fixed"),
            /** Keeps X/Z near a fixed point while finding safe ground on the surface. */
            FIXED_SURFACE("fixed_surface"),
            RANDOM_SPAWN("random_spawn");

            public static final Codec<Type> CODEC = Codec.STRING.xmap(Type::fromName, Type::getSerializedName);

            private final String serializedName;

            Type(String serializedName) {
                this.serializedName = serializedName;
            }

            public static Type fromName(String name) {
                return switch (name.toLowerCase(Locale.ROOT)) {
                    case "same", "current", "equal" -> SAME;
                    case "same_3d", "same_height", "identical" -> SAME_3D;
                    case "scaled", "simple", "scale" -> SCALED;
                    case "fixed", "point" -> FIXED;
                    case "fixed_surface" -> FIXED_SURFACE;
                    case "random_spawn", "random", "spawn" -> RANDOM_SPAWN;
                    default -> throw new IllegalArgumentException("Unknown celestial coordinate rule: " + name);
                };
            }
        }
    }

    public record ReturnRule(Type type, int x, int y, int z, int radius) {
        public static final ReturnRule DEFAULT = new ReturnRule(Type.ENTRY_PORTAL, 0, 64, 0, 8);

        private static final Codec<ReturnRule> OBJECT_CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Type.CODEC.optionalFieldOf("type", Type.ENTRY_PORTAL).forGetter(ReturnRule::type),
            Codec.INT.optionalFieldOf("x", 0).forGetter(ReturnRule::x),
            Codec.INT.optionalFieldOf("y", 64).forGetter(ReturnRule::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(ReturnRule::z),
            Codec.INT.optionalFieldOf("radius", 8).forGetter(ReturnRule::radius)
        ).apply(ins, ReturnRule::new));

        /** Accept both the documented object form and a concise string rule in datapacks. */
        public static final Codec<ReturnRule> CODEC = Codec.either(OBJECT_CODEC, Type.CODEC).xmap(
            either -> either.map(rule -> rule, type -> new ReturnRule(type, 0, 64, 0, 8)),
            Either::left
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ReturnRule> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                rule -> rule.type().getSerializedName(),
                ByteBufCodecs.INT,
                ReturnRule::x,
                ByteBufCodecs.INT,
                ReturnRule::y,
                ByteBufCodecs.INT,
                ReturnRule::z,
                ByteBufCodecs.INT,
                ReturnRule::radius,
                (type, x, y, z, radius) -> new ReturnRule(Type.fromName(type), x, y, z, radius)
            );

        public ReturnRule {
            radius = Math.max(0, radius);
        }

        @Getter
        public enum Type {
            ENTRY_PORTAL("entry_portal"),
            RANDOM_PORTAL("random_portal"),
            FIXED_PORTAL("fixed_portal");

            public static final Codec<Type> CODEC = Codec.STRING.xmap(Type::fromName, Type::getSerializedName);

            private final String serializedName;

            Type(String serializedName) {
                this.serializedName = serializedName;
            }

            public static Type fromName(String name) {
                return switch (name.toLowerCase(Locale.ROOT)) {
                    case "entry_portal", "entry", "entry_point" -> ENTRY_PORTAL;
                    case "random_portal", "random", "random_spawn" -> RANDOM_PORTAL;
                    case "fixed_portal", "fixed", "point" -> FIXED_PORTAL;
                    default -> throw new IllegalArgumentException("Unknown celestial return rule: " + name);
                };
            }
        }
    }

    /** A small holder avoids exposing a MapCodec implementation detail in the record header. */
    public static final class MapCodecHolder {
        public final Codec<CelestialTravelData> codec = RecordCodecBuilder.create(ins -> ins.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(CelestialTravelData::dimension),
            CoordinateRule.CODEC.optionalFieldOf("coordinate_rule", CoordinateRule.DEFAULT)
                .forGetter(CelestialTravelData::coordinateRule),
            ReturnRule.CODEC.optionalFieldOf("return_rule", ReturnRule.DEFAULT)
                .forGetter(CelestialTravelData::returnRule)
        ).apply(ins, CelestialTravelData::new));

        public final StreamCodec<RegistryFriendlyByteBuf, CelestialTravelData> streamCodec = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            CelestialTravelData::dimension,
            CoordinateRule.STREAM_CODEC,
            CelestialTravelData::coordinateRule,
            ReturnRule.STREAM_CODEC,
            CelestialTravelData::returnRule,
            CelestialTravelData::new
        );

        private MapCodecHolder() {
        }
    }

    public static final Codec<CelestialTravelData> CODEC = CODECS.codec;
    public static final StreamCodec<RegistryFriendlyByteBuf, CelestialTravelData> STREAM_CODEC = CODECS.streamCodec;

    public CelestialTravelData {
        coordinateRule = coordinateRule == null ? CoordinateRule.DEFAULT : coordinateRule;
        returnRule = returnRule == null ? ReturnRule.DEFAULT : returnRule;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", this.dimension.toString());
        tag.putString("coordinateType", this.coordinateRule.type().getSerializedName());
        tag.putDouble("coordinateScale", this.coordinateRule.scale());
        tag.putInt("coordinateX", this.coordinateRule.x());
        tag.putInt("coordinateY", this.coordinateRule.y());
        tag.putInt("coordinateZ", this.coordinateRule.z());
        tag.putInt("coordinateRadius", this.coordinateRule.radius());
        tag.putString("returnType", this.returnRule.type().getSerializedName());
        tag.putInt("returnX", this.returnRule.x());
        tag.putInt("returnY", this.returnRule.y());
        tag.putInt("returnZ", this.returnRule.z());
        tag.putInt("returnRadius", this.returnRule.radius());
        return tag;
    }

    @Nullable
    public static CelestialTravelData fromTag(CompoundTag tag) {
        String dimensionName = tag.getStringOr("dimension", "");
        if (dimensionName.isEmpty()) return null;
        try {
            Identifier dimension = Identifier.tryParse(dimensionName);
            if (dimension == null) return null;
            CoordinateRule coordinateRule = new CoordinateRule(
                CoordinateRule.Type.fromName(tag.contains("coordinateType")
                    ? tag.getStringOr("coordinateType", "") : CoordinateRule.Type.FIXED.getSerializedName()),
                tag.contains("coordinateScale") ? tag.getDoubleOr("coordinateScale", 0d) : 1.0,
                tag.getIntOr("coordinateX", 0),
                tag.contains("coordinateY") ? tag.getIntOr("coordinateY", 0) : 64,
                tag.getIntOr("coordinateZ", 0),
                tag.contains("coordinateRadius") ? tag.getIntOr("coordinateRadius", 0) : 8
            );
            ReturnRule returnRule = new ReturnRule(
                ReturnRule.Type.fromName(tag.contains("returnType")
                    ? tag.getStringOr("returnType", "") : ReturnRule.Type.ENTRY_PORTAL.getSerializedName()),
                tag.getIntOr("returnX", 0),
                tag.contains("returnY") ? tag.getIntOr("returnY", 0) : 64,
                tag.getIntOr("returnZ", 0),
                tag.contains("returnRadius") ? tag.getIntOr("returnRadius", 0) : 8
            );
            return new CelestialTravelData(dimension, coordinateRule, returnRule);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
