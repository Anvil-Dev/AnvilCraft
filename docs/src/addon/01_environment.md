## 一、准备工作

1. 安装 `IntelliJ IDEA`
2. 安装 `IntelliJ IDEA` 的插件
    * [Chinese ​(Simplified)​ Language Pack / 中文语言包](https://plugins.jetbrains.com/plugin/13710-chinese-simplified-language-pack----)
    * [Minecraft Development](https://plugins.jetbrains.com/plugin/8327-minecraft-development)
    * [Architectury](https://plugins.jetbrains.com/plugin/16210-architectury)

## 二、从附属模板创建仓库

1. 打开 [附属模板仓库](https://github.com/Gu-ZT/AnvilCraftMod-Addon-Example) ，单击右上角的 `Use this template`
   ，选择 `Create a new repository` ;
2. 在新打开的页面输入附属模组的相关信息，例如名称和简介，我们推荐按照 `AnvilCraft-${附属模组名称}`
   的格式命名，例如 `AnvilCraft-Demo` ；
3. 将仓库克隆至本地；
    * 这一步骤请自行搜索

## 三、构建环境

1. 使用 `IntelliJ IDEA` 打开克隆到本地的仓库目录
2. 打开 `gradle.properties` ，修改为你模组的信息
    * `anvilcraft_version`
        * 铁砧工艺的版本号
        * 可以前往 [此处](https://server.cjsah.net:1002/maven/dev/dubhe/anvilcraft-common-1.20.1/maven-metadata.xml)
          查看最新版本号
    * `maven_group`
        * Maven 分组
        * 通常为主软件包名
        * 推荐使用 `dev.anvilcraft.${附属模组ID}`
        * 例如：`dev.anvilcraft.demo`
    * `mod_id`
        * 模组ID
        * 建议使用 `anvilcraft_附属模组ID`
        * 例如：`anvilcraft_demo`
    * `mod_name`
        * 模组名称
        * 建议使用 `AnvilCraft-${附属模组名称}`
        * 例如：`AnvilCraft-Demo`
    * `mod_description`
        * 模组介绍
    * `mod_license`
        * 模组开源协议
        * 如果直接包含铁砧工艺的源码或修改后的铁砧工艺源码，则此项不能修改
    * `mod_version`
        * 模组版本号
    * `mod_url`
        * 模组网页链接
        * 例如：https://github.com/Gu-ZT/AnvilCraft-Demo
3. 修改以下路径内的软件包名为你自己的包名
    * `common/src/main/java`
    * `forge/src/main/java`
    * `fabric/src/main/java`
    * 例如：`dev.anvilcraft.demo`
4. 修改 `ExampleMod.java` 内的 `MOD_ID` 为你自己的 mod id
    * 例如：`anvilcraft_demo`
5. 修改 `ExampleMod.java` 为你自己的 MOD 类名
    * 例如：`AnvilCraftDemo.java`
6. 修改 `ExampleModFabric.java` 为你自己的 MOD 类名
    * 例如：`AnvilCraftDemoFabric.java`
7. 修改 `ExampleModForge.java` 为你自己的 MOD 类名
    * 例如：`AnvilCraftDemoForge.java`
8. 修改 `ExampleModForge.java` 为你自己的 MOD 类名
    * 例如：`AnvilCraftDemoForge.java`
9. 修改 `ExampleModDatagen.java` 为你自己的 MOD 类名
    * 例如：`AnvilCraftDemoDatagen.java`
10. 修改 `common/src/main/resources/anvilcraft_addon_example.accesswidener` 为你自己的 MOD accesswidener 名称
    * 例如：`fabric/src/main/resources/anvilcraft_demo.accesswidener`
11. 修改 `common/src/main/resources/anvilcraft_addon_example-common.mixins.json` 为你自己的 MOD mixins.json 名称
    * 例如：`fabric/src/main/resources/anvilcraft_demo-common.mixins.json`
12. 修改 `fabric/src/main/resources/anvilcraft_addon_example.mixins.json` 为你自己的 MOD mixins.json 名称
    * 例如：`fabric/src/main/resources/anvilcraft_demo.mixins.json`
13. 修改 `forge/src/main/resources/anvilcraft_addon_example.mixins.json` 为你自己的 MOD mixins.json 名称
    * 例如：`forge/src/main/resources/anvilcraft_demo.mixins.json`
14. 修改 `fabric/src/main/resources/fabric.mod.json`
15. 修改 `common/src/main/resources/assets/anvilcraft_addon_example` 路径名为你自己的 assets 命名空间
    * 例如：`common/src/main/resources/assets/anvilcraft_demo`
16. 重载 Gradle 脚本
17. 运行 `Tasks -> loom -> dataCopy` 任务
18. 运行 `Tasks -> loom -> genSources` 任务
19. 重载 Gradle 脚本
20. 至此开发环境的准备工作已全部就绪
