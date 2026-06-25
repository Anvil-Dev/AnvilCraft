package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/// 特殊天体数据 —— 从 {@link SpecialCelestialBodyRecipe} 创建，
/// 绕过常规三步图表匹配和贴图烘焙管线。
///
/// 所有属性在创建时从配方缓存，渲染和 NBT 反序列化时无需查配方管理器。
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public record SpecialCelestialBodyData(
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
    String model
) implements CelestialBodyData {

    /// 从配方和其资源路径ID创建。
    public static SpecialCelestialBodyData fromRecipe(SpecialCelestialBodyRecipe recipe, String recipeId) {
        return new SpecialCelestialBodyData(
            recipeId,
            recipe.name(),
            recipe.space(),
            recipe.axialTilt(),
            recipe.rotationSpeed(),
            recipe.magneticFieldStrength(),
            recipe.temperature(),
            recipe.hasAtmosphere(),
            recipe.getLiquidCoverage(),
            recipe.isErrorPlanet(),
            recipe.needsCustomModel(),
            recipe.model()
        );
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

    /// 获取此特殊天体的独立模型/贴图资源路径
    public ModelResourceLocation getModelLocation() {
        return ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/" + model));
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
        tag.putBoolean("hasAtmosphere", hasAtmosphere);
        tag.putBoolean("isErrorPlanet", isErrorPlanet);
        tag.putBoolean("needsCustomModel", needsCustomModel);
        tag.putString("model", model);
        if (temperature != null) {
            tag.putString("temperature", temperature.getSerializedName());
        }
        if (liquidCoverage != null) {
            tag.putString("liquidCoverage", liquidCoverage.getSerializedName());
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
        boolean hasAtmosphere = tag.getBoolean("hasAtmosphere");
        boolean isErrorPlanet = tag.getBoolean("isErrorPlanet");
        boolean needsCustomModel = tag.getBoolean("needsCustomModel");
        /// 向后兼容：优先读新 key "model"，回退到旧 key "textureName"
        String model = tag.contains("model")
            ? tag.getString("model")
            : tag.getString("textureName");
        Temperature temperature = tag.contains("temperature")
            ? Temperature.fromName(tag.getString("temperature")) : null;
        LiquidCoverage liquidCoverage = tag.contains("liquidCoverage")
            ? LiquidCoverage.fromName(tag.getString("liquidCoverage")) : null;
        return new SpecialCelestialBodyData(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength,
            temperature, hasAtmosphere, liquidCoverage,
            isErrorPlanet, needsCustomModel, model
        );
    }
}
