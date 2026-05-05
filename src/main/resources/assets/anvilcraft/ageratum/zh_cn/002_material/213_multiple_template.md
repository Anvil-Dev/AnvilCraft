---
navigation:
  title: "§6多合一锻造模板"
  icon: "anvilcraft:four_to_one_smithing_template"
  parent: anvilcraft_guideme:material.md
items:
  - anvilcraft:two_to_one_smithing_template
  - anvilcraft:four_to_one_smithing_template
  - anvilcraft:eight_to_one_smithing_template
---

# 多合一锻造模板

<row halign="center">
<item id="anvilcraft:two_to_one_smithing_template"/>
<item id="anvilcraft:four_to_one_smithing_template"/>
<item id="anvilcraft:eight_to_one_smithing_template"/>
</row>

用于在[余烬锻造台](../005_block/221_ember_smithing_table.md)合成装备

# 合成

- 使用对应数量的任意不同锻造模板，在[<translate key="block.anvilcraft.stamping_platform"/>](../006_struct/000_item_processing.md)砸合而成

> 包括升级装备用的锻造模板和盔甲纹饰的锻造模板皆可，多合一锻造模板也算一种锻造模板
>
> eg:下界合金锻造模板+皇家钢锻造模板→二合一锻造模板
>
> eg:猪鼻锻造模板+肋骨锻造模板+余烬锻造模板+海岸锻造模板→四合一锻造模板

# 模板解离

1. 将有附魔的[<translate key="item.anvilcraft.eight_to_one_smithing_template"/>](213_multiple_template.md)物品实体用任意方式摧毁
2. 随机取一条附魔(选择有可能重复)，按如下列表生成新物品
3. 最多选择4次(即最多通过这种方式产生4个新模板)

<row halign="center" valign="center">
<item id="minecraft:snout_armor_trim_smithing_template"/>
->  灵魂疾行  ;
<item id="minecraft:rib_armor_trim_smithing_template"/>
->  火焰保护、火焰附加、火矢  ;
<item id="minecraft:dune_armor_trim_smithing_template"/>
->  爆炸保护  ;
<item id="minecraft:silence_armor_trim_smithing_template"/>
->  迅捷潜行
</row>

<row halign="center" valign="center">
<item id="minecraft:ward_armor_trim_smithing_template"/>
->  保护  ;
<item id="minecraft:vex_armor_trim_smithing_template"/>
->  经验修补  ;
<item id="minecraft:sentry_armor_trim_smithing_template"/>
->  无限  ;
<item id="minecraft:bolt_armor_trim_smithing_template"/>
->  致密、破甲  ;
<item id="minecraft:flow_armor_trim_smithing_template"/>
->  风暴
</row>

<row halign="center" valign="center">
<item id="minecraft:wild_armor_trim_smithing_template"/>
->  弹射物保护  ;
<item id="minecraft:spire_armor_trim_smithing_template"/>
->  时运  ;
<item id="minecraft:eye_armor_trim_smithing_template"/>
->  抢夺  ;
<item id="minecraft:coast_armor_trim_smithing_template"/>
->  海之眷顾、饵钓
</row>

<row halign="center" valign="center">
<item id="minecraft:tide_armor_trim_smithing_template"/>
->  深海探索者、水下呼吸、水下速掘、穿刺、激流
</row>
<row halign="center" valign="center">
<item id="minecraft:host_armor_trim_smithing_template"/>
<item id="minecraft:wayfinder_armor_trim_smithing_template"/>
<item id="minecraft:raiser_armor_trim_smithing_template"/>
<item id="minecraft:shaper_armor_trim_smithing_template"/>
->  其他附魔
</row>

<info>
如果选择了4次，且4次的结果（指生成的新模板）都不同，则额外生成一个超限[<translate key="item.anvilcraft.transcendium_upgrade_smithing_template"/>](311_transcendium_template.md)
</info>