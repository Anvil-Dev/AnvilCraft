# Java 与 Gradle 项目规范

本规范仅适用于 Java 与 Gradle 项目。修改 Java 源码、空安全注解或生成资源前先阅读本文全文。

## 代码风格与静态检查

以项目根目录的 `style.xml` 作为 Java 代码风格与 Checkstyle 要求的唯一标准；禁止添加 Checkstyle 抑制规则绕过违规，必须直接修复代码或在请求范围内记录确需破例的理由。

## 空安全注解约束

空安全注解包选用优先级：**JSpecify（`org.jspecify.annotations`） > Javax / JSR-305（`javax.annotation`） > JetBrains（`org.jetbrains.annotations`）**。若项目依赖中包含 JSpecify，必须优先使用 JSpecify 的 `@NullMarked`、`@Nullable` 等注解，禁止在同一模块中混用多套空安全注解。

当包（`package-info.java`）或类已启用默认全局非空（优先使用 JSpecify 的 `@NullMarked`；未引入 JSpecify 的存量项目兼容 `@MethodsReturnNonnullByDefault`、`@ParametersAreNonnullByDefault`、`@FieldsAreNonnullByDefault`）时，作用域内的所有返回值、参数、字段及泛型类型参数默认均为非空，禁止添加冗余的 `@NonNull` / `@Nonnull` / `@NotNull` 注解。

当返回值、参数或字段可能为 `null` 时，必须按上述优先级显式添加对应的 `@Nullable` 注解；解引用任何 `@Nullable` 对象前必须进行非空检查，防止触发 `NullPointerException`。

## 构建与数据生成验证

在 Windows 环境下修改 Java 源码后，执行 `./gradlew.bat compileJava --console=plain` 验证编译。当修改涉及生成数据或静态资源时，执行 `./gradlew.bat runData --console=plain` 并检查生成的 diff。
