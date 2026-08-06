plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "github.io.Frenxys"
version = project.property("pluginVersion") as String

java {
    // Paper API 26.2 ships Java 25 class files, so we compile with JDK 25+.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

// Compile against JVM 25 (paper-api requirement) while emitting Java 21 bytecode,
// so the jar runs on every server from 1.21 up to 26.2.
val jvmVersion = Attribute.of("org.gradle.jvm.version", Int::class.javaObjectType)
configurations.compileClasspath {
    attributes.attribute(jvmVersion, 25)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    // Paper API — switch the version in gradle.properties to target another MC version.
    // Compile against the OLDEST version you want to support to get a universal jar.
    compileOnly("io.papermc.paper:paper-api:${project.property("paperVersion")}")

    // LuckPerms API (softdepend)
    compileOnly("net.luckperms:api:5.4")

    // SQLite JDBC — shaded & relocated into the final jar (brings slf4j-api transitively)
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

tasks.compileJava {
    // Emit Java 21 bytecode (runs on 1.21+ servers) with the JDK 25 toolchain.
    options.compilerArgs.addAll(listOf("--release", "21"))
    options.encoding = "UTF-8"
}

tasks.jar {
    // Keep the plain (unshaded) jar out of the way of the final shaded artifact.
    archiveBaseName.set("NA")
    archiveClassifier.set("thin")
}

tasks.shadowJar {
    archiveBaseName.set("NA")
    archiveVersion.set(project.property("pluginVersion") as String)
    archiveClassifier.set("")

    // Same relocations as the original maven-shade-plugin config:
    // org.xerial -> github.io.Frenxys.shaded.xerial
    // org.sqlite -> github.io.Frenxys.shaded.sqlite
    relocate("org.xerial", "github.io.Frenxys.shaded.xerial")
    relocate("org.sqlite", "github.io.Frenxys.shaded.sqlite")

    // Drop signature files (same filter as the original POM)
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    // Let the service-file transformer see all duplicates (silences the
    // 'DuplicatesStrategy is EXCLUDE' warning). No real duplicate classes exist
    // in this dependency set, so INCLUDE is harmless.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    // Keep META-INF/services/java.sql.Driver so SQLite JDBC can find its driver
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
