# AnvilCraft 多版本发布

从默认分支 `dev/1.21/1.6` 运行 **Actions → Create Multi-Version Releases → Run workflow**，一次发布所有受管开发分支。总控自动上传 Modrinth、CurseForge、Maven 和 GitHub ReleaseFile，无需选择平台。两站失败不重试且不阻塞发布；Maven 或 ReleaseFile 失败会使整体运行失败。

在必填的 `tag` 输入框填写发布版本，例如 `1.6.0+pre-release.13`，不加 `v` 前缀。总控通过 `-Pmod_version=<tag>` 覆盖各分支的 Gradle 构建版本，不要求预先修改或同步 `gradle.properties`。生成的 Git tag、JAR 和 Maven 版本均使用输入值；上述预发布不升 patch，纯 `X.Y.Z` 发布后创建下一 patch 的 PR。普通开发 CI 仍上架 Alpha 快照。

版本后缀、自动日志、预检、演练与续跑说明见[默认分支发布文档](https://github.com/Anvil-Dev/AnvilCraft/blob/dev/1.21/1.6/RELEASING.md)。日志的上一 tag 按提交祖先关系选择，预发布可接正式版或预发布，正式版只接正式版。

本分支继续提供自己的 Gradle Wrapper、JDK 和构建配置。Gradle 的 Cjsah Maven 依赖仓库地址为 `https://server.cjsah.net:1002/maven/releases/`；内部 Maven 上传继续读取机密 `MAVEN_URL`。
