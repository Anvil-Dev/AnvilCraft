package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * 通过种子物品发现的隐藏特殊天体数据。
 * 此类天体绕过普通三步星图匹配和动态贴图烘焙，直接使用固定模型或贴图。
 * 创建时会缓存 {@link SpecialCelestialBodyRecipe} 中的全部属性，渲染和 NBT 反序列化无需再查询配方。
 */
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
    String textureName,
    @Nullable CompoundTag playerHeadProfile
) implements CelestialBodyData {

    /** 根据配方及其资源标识创建特殊天体数据。 */
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
            recipe.textureName(),
            null
        );
    }

    /**
     * 根据玩家档案 NBT 创建动态玩家头颅天体，直接使用头颅模型渲染，大小由空间砧子数量决定。
     */
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
            "player_head",
            profileNbt
        );
    }

    /** 是否为动态玩家头颅天体。 */
    public boolean isPlayerHead() {
        return this.playerHeadProfile != null;
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

    /**
     * 获取特殊天体的独立模型或贴图资源标识。
     * 无命名空间的旧格式仍指向 AnvilCraft 的天体模型目录，完整资源标识则直接使用。
     */
    public Identifier getModelLocation() {
        if (this.textureName.indexOf(':') >= 0) {
            return Identifier.parse(this.textureName);
        }
        return AnvilCraft.of("block/celestial_body/" + this.textureName);
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
        tag.putBoolean("hasAtmosphere", this.hasAtmosphere);
        tag.putBoolean("isErrorPlanet", this.isErrorPlanet);
        tag.putBoolean("needsCustomModel", this.needsCustomModel);
        tag.putString("textureName", this.textureName);
        if (this.temperature != null) {
            tag.putString("temperature", this.temperature.getSerializedName());
        }
        if (this.liquidCoverage != null) {
            tag.putString("liquidCoverage", this.liquidCoverage.getSerializedName());
        }
        if (this.playerHeadProfile != null) {
            tag.put("playerHeadProfile", this.playerHeadProfile);
        }
        return tag;
    }

    /** 从 NBT 反序列化特殊天体数据。 */
    public static SpecialCelestialBodyData fromTag(CompoundTag tag) {
        String recipeId = tag.getStringOr("recipeId", "");
        String name = tag.getStringOr("name", "");
        int size = tag.getIntOr("size", 0);
        float axialTilt = tag.getFloatOr("axialTilt", 0f);
        int rotationSpeed = tag.getIntOr("rotationSpeed", 0);
        int magneticFieldStrength = tag.getIntOr("magneticFieldStrength", 0);
        boolean hasAtmosphere = tag.getBooleanOr("hasAtmosphere", false);
        boolean isErrorPlanet = tag.getBooleanOr("isErrorPlanet", false);
        boolean needsCustomModel = tag.getBooleanOr("needsCustomModel", false);
        String textureName = tag.getStringOr("textureName", "");
        String tempStr = tag.getStringOr("temperature", "");
        Temperature temperature = !tempStr.isEmpty() ? Temperature.fromName(tempStr) : null;
        String lcStr = tag.getStringOr("liquidCoverage", "");
        LiquidCoverage liquidCoverage = !lcStr.isEmpty() ? LiquidCoverage.fromName(lcStr) : null;
        CompoundTag playerHeadProfile = tag.contains("playerHeadProfile")
            ? tag.getCompoundOrEmpty("playerHeadProfile") : null;
        return new SpecialCelestialBodyData(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength,
            temperature, hasAtmosphere, liquidCoverage,
            isErrorPlanet, needsCustomModel, textureName, playerHeadProfile
        );
    }
}
