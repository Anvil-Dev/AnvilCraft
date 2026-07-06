package dev.dubhe.anvilcraft.init.item.tabs;

import dev.dubhe.anvilcraft.init.item.ModItems;

public class Ingredients extends DisplayItemsGenerator {
    @Override
    public void accept() {
        this.plain(ModItems.MAGNET_INGOT); // 磁铁锭
        this.plain(ModItems.SPONGE_GEMMULE); // 海绵芽球
        this.plain(ModItems.ROYAL_STEEL_INGOT); // 皇家钢锭
        this.plain(ModItems.ROYAL_STEEL_NUGGET); // 皇家钢粒
        this.plain(ModItems.FROST_METAL_INGOT); // 浮霜金属锭
        this.plain(ModItems.FROST_METAL_NUGGET); // 浮霜金属粒
        this.plain(ModItems.EMBER_METAL_INGOT); // 余烬金属锭
        this.plain(ModItems.EMBER_METAL_NUGGET); // 余烬金属粒
        this.plain(ModItems.TRANSCENDIUM_INGOT); // 超限合金锭
        this.plain(ModItems.TRANSCENDIUM_NUGGET); // 超限合金粒
        this.plain(ModItems.CURSED_GOLD_INGOT); // 诅咒金锭
        this.plain(ModItems.CURSED_GOLD_NUGGET); // 诅咒金粒
        this.plain(ModItems.TOPAZ); // 黄玉
        this.plain(ModItems.RUBY); // 红宝石
        this.plain(ModItems.SAPPHIRE); // 蓝宝石
        this.plain(ModItems.EXP_GEM); // 经验宝石
        this.plain(ModItems.RESIN); // 树脂
        this.plain(ModItems.AMBER); // 琥珀
        this.plain(ModItems.HARDEND_RESIN); // 硬化树脂
        this.plain(ModItems.WOOD_FIBER); // 木质纤维
        this.plain(ModItems.CIRCUIT_BOARD); // 电路板
        this.plain(ModItems.PROCESSOR); // 处理器
        this.plain(ModItems.TUNGSTEN_NUGGET); // 钨粒
        this.plain(ModItems.TUNGSTEN_INGOT); // 钨锭
        this.plain(ModItems.TITANIUM_NUGGET); // 钛粒
        this.plain(ModItems.TITANIUM_INGOT); // 钛锭
        this.plain(ModItems.ZINC_NUGGET); // 锌粒
        this.plain(ModItems.ZINC_INGOT); // 锌锭
        this.plain(ModItems.TIN_NUGGET); // 锡粒
        this.plain(ModItems.TIN_INGOT); // 锡锭
        this.plain(ModItems.LEAD_NUGGET); // 铅粒
        this.plain(ModItems.LEAD_INGOT); // 铅锭
        this.plain(ModItems.SILVER_NUGGET); // 银粒
        this.plain(ModItems.SILVER_INGOT); // 银锭
        this.plain(ModItems.URANIUM_NUGGET); // 铀粒
        this.plain(ModItems.URANIUM_INGOT); // 铀锭
        this.plain(ModItems.PLUTONIUM_NUGGET); // 钚粒
        this.plain(ModItems.PLUTONIUM_INGOT); // 钚锭
        this.plain(ModItems.COPPER_NUGGET); // 铜粒
        this.plain(ModItems.BRONZE_INGOT); // 青铜锭
        this.plain(ModItems.BRONZE_NUGGET); // 青铜粒
        this.plain(ModItems.BRASS_INGOT); // 黄铜锭
        this.plain(ModItems.BRASS_NUGGET); // 黄铜粒
        this.plain(ModItems.LIME_POWDER); // 石灰粉
        this.plain(ModItems.LEVITATION_POWDER); // 飘浮粉
        this.plain(ModItems.RAW_ZINC); // 粗锌
        this.plain(ModItems.RAW_TIN); // 粗锡
        this.plain(ModItems.RAW_TITANIUM); // 粗钛
        this.plain(ModItems.RAW_TUNGSTEN); // 粗钨
        this.plain(ModItems.RAW_LEAD); // 粗铅
        this.plain(ModItems.RAW_SILVER); // 粗银
        this.plain(ModItems.RAW_URANIUM); // 粗铀
        this.plain(ModItems.VOID_MATTER); // 虚空物质
        this.plain(ModItems.EXCITED_STATE_VOID_MATTER); // 激发态虚空物质
        this.plain(ModItems.EARTH_CORE_SHARD); // 地核碎片
        this.plain(ModItems.MULTIPHASE_MATTER); // 多相物质
        this.plain(ModItems.HEAVY_HALBERD_CORE); // 重戟核心
        this.plain(ModItems.RESONATOR_CORE); // 共振器核心
        this.plain(ModItems.MULTIPHASE_TRANSCENDIUM); // 多相超限合金
        this.plain(ModItems.NEGATIVE_MATTER); // 负物质
        this.plain(ModItems.NEGATIVE_MATTER_NUGGET); // 负物质粒
        this.plain(ModItems.DYSON_SPHERE_COMPONENT); // 戴森球组件
        this.plain(ModItems.PENROSE_SPHERE_COMPONENT); // 彭罗斯球组件
        this.plain(ModItems.MATTER_DECOMPRESSOR_COMPONENT); // 物质解压器组件
        this.plain(ModItems.WORMHOLE_STABILIZER_COMPONENT); // 虫洞稳定器组件
        this.plain(ModItems.NEUTRONIUM_INGOT); // 中子锭
        this.plain(ModItems.STABLE_NEUTRONIUM_INGOT); // 稳态中子锭
        this.plain(ModItems.CHARGED_NEUTRONIUM_INGOT); // 充能中子锭
        this.plain(ModItems.EXP_BUCKET); // 液态经验桶
        this.plain(ModItems.OIL_BUCKET); // 原油桶
        this.plain(ModItems.MELT_GEM_BUCKET); // 熔融宝石桶
        ModItems.CEMENT_BUCKETS.forEach((color, bucketItem) -> this.plain(bucketItem)); //水泥桶
    }
}
