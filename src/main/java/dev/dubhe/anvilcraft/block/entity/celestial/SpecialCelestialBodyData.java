package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ColorRGBA;

import javax.annotation.Nullable;

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

    /// 此天体是否为玩家头颅动态天体。
    public boolean hasAtmosphere() {
        return atmosphereColor != null;
    }

    public boolean isPlayerHead() {
        return playerHeadProfile != null;
    }

    /// 此已发现天体是否有数据驱动的着陆目标。
    public boolean isLandable() {
        return landing != null;
    }

    @Nullable
    public CelestialTravelData landingData() {
        return landing;
    }

    @Nullable
    public CelestialTravelData travelData() {
        return landing;
    }

    /** 兼容使用旧 {@code travel} 术语的调用方。 */
    @Nullable
    public CelestialTravelData travel() {
        return landing;
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
    public ResourceLocation getModelLocation() {
        if (model.indexOf(':') >= 0) {
            return ResourceLocation.parse(model);
        }
        return AnvilCraft.of("block/celestial_body/" + model);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", CelestialBodyType.SPECIAL.getSerializedName());
        tag.putString("recipeId", recipeId);
        tag.putString("name", name);
        tag.putInt("size", size);
        tag.putFloat("axialTilt", axialTilt);
        tag.putInt("rotationSpeed", rotationSpeed);
        tag.putInt("magneticFieldStrength", magneticFieldStrength);
        tag.putBoolean("isErrorPlanet", isErrorPlanet);
        tag.putBoolean("needsCustomModel", needsCustomModel);
        tag.putBoolean("canBeShattered", canBeShattered);
        tag.putString("model", model);
        if (temperature != null) {
            tag.putString("temperature", temperature.getSerializedName());
        }
        if (atmosphereColor != null) {
            tag.putInt("atmosphereColor", atmosphereColor.rgba());
        }
        if (liquidCoverage != null) {
            tag.putString("liquidCoverage", liquidCoverage.getSerializedName());
        }
        if (playerHeadProfile != null) {
            tag.put("playerHeadProfile", playerHeadProfile);
        }
        if (landing != null) {
            tag.put("landing", landing.toTag());
        }
        return tag;
    }

    /// 从NBT反序列化SpecialCelestialBodyData。
    public static SpecialCelestialBodyData fromTag(CompoundTag tag) {
        String recipeId = tag.getString("recipeId");
        String name = tag.getString("name");
        int size = tag.getInt("size");
        float axialTilt = tag.getFloat("axialTilt");
        int rotationSpeed = tag.getInt("rotationSpeed");
        int magneticFieldStrength = tag.getInt("magneticFieldStrength");
        boolean isErrorPlanet = tag.getBoolean("isErrorPlanet");
        boolean needsCustomModel = tag.getBoolean("needsCustomModel");
        /// 向后兼容：优先读取新键 {@code model}，回退到旧键 {@code textureName}。
        String model = tag.contains("model")
            ? tag.getString("model")
            : tag.getString("textureName");
        Temperature temperature = tag.contains("temperature")
            ? Temperature.fromName(tag.getString("temperature")) : null;
        @Nullable ColorRGBA atmosphereColor = tag.contains("atmosphereColor")
            ? new ColorRGBA(tag.getInt("atmosphereColor"))
            : legacyAtmosphereColor(temperature, tag.getBoolean("hasAtmosphere"));
        LiquidCoverage liquidCoverage = tag.contains("liquidCoverage")
            ? LiquidCoverage.fromName(tag.getString("liquidCoverage")) : null;
        CompoundTag playerHeadProfile = tag.contains("playerHeadProfile")
            ? tag.getCompound("playerHeadProfile") : null;
        String landingKey = tag.contains("landing") ? "landing" : "travel";
        CelestialTravelData landing = tag.contains(landingKey)
            ? CelestialTravelData.fromTag(tag.getCompound(landingKey)) : null;
        boolean canBeShattered = tag.contains("canBeShattered")
            ? tag.getBoolean("canBeShattered") : legacyCanBeShattered(landing);
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
        return landing != null && CelestialTravelManager.OVERWORLD_LIKE_DIMENSION.equals(landing.dimension());
    }
}
