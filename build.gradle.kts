plugins {
    id("java")
    id("application")
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

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

application {
    // Since we are fully modular, we define the module logic here
    mainModule.set("kienzle.desktop")
    mainClass.set("kienzle.desktop.HelloApplication")
}

// NOTE: We removed the 'doFirst' blocks.
// Gradle handles the module path automatically when module-info.java exists
// AND the dependencies (with correct classifiers) are detected as modules.

tasks.named<Test>("test") {
    useJUnitPlatform()
}
