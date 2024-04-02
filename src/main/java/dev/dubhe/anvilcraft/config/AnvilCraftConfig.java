package dev.dubhe.anvilcraft.config;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
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
    public int magnetAttractsDistance = 5;
    @SerializedName("// 手持磁铁吸引的最大半径")
    public final String magnetItemAttractsRadiusNote = "";
    @SerializedName("magnet_item_attracts_radius")
    public double magnetItemAttractsRadius = 8;
    @SerializedName("// 铁砧每掉落一格产生的红石EMP距离")
    public final String redstoneEmpRadiusNote = "";
    @SerializedName("redstone_emp_radius")
    public int redstoneEmpRadius = 6;
    @SerializedName("// 红石EMP的最大距离")
    public final String redstoneEmpMaxRadiusNote = "";
    @SerializedName("redstone_emp_max_radius")
    public int redstoneEmpMaxRadius = 24;
    @SerializedName("// 溜槽的最大冷却时间（单位: tick）")
    public final String chuteMaxCooldownNote = "";
    @SerializedName("chute_max_cooldown")
    public int chuteMaxCooldown = 4;
}
