# 贡献指南 | [Contribution Guide](./CONTRIBUTING.md)

## 配置你的 `IDEA`

1. 导入 Java 代码样式配置（必须）
    - [此处查看](https://gist.github.com/Gu-ZT/c3dfd97991daea73d1a5316e0974778f)
    - `设置` -> `编辑器` -> `代码样式` -> `Java` -> `方案` -> `导入方案`
2. 导入配色方案（可选）
    - [此处查看](https://gist.github.com/Gu-ZT/2410fd75cf9b5da09d0b77a57c1caaf7)
    - `设置` -> `编辑器` -> `配色方案` -> `导入方案`
3. 添加 `CheckStyle-IDEA` 插件（可选）
    - [此处查看](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea)
    - `插件` -> `Marketplace`
    - 搜索由 `Jamie Shiell` 制作的 `CheckStyle-IDEA` 插件并应用
    - `设置` -> `工具` -> `CheckStyle` -> `Configuration File` -> `+`/`添加` -> 选择项目根目录下的 `style.xml` 文件
    - `设置` -> `工具` -> `CheckStyle` -> `Configuration File` -> 将除刚添加的条目外的其它条目取消勾选
4. 添加 `Minecraft Development` 插件（推荐）
    - [此处查看](https://plugins.jetbrains.com/plugin/8327-minecraft-development)
    - `插件` -> `Marketplace` -> 搜索 `Minecraft Development` 并应用

## 开发环境搭建

1. 安装 JDK 21
    - 推荐使用 [JetBrains Runtime 21](https://github.com/JetBrains/JetBrainsRuntime/releases)
2. 克隆仓库
    - `git clone https://github.com/Anvil-Dev/AnvilCraft.git`
3. 使用 IDEA 打开项目
    - `文件` -> `打开` -> 选择项目根目录
    - IDEA 会自动识别 Gradle 项目并导入依赖
4. 构建项目
    - 在终端中运行 `./gradlew build`（Linux/macOS）或 `gradlew.bat build`（Windows）
    - 或在 IDEA 中使用 `Gradle` 面板 -> `Tasks` -> `build` -> `build`

## 各类注解的使用

1. 在所有软件包下放置 `package-info.java` 文件，并在其中为软件包添加以下注解
    - `com.mojang.logging.annotations.MethodsReturnNonnullByDefault`
    - `javax.annotation.ParametersAreNonnullByDefault`
    - `com.mojang.logging.annotations.FieldsAreNonnullByDefault`
2. 对于需要表示为 `null` 的`参数`/`字段`/`返回值`等，请使用 `javax.annotation.Nullable`

## 提交规范

1. 提交时，请使用 `git commit -m "commit message"` 的格式
2. 如果该 `commit` 是对于 `issue` 的解决或修复，请在 `commit message` 中填写 `#issue number`
3. 我们不限制在 `commit message` 中使用的语言，但请至少包含一个英文的简单描述，并将其放置在其它语言的描述之前
4. 如果你在 `dev.dubhe.anvilcraft.api` 软件包中添加了新的类和公开方法，请为他们添加完善的 `Javadoc`，
   如果你修改了此软件包内的任何公开或非公开的方法，请保证它们的二进制兼容性

## Pull requests

1. `Pull requests` 应提交至 `dev/1.21/1.6` 分支
2. `Pull requests` 标题应至少包含一个英文的简单描述，并将其放置在其它语言标题之前
3. 你应在 `Pull requests` 的描述中详细解释你所做的更改
4. 你应在 `Pull requests` 的描述中使用 `fixed` 或 `resolved` 链接对应的 `issue`
5. 以下是一个简单的 `Pull requests` 示例
    * ```markdown
      ### Fix drop of mob amber blocks 修复生物琥珀块挖掘掉落物问题
      - 修复了 #1533 的问题。
      - 现在挖掘含生物琥珀块时，会正确地保存其包含生物的数据。
      - 修复了部分数据与语言文件的错误。
      - fixed #1533
      ```
    * > ### Fix drop of mob amber blocks 修复生物琥珀块挖掘掉落物问题
      > - 修复了 #1533 的问题。
      > - 现在挖掘含生物琥珀块时，会正确地保存其包含生物的数据。
      > - 修复了部分数据与语言文件的错误。
      > - fixed #1533

## CI/CD 信息

1. 代码风格检查
    - 每次提交 `Pull Request` 时，GitHub Actions 会自动使用 `style.xml` 中的 Checkstyle 规则检查代码风格
    - 检查结果将以行内评论的方式显示在 `Pull Request` 中
2. 构建与测试
    - 每次提交 `Pull Request` 至 `releases/**` 或 `dev/**` 分支时，会自动触发构建
    - 构建包括编译和 GameTest 测试
    - 构建产物可在 GitHub Actions 的 Artifacts 中下载
