plugins {
    `java-library`
    id("org.graalvm.buildtools.native") version "0.10.2"
}

group = "uk.co.rupesiro"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform)
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

tasks.withType<Test> {
    useJUnitPlatform()
}
