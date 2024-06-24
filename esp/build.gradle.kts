plugins {
    `java-library`
    id("org.graalvm.buildtools.native") version "0.10.2"
    id("io.freefair.lombok") version "8.6"
}

group = "uk.co.rupesiro"
version = "1.0.0-dev"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.bundles.testing.runtime)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

graalvmNative {
    toolchainDetection.set(true)
    metadataRepository {
        enabled = true
    }

    agent {
        defaultMode = "conditional"
        modes {
            conditional {
                userCodeFilterPath = "user-code-filter.json"
            }
        }
        metadataCopy {
            mergeWithExisting = false
            inputTaskNames.add("test")
            outputDirectories.add("src/main/resources/META-INF/native-image/$group.$name")
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.javaModuleVersion = provider { version as String }
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("jqwik")
    }
}
