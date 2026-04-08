# Repository Guidelines

## Project Structure & Module Organization
- `core/` contains the main editor logic and scene/tools implementation (`core/src/main/kotlin`).
- `core-ui/` holds reusable LibGDX UI/rendering utilities (`core-ui/src/main/kotlin`).
- `desktop/` is the desktop launcher and LWJGL3 setup (`desktop/src`).
- `core-testing/` provides headless/test helpers and dependencies for non-graphics tests.
- `assets/` stores runtime assets (skins, fonts, icons, default sample files).
- `dist/` and `tmp/` are build outputs used by release/run scripts.

## Build, Test, and Development Commands
- `./gradlew dist` builds the desktop jar; output lands in `desktop/build/libs/`.
- `./gradlew test` runs unit tests across all modules.
- `./gradlew :core:test` or `./gradlew :core-ui:test` runs module-specific tests.
- `./run.sh` generates a version file, builds a dev jar, and launches it with a sample `.vxdi` file.
- `./dist.sh <tag> [jdk-list]` creates release jars for multiple JDKs into `dist/`.

## Coding Style & Naming Conventions
- Kotlin is the primary language with a small Java launcher; follow idiomatic Kotlin.
- Indentation is 4 spaces; keep line wrapping and spacing consistent with nearby files.
- Classes and files use `PascalCase` (e.g., `SceneController.kt`); functions and variables use `camelCase`.
- No repo-specific formatter is configured; avoid large formatting-only diffs.

## Testing Guidelines
- Tests use JUnit 4 and JUnit Jupiter; see `core/src/test/kotlin` and `core-ui/src/test/kotlin`.
- Name tests with a `*Test.kt` suffix (e.g., `Vector3iTest.kt`).
- Prefer headless or non-graphics tests when possible (see `core-testing/`).

## Commit & Pull Request Guidelines
- Recent history uses a Conventional Commits style: `type(scope): summary`.
  Example: `fix(ui): fix rectangle operations`.
- Keep commits scoped and descriptive; include a clear summary of behavior changes.
- PRs should include: a short summary, testing notes (commands run), and screenshots/GIFs for UI changes.

## Configuration Tips
- Gradle uses `-PjavaCompatVersion=<major>` for target compatibility; release scripts exercise multiple JDKs.
- Version metadata is generated into `core/src/main/kotlin/com/voxd31/editor/Voxd31EditorVersion.kt` by the scripts.
