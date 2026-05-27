---
navigation:
  title: "磁铁"
  icon: "anvilcraft:magnet_ingot"
items:
  - anvilcraft:magnet_ingot
  - anvilcraft:magnet_block
  - anvilcraft:hollow_magnet_block
  - anvilcraft:ferrite_core_magnet_block
---

# 磁铁
<row halign="center">
<item id="anvilcraft:magnet_ingot"/>
<item id="anvilcraft:magnet_block"/>
<item id="anvilcraft:hollow_magnet_block"/>
<item id="anvilcraft:ferrite_core_magnet_block"/>
</row>

# 首次获取
需要使用<ref item="minecraft:lightning_rod"/>吸引雷电，
将<ref item="minecraft:iron_block"/>雷击转化为<ref item="anvilcraft:hollow_magnet_block"/>


<tip>
使用<ref item="anvilcraft:topaz"/>右键<ref item="minecraft:lightning_rod"/>，会消耗<ref item="anvilcraft:topaz"/>并立刻制造一道闪电
</tip>

默认的雷击转化磁铁块范围如下

<structure id="../structures/lightning_convert_magnets.snbt"/>

<row halign="center">
<recipe id="anvilcraft:magnet_ingot_from_hollow_block"/>
<recipe id="anvilcraft:hollow_magnet_block"/>
</row>

# 磁化
- 使用<ref item="minecraft:iron_ingot"/>右键<ref item="anvilcraft:hollow_magnet_block"/>，或者合成，获得<ref item="anvilcraft:ferrite_core_magnet_block"/>
- 将<ref item="anvilcraft:ferrite_core_magnet_block"/>放置在世界中，其会慢慢转变为<ref item="anvilcraft:magnet_block"/>，将<ref item="minecraft:iron_ingot"/>转化为<ref item="anvilcraft:magnet_ingot"/>
- 可以合成，或是空手右键取出<ref item="anvilcraft:magnet_ingot"/>

<row halign="center">
<recipe id="anvilcraft:ferrite_core_magnet_block"/>
<recipe id="anvilcraft:magnet_ingot_from_block"/>
<recipe id="anvilcraft:magnet_block"/>
</row>

# 磁效应
- <ref item="anvilcraft:magnet_block"/>、<ref item="anvilcraft:ferrite_core_magnet_block"/>、<ref item="anvilcraft:hollow_magnet_block"/>可以吸引正下方5格内的各种铁砧
- 收到红石信号消磁 
- 反复激活，可以将铁砧抬起再砸下

# 电磁效应
具有发电能力后，可利用<ref item="anvilcraft:charger"/>，
将<ref item="minecraft:iron_ingot"/>充能为<ref item="anvilcraft:magnet_ingot"/>

<recipe id="anvilcraft:charger_charging/magnet_ingot"/>