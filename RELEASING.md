# AnvilCraft 双分支发布

在默认分支 `dev/1.21/1.6` 打开 **Actions → Create Multi-Version Releases → Run workflow**。流程锁定 `dev/1.21/1.6` 和 `dev/26.1/1.6` 的完整 SHA，要求两条分支的根 `mod_version` 完全一致。先合入 `dev/26.1/1.6` 的构建适配，再合入默认分支总控。预检会拒绝尚未完成适配的分支。

勾选 `dry_run` 时只构建和校验，不创建 tag、发布文件或升版 PR。两条分支分别使用自己的 Gradle Wrapper、JDK 和依赖版本运行一次 `stageRelease`。校验包括主包、源码包、NeoForge 描述文件、内嵌 AnvilLib 和 Ageratum 的实际字节、Maven 坐标、POM 的 `api` 依赖范围及 Gradle 模块元数据。

`generate_release_notes` 默认关闭。开启后调用 GitHub 的 [Generate release notes API](https://docs.github.com/en/rest/releases/releases#generate-release-notes-content-for-a-release)，以锁定的默认分支 SHA 和显式指定的上一 tag 生成 PR、贡献者和变更链接。正文保留 GitHub 原始内容，附上两份构件的源码链接和 SHA-256。内容保存在 `source-plan` 构件和 tag 注释中，重跑复用原文，`dry_run` 摘要也会展示它。

上一 tag 从已公开的 Release 中按发布时间由近到远选择，必须位于锁定提交的祖先历史中，允许继承自旧分支的 tag。Pre-release 可以接上一 Release 或 Pre-release，Release 只接上一 Release。草稿、当前 tag 和其他分支的后继或分叉提交均跳过。选择结果和原生说明一起冻结，重跑沿用。仓库首次发布使用 GitHub 原生初次发布说明；已有发布但找不到合格祖先时，摘要说明原因并跳过增量日志，避免 GitHub 自动选错范围。

版本后缀使用 `+`，`N` 为无前导零的非负整数。

| mod_version | GitHub 类型 | Modrinth / CurseForge 类型 | 发布后升 patch |
| --- | --- | --- | --- |
| `X.Y.Z+alpha.N` | Pre-release | Alpha | 否 |
| `X.Y.Z+beta.N` | Pre-release | Beta | 否 |
| `X.Y.Z+pre-release.N` | Pre-release | Beta | 否 |
| `X.Y.Z+rc.N` | Pre-release | Beta | 否 |
| `X.Y.Z+build.N` | Release | Release | 否 |
| `X.Y.Z+hotfix.N` | Release | Release | 否 |
| `X.Y.Z` | Release | Release | 是 |

例如两条分支都设为 `1.6.0+rc.1` 时，tag 为 `v1.6.0+rc.1`，JAR 和 Maven 版本保留完整后缀。下一次发布前需要先将两条分支推进到同一版本。普通开发推送继续构建 `+snapshot.N` 并上架 Beta，PR 构建使用 `+pr.N`，沿用现有服务端启动测试。

带注解 tag 指向锁定的 `dev/1.21/1.6` 提交，注释包含两个源码 SHA、运行编号、平台项目 ID 和文件校验值。GitHub 草稿只附两个主 JAR，文件名使用实际游戏版本，例如 `anvilcraft-neoforge-1.21.1-1.6.0.jar` 和 `anvilcraft-neoforge-26.1.2-1.6.0.jar`。

总控自动运行 Modrinth、CurseForge、Maven 和 ReleaseFile 上传，不提供平台选择。Maven 或 ReleaseFile 失败会使整体运行失败并保留草稿；两站独立运行，失败只记录警告和回执，公开 Release 与升版任务不等待两站。

| 渠道 | 完成条件 | 失败处理 |
| --- | --- | --- |
| 内部 Maven | 以现有 `dev.dubhe` 坐标上传两份已校验的发布文件，读取远端逐一核验 | 保留 GitHub 草稿，重跑原运行继续上传；远端同名文件不同则报告冲突 |
| GitHub | 两个主包校验通过，两个 Maven 成功回执齐全 | 重跑原运行续传缺失附件或重试公开草稿 |
| Modrinth、CurseForge | 各自接收主包和源码附件 | 四项独立任务记录结果，失败不重试；失败或仍在上传均不阻塞 GitHub 公开与升版 |

纯 `X.Y.Z` 的 Release 公开后，分别从两条开发分支的最新提交创建下一 patch 的 PR，仅修改根 `mod_version`。PR 标题和唯一提交标题都带 `[skip ci]`；维护者按正常审核流程 squash 合并时保留默认标题，即可跳过该次开发构建。重跑复用固定升版分支和已有 PR，已升至目标版本则完成，其他版本报告冲突。

## 续跑

正式流程中断时，在原运行使用 **Re-run failed jobs**。构建复用原运行的 Actions 构件，Maven 上传先比对远端文件，GitHub 附件和 tag 必须与原始清单一致。单独重跑升版任务也会复用原 PR。发布与演练共用仓库级并发锁。

两站上传各执行一次。原运行重跑时复用成功回执，已有失败或未决记录不会再次上传。Actions 构件过期后，文件仍按原始校验值从 GitHub 和内部 Maven 读取。

进度记录在锁定源码提交的状态中，名称形如 `release/v1.6.0/26.1/maven`、`release/v1.6.0/26.1/modrinth`、`release/v1.6.0/26.1/curseforge/jar` 和 `release/v1.6.0/26.1/curseforge/sources`。每条状态链接到对应 Actions 运行，摘要列出平台、游戏版本、回执或失败原因。

Modrinth 上传结果不明时，自动按主包 SHA-512 查询远端并核对版本类型、文件和游戏版本。CurseForge 为主包和源码包分别保留文件 ID；上传超时而未得到 ID 时，必须先在作者后台核对项目、文件名和哈希。确认已接收后使用 `success` 和文件 ID 补录状态。原运行重跑时只核对远端或复用回执，不重传失败文件。

```sh
gh api --method POST "repos/Anvil-Dev/AnvilCraft/statuses/$SOURCE_SHA" -f context="$CONTEXT" -f state="$STATE" -f description="$RECEIPT"
```

## 凭据

沿用仓库的 `MAVEN_URL`、`MAVEN_USER`、`MAVEN_PASS`、`MODRINTH_TOKEN` 和 `CURSEFORGE_TOKEN`。内部 Maven 地址需使用 HTTPS，账号需能读取并上传原有发布路径。两站任务仅在 Actions 构件过期时读取 Maven 附件。仓库需允许 Actions 创建 PR，`GITHUB_TOKEN` 按任务授权写入 tag、Release、提交状态和升版分支。

Gradle 的 Cjsah Maven 依赖仓库统一使用 `https://server.cjsah.net:1002/maven/releases/`；内部 Maven 上传继续读取机密 `MAVEN_URL`，两者分别配置。
