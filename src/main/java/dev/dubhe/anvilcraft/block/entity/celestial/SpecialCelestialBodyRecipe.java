package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;

/// 特殊天体配方 —— 通过种子物品在锻星砧中发现隐藏天体。
///
/// 配方运行流程：
/// 1. 玩家在种子格放入种子物品（如草方块、泥土等），开始搜索
/// 2. 服务器在搜索完成时调用 {@link #isEffectiveSeedItem} 检查种子物品
///    是否匹配（基于世界种子的伪随机选择，每个世界不同）
/// 3. 匹配成功后，从配方的固定参数（{@code time}/{@code space}/{@code mass}/{@code energy}）直接构造
///    {@link SpecialCelestialBodyData}，跳过常规的三步图表匹配
/// 4. 温度由 {@code energy} 砧子数自动推导，文明由 {@code offerings} 非空自动判定
/// 5. 资源通过 {@link #generateResources} 从配方的 {@code minerals}/{@code fluids}/
///    {@code biologicalItems}/{@code biologicalFluids}/{@code offerings} 列表直接生成
///
/// {@code model} 字段在两种渲染模式下含义不同：
/// - {@code needs_custom_model=false} → 贴图名，用于 {@code CelestialBodyTextureBakery} 程序化生成贴图
/// - {@code needs_custom_model=true}  → 模型资源 ID；旧格式模型名仍加载 AnvilCraft 天体模型目录中的模型
/// - {@code atmosphere}               → 十六进制 RGB 颜色；省略时没有大气层
@SuppressWarnings("checkstyle:LineLength")
public record SpecialCelestialBodyRecipe(
    String name,
    String model,
    boolean needsCustomModel,
    boolean canBeShattered,
    int time,
    int space,
    int mass,
    int energy,
    Optional<ColorRGBA> atmosphere,
    Optional<LiquidCoverage> liquidCoverage,
    int magneticFieldStrength,
    int rotationSpeed,
    float axialTilt,
    List<ResourceLocation> seedItems,
    List<WeightedEntry> minerals,
    List<WeightedEntry> fluids,
    List<WeightedEntry> biologicalItems,
    List<WeightedEntry> biologicalFluids,
    List<WeightedEntry> offerings,
    List<DemandEntry> templeBlessings,
    List<DemandEntry> templePunishments,
    Optional<CelestialTravelData> landing
) implements Recipe<SpecialCelestialBodyInput> {

    /**
     * 兼容着陆规则加入前编译的集成。
     *
     * @deprecated 请使用带有 {@code Optional<ColorRGBA> atmosphere} 参数的构造器。
     */
    @Deprecated
    public SpecialCelestialBodyRecipe(
        String name,
        String model,
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
        List<ResourceLocation> seedItems,
        List<WeightedEntry> minerals,
        List<WeightedEntry> fluids,
        List<WeightedEntry> biologicalItems,
        List<WeightedEntry> biologicalFluids,
        List<WeightedEntry> offerings,
        List<DemandEntry> templeBlessings,
        List<DemandEntry> templePunishments
    ) {
        this(
            name, model, needsCustomModel, time, space, mass, energy, hasAtmosphere, liquidCoverage,
            magneticFieldStrength, rotationSpeed, axialTilt, seedItems, minerals, fluids,
            biologicalItems, biologicalFluids, offerings, templeBlessings, templePunishments, Optional.empty()
        );
    }

    /**
     * 兼容可粉碎性尚未由数据驱动时编写的集成。
     *
     * @deprecated 请使用带有 {@code Optional<ColorRGBA> atmosphere} 参数的构造器。
     */
    @Deprecated
    public SpecialCelestialBodyRecipe(
        String name,
        String model,
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
        List<ResourceLocation> seedItems,
        List<WeightedEntry> minerals,
        List<WeightedEntry> fluids,
        List<WeightedEntry> biologicalItems,
        List<WeightedEntry> biologicalFluids,
        List<WeightedEntry> offerings,
        List<DemandEntry> templeBlessings,
        List<DemandEntry> templePunishments,
        Optional<CelestialTravelData> landing
    ) {
        this(
            name, model, needsCustomModel, false, time, space, mass, energy,
            legacyAtmosphere(energy, hasAtmosphere), liquidCoverage,
            magneticFieldStrength, rotationSpeed, axialTilt, seedItems, minerals, fluids,
            biologicalItems, biologicalFluids, offerings, templeBlessings, templePunishments, landing
        );
    }

    /**
     * 兼容大气层颜色尚不可配置时编译的集成。
     *
     * @deprecated 请使用带有 {@code Optional<ColorRGBA> atmosphere} 参数的构造器。
     */
    @Deprecated
    public SpecialCelestialBodyRecipe(
        String name,
        String model,
        boolean needsCustomModel,
        boolean canBeShattered,
        int time,
        int space,
        int mass,
        int energy,
        boolean hasAtmosphere,
        Optional<LiquidCoverage> liquidCoverage,
        int magneticFieldStrength,
        int rotationSpeed,
        float axialTilt,
        List<ResourceLocation> seedItems,
        List<WeightedEntry> minerals,
        List<WeightedEntry> fluids,
        List<WeightedEntry> biologicalItems,
        List<WeightedEntry> biologicalFluids,
        List<WeightedEntry> offerings,
        List<DemandEntry> templeBlessings,
        List<DemandEntry> templePunishments,
        Optional<CelestialTravelData> landing
    ) {
        this(
            name, model, needsCustomModel, canBeShattered, time, space, mass, energy,
            legacyAtmosphere(energy, hasAtmosphere), liquidCoverage,
            magneticFieldStrength, rotationSpeed, axialTilt, seedItems, minerals, fluids,
            biologicalItems, biologicalFluids, offerings, templeBlessings, templePunishments, landing
        );
    }

    /** 供将此字段称为 {@code travel} 数据的集成调用的别名。 */
    public Optional<CelestialTravelData> landingData() {
        return landing;
    }

    /** 兼容将此数据称为 {@code travel} 的数据包集成。 */
    public Optional<CelestialTravelData> travel() {
        return landing;
    }

    public boolean isLandable() {
        return landing.isPresent();
    }

    /// === 加权条目 ===

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

        public ResourceLocation resourceId() {
            return ResourceLocation.parse(id);
        }
    }

    /// === 需求条目（神殿需求物品，使用数量而非权重）===

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

    /// === 枚举编解码器 ===

    private static final Codec<LiquidCoverage> LIQUID_COVERAGE_CODEC =
        Codec.STRING.xmap(LiquidCoverage::fromName, LiquidCoverage::getSerializedName);

    private static final Codec<ResourceLocation> RESOURCE_LOCATION_CODEC =
        ResourceLocation.CODEC;

    private static final StreamCodec<ByteBuf, LiquidCoverage> LIQUID_COVERAGE_STREAM =
        ByteBufCodecs.STRING_UTF8.map(LiquidCoverage::fromName, LiquidCoverage::getSerializedName);

    private static final StreamCodec<ByteBuf, ColorRGBA> ATMOSPHERE_COLOR_STREAM =
        ByteBufCodecs.INT.map(ColorRGBA::new, ColorRGBA::rgba);

    private static final Lifecycle LEGACY_ATMOSPHERE_LIFECYCLE = Lifecycle.deprecated(1);

    /// 旧版 {@code has_atmosphere} 字段存在时，将配方标记为已弃用。
    private static final MapCodec<Optional<Boolean>> LEGACY_HAS_ATMOSPHERE_CODEC =
        Codec.BOOL.optionalFieldOf("has_atmosphere").flatXmap(
            SpecialCelestialBodyRecipe::withLegacyAtmosphereLifecycle,
            SpecialCelestialBodyRecipe::withLegacyAtmosphereLifecycle
        );

    /// === 资源字段（仅用于编解码器的包装器，以避免超过 16 字段分组限制）===

    private record ResourceFields(
        List<WeightedEntry> minerals,
        List<WeightedEntry> fluids,
        List<WeightedEntry> biologicalItems,
        List<WeightedEntry> biologicalFluids,
        List<WeightedEntry> offerings,
        List<DemandEntry> templeBlessings,
        List<DemandEntry> templePunishments
    ) {
        static final Codec<ResourceFields> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            WeightedEntry.CODEC.listOf().optionalFieldOf("minerals", List.of()).forGetter(ResourceFields::minerals),
            WeightedEntry.CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(ResourceFields::fluids),
            WeightedEntry.CODEC.listOf().optionalFieldOf("biological_items", List.of()).forGetter(ResourceFields::biologicalItems),
            WeightedEntry.CODEC.listOf().optionalFieldOf("biological_fluids", List.of()).forGetter(ResourceFields::biologicalFluids),
            WeightedEntry.CODEC.listOf().optionalFieldOf("offerings", List.of()).forGetter(ResourceFields::offerings),
            DemandEntry.CODEC.listOf().optionalFieldOf("temple_blessings", List.of()).forGetter(ResourceFields::templeBlessings),
            DemandEntry.CODEC.listOf().optionalFieldOf("temple_punishments", List.of()).forGetter(ResourceFields::templePunishments)
        ).apply(ins, ResourceFields::new));
    }

    private record RenderingFields(boolean needsCustomModel, Optional<Boolean> canBeShattered) {
        static final MapCodec<RenderingFields> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("needs_custom_model", false).forGetter(RenderingFields::needsCustomModel),
            Codec.BOOL.optionalFieldOf("can_be_shattered").forGetter(RenderingFields::canBeShattered)
        ).apply(ins, RenderingFields::new));
    }

    private record AtmosphereFields(Optional<ColorRGBA> atmosphere, Optional<Boolean> hasAtmosphere) {
        static final MapCodec<AtmosphereFields> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ColorRGBA.CODEC.optionalFieldOf("atmosphere").forGetter(AtmosphereFields::atmosphere),
            LEGACY_HAS_ATMOSPHERE_CODEC.forGetter(AtmosphereFields::hasAtmosphere)
        ).apply(ins, AtmosphereFields::new));
    }

    /// === 顶层编解码器 ===

    public static final MapCodec<SpecialCelestialBodyRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING.fieldOf("name").forGetter(SpecialCelestialBodyRecipe::name),
        Codec.INT.fieldOf("time").forGetter(SpecialCelestialBodyRecipe::time),
        Codec.INT.fieldOf("space").forGetter(SpecialCelestialBodyRecipe::space),
        Codec.INT.fieldOf("mass").forGetter(SpecialCelestialBodyRecipe::mass),
        Codec.INT.fieldOf("energy").forGetter(SpecialCelestialBodyRecipe::energy),
        Codec.STRING.fieldOf("model").forGetter(SpecialCelestialBodyRecipe::model),
        AtmosphereFields.CODEC.forGetter(recipe -> new AtmosphereFields(recipe.atmosphere(), Optional.empty())),
        LIQUID_COVERAGE_CODEC.optionalFieldOf("liquid_coverage").forGetter(SpecialCelestialBodyRecipe::liquidCoverage),
        Codec.INT.fieldOf("magnetic_field").forGetter(SpecialCelestialBodyRecipe::magneticFieldStrength),
        Codec.INT.fieldOf("rotation_speed").forGetter(SpecialCelestialBodyRecipe::rotationSpeed),
        Codec.FLOAT.fieldOf("axial_tilt").forGetter(SpecialCelestialBodyRecipe::axialTilt),
        RESOURCE_LOCATION_CODEC.listOf().fieldOf("seed_items").forGetter(SpecialCelestialBodyRecipe::seedItems),
        RenderingFields.CODEC.forGetter(recipe -> new RenderingFields(
            recipe.needsCustomModel(),
            Optional.of(recipe.canBeShattered())
        )),
        ResourceFields.CODEC.fieldOf("resources").forGetter(
            r -> new ResourceFields(r.minerals, r.fluids, r.biologicalItems, r.biologicalFluids,
                r.offerings, r.templeBlessings, r.templePunishments)
        ),
        CelestialTravelData.CODEC.optionalFieldOf("landing").forGetter(SpecialCelestialBodyRecipe::landing),
        CelestialTravelData.CODEC.optionalFieldOf("travel").forGetter(recipe -> Optional.empty())
    ).apply(ins, SpecialCelestialBodyRecipe::fromCodec));

    @SuppressWarnings("unused")
    private static SpecialCelestialBodyRecipe fromCodec(
        String name, int time, int space, int mass, int energy,
        String texture, AtmosphereFields atmosphereFields,
        Optional<LiquidCoverage> liquidCoverage, int magneticField, int rotationSpeed, float axialTilt,
        List<ResourceLocation> seedItems, RenderingFields rendering,
        ResourceFields res, Optional<CelestialTravelData> landing, Optional<CelestialTravelData> travel
    ) {
        Optional<CelestialTravelData> resolvedLanding = landing.isPresent() ? landing : travel;
        Optional<ColorRGBA> atmosphere = atmosphereFields.atmosphere();
        if (atmosphere.isEmpty() && atmosphereFields.hasAtmosphere().orElse(false)) {
            atmosphere = legacyAtmosphere(energy, true);
        }
        boolean canBeShattered = rendering.canBeShattered().orElseGet(
            () -> legacyCanBeShattered(resolvedLanding)
        );
        return new SpecialCelestialBodyRecipe(
            name, texture, rendering.needsCustomModel(), canBeShattered,
            time, space, mass, energy,
            atmosphere, liquidCoverage,
            magneticField, rotationSpeed, axialTilt,
            seedItems,
            res.minerals, res.fluids, res.biologicalItems, res.biologicalFluids,
            res.offerings, res.templeBlessings, res.templePunishments,
            resolvedLanding
        );
    }

    private static boolean legacyCanBeShattered(Optional<CelestialTravelData> landing) {
        return landing.map(CelestialTravelData::dimension)
            .filter(CelestialTravelManager.OVERWORLD_LIKE_DIMENSION::equals)
            .isPresent();
    }

    private static Optional<ColorRGBA> legacyAtmosphere(int energy, boolean hasAtmosphere) {
        if (!hasAtmosphere) return Optional.empty();
        return Optional.of(defaultAtmosphereColor(CelestialBodyMatcher.energyToTemperature(energy)));
    }

    private static DataResult<Optional<Boolean>> withLegacyAtmosphereLifecycle(Optional<Boolean> hasAtmosphere) {
        Lifecycle lifecycle = hasAtmosphere.isPresent() ? LEGACY_ATMOSPHERE_LIFECYCLE : Lifecycle.stable();
        return DataResult.success(hasAtmosphere, lifecycle);
    }

    public static ColorRGBA defaultAtmosphereColor(Temperature temperature) {
        return switch (temperature) {
            case FREEZING -> new ColorRGBA(0x6699E6);
            case COLD -> new ColorRGBA(0x80B3E6);
            case MILD -> new ColorRGBA(0x99CCFF);
            case HOT -> new ColorRGBA(0xE6804D);
            case SCORCHED -> new ColorRGBA(0xFF4D1A);
        };
    }

    /// === 流编解码器 ===

    public static final StreamCodec<RegistryFriendlyByteBuf, SpecialCelestialBodyRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SpecialCelestialBodyRecipe decode(RegistryFriendlyByteBuf buf) {
            String name = buf.readUtf();
            int time = buf.readInt();
            int space = buf.readInt();
            int mass = buf.readInt();
            int energy = buf.readInt();
            String model = buf.readUtf();
            Optional<ColorRGBA> atmosphere = ByteBufCodecs.optional(ATMOSPHERE_COLOR_STREAM).decode(buf);
            Optional<LiquidCoverage> liquidCoverage = ByteBufCodecs.optional(LIQUID_COVERAGE_STREAM).decode(buf);
            int magneticFieldStrength = buf.readInt();
            int rotationSpeed = buf.readInt();
            float axialTilt = buf.readFloat();
            List<ResourceLocation> seedItems = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            boolean needsCustomModel = buf.readBoolean();
            boolean canBeShattered = buf.readBoolean();
            List<WeightedEntry> minerals = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<WeightedEntry> fluids = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<WeightedEntry> biologicalItems = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<WeightedEntry> biologicalFluids = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<WeightedEntry> offerings = WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<DemandEntry> templeBlessings = DemandEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<DemandEntry> templePunishments = DemandEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            Optional<CelestialTravelData> landing = ByteBufCodecs.optional(CelestialTravelData.STREAM_CODEC).decode(buf);
            return new SpecialCelestialBodyRecipe(
                name, model, needsCustomModel, canBeShattered,
                time, space, mass, energy,
                atmosphere, liquidCoverage,
                magneticFieldStrength, rotationSpeed, axialTilt,
                seedItems,
                minerals, fluids, biologicalItems, biologicalFluids,
                offerings, templeBlessings, templePunishments,
                landing
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, SpecialCelestialBodyRecipe r) {
            buf.writeUtf(r.name());
            buf.writeInt(r.time());
            buf.writeInt(r.space());
            buf.writeInt(r.mass());
            buf.writeInt(r.energy());
            buf.writeUtf(r.model());
            ByteBufCodecs.optional(ATMOSPHERE_COLOR_STREAM).encode(buf, r.atmosphere());
            ByteBufCodecs.optional(LIQUID_COVERAGE_STREAM).encode(buf, r.liquidCoverage());
            buf.writeInt(r.magneticFieldStrength());
            buf.writeInt(r.rotationSpeed());
            buf.writeFloat(r.axialTilt());
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.seedItems());
            buf.writeBoolean(r.needsCustomModel());
            buf.writeBoolean(r.canBeShattered());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.minerals());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.fluids());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.biologicalItems());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.biologicalFluids());
            WeightedEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.offerings());
            DemandEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.templeBlessings());
            DemandEntry.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.templePunishments());
            ByteBufCodecs.optional(CelestialTravelData.STREAM_CODEC).encode(buf, r.landing());
        }
    };

    /// === 派生属性 ===

    public boolean hasAtmosphere() {
        return atmosphere.isPresent();
    }

    /// 温度由 energy 砧子数自动推导。
    @NotNull
    public Temperature temperature() {
        return CelestialBodyMatcher.energyToTemperature(energy);
    }

    /// 只有内置的 {@code ERROR_PLANET} 是错误行星。
    public boolean isErrorPlanet() {
        return name.equals("error_planet");
    }

    /// 此天体是否有文明 —— 定义 {@code offerings} 时为 {@code true}。
    public boolean hasCivilization() {
        return !offerings.isEmpty();
    }

    /// === 核心方法 ===

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
        for (WeightedEntry entry : biologicalFluids) {
            r.addBiologicalFluid(new PlanetaryResourceSet.WeightedFluidStack(entry.resourceId(), entry.weight()));
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

    /// === 配方实现 ===

    @Override
    public boolean matches(@NotNull SpecialCelestialBodyInput input, @NotNull Level level) {
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SpecialCelestialBodyInput input, HolderLookup.@NotNull Provider registries) {
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
        return ModRecipeTypes.SPECIAL_CELESTIAL_BODY_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipeTypes.SPECIAL_CELESTIAL_BODY_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    private static Item resolveItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    public static final class Serializer implements RecipeSerializer<SpecialCelestialBodyRecipe> {
        @Override
        public @NotNull MapCodec<SpecialCelestialBodyRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, SpecialCelestialBodyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
