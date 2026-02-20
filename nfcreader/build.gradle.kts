@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.vanniktechMavenPublish)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.dokka)
    id("signing")
}

allprojects { version = libs.versions.nfcreader.version.get() }

dokka {
    moduleName.set("NfcReaderKMP")
    moduleVersion.set("${project.version}")

    dokkaPublications.configureEach {
        outputDirectory.set(rootDir.resolve("docs/api/${project.version}"))
    }

    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(true)

            documentedVisibilities(VisibilityModifier.Public, VisibilityModifier.Internal)

            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(
                    uri("https://github.com/dalafiarisamuel/NfcReaderKMP/tree/main/nfcreader/src")
                )
                remoteLineSuffix.set("#L")
            }

            skipEmptyPackages.set(true)

            if (name == "commonMain") {
                displayName.set("Common")
                analysisPlatform.set(KotlinPlatform.Common)
            }

            if (name == "androidMain") {
                displayName.set("Android")
                analysisPlatform.set(KotlinPlatform.AndroidJVM)
                suppress.set(false)
            }

            if (name == "iosMain") {
                displayName.set("iOS")
                // analysisPlatform.set(KotlinPlatform.Native)
                suppress.set(false)
            }
        }
    }

    dokkaPublications.configureEach {
        pluginsConfiguration.html {
            footerMessage.set("Built with ❤️ for the KMP community by Samuel Dalafiari")
        }
    }
}

signing {
    useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_KEY_PASSWORD"))
    sign(publishing.publications)

    // Temporary workaround, see
    // https://github.com/gradle/gradle/issues/26091#issuecomment-1722947958
    tasks.withType<AbstractPublishToMaven>().configureEach {
        val signingTasks = tasks.withType<Sign>()
        mustRunAfter(signingTasks)
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.devtamuno.kmp.nfcreader.resources"
}

kotlin {
    androidLibrary {
        namespace = "com.devtamuno.kmp.nfcreader"
        compileSdk = 36
        minSdk = 24

        androidResources { enable = true }

        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

        withHostTestBuilder {}

        withDeviceTestBuilder { sourceSetTreeName = "test" }
            .configure { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.get().compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    abiValidation {
        enabled.set(true)
        klib {
            enabled = true
            keepUnsupportedTargets = true
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "nfcreaderKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.material3)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.stdlib)
                implementation(libs.compose.components.resources)
                implementation(libs.compottie)
                implementation(libs.compottie.resources)
            }
        }

        commonTest { dependencies { implementation(libs.kotlin.test) } }

        androidMain { dependencies { implementation(libs.androidx.activity.compose) } }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain { dependencies {} }
    }
}
