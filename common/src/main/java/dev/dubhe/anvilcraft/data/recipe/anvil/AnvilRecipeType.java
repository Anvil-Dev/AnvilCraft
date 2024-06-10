package dev.dubhe.anvilcraft.data.recipe.anvil;

public enum AnvilRecipeType {
    GENERIC("generic"), // 通用
    STAMPING("stamping"), // 冲压
    SIEVING("sieving"), // 过筛
    BULGING("bulging"), // 膨发
    BULGING_LIKE("bulging_like"), // 类膨发
    FLUID_HANDLING("fluid_handling"), // 流体处理
    CRYSTALLIZE("crystallize"), // 晶化
    COMPACTION("compaction"), // 压实
    COMPRESS("compress"), // 压缩
    COOKING("cooking"), // 煎炒
    BOIL("boil"), // 炖煮
    ITEM_INJECT("item_inject"), // 物品注入
    BLOCK_SMASH("block_smash"), // 方块粉碎
    ITEM_SMASH("item_smash"), // 物品粉碎
    SQUEEZE("squeeze"), // 挤压
    SUPER_HEATING("super_heating"), // 超级加热
    TIMEWARP("timewarp"); // 时移
    private final String type;

    AnvilRecipeType(String type) {
        this.type = type;
    }

    public String toString() {
        return this.type;
    }

    /**
     * 从name获取{@link AnvilRecipeType}
     */
    public static AnvilRecipeType of(String name) {
        for (AnvilRecipeType type : AnvilRecipeType.values()) if (name.equals(type.toString())) return type;
        return AnvilRecipeType.GENERIC;
    }
}
