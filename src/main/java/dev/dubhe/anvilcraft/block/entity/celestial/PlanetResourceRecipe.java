package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/// 定义行星根据其天体参数产出哪些资源的配方。
///
/// 使用单一配方类型（{@code anvilcraft:planet_resource}），
/// 通过 {@code category} 鉴别器确定资源生成规则。
/// 每个类别的数据存储在嵌套子记录中。
public record PlanetResourceRecipe(
    Category category,
    Optional<MineralData> mineral,
    Optional<FluidData> fluid,
    Optional<GiantData> giant,
    Optional<BiologicalData> biological,
    Optional<OfferingData> offering,
    Optional<WastelandData> wasteland
) implements Recipe<PlanetResourceInput> {

    /// === 类别 ===

    public enum Category {
        MINERAL("mineral"), FLUID("fluid"), GIANT_ITEM("giant_item"), GIANT_FLUID("giant_fluid"), BIOLOGICAL("biological"), OFFERING(
            "offering"), WASTELAND("wasteland");

        public static final Codec<Category> CODEC = Codec.STRING.xmap(Category::fromName, Category::getSerializedName);

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getSerializedName() {
            return name;
        }

        public static Category fromName(String name) {
            for (Category value : values()) {
                if (value.name.equals(name)) return value;
            }
            throw new IllegalArgumentException("Unknown planet resource category: " + name);
        }
    }

    /// === 加权条目 ===

    public record WeightedEntry(String id, int weight) {
        public static final Codec<WeightedEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("id")
                    .forGetter(WeightedEntry::id), Codec.INT.fieldOf("weight").forGetter(WeightedEntry::weight)
            )
            .apply(ins, WeightedEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WeightedEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WeightedEntry::id,
            ByteBufCodecs.INT,
            WeightedEntry::weight,
            WeightedEntry::new
        );

        public ResourceLocation resourceId() {
            return ResourceLocation.parse(id);
        }
    }

    /// === 生命概率 ===

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
                case COLD -> cold;
                case HOT -> hot;
                case MILD -> mild;
                default -> 0;
            };
        }
    }

    /// === 各类别数据记录 ===

    /// 矿物资源配置。
    /// category=mineral：所有岩石行星从来源标签产出矿物。
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

    /// 流体资源映射。
    /// category=fluid：定义岩石行星表面产出何种流体。
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
    }

    /// 气态行星物品或流体资源。
    /// category=giant_item或giant_fluid：气态行星资源的加权条目。
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

    /// 生物资源配置。
    /// category=biological：定义生命概率、实体标签和温和温度下的额外流体。
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

    /// 祭品（文明）资源条目。
    /// category=offering：低级文明的加权物品。
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

    /// 废土资源条目。
    /// category=wasteland：废土世界的加权物品。
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

    /// === 编解码器 ===

    public static final MapCodec<PlanetResourceRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Category.CODEC.fieldOf("category").forGetter(PlanetResourceRecipe::category),
        MineralData.CODEC.optionalFieldOf("mineral").forGetter(PlanetResourceRecipe::mineral),
        FluidData.CODEC.optionalFieldOf("fluid").forGetter(PlanetResourceRecipe::fluid),
        GiantData.CODEC.optionalFieldOf("giant").forGetter(PlanetResourceRecipe::giant),
        BiologicalData.CODEC.optionalFieldOf("biological").forGetter(PlanetResourceRecipe::biological),
        OfferingData.CODEC.optionalFieldOf("offering").forGetter(PlanetResourceRecipe::offering),
        WastelandData.CODEC.optionalFieldOf("wasteland").forGetter(PlanetResourceRecipe::wasteland)
    ).apply(ins, PlanetResourceRecipe::new));

    /// === 流编解码器 ===

    public static final StreamCodec<RegistryFriendlyByteBuf, PlanetResourceRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull PlanetResourceRecipe decode(RegistryFriendlyByteBuf buf) {
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

    /// === 配方实现 ===

    @Override
    public boolean matches(PlanetResourceInput input, @NotNull Level level) {
        CelestialBodyData body = input.body();
        return switch (category) {
            case MINERAL -> body instanceof RockyPlanetData;
            case FLUID -> {
                if (!(body instanceof RockyPlanetData rocky)) yield false;
                FluidData fd = fluid.orElse(null);
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
            case BIOLOGICAL -> body instanceof RockyPlanetData;
            case OFFERING -> body instanceof RockyPlanetData;
            case WASTELAND -> body instanceof RockyPlanetData;
        };
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull PlanetResourceInput input, HolderLookup.@NotNull Provider registries) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.PLANET_RESOURCE_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipeTypes.PLANET_RESOURCE_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /// === 便捷访问器（供生成器使用） ===

    public MineralData mineralData() {
        return mineral.orElse(null);
    }

    public FluidData fluidData() {
        return fluid.orElse(null);
    }

    public GiantData giantData() {
        return giant.orElse(null);
    }

    public BiologicalData biologicalData() {
        return biological.orElse(null);
    }

    public OfferingData offeringData() {
        return offering.orElse(null);
    }

    public WastelandData wastelandData() {
        return wasteland.orElse(null);
    }

    /// === 序列化器 ===

    public static final class Serializer implements RecipeSerializer<PlanetResourceRecipe> {
        @Override
        public @NotNull MapCodec<PlanetResourceRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, PlanetResourceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
