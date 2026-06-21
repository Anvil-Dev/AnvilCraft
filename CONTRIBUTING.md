# Contribution Guide | [贡献指南](./CONTRIBUTING.cn.md)

## Configure your `IDEA`

1. Import Java code style configuration (must)
    - [HERE TO VIEW](https://gist.github.com/Gu-ZT/c3dfd97991daea73d1a5316e0974778f)
    - `Settings` -> `Editor` -> `Code Style` -> `Java` -> `Scheme` -> `Import Scheme`
2. Import color scheme (optional)
    - [HERE TO VIEW](https://gist.github.com/Gu-ZT/2410fd75cf9b5da09d0b77a57c1caaf7)
    - `Settings` -> `Editor` -> `Color Scheme` -> `Import Scheme`
3. Add `CheckStyle-IDEA` plugin (optional)
    - [HERE TO VIEW](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea)
    - `Plugins` -> `Marketplace`
    - Search for `CheckStyle-IDEA` plugin, made by `Jamie Shiell` and apply it
    - `Settings` -> `Tools` -> `CheckStyle` -> `Configuration File` -> `+`/`Add` -> Choose the `style.xml` file in the root folder of this project
    - `Settings` -> `Tools` -> `CheckStyle` -> `Configuration File` -> Uncheck other entries except the entry added before
4. Add `Minecraft Development` plugin (recommended)
    - [HERE TO VIEW](https://plugins.jetbrains.com/plugin/8327-minecraft-development)
    - `Plugins` -> `Marketplace` -> Search for `Minecraft Development` and apply it

## Development environment setup

1. Install JDK 21
    - [JetBrains Runtime 21](https://github.com/JetBrains/JetBrainsRuntime/releases) is recommended
2. Clone the repository
    - `git clone https://github.com/Anvil-Dev/AnvilCraft.git`
3. Open the project in IDEA
    - `File` -> `Open` -> Select the project root directory
    - IDEA will automatically detect the Gradle project and import dependencies
4. Build the project
    - Run `./gradlew build` (Linux/macOS) or `gradlew.bat build` (Windows) in the terminal
    - Or use IDEA: `Gradle` panel -> `Tasks` -> `build` -> `build`

## The use of various annotations

1. In all packages, place the `package-info.java` file and add the following annotations to the package.
    - `com.mojang.logging.annotations.MethodsReturnNonnullByDefault`
    - `javax.annotation.ParametersAreNonnullByDefault`
    - `com.mojang.logging.annotations.FieldsAreNonnullByDefault`
2. For parameters/fields/return values that need to be indicated as `null`, use `javax.annotation.Nullable`.

## Submit specifications

1. When committing, please use the format `git commit -m "commit message"`.
2. If the `commit` is for resolving or fixing an `issue`, please include `#issue number` in the `commit message`.
3. We do not restrict the language used in the `commit message`, but please include at least a simple English
   description and place it before descriptions in other languages.
4. If you add new classes or public methods in the `dev.dubhe.anvilcraft.api` package, please provide complete `Javadoc`
   documentation for them. If you modify any public or non-public methods in this package, ensure their binary
   compatibility.

## Pull requests

1. `Pull Requests` should be submitted to the `dev/1.21/1.6` branch.
2. The title of a `Pull Request` should include at least a simple English description and place it before titles in
   other languages.
3. You should provide a detailed explanation of your changes in the `Pull Request` description.
4. Use `fixed` or `resolved` in the `Pull Request` description to link the corresponding `issue`.
5. Below is a simple `Pull Request` example:
   * ```markdown
     ### Fix drop of mob amber blocks 修复生物琥珀块挖掘掉落物问题
     - fixed #1533
     - When mining mob amber block now, the data containing mob will be correctly saved.
     - Fixed some errors in data and language files.
     - 修复了 #1533 的问题。
     - 现在挖掘含生物琥珀块时，会正确地保存其包含生物的数据。
     - 修复了部分数据与语言文件的错误。
     ```
   * > ### Fix drop of mob amber blocks 修复生物琥珀块挖掘掉落物问题
     > - fixed #1533
     > - When mining mob amber block now, the data containing mob will be correctly saved.
     > - Fixed some errors in data and language files.
     > - 修复了 #1533 的问题。
     > - 现在挖掘含生物琥珀块时，会正确地保存其包含生物的数据。
     > - 修复了部分数据与语言文件的错误。

## CI/CD Information

1. Code style check
    - Each time a `Pull Request` is submitted, GitHub Actions will automatically check code style using the Checkstyle
      rules in `style.xml`
    - Check results will be displayed as inline comments in the `Pull Request`
2. Build and test
    - Build is automatically triggered when a `Pull Request` is submitted to `releases/**` or `dev/**` branches
    - The build includes compilation and GameTest testing
    - Build artifacts can be downloaded from GitHub Actions Artifacts
