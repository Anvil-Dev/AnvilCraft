package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;

public class AnvilCraftConfig {
    @SerializedName("anvil_efficiency")
    public int anvilEfficiency = 64;
    @SerializedName("lightning_strike_depth")
    public int lightningStrikeDepth = 2;
}
