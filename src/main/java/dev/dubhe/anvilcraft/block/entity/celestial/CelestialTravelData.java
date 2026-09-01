package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Data-driven travel rules for a special celestial body.
 *
 * <p>The absence of this value means that the body cannot be reached through a
 * Celestial Forging Anvil portal.  Keeping the value optional lets datapacks
 * opt a planet in without adding a second, easily desynchronised boolean.</p>
 */
public record CelestialTravelData(
    ResourceLocation dimension,
    CoordinateRule coordinateRule,
    ReturnRule returnRule
) {
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
            rule -> Either.left(rule)
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
            rule -> Either.left(rule)
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
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(CelestialTravelData::dimension),
            CoordinateRule.CODEC.optionalFieldOf("coordinate_rule", CoordinateRule.DEFAULT)
                .forGetter(CelestialTravelData::coordinateRule),
            ReturnRule.CODEC.optionalFieldOf("return_rule", ReturnRule.DEFAULT)
                .forGetter(CelestialTravelData::returnRule)
        ).apply(ins, CelestialTravelData::new));

        public final StreamCodec<RegistryFriendlyByteBuf, CelestialTravelData> streamCodec = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
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
        tag.putString("dimension", dimension.toString());
        tag.putString("coordinateType", coordinateRule.type().getSerializedName());
        tag.putDouble("coordinateScale", coordinateRule.scale());
        tag.putInt("coordinateX", coordinateRule.x());
        tag.putInt("coordinateY", coordinateRule.y());
        tag.putInt("coordinateZ", coordinateRule.z());
        tag.putInt("coordinateRadius", coordinateRule.radius());
        tag.putString("returnType", returnRule.type().getSerializedName());
        tag.putInt("returnX", returnRule.x());
        tag.putInt("returnY", returnRule.y());
        tag.putInt("returnZ", returnRule.z());
        tag.putInt("returnRadius", returnRule.radius());
        return tag;
    }

    @Nullable
    public static CelestialTravelData fromTag(CompoundTag tag) {
        String dimensionName = tag.getString("dimension");
        if (dimensionName.isEmpty()) return null;
        try {
            ResourceLocation dimension = ResourceLocation.parse(dimensionName);
            CoordinateRule coordinateRule = new CoordinateRule(
                CoordinateRule.Type.fromName(tag.contains("coordinateType")
                    ? tag.getString("coordinateType") : CoordinateRule.Type.FIXED.getSerializedName()),
                tag.contains("coordinateScale") ? tag.getDouble("coordinateScale") : 1.0,
                tag.getInt("coordinateX"),
                tag.contains("coordinateY") ? tag.getInt("coordinateY") : 64,
                tag.getInt("coordinateZ"),
                tag.contains("coordinateRadius") ? tag.getInt("coordinateRadius") : 8
            );
            ReturnRule returnRule = new ReturnRule(
                ReturnRule.Type.fromName(tag.contains("returnType")
                    ? tag.getString("returnType") : ReturnRule.Type.ENTRY_PORTAL.getSerializedName()),
                tag.getInt("returnX"),
                tag.contains("returnY") ? tag.getInt("returnY") : 64,
                tag.getInt("returnZ"),
                tag.contains("returnRadius") ? tag.getInt("returnRadius") : 8
            );
            return new CelestialTravelData(dimension, coordinateRule, returnRule);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
