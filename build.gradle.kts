plugins {
    id("java")
    id("application")
    id("jacoco")
}

group = "kienzle"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val javafxVersion = "17.0.17"

// DETECT OS: Necessary to get the correct JavaFX binaries (jars with classes)
val osName = System.getProperty("os.name").lowercase()
val osClassifier = when {
    osName.contains("win") -> "win"
    osName.contains("mac") -> "mac"
    else -> "linux"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    // This tells Gradle to put modules on the module-path automatically
    modularity.inferModulePath.set(true)
}

dependencies {
    // JavaFX: We MUST specify the classifier (osClassifier) to get the jar with module-info
    implementation("org.openjfx:javafx-controls:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-base:$javafxVersion:$osClassifier")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$osClassifier")

    // Third-party libraries
    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0")
    implementation("org.apache.pdfbox:pdfbox:3.0.5")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    // Since we are fully modular, we define the module logic here
    mainModule.set("kienzle.desktop")
    mainClass.set("kienzle.desktop.LeaseManagerApplication")
}

// NOTE: We removed the 'doFirst' blocks.
// Gradle handles the module path automatically when module-info.java exists
// AND the dependencies (with correct classifiers) are detected as modules.

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

// Builds a self-contained application image (runtime included) with the JDK's jpackage:
// ./gradlew packageApp  ->  build/jpackage/LeaseManager
// jlink cannot handle the automatic modules (PDFBox, ZXing ...), so only JavaFX goes onto the module path
// of the bundled runtime; the application and the other libraries run from the class path.
val prepareJpackage by tasks.registering(Sync::class) {
    dependsOn(tasks.named("installDist"))
    val lib = layout.buildDirectory.dir("install/${project.name}/lib")
    from(lib) { exclude("javafx-*") ; into("input") }
    from(lib) { include("javafx-*") ; into("javafx") }
    into(layout.buildDirectory.dir("jpackage-work"))
}

val packageApp by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Creates a self-contained app image with jpackage"
    dependsOn(prepareJpackage)
    val launcher = javaToolchains.launcherFor(java.toolchain)
    val work = layout.buildDirectory.dir("jpackage-work")
    val outDir = layout.buildDirectory.dir("jpackage")
    doFirst { delete(outDir) }
    executable(launcher.map { it.metadata.installationPath.file("bin/jpackage").asFile.absolutePath }.get())
    args(
        "--type", "app-image",
        "--name", "LeaseManager",
        "--app-version", "1.0",
        "--input", work.get().dir("input").asFile.absolutePath,
        "--main-jar", "${project.name}-${project.version}.jar",
        "--main-class", "kienzle.desktop.LeaseManagerApplication",
        "--module-path", work.get().dir("javafx").asFile.absolutePath,
        "--add-modules", "javafx.controls,javafx.fxml,java.logging,java.desktop,java.prefs",
        "--dest", outDir.get().asFile.absolutePath
    )
}
