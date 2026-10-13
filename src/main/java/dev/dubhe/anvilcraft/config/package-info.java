/// 存储了铁砧工艺的所有配置类
///
/// 对于其内所有配置项，有以下风格要求：
///
/// **注解**
/// - 注解顺序固定为 [`@SerializedName`][com.google.gson.annotations.SerializedName]（可选，用于自定义名称，
///   缺省时由字段名自动生成）、[`@Comment`][dev.anvilcraft.lib.v2.config.Comment]（**必需**，用于定义描述和工具提示）、
///   [`@BoundedDiscrete`][dev.anvilcraft.lib.v2.config.BoundedDiscrete]（可选，用于定义数值取值范围）
/// - 使用 `@BoundedDiscrete` 时必须将 `min` 写在 `max` 前面
///
/// **排版**
/// - 不同配置项之间必须空一行
/// - 同一分区内同元素的不同配置必须相邻放置，如 `world.giantAnvilMultiblockMinSize` 和
///   `world.giantAnvilMultiblockMaxSize` 必须放置在一起
///
/// **用词**
/// - 已有的游戏元素必须使用其在**游戏内/代码里**的名称，如 `撼地` 必须写作 `Shock`
/// - 专有名词必须**首字母大写**，如 `Grass Block`、`Giant Anvil`、`Grass`、`Handheld Magnet`、`Overworld`、`Mun` 等
/// - 缩写必须**全大写**，如 `EXP`、`EMP`、`FE` 等；自动生成的名称不合要求时用 `@SerializedName` 覆盖
/// - `Min/Max` 在名称中使用 `Min/Max`，在描述中使用 `Minimum/Maximum`
///
/// **描述**
/// - 描述必须以 `Controls` 开头：布尔类型后接 `… whether …`，其余类型后接 `… the …`
///   - 使用 [`@CollapsibleObject`][dev.anvilcraft.lib.v2.config.CollapsibleObject] 的折叠项，
///     其描述必须以 `Adjusts settings related to …` 开头
/// - 描述中不允许使用句号，需要写第二句时以换行代替
/// - 描述中所有的换行以多行文本块实现，不允许手写 `\n` 或文本拼接
/// - 配置项取值为时长、距离、容量等**存在多种单位而易混淆**的度量时，必须在描述末尾标明单位，
///   格式为 `(in [units of <number>] <plural form of unit>[/<per unit>])`，如 `(in ticks)`、`(in blocks)`、`(in mB/tick)`
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
package dev.dubhe.anvilcraft.config;

import com.mojang.logging.annotations.FieldsAreNonnullByDefault;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
