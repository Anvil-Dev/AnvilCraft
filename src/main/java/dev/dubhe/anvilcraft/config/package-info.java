/// 存储了铁砧工艺的所有配置类
///
/// 对于其内所有配置项，有以下风格要求（不分先后）：
/// - 对于配置项本身，注解顺序为
///   1. [`@SerializedName`][com.google.gson.annotations.SerializedName]（可选，用于自定义名称，名称会自动生成）
///   2. [`@Comment`][dev.anvilcraft.lib.v2.config.Comment]（**必需**，用于定义描述和工具提示）
///   3. [`@BoundedDiscrete`][dev.anvilcraft.lib.v2.config.BoundedDiscrete]（可选，用于定义数值取值范围）
/// - 描述中的专有名词必须**大写**，如 `Grass Block`、`Giant Anvil`、`Grass`、`Handheld Magnet`、`Overworld`、`Mun`等
/// - 已有的游戏元素必须使用其在**游戏内/代码里**的名称，如 `撼地` 必须使用 `Shock` 称呼
/// - 在同个目录中同元素的不同配置在代码中必须放置在一起，如
/// `worlds.giantAnvilMultiblockMinSize` 和 `worlds.giantAnvilMultiblockMaxSize` 必须放置在一起
/// - 不同配置项之间必须空一行
/// - 使用 `@BoundedDiscrete` 注解时必须将 `min` 写在 `max` 前面
/// - 描述不允许使用句号，需要写第二句时以换行代替
/// - 描述中所有的换行以多行文本块实现，不要手写 `\n`，也不要文本拼接
/// - `Min/Max` 在名称中使用 `Min/Max`，在描述中使用 `Minimum/Maximum`
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
package dev.dubhe.anvilcraft.config;

import com.mojang.logging.annotations.FieldsAreNonnullByDefault;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
