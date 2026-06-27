plugins {
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.gradleup.tapmoc)
}

group = "net.lsafer.compose-simplenav"

tapmoc {
    java(libs.versions.java.get().toInt())
    kotlin(libs.versions.kotlin.get())
}

kotlin {
    jvm("desktop")
    js { browser() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
    android {
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        namespace = "net.lsafer.compose.simplenav"
    }
    sourceSets {
        val commonMain by getting
        val desktopMain by getting
        val androidMain by getting
        val jsMain by getting
        val wasmJsMain by getting

        val jvmCommon by creating
        jvmCommon.dependsOn(commonMain)
        desktopMain.dependsOn(jvmCommon)
        androidMain.dependsOn(jvmCommon)

        val webCommon by creating
        webCommon.dependsOn(commonMain)
        jsMain.dependsOn(webCommon)
        wasmJsMain.dependsOn(webCommon)
    }
    sourceSets.commonMain.dependencies {
        implementation(compose.runtime)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.atomicfu)
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
    sourceSets.named("webCommon").dependencies {
        implementation(libs.kotlinx.browser)
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (project.properties["doSign"] == "yes")
        signAllPublications()
    coordinates(
        groupId = group.toString(),
        artifactId = "compose-simplenav",
        version = version.toString(),
    )
    pom {
        name = "Compose SimpleNav"
        description = "Simple navigation library for Compose Multiplatform"
        inceptionYear = "2025"
        url = "https://github.com/LSafer/compose-simplenav/"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "LSafer"
                name = "Sulaiman Oboody"
                url = "https://github.com/LSafer/"
            }
        }
        scm {
            url = "https://github.com/LSafer/compose-simplenav/"
            connection = "scm:git:git://github.com/LSafer/compose-simplenav.git"
            developerConnection = "scm:git:ssh://git@github.com/LSafer/compose-simplenav.git"
        }
    }
}
