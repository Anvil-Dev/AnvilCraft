package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Recipe defining a hidden (special) celestial body discoverable via seed items
 * in the Celestial Forging Anvil.
 */
@SuppressWarnings("checkstyle:LineLength")
public record SpecialCelestialBodyRecipe(
    String name,
    String textureName,
    boolean needsCustomModel,
    int time,
    int space,
    int mass,
    int energy,
    boolean hasAtmosphere,
    Optional<LiquidCoverage> liquidCoverage,
    int magneticFieldStrength,
    int rotationSpeed,
    float axialTilt,
    List<Identifier> seedItems,
    List<WeightedEntry> minerals,
    List<WeightedEntry> fluids,
    List<WeightedEntry> biologicalItems,
    List<WeightedEntry> offerings,
    List<DemandEntry> templeBlessings,
    List<DemandEntry> templePunishments
) implements Recipe<SpecialCelestialBodyInput> {

    public record WeightedEntry(String id, int weight) {
        public static final Codec<WeightedEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("id").forGetter(WeightedEntry::id),
            Codec.INT.fieldOf("weight").forGetter(WeightedEntry::weight)
        ).apply(ins, WeightedEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WeightedEntry::id,
            ByteBufCodecs.INT,
            WeightedEntry::weight,
            WeightedEntry::new
        );

        public Identifier resourceId() {
            return Identifier.parse(id);
        }
    }

    public record DemandEntry(String id, int count) {
        public static final Codec<DemandEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("id").forGetter(DemandEntry::id),
            Codec.INT.fieldOf("count").forGetter(DemandEntry::count)
        ).apply(ins, DemandEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DemandEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            DemandEntry::id,
            ByteBufCodecs.INT,
            DemandEntry::count,
            DemandEntry::new
        );
    }

    private static final Codec<LiquidCoverage> LIQUID_COVERAGE_CODEC =
        Codec.STRING.xmap(LiquidCoverage::fromName, LiquidCoverage::getSerializedName);

    private static final StreamCodec<ByteBuf, LiquidCoverage> LIQUID_COVERAGE_STREAM =
        ByteBufCodecs.STRING_UTF8.map(LiquidCoverage::fromName, LiquidCoverage::getSerializedName);

    private record ResourceFields(
        List<WeightedEntry> minerals,
        List<WeightedEntry> fluids,
        List<WeightedEntry> biologicalItems,
        List<WeightedEntry> offerings,
        List<DemandEntry> templeBlessings,
        List<DemandEntry> templePunishments
    ) {
        static final Codec<ResourceFields> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            WeightedEntry.CODEC.listOf().optionalFieldOf("minerals", List.of()).forGetter(ResourceFields::minerals),
            WeightedEntry.CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(ResourceFields::fluids),
            WeightedEntry.CODEC.listOf().optionalFieldOf("biological_items", List.of()).forGetter(ResourceFields::biologicalItems),
            WeightedEntry.CODEC.listOf().optionalFieldOf("offerings", List.of()).forGetter(ResourceFields::offerings),
            DemandEntry.CODEC.listOf().optionalFieldOf("temple_blessings", List.of()).forGetter(ResourceFields::templeBlessings),
            DemandEntry.CODEC.listOf().optionalFieldOf("temple_punishments", List.of()).forGetter(ResourceFields::templePunishments)
        ).apply(ins, ResourceFields::new));
    }

    public static final MapCodec<SpecialCelestialBodyRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING.fieldOf("name").forGetter(SpecialCelestialBodyRecipe::name),
        Codec.INT.fieldOf("time").forGetter(SpecialCelestialBodyRecipe::time),
        Codec.INT.fieldOf("space").forGetter(SpecialCelestialBodyRecipe::space),
        Codec.INT.fieldOf("mass").forGetter(SpecialCelestialBodyRecipe::mass),
        Codec.INT.fieldOf("energy").forGetter(SpecialCelestialBodyRecipe::energy),
        Codec.STRING.fieldOf("texture").forGetter(SpecialCelestialBodyRecipe::textureName),
        Codec.BOOL.fieldOf("has_atmosphere").forGetter(SpecialCelestialBodyRecipe::hasAtmosphere),
        LIQUID_COVERAGE_CODEC.optionalFieldOf("liquid_coverage").forGetter(SpecialCelestialBodyRecipe::liquidCoverage),
        Codec.INT.fieldOf("magnetic_field").forGetter(SpecialCelestialBodyRecipe::magneticFieldStrength),
        Codec.INT.fieldOf("rotation_speed").forGetter(SpecialCelestialBodyRecipe::rotationSpeed),
        Codec.FLOAT.fieldOf("axial_tilt").forGetter(SpecialCelestialBodyRecipe::axialTilt),
        Identifier.CODEC.listOf().fieldOf("seed_items").forGetter(SpecialCelestialBodyRecipe::seedItems),
        Codec.BOOL.optionalFieldOf("needs_custom_model", false).forGetter(SpecialCelestialBodyRecipe::needsCustomModel),
        ResourceFields.CODEC.fieldOf("resources").forGetter(
            r -> new ResourceFields(r.minerals, r.fluids, r.biologicalItems,
                r.offerings, r.templeBlessings, r.templePunishments)
        )
    ).apply(ins, SpecialCelestialBodyRecipe::fromCodec));

    @SuppressWarnings("unused")
    private static SpecialCelestialBodyRecipe fromCodec(
        String name, int time, int space, int mass, int energy,
        String texture, boolean hasAtmosphere,
        Optional<LiquidCoverage> liquidCoverage, int magneticField, int rotationSpeed, float axialTilt,
        List<Identifier> seedItems, boolean needsCustomModel,
        ResourceFields res
    ) {
        return new SpecialCelestialBodyRecipe(
            name, texture, needsCustomModel,
            time, space, mass, energy,
            hasAtmosphere, liquidCoverage,
            magneticField, rotationSpeed, axialTilt,
            seedItems,
            res.minerals, res.fluids, res.biologicalItems,
            res.offerings, res.templeBlessings, res.templePunishments
        );
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, SpecialCelestialBodyRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SpecialCelestialBodyRecipe decode(RegistryFriendlyByteBuf buf) {
            String name = buf.readUtf();
            int time = buf.readInt();
            int space = buf.readInt();
            int mass = buf.readInt();
            int energy = buf.readInt();
            String textureName = buf.readUtf();
            boolean hasAtmosphere = buf.readBoolean();
            Optional<LiquidCoverage> liquidCoverage = ByteBufCodecs.optional(LIQUID_COVERAGE_STREAM).decode(buf);
            int magneticFieldStrength = buf.readInt();
            int rotationSpeed = buf.readInt();
            float axialTilt = buf.readFloat();
            List<Identifier> seedItems = Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            boolean needsCustomModel = buf.readBoolean();
            List<WeightedEntry> minerals = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<WeightedEntry> fluids = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<WeightedEntry> biologicalItems = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<WeightedEntry> offerings = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<DemandEntry> templeBlessings = DemandEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<DemandEntry> templePunishments = DemandEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            return new SpecialCelestialBodyRecipe(
                name, textureName, needsCustomModel,
                time, space, mass, energy,
                hasAtmosphere, liquidCoverage,
                magneticFieldStrength, rotationSpeed, axialTilt,
                seedItems,
                minerals, fluids, biologicalItems,
                offerings, templeBlessings, templePunishments
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SpecialCelestialBodyRecipe r) {
            buf.writeUtf(r.name());
            buf.writeInt(r.time());
            buf.writeInt(r.space());
            buf.writeInt(r.mass());
            buf.writeInt(r.energy());
            buf.writeUtf(r.textureName());
            buf.writeBoolean(r.hasAtmosphere());
            ByteBufCodecs.optional(LIQUID_COVERAGE_STREAM).encode(buf, r.liquidCoverage());
            buf.writeInt(r.magneticFieldStrength());
            buf.writeInt(r.rotationSpeed());
            buf.writeFloat(r.axialTilt());
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.seedItems());
            buf.writeBoolean(r.needsCustomModel());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.minerals());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.fluids());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.biologicalItems());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.offerings());
            DemandEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.templeBlessings());
            DemandEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.templePunishments());
        }
    };

    public static final RecipeSerializer<SpecialCelestialBodyRecipe> SERIALIZER = new RecipeSerializer<>(
        CODEC, STREAM_CODEC
    );

    @NotNull
    public Temperature temperature() {
        return energyToTemperature(energy);
    }

    private static Temperature energyToTemperature(int energy) {
        if (energy <= 12) return Temperature.FREEZING;
        if (energy <= 15) return Temperature.COLD;
        if (energy == 16) return Temperature.MILD;
        if (energy <= 22) return Temperature.HOT;
        return Temperature.SCORCHED;
    }

    public boolean isErrorPlanet() {
        return name.equals("error_planet");
    }

    public boolean hasCivilization() {
        return !offerings.isEmpty();
    }

    public Item getEffectiveSeedItem(long worldSeed) {
        if (seedItems.isEmpty()) return Items.AIR;
        if (seedItems.size() == 1) {
            return resolveItem(seedItems.getFirst());
        }
        Random random = new Random(worldSeed * 31L + name.hashCode() * 7919L);
        return resolveItem(seedItems.get(random.nextInt(seedItems.size())));
    }

    public boolean isEffectiveSeedItem(Item consumedItem, long worldSeed) {
        Item effective = getEffectiveSeedItem(worldSeed);
        return consumedItem == effective;
    }

    public PlanetaryResourceSet generateResources() {
        PlanetaryResourceSet r = new PlanetaryResourceSet();
        for (WeightedEntry entry : minerals) {
            r.addMineral(new PlanetaryResourceSet.WeightedItemStack(entry.resourceId(), entry.weight()));
        }
        for (WeightedEntry entry : fluids) {
            r.addFluid(new PlanetaryResourceSet.WeightedFluidStack(entry.resourceId(), entry.weight()));
        }
        for (WeightedEntry entry : biologicalItems) {
            r.addBiologicalItem(new PlanetaryResourceSet.WeightedItemStack(entry.resourceId(), entry.weight()));
        }
        for (WeightedEntry entry : offerings) {
            r.addOffering(new PlanetaryResourceSet.WeightedItemStack(entry.resourceId(), entry.weight()));
        }
        if (hasCivilization()) {
            r.setHasCivilization();
        }
        return r;
    }

    @Nullable
    public LiquidCoverage getLiquidCoverage() {
        return liquidCoverage.orElse(null);
    }

    @Override
    public boolean matches(@NotNull SpecialCelestialBodyInput input, @NotNull Level level) {
        return true;
    }

    @Deprecated
    @Override
    public @NotNull ItemStack assemble(@NotNull SpecialCelestialBodyInput input) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public @NotNull RecipeType<SpecialCelestialBodyRecipe> getType() {
        return ModRecipeTypes.SPECIAL_CELESTIAL_BODY.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public @NotNull RecipeSerializer<SpecialCelestialBodyRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull String group() {
        return "special_celestial_body";
    }

    private static Item resolveItem(Identifier id) {
        return BuiltInRegistries.ITEM.get(id).map(Holder.Reference::value).orElse(Items.AIR);
    }
}
