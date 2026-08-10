package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 根据天体参数定义行星产出资源的配方。
 */
public record PlanetResourceRecipe(
    Category category,
    Optional<MineralData> mineral,
    Optional<FluidData> fluid,
    Optional<GiantData> giant,
    Optional<BiologicalData> biological,
    Optional<OfferingData> offering,
    Optional<WastelandData> wasteland
) implements Recipe<PlanetResourceInput> {

    public enum Category {
        MINERAL("mineral"), FLUID("fluid"), GIANT_ITEM("giant_item"), GIANT_FLUID("giant_fluid"), BIOLOGICAL("biological"), OFFERING(
            "offering"), WASTELAND("wasteland");

        public static final Codec<Category> CODEC = Codec.STRING.xmap(Category::fromName, Category::getSerializedName);

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getSerializedName() {
            return this.name;
        }

        public static Category fromName(String name) {
            for (Category value : Category.values()) {
                if (value.name.equals(name)) return value;
            }
            throw new IllegalArgumentException("Unknown planet resource category: " + name);
        }
    }

    public record WeightedChoice(String id, int weight) {
        public static final Codec<WeightedChoice> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("id").forGetter(WeightedChoice::id),
            Codec.INT.fieldOf("weight").forGetter(WeightedChoice::weight)
        ).apply(ins, WeightedChoice::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedChoice> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WeightedChoice::id,
            ByteBufCodecs.INT,
            WeightedChoice::weight,
            WeightedChoice::new
        );

        public Identifier resourceId() {
            return Identifier.parse(this.id);
        }
    }

    public record WeightedEntry(List<WeightedChoice> choices, int weight) {
        private record DirectEntry(String id, int weight) {
            private static final Codec<DirectEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("id").forGetter(DirectEntry::id),
                Codec.INT.fieldOf("weight").forGetter(DirectEntry::weight)
            ).apply(ins, DirectEntry::new));
        }

        private record ChoiceEntry(List<WeightedChoice> choices, int weight) {
            private static final Codec<ChoiceEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                WeightedChoice.CODEC.listOf().fieldOf("choices").forGetter(ChoiceEntry::choices),
                Codec.INT.fieldOf("weight").forGetter(ChoiceEntry::weight)
            ).apply(ins, ChoiceEntry::new));
        }

        public static final Codec<WeightedEntry> CODEC = Codec.either(DirectEntry.CODEC, ChoiceEntry.CODEC)
            .xmap(
                either -> either.map(
                    direct -> new WeightedEntry(direct.id(), direct.weight()),
                    choice -> new WeightedEntry(choice.choices(), choice.weight())
                ),
                entry -> entry.isDirect()
                    ? Either.left(new DirectEntry(entry.choices().getFirst().id(), entry.weight()))
                    : Either.right(new ChoiceEntry(entry.choices(), entry.weight()))
            );

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedEntry> STREAM_CODEC = StreamCodec.composite(
            WeightedChoice.STREAM_CODEC.apply(ByteBufCodecs.list()),
            WeightedEntry::choices,
            ByteBufCodecs.INT,
            WeightedEntry::weight,
            WeightedEntry::new
        );

        public WeightedEntry {
            choices = List.copyOf(choices);
            if (choices.isEmpty()) {
                throw new IllegalArgumentException("Planet resource entry must contain at least one choice");
            }
        }

        public WeightedEntry(String id, int weight) {
            this(List.of(new WeightedChoice(id, 1)), weight);
        }

        public boolean isDirect() {
            return this.choices.size() == 1;
        }

        public Identifier select(RandomSource random) {
            if (this.isDirect()) return this.choices.getFirst().resourceId();
            int totalWeight = 0;
            for (WeightedChoice choice : this.choices) {
                totalWeight += Math.max(0, choice.weight());
            }
            if (totalWeight <= 0) return this.choices.getFirst().resourceId();
            int selected = random.nextInt(totalWeight);
            for (WeightedChoice choice : this.choices) {
                selected -= Math.max(0, choice.weight());
                if (selected < 0) return choice.resourceId();
            }
            return this.choices.getLast().resourceId();
        }
    }

    public static List<WeightedEntry> entries(Consumer<EntriesBuilder> consumer) {
        EntriesBuilder builder = new EntriesBuilder();
        consumer.accept(builder);
        return builder.build();
    }

    public static final class EntriesBuilder {
        private final List<WeightedEntry> entries = new java.util.ArrayList<>();

        public EntriesBuilder id(String id, int weight) {
            return this.id(Identifier.parse(id), weight);
        }

        public EntriesBuilder id(Identifier id, int weight) {
            this.entries.add(new WeightedEntry(id.toString(), requirePositive(weight)));
            return this;
        }

        public EntriesBuilder item(ItemLike item, int weight) {
            return this.id(BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(item).asItem()), weight);
        }

        public EntriesBuilder fluid(Fluid fluid, int weight) {
            return this.id(BuiltInRegistries.FLUID.getKey(Objects.requireNonNull(fluid)), weight);
        }

        public EntriesBuilder fluid(Supplier<? extends Fluid> fluid, int weight) {
            return this.fluid(Objects.requireNonNull(fluid).get(), weight);
        }

        public EntriesBuilder chooseOne(int weight, Consumer<ChoiceBuilder> consumer) {
            ChoiceBuilder builder = new ChoiceBuilder();
            consumer.accept(builder);
            this.entries.add(new WeightedEntry(builder.build(), requirePositive(weight)));
            return this;
        }

        public List<WeightedEntry> build() {
            return List.copyOf(this.entries);
        }
    }

    public static final class ChoiceBuilder {
        private final List<WeightedChoice> choices = new java.util.ArrayList<>();

        public ChoiceBuilder id(String id, int weight) {
            return this.id(Identifier.parse(id), weight);
        }

        public ChoiceBuilder id(Identifier id, int weight) {
            this.choices.add(new WeightedChoice(id.toString(), requirePositive(weight)));
            return this;
        }

        public ChoiceBuilder item(ItemLike item, int weight) {
            return this.id(BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(item).asItem()), weight);
        }

        public ChoiceBuilder fluid(Fluid fluid, int weight) {
            return this.id(BuiltInRegistries.FLUID.getKey(Objects.requireNonNull(fluid)), weight);
        }

        public ChoiceBuilder fluid(Supplier<? extends Fluid> fluid, int weight) {
            return this.fluid(Objects.requireNonNull(fluid).get(), weight);
        }

        private List<WeightedChoice> build() {
            if (this.choices.isEmpty()) {
                throw new IllegalStateException("Planet resource choice group cannot be empty");
            }
            return List.copyOf(this.choices);
        }
    }

    private static int requirePositive(int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Planet resource weight must be positive");
        return weight;
    }

    public record LifeChances(int cold, int hot, int mild) {
        public static final LifeChances DEFAULT = new LifeChances(5, 5, 10);

        public static final Codec<LifeChances> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("cold", 5).forGetter(LifeChances::cold),
            Codec.INT.optionalFieldOf("hot", 5).forGetter(LifeChances::hot),
            Codec.INT.optionalFieldOf("mild", 10).forGetter(LifeChances::mild)
        ).apply(ins, LifeChances::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, LifeChances> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            LifeChances::cold,
            ByteBufCodecs.INT,
            LifeChances::hot,
            ByteBufCodecs.INT,
            LifeChances::mild,
            LifeChances::new
        );

        public int forTemperature(Temperature temp) {
            return switch (temp) {
                case COLD -> this.cold;
                case HOT -> this.hot;
                case MILD -> this.mild;
                default -> 0;
            };
        }
    }

    public record MineralData(String sourceTag, String blacklistTag, int step) {
        public static final Codec<MineralData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("source_tag", "c:raw_materials").forGetter(MineralData::sourceTag),
            Codec.STRING.optionalFieldOf("blacklist_tag", "anvilcraft:non_planetary_minerals").forGetter(MineralData::blacklistTag),
            Codec.INT.optionalFieldOf("step", 10).forGetter(MineralData::step)
        ).apply(ins, MineralData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MineralData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            MineralData::sourceTag,
            ByteBufCodecs.STRING_UTF8,
            MineralData::blacklistTag,
            ByteBufCodecs.INT,
            MineralData::step,
            MineralData::new
        );
    }

    public record FluidData(String planetType, String temperature, String liquidMin, String outputFluid) {
        public static final Codec<FluidData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("planet_type", "rocky_planet").forGetter(FluidData::planetType),
            Codec.STRING.optionalFieldOf("temperature", "").forGetter(FluidData::temperature),
            Codec.STRING.optionalFieldOf("liquid_min", "low").forGetter(FluidData::liquidMin),
            Codec.STRING.fieldOf("output_fluid").forGetter(FluidData::outputFluid)
        ).apply(ins, FluidData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FluidData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            FluidData::planetType,
            ByteBufCodecs.STRING_UTF8,
            FluidData::temperature,
            ByteBufCodecs.STRING_UTF8,
            FluidData::liquidMin,
            ByteBufCodecs.STRING_UTF8,
            FluidData::outputFluid,
            FluidData::new
        );

        public FluidData(String planetType, String temperature, String liquidMin, Fluid outputFluid) {
            this(
                planetType,
                temperature,
                liquidMin,
                BuiltInRegistries.FLUID.getKey(Objects.requireNonNull(outputFluid)).toString()
            );
        }

        public FluidData(
            String planetType,
            String temperature,
            String liquidMin,
            Supplier<? extends Fluid> outputFluid
        ) {
            this(planetType, temperature, liquidMin, Objects.requireNonNull(outputFluid).get());
        }
    }

    public record GiantData(List<WeightedEntry> entries, String pressureType) {
        public static final Codec<GiantData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            WeightedEntry.CODEC.listOf()
                .fieldOf("entries")
                .forGetter(GiantData::entries),
            Codec.STRING.fieldOf("pressure_type").forGetter(GiantData::pressureType)
        ).apply(ins, GiantData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, GiantData> STREAM_CODEC = StreamCodec.composite(
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            GiantData::entries,
            ByteBufCodecs.STRING_UTF8,
            GiantData::pressureType,
            GiantData::new
        );
    }

    public record BiologicalData(
        LifeChances lifeChances, String landEntityTag, String aquaticEntityTag, String dropBlacklistTag, List<WeightedEntry> mildExtraFluids
    ) {
        public static final Codec<BiologicalData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            LifeChances.CODEC.optionalFieldOf("life_chances", LifeChances.DEFAULT).forGetter(BiologicalData::lifeChances),
            Codec.STRING.optionalFieldOf("land_entity_tag", "anvilcraft:planetary_land_animals").forGetter(BiologicalData::landEntityTag),
            Codec.STRING.optionalFieldOf("aquatic_entity_tag", "anvilcraft:planetary_aquatic_animals")
                .forGetter(BiologicalData::aquaticEntityTag),
            Codec.STRING.optionalFieldOf("drop_blacklist_tag", "anvilcraft:non_planetary_mob_drops")
                .forGetter(BiologicalData::dropBlacklistTag),
            WeightedEntry.CODEC.listOf().optionalFieldOf("mild_extra_fluids", List.of()).forGetter(BiologicalData::mildExtraFluids)
        ).apply(ins, BiologicalData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, BiologicalData> STREAM_CODEC = StreamCodec.composite(
            LifeChances.STREAM_CODEC,
            BiologicalData::lifeChances,
            ByteBufCodecs.STRING_UTF8,
            BiologicalData::landEntityTag,
            ByteBufCodecs.STRING_UTF8,
            BiologicalData::aquaticEntityTag,
            ByteBufCodecs.STRING_UTF8,
            BiologicalData::dropBlacklistTag,
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            BiologicalData::mildExtraFluids,
            BiologicalData::new
        );
    }

    public record OfferingData(
        List<WeightedEntry> entries, int civilizationChance, int ageMin, int ageMax
    ) {
        public static final Codec<OfferingData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            WeightedEntry.CODEC.listOf().fieldOf("entries").forGetter(OfferingData::entries),
            Codec.INT.optionalFieldOf("civilization_chance", 50).forGetter(OfferingData::civilizationChance),
            Codec.INT.optionalFieldOf("age_min", 32).forGetter(OfferingData::ageMin),
            Codec.INT.optionalFieldOf("age_max", 43).forGetter(OfferingData::ageMax)
        ).apply(ins, OfferingData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, OfferingData> STREAM_CODEC = StreamCodec.composite(
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            OfferingData::entries,
            ByteBufCodecs.INT,
            OfferingData::civilizationChance,
            ByteBufCodecs.INT,
            OfferingData::ageMin,
            ByteBufCodecs.INT,
            OfferingData::ageMax,
            OfferingData::new
        );
    }

    public record WastelandData(
        List<WeightedEntry> entries, int ageMin, int wastelandChance
    ) {
        public static final Codec<WastelandData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            WeightedEntry.CODEC.listOf().fieldOf("entries").forGetter(WastelandData::entries),
            Codec.INT.optionalFieldOf("age_min", 35).forGetter(WastelandData::ageMin),
            Codec.INT.optionalFieldOf("wasteland_chance", 10).forGetter(WastelandData::wastelandChance)
        ).apply(ins, WastelandData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WastelandData> STREAM_CODEC = StreamCodec.composite(
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            WastelandData::entries,
            ByteBufCodecs.INT,
            WastelandData::ageMin,
            ByteBufCodecs.INT,
            WastelandData::wastelandChance,
            WastelandData::new
        );
    }

    public static Builder builder(Category category) {
        return new Builder(category);
    }

    public static final class Builder {
        private final Category category;
        private Optional<MineralData> mineral = Optional.empty();
        private Optional<FluidData> fluid = Optional.empty();
        private Optional<GiantData> giant = Optional.empty();
        private Optional<BiologicalData> biological = Optional.empty();
        private Optional<OfferingData> offering = Optional.empty();
        private Optional<WastelandData> wasteland = Optional.empty();

        private Builder(Category category) {
            this.category = Objects.requireNonNull(category);
        }

        public Builder mineral(MineralData mineral) {
            this.mineral = Optional.of(Objects.requireNonNull(mineral));
            return this;
        }

        public Builder fluid(FluidData fluid) {
            this.fluid = Optional.of(Objects.requireNonNull(fluid));
            return this;
        }

        public Builder giant(GiantData giant) {
            this.giant = Optional.of(Objects.requireNonNull(giant));
            return this;
        }

        public Builder biological(BiologicalData biological) {
            this.biological = Optional.of(Objects.requireNonNull(biological));
            return this;
        }

        public Builder offering(OfferingData offering) {
            this.offering = Optional.of(Objects.requireNonNull(offering));
            return this;
        }

        public Builder wasteland(WastelandData wasteland) {
            this.wasteland = Optional.of(Objects.requireNonNull(wasteland));
            return this;
        }

        public PlanetResourceRecipe build() {
            return new PlanetResourceRecipe(
                this.category,
                this.mineral,
                this.fluid,
                this.giant,
                this.biological,
                this.offering,
                this.wasteland
            );
        }
    }

    public static final MapCodec<PlanetResourceRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Category.CODEC.fieldOf("category").forGetter(PlanetResourceRecipe::category),
        MineralData.CODEC.optionalFieldOf("mineral").forGetter(PlanetResourceRecipe::mineral),
        FluidData.CODEC.optionalFieldOf("fluid").forGetter(PlanetResourceRecipe::fluid),
        GiantData.CODEC.optionalFieldOf("giant").forGetter(PlanetResourceRecipe::giant),
        BiologicalData.CODEC.optionalFieldOf("biological").forGetter(PlanetResourceRecipe::biological),
        OfferingData.CODEC.optionalFieldOf("offering").forGetter(PlanetResourceRecipe::offering),
        WastelandData.CODEC.optionalFieldOf("wasteland").forGetter(PlanetResourceRecipe::wasteland)
    ).apply(ins, PlanetResourceRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetResourceRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlanetResourceRecipe decode(RegistryFriendlyByteBuf buf) {
            Category category = Category.fromName(buf.readUtf());
            Optional<MineralData> mineral = ByteBufCodecs.optional(MineralData.STREAM_CODEC).decode(buf);
            Optional<FluidData> fluid = ByteBufCodecs.optional(FluidData.STREAM_CODEC).decode(buf);
            Optional<GiantData> giant = ByteBufCodecs.optional(GiantData.STREAM_CODEC).decode(buf);
            Optional<BiologicalData> biological = ByteBufCodecs.optional(BiologicalData.STREAM_CODEC).decode(buf);
            Optional<OfferingData> offering = ByteBufCodecs.optional(OfferingData.STREAM_CODEC).decode(buf);
            Optional<WastelandData> wasteland = ByteBufCodecs.optional(WastelandData.STREAM_CODEC).decode(buf);
            return new PlanetResourceRecipe(category, mineral, fluid, giant, biological, offering, wasteland);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, PlanetResourceRecipe recipe) {
            buf.writeUtf(recipe.category().getSerializedName());
            ByteBufCodecs.optional(MineralData.STREAM_CODEC).encode(buf, recipe.mineral());
            ByteBufCodecs.optional(FluidData.STREAM_CODEC).encode(buf, recipe.fluid());
            ByteBufCodecs.optional(GiantData.STREAM_CODEC).encode(buf, recipe.giant());
            ByteBufCodecs.optional(BiologicalData.STREAM_CODEC).encode(buf, recipe.biological());
            ByteBufCodecs.optional(OfferingData.STREAM_CODEC).encode(buf, recipe.offering());
            ByteBufCodecs.optional(WastelandData.STREAM_CODEC).encode(buf, recipe.wasteland());
        }
    };

    public static final RecipeSerializer<PlanetResourceRecipe> SERIALIZER = new RecipeSerializer<>(
        PlanetResourceRecipe.CODEC, PlanetResourceRecipe.STREAM_CODEC
    );

    @Override
    public boolean matches(PlanetResourceInput input, Level level) {
        CelestialBodyData body = input.body();
        return switch (this.category) {
            case MINERAL, BIOLOGICAL, OFFERING, WASTELAND -> body instanceof RockyPlanetData;
            case FLUID -> {
                if (!(body instanceof RockyPlanetData rocky)) yield false;
                FluidData fd = this.fluid.orElse(null);
                if (fd == null) yield false;
                if (!fd.planetType.isEmpty() && !fd.planetType.equals(body.type().getSerializedName())) {
                    yield false;
                }
                if (!fd.temperature.isEmpty() && !fd.temperature.equals(rocky.temperature().getSerializedName())) {
                    yield false;
                }
                if (!fd.liquidMin.isEmpty()) {
                    LiquidCoverage min = LiquidCoverage.fromName(fd.liquidMin);
                    if (rocky.liquidCoverage().ordinal() < min.ordinal()) yield false;
                }
                yield rocky.liquidCoverage() != LiquidCoverage.NONE;
            }
            case GIANT_ITEM, GIANT_FLUID -> {
                if (!(body instanceof GiantPlanetData giantBody)) yield false;
                GiantData gd = this.giant().orElse(null);
                if (gd == null) yield false;
                yield gd.pressureType.isEmpty() || gd.pressureType.equals(giantBody.pressureType().getSerializedName());
            }
        };
    }

    @Deprecated
    @Override
    public ItemStack assemble(PlanetResourceInput input) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public RecipeType<PlanetResourceRecipe> getType() {
        return ModRecipeTypes.PLANET_RESOURCE.get();
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
    public RecipeSerializer<PlanetResourceRecipe> getSerializer() {
        return PlanetResourceRecipe.SERIALIZER;
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
    public String group() {
        return "planet_resource";
    }

    public @Nullable MineralData mineralData() {
        return this.mineral.orElse(null);
    }

    public @Nullable FluidData fluidData() {
        return this.fluid.orElse(null);
    }

    public @Nullable GiantData giantData() {
        return this.giant.orElse(null);
    }

    public @Nullable BiologicalData biologicalData() {
        return this.biological.orElse(null);
    }

    public @Nullable OfferingData offeringData() {
        return this.offering.orElse(null);
    }

    public @Nullable WastelandData wastelandData() {
        return this.wasteland.orElse(null);
    }
}
