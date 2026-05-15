plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    jacoco
}

android {
    namespace = "com.aiapkbuilder.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aiapkbuilder.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENAI_BASE_URL", "\"https://api.openai.com/v1/\"")
        buildConfigField("String", "DEFAULT_AI_MODEL", "\"gpt-4o\"")
        buildConfigField("String", "GITHUB_API_BASE", "\"https://api.github.com/\"")
        buildConfigField("String", "CODEMAGIC_API_BASE", "\"https://api.codemagic.io/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.splashscreen)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Room DB
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Utilities
    implementation(libs.coil.compose)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.lottie.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// JaCoCo code coverage configuration
jacoco {
    version = "0.8.11"
}

tasks.register("testDebugUnitTestCoverage") {
    group = "verification"
    description = "Generates code coverage report for debug unit tests"
    dependsOn("testDebugUnitTest")

    doLast {
        val projectName = project.name
        val coverageSourceDirs = listOf(
            "src/main/java",
            "src/main/kotlin"
        )
        val classFilesTree = fileTree(
            mapOf(
                "dir" to "${layout.buildDirectory.asFile.get()}/intermediates/classes/debug",
                "excludes" to listOf(
                    "**/R.class",
                    "**/R\$*.class",
                    "**/*Test*.class",
                    "**/*\$ViewInjector*.*",
                    "**/*\$\$*.*"
                )
            )
        )

        jacoco.applyTo(classFilesTree)

        val executionDataFileTree = fileTree(
            mapOf(
                "dir" to layout.buildDirectory.asFile.get(),
                "includes" to listOf("jacoco/testDebugUnitTest.exec")
            )
        )

        val report = reports.create("testDebugUnitTestCoverage") {
            onlyIf { true }
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        val finalizedBy = tasks.create("testDebugUnitTestCoverageReport", JacocoReport::class) {
            dependsOn("testDebugUnitTest")
            group = "verification"
            description = "Generate JaCoCo coverage report"

            sourceDirectories.setFrom(files(coverageSourceDirs))
            classDirectories.setFrom(classFilesTree)
            executionData.setFrom(executionDataFileTree)

            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }
        }

        dependsOn(finalizedBy)
    }
}