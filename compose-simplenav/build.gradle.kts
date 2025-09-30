import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose)
}

group = "net.lsafer.compose-simplenav"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    jvm("desktop")
    js { browser() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    sourceSets {
        val commonMain by getting
        val jsMain by getting
        val wasmJsMain by getting

        val webCommon by creating
        webCommon.dependsOn(commonMain)
        jsMain.dependsOn(webCommon)
        wasmJsMain.dependsOn(webCommon)
    }
    sourceSets.commonMain.dependencies {
        implementation(compose.runtime)
        implementation(libs.kotlinx.serialization.json)
    }
    sourceSets.named("webCommon").dependencies {
        implementation(libs.kotlinx.browser)
    }
}

android {
    namespace = "net.lsafer.compose.simplenav"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

mavenPublishing {
    publishToMavenCentral()
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
