package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;

public class AnvilCraftConfig {
    @SerializedName("// 铁砧同时处理的最大物品数量")
    public final String anvilEfficiencyNote = "";
    @SerializedName("anvil_efficiency")
    public int anvilEfficiency = 64;
    @SerializedName("// 雷击能到达的最大深度")
    public final String lightningStrikeDepthNote = "";
    @SerializedName("lightning_strike_depth")
    public int lightningStrikeDepth = 2;
    @SerializedName("// 磁铁吸引的最大距离")
    public final String magnetAttractsDistanceNote = "";
    @SerializedName("magnet_attracts_distance")
    public int magnetAttractsDistance = 4;
    @SerializedName("// 手持磁铁吸引的最大半径")
    public final String magnetItemAttractsRadiusNote = "";
    @SerializedName("magnet_attracts_distance")
    public double magnetItemAttractsRadius = 8;
}
