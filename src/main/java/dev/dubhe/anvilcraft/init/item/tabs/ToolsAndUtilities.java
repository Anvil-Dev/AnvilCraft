package dev.dubhe.anvilcraft.init.item.tabs;

import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.item.enchantment.Enchantments;

public class ToolsAndUtilities extends DisplayItemsGenerator {
    @Override
    public void accept() {
        this.plain(ModItems.GUIDE_BOOK); // 铁砧工艺指南
        this.plain(ModItems.MAGNET); // 手持磁铁
        this.plain(ModItems.GEODE); // 晶洞
        this.enchanting(ModItems.AMETHYST_PICKAXE, Enchantments.FORTUNE, 3); // 紫水晶镐
        this.enchanting(ModItems.AMETHYST_AXE, ModEnchantments.FELLING_KEY, 1); // 紫水晶斧
        this.enchanting(ModItems.AMETHYST_HOE, ModEnchantments.HARVEST_KEY, 1); // 紫水晶锄
        this.enchanting(ModItems.AMETHYST_SWORD, ModEnchantments.BEHEADING_KEY, 1); // 紫水晶剑
        this.enchanting(ModItems.AMETHYST_SHOVEL, Enchantments.EFFICIENCY, 3); // 紫水晶锹
        this.plain(ModItems.ROYAL_STEEL_PICKAXE); // 皇家钢镐
        this.plain(ModItems.ROYAL_STEEL_AXE); // 皇家钢斧
        this.plain(ModItems.ROYAL_STEEL_SHOVEL); // 皇家钢锹
        this.plain(ModItems.ROYAL_STEEL_HOE); // 皇家钢锄
        this.plain(ModItems.ROYAL_STEEL_SWORD); // 皇家钢剑
        this.plain(ModItems.FROST_METAL_PICKAXE); // 浮霜金属镐
        this.plain(ModItems.FROST_METAL_AXE); // 浮霜金属斧
        this.plain(ModItems.FROST_METAL_SHOVEL); // 浮霜金属锹
        this.plain(ModItems.FROST_METAL_HOE); // 浮霜金属锄
        this.plain(ModItems.FROST_METAL_SWORD); // 浮霜金属剑
        this.plain(ModItems.EMBER_METAL_PICKAXE); // 余烬金属镐
        this.plain(ModItems.EMBER_METAL_AXE); // 余烬金属斧
        this.plain(ModItems.EMBER_METAL_SHOVEL); // 余烬金属锹
        this.plain(ModItems.EMBER_METAL_HOE); // 余烬金属锄
        this.plain(ModItems.EMBER_METAL_SWORD); // 余烬金属剑
        this.plain(ModItems.ANVIL_HAMMER); // 铁砧锤
        this.plain(ModItems.ROYAL_ANVIL_HAMMER); // 皇家铁砧锤
        this.plain(ModItems.EMBER_ANVIL_HAMMER); // 余烬铁砧锤
        this.plain(ModItems.TRANSCENDENCE_ANVIL_HAMMER); // 超限铁砧锤
        this.plain(ModItems.DRAGON_ROD); // 龙杖
        this.plain(ModItems.ROYAL_DRAGON_ROD); // 皇家龙杖
        this.plain(ModItems.EMBER_DRAGON_ROD); // 余烬龙杖
        this.plain(ModItems.TRANSCENDENCE_DRAGON_ROD); // 超限龙杖
        this.plain(ModItems.FROST_METAL_HEAVY_HALBERD); // 浮霜金属重戟
        this.plain(ModItems.EMBER_METAL_HEAVY_HALBERD); // 余烬金属重戟
        this.plain(ModItems.TRANSCENDENCE_HEAVY_HALBERD); // 超限重戟
        this.plain(ModItems.FROST_METAL_RESONATOR); // 浮霜金属共振器
        this.plain(ModItems.EMBER_METAL_RESONATOR); // 余烬金属共振器
        this.plain(ModItems.TRANSCENDENCE_RESONATOR); // 超限共振器
        this.plain(ModItems.MULTITOOL_ITEM); // 多用途工具
        this.plain(ModItems.SPECTRAL_SLINGSHOT); // 幻灵弹弓
        this.plain(ModItems.ENERGY_WEAPON_PLATFORM); // 能量武器平台
        this.energy(ModItems.SPECTRAL_WEAPON_LAUNCHER); // 幻灵武器发射器
        this.energy(ModItems.ANVIL_RAILGUN); // 铁砧轨道炮
        this.energy(ModItems.CORRUPTED_BEACON_ACTIVATOR); // 腐化信标激发器
        this.energy(ModItems.TESLA_GUN); // 特斯拉枪
        this.energy(ModItems.LASER_GUN); // 激光枪
        this.plain(ModItems.IONOCRAFT); // 飘升机
        this.ionoCraftBackpack(ModItems.IONOCRAFT_BACKPACK); // 飘升机背包
        this.plain(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE); // 锻造模板（皇家钢升级）
        this.plain(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE); // 锻造模板（浮霜金属升级）
        this.plain(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE); // 锻造模板（余烬金属升级）
        this.plain(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE); // 锻造模板（超限合金升级）
        this.plain(ModItems.PERMUTATION_TEMPLATE_ITEM); // 嬗变锻造模板
        this.plain(ModItems.DEFORMATION_TEMPLATE_ITEM); // 形变锻造模板
        this.plain(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE); // 二合一锻造模板
        this.plain(ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE); // 四合一锻造模板
        this.plain(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE); // 八合一锻造模板
        this.plain(ModItems.DISK); // 磁盘
        this.plain(ModItems.STRUCTURE_DISK); // 结构磁盘
        this.plain(ModItems.FILTER); // 过滤器
        this.plain(ModItems.CRAB_CLAW); // 蟹钳
        this.plain(ModItems.AMULET_BOX); // 护符盒
        this.plain(ModItems.TOTEM_OF_RECOVERY); // 追溯图腾
        this.plain(ModItems.TOTEM_OF_RAGE); // 狂暴图腾
        this.plain(ModItems.EMERALD_AMULET); // 绿宝石护符
        this.plain(ModItems.TOPAZ_AMULET); // 黄玉护符
        this.plain(ModItems.RUBY_AMULET); // 红宝石护符
        this.plain(ModItems.SAPPHIRE_AMULET); // 蓝宝石护符
        this.plain(ModItems.ANVIL_AMULET); // 铁砧护符
        this.plain(ModItems.COMRADE_AMULET); // 战友护符
        this.plain(ModItems.FEATHER_AMULET); // 羽毛护符
        this.plain(ModItems.CAT_AMULET); // 猫护符
        this.plain(ModItems.DOG_AMULET); // 狗护符
        this.plain(ModItems.SILENCE_AMULET); // 寂静护符
        this.plain(ModItems.ABNORMAL_AMULET); // 异常护符
        this.plain(ModItems.GEM_AMULET); // 宝石护符
        this.plain(ModItems.NATURE_AMULET); // 自然护符
        this.plain(ModItems.CAPACITOR); // 电容器
        this.plain(ModItems.CAPACITOR_EMPTY); // 电容器 (空)
        this.plain(ModItems.SUPER_CAPACITOR); // 超级电容器
        this.plain(ModItems.SUPER_CAPACITOR_EMPTY); // 超级电容器 (空)
        this.plain(ModItems.TIN_CAN); // 锡罐
        this.plain(ModItems.RECOVERY_PEARL); // 追溯珍珠
        this.plain(ModItems.SEEDS_PACK); // 种子包
        this.plain(ModItems.STRUCTURE_TOOL); // 结构工具
        this.plain(ModItems.PILL_BOX); // 药盒
        this.plain(ModFoodItems.CREAM); // 奶油
        this.plain(ModFoodItems.FLOUR); // 面粉
        this.plain(ModFoodItems.DOUGH); // 面团
        this.plain(ModFoodItems.COCOA_LIQUOR); // 可可液块
        this.plain(ModFoodItems.COCOA_BUTTER); // 可可脂
        this.plain(ModFoodItems.COCOA_POWDER); // 可可粉
        this.plain(ModFoodItems.CHOCOLATE); // 巧克力
        this.plain(ModFoodItems.CHOCOLATE_BLACK); // 黑巧克力
        this.plain(ModFoodItems.CHOCOLATE_WHITE); // 白巧克力
        this.plain(ModFoodItems.CREAMY_BREAD_ROLL); // 奶油面包卷
        this.plain(ModFoodItems.BEEF_MUSHROOM_STEW); // 牛肉炖蘑菇
        this.plain(ModFoodItems.UTUSAN); // 五毒散
        this.plain(ModFoodItems.CANNED_FOOD); // 罐头食品
        this.plain(ModFoodItems.PILL); // 药片
    }
}
