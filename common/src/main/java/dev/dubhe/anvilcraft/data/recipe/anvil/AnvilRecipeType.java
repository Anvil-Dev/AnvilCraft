package dev.dubhe.anvilcraft.data.recipe.anvil;

public enum AnvilRecipeType {
    NULL("null"),
    STAMPING("stamping"),
    SIEVING("sieving"),
    BULGING("bulging"),
    BULGING_LIKE("bulging_like"),
    FLUID_HANDLING("fluid_handling"),
    CRYSTALLIZE("crystallize"),
    COMPACTION("compaction"),
    COMPRESS("compress"),
    COOKING("cooking"),
    BOIL("boil"),
    ITEM_INJECT("item_inject"),
    BLOCK_SMASH("block_smash"),
    ITEM_SMASH("item_smash"),
    SQUEEZE("squeeze"),
    SUPER_HEATING("super_heating"),
    TIMEWARP("timewarp");
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
        return AnvilRecipeType.NULL;
    }
}
