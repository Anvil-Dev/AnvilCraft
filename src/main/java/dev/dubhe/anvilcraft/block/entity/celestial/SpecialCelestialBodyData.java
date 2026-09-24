package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ColorRGBA;
import org.jspecify.annotations.Nullable;

/// 特殊天体数据 —— 从 {@link SpecialCelestialBodyRecipe} 创建，
/// 绕过常规三步图表匹配和贴图烘焙管线。
/// 所有属性在创建时从配方缓存，渲染和 NBT 反序列化时无需查配方管理器。
public record SpecialCelestialBodyData(
    String recipeId,
    String name,
    int size,
    float axialTilt,
    int rotationSpeed,
    int magneticFieldStrength,
    @Nullable Temperature temperature,
    @Nullable ColorRGBA atmosphereColor,
    @Nullable LiquidCoverage liquidCoverage,
    boolean isErrorPlanet,
    boolean needsCustomModel,
    boolean canBeShattered,
    String model,
    @Nullable CompoundTag playerHeadProfile,
    @Nullable CelestialTravelData landing
) implements CelestialBodyData {

    /// {@code model} 取此值时天体没有烘焙贴图，表面直接复用末地折跃门那套跟随玩家视角的虚空效果。
    public static final String END_GATEWAY_MODEL = "end_gateway";

    /** 兼容着陆规则存在前写入的存档数据。 */
    public SpecialCelestialBodyData(
        String recipeId,
        String name,
        int size,
        float axialTilt,
        int rotationSpeed,
        int magneticFieldStrength,
        @Nullable Temperature temperature,
        boolean hasAtmosphere,
        @Nullable LiquidCoverage liquidCoverage,
        boolean isErrorPlanet,
        boolean needsCustomModel,
        String model,
        @Nullable CompoundTag playerHeadProfile
    ) {
        this(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength, temperature,
            legacyAtmosphereColor(temperature, hasAtmosphere), liquidCoverage,
            isErrorPlanet, needsCustomModel, false, model, playerHeadProfile, null
        );
    }

    /** 兼容可粉碎性写入存档前创建的存档数据。 */
    public SpecialCelestialBodyData(
        String recipeId,
        String name,
        int size,
        float axialTilt,
        int rotationSpeed,
        int magneticFieldStrength,
        @Nullable Temperature temperature,
        boolean hasAtmosphere,
        @Nullable LiquidCoverage liquidCoverage,
        boolean isErrorPlanet,
        boolean needsCustomModel,
        String model,
        @Nullable CompoundTag playerHeadProfile,
        @Nullable CelestialTravelData landing
    ) {
        this(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength, temperature,
            legacyAtmosphereColor(temperature, hasAtmosphere), liquidCoverage,
            isErrorPlanet, needsCustomModel, legacyCanBeShattered(landing), model,
            playerHeadProfile, landing
        );
    }

    /** 兼容大气层颜色写入存档前创建的存档数据。 */
    public SpecialCelestialBodyData(
        String recipeId,
        String name,
        int size,
        float axialTilt,
        int rotationSpeed,
        int magneticFieldStrength,
        @Nullable Temperature temperature,
        boolean hasAtmosphere,
        @Nullable LiquidCoverage liquidCoverage,
        boolean isErrorPlanet,
        boolean needsCustomModel,
        boolean canBeShattered,
        String model,
        @Nullable CompoundTag playerHeadProfile,
        @Nullable CelestialTravelData landing
    ) {
        this(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength, temperature,
            legacyAtmosphereColor(temperature, hasAtmosphere), liquidCoverage,
            isErrorPlanet, needsCustomModel, canBeShattered, model, playerHeadProfile, landing
        );
    }

    /// 从配方及其资源路径 ID 创建。
    public static SpecialCelestialBodyData fromRecipe(SpecialCelestialBodyRecipe recipe, String recipeId) {
        return new SpecialCelestialBodyData(
            recipeId,
            recipe.name(),
            recipe.space(),
            recipe.axialTilt(),
            recipe.rotationSpeed(),
            recipe.magneticFieldStrength(),
            recipe.temperature(),
            recipe.atmosphere().orElse(null),
            recipe.getLiquidCoverage(),
            recipe.isErrorPlanet(),
            recipe.needsCustomModel(),
            recipe.canBeShattered(),
            recipe.model(),
            null,
            recipe.landing().orElse(null)
        );
    }

    /// 从玩家头颅的档案 NBT 创建动态天体（无资源，使用头颅模型渲染）。
    /// 天体大小由空间砧子数量决定。
    public static SpecialCelestialBodyData fromPlayerHead(CompoundTag profileNbt, int space) {
        return new SpecialCelestialBodyData(
            "player_head",
            "player_head",
            space,
            0f,
            2,
            0,
            Temperature.MILD,
            false,
            LiquidCoverage.NONE,
            false,
            true,
            false,
            "player_head",
            profileNbt,
            null
        );
    }

    public boolean hasAtmosphere() {
        return this.atmosphereColor != null;
    }

    public boolean isPlayerHead() {
        return this.playerHeadProfile != null;
    }

    /// 此天体是否用末地折跃门效果渲染，而不是烘焙贴图或独立模型。
    public boolean usesEndGatewayModel() {
        return !this.needsCustomModel && END_GATEWAY_MODEL.equals(this.model);
    }

    /// 此已发现天体是否有数据驱动的着陆目标。
    public boolean isLandable() {
        return this.landing != null;
    }

    @Nullable
    public CelestialTravelData landingData() {
        return this.landing;
    }

    @Nullable
    public CelestialTravelData travelData() {
        return this.landing;
    }

    /** 兼容使用旧 {@code travel} 术语的调用方。 */
    @Nullable
    public CelestialTravelData travel() {
        return this.landing;
    }

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.SPECIAL;
    }

    @Override
    public CelestialBodyClass bodyClass() {
        return CelestialBodyClass.LARGE_MOON;
    }

    @Override
    public RingType ringType() {
        return RingType.NONE;
    }

    /// 获取此特殊天体的独立模型/贴图资源路径。
    /// 无命名空间的旧格式仍指向 AnvilCraft 的天体模型目录；完整资源 ID 则直接使用。
    public Identifier getModelLocation() {
        if (this.model.indexOf(':') >= 0) {
            return Identifier.parse(this.model);
        }
        return AnvilCraft.of("block/celestial_body/" + this.model);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", CelestialBodyType.SPECIAL.getSerializedName());
        tag.putString("recipeId", this.recipeId);
        tag.putString("name", this.name);
        tag.putInt("size", this.size);
        tag.putFloat("axialTilt", this.axialTilt);
        tag.putInt("rotationSpeed", this.rotationSpeed);
        tag.putInt("magneticFieldStrength", this.magneticFieldStrength);
        tag.putBoolean("isErrorPlanet", this.isErrorPlanet);
        tag.putBoolean("needsCustomModel", this.needsCustomModel);
        tag.putBoolean("canBeShattered", this.canBeShattered);
        tag.putString("model", this.model);
        if (this.temperature != null) {
            tag.putString("temperature", this.temperature.getSerializedName());
        }
        if (this.atmosphereColor != null) {
            tag.putInt("atmosphereColor", this.atmosphereColor.rgba());
        }
        if (this.liquidCoverage != null) {
            tag.putString("liquidCoverage", this.liquidCoverage.getSerializedName());
        }
        if (this.playerHeadProfile != null) {
            tag.put("playerHeadProfile", this.playerHeadProfile);
        }
        if (this.landing != null) {
            tag.put("landing", this.landing.toTag());
        }
        return tag;
    }

    /// 从NBT反序列化SpecialCelestialBodyData。
    public static SpecialCelestialBodyData fromTag(CompoundTag tag) {
        String recipeId = tag.getStringOr("recipeId", "");
        String name = tag.getStringOr("name", "");
        int size = tag.getIntOr("size", 0);
        float axialTilt = tag.getFloatOr("axialTilt", 0f);
        int rotationSpeed = tag.getIntOr("rotationSpeed", 0);
        int magneticFieldStrength = tag.getIntOr("magneticFieldStrength", 0);
        boolean isErrorPlanet = tag.getBooleanOr("isErrorPlanet", false);
        boolean needsCustomModel = tag.getBooleanOr("needsCustomModel", false);
        /// 向后兼容：优先读取新键 {@code model}，回退到旧键 {@code textureName}。
        String model = tag.contains("model")
            ? tag.getStringOr("model", "")
            : tag.getStringOr("textureName", "");
        Temperature temperature = tag.contains("temperature")
            ? Temperature.fromName(tag.getStringOr("temperature", "")) : null;
        @Nullable ColorRGBA atmosphereColor = tag.contains("atmosphereColor")
            ? new ColorRGBA(tag.getIntOr("atmosphereColor", 0))
            : legacyAtmosphereColor(temperature, tag.getBooleanOr("hasAtmosphere", false));
        LiquidCoverage liquidCoverage = tag.contains("liquidCoverage")
            ? LiquidCoverage.fromName(tag.getStringOr("liquidCoverage", "")) : null;
        CompoundTag playerHeadProfile = tag.contains("playerHeadProfile")
            ? tag.getCompoundOrEmpty("playerHeadProfile") : null;
        String landingKey = tag.contains("landing") ? "landing" : "travel";
        CelestialTravelData landing = tag.contains(landingKey)
            ? CelestialTravelData.fromTag(tag.getCompoundOrEmpty(landingKey)) : null;
        boolean canBeShattered = tag.contains("canBeShattered")
            ? tag.getBooleanOr("canBeShattered", false) : legacyCanBeShattered(landing);
        return new SpecialCelestialBodyData(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength,
            temperature, atmosphereColor, liquidCoverage,
            isErrorPlanet, needsCustomModel, canBeShattered, model, playerHeadProfile, landing
        );
    }

    @Nullable
    private static ColorRGBA legacyAtmosphereColor(@Nullable Temperature temperature, boolean hasAtmosphere) {
        if (!hasAtmosphere) return null;
        if (temperature == null) return new ColorRGBA(0xFFFFFF);
        return SpecialCelestialBodyRecipe.defaultAtmosphereColor(temperature);
    }

    private static boolean legacyCanBeShattered(@Nullable CelestialTravelData landing) {
        return landing != null && CelestialTravelData.OVERWORLD_LIKE_DIMENSION.equals(landing.dimension());
    }
}
