# Copilot Instructions

## Build and test

- Use the Gradle wrapper with Java 17: `./gradlew run` launches the JavaFX application, `./gradlew build` compiles and runs the full test suite, and `./gradlew test` runs tests only.
- Run one test class with `./gradlew test --tests 'kienzle.desktop.ContractDefaultsTest'`; run one test method with `./gradlew test --tests 'kienzle.desktop.ContractDefaultsTest.newContract_hasDefaultTexts'`.
- The `test` task runs JUnit Jupiter and finalizes with `jacocoTestReport`; coverage is written under `build/reports/jacoco/test/`.
- There is no separate lint task configured.

## Architecture

- This is a Java 17 modular JavaFX desktop application. `LeaseManagerApplication` loads localization, builds the `AppContext`, and loads `ContractView.fxml` through `ViewLoader`.
- FXML views and localized bundles live in `src/main/resources/kienzle/desktop/`. `ViewLoader` installs `MainLayoutController` through a controller factory so it can receive the application context; nested FXML controllers use their no-argument constructors.
- `AppContext` is the composition root. It constructs `JsonContractRepository`, `ContractService`, PDF services, package services, and exposes `ContractWorkflow` to the UI. Keep JavaFX controllers dependent on the workflow/context rather than directly constructing persistence or PDF services.
- `ContractWorkflow` is the UI-facing facade. Its lifecycle, document, and package use cases delegate to the `contract`, `pdf`, and package-service layers. Contract JSON records and their documents are stored beneath `~/.lease-manager` by default; active and archived records are distinct stores and archive/restore moves records and documents transactionally.
- PDF creation, stamping, QR codes, and scan import belong in `kienzle.desktop.pdf`; contract state, repositories, and document relocation belong in `kienzle.desktop.contract`.

## Repository conventions

- Preserve the Java module descriptor in `src/main/java/module-info.java`: add `requires`, `exports`, and `opens` entries when new dependencies or FXML/Gson-reflected packages need them.
- UI strings come from `messages_de.properties` and `messages_en.properties` through `I18n`; add matching keys to both bundles instead of hard-coding user-facing text.
- Use `@TempDir` for file-system tests. Tests that instantiate JavaFX controls start the toolkit once with `JavaFxTestSupport.startToolkit()`.
- Preserve filesystem safety and lifecycle invariants: documents stay beneath the configured data directory, and contract status transitions continue through the workflow/services rather than UI-side file manipulation.
