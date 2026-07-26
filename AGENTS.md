# Repository Agent Instructions

## Source Lookup

- Use the IntelliJ IDEA MCP tools together with the `workspace-agent-bridge` skill as the primary way to search,
  inspect, and navigate project and library source code.
- If IDEA source lookup fails for dependency code, search source JARs under `~/.gradle/caches/`.
- Core Unix utilities are available when command-line inspection is useful.

## Scope and Existing Work

- Make the smallest change that fully satisfies the request.
- Follow nearby project patterns and existing APIs before introducing new abstractions.
- Preserve unrelated working-tree changes. Do not revert, clean up, or reformat files outside the requested scope.
- Do not broaden a file-specific request into a repository-wide refactor unless explicitly asked.

## Code Style

- Treat the project-root `style.xml` as the source of truth for Java code style and Checkstyle requirements.
- Do not add a Checkstyle suppression merely to bypass a violation. Fix the code or document a genuinely required
  exception within the requested scope.
- Prefer self-explanatory code and avoid comments that merely restate what the code already makes clear.
- Keep text files UTF-8 encoded and preserve the existing line-ending style of files being edited.

## Nullness Annotations

This project uses **strict non-null by default** across all packages, enforced via `package-info.java`:

```java
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@FieldsAreNonnullByDefault
package example.packageId;

import com.mojang.logging.annotations.FieldsAreNonnullByDefault;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
```

- All method return values, parameters, and fields are considered @Nonnull by default.
- When a value may be null, explicitly annotate it with javax.annotation.Nullable.
- Do not use @Nonnull annotations — they are redundant.
- Always check @Nullable values before dereferencing to avoid NullPointerException.
- These defaults apply project-wide unless overridden by a local package-info.java.

## Verification

- Run focused tests or checks that cover the changed behavior before broader validation.
- For Java source changes, run `./gradlew.bat compileJava --console=plain` on Windows.
- When generated data or assets change, run `./gradlew.bat runData --console=plain` when applicable and inspect the
  generated diff.
- Run `git diff --check` for tracked changes and inspect `git status --short` so untracked artifacts are not missed.
- Report which verification commands were run and identify any checks that could not be completed.
