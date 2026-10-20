plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.drquizman.tv"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.drquizman.tv"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "0.2.5"
        buildConfigField("String", "PACK_SERVICE_URL", "\"${providers.gradleProperty("packServiceUrl").orElse("https://xoerzqgvcpjrbuwtbtdo.supabase.co/functions/v1/quizman").get()}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        jniLibs {
            // Extract native libraries at installation for older TV package installers.
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation("com.google.android.filament:filament-android:1.75.1")
    implementation("com.google.android.filament:gltfio-android:1.75.1")
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.tv:tv-foundation:1.0.0")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("io.ktor:ktor-server-cio:2.3.13")
    implementation("io.ktor:ktor-server-websockets:2.3.13")
    implementation("io.ktor:ktor-server-core:2.3.13")
    implementation("com.google.zxing:core:3.5.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

val prepareHostModel by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("node", "tools/prepare-android-host.cjs")
    inputs.files(rootProject.file("assets/host-3d-v1/archive-doctor/doctor-animated.glb"),
        rootProject.file("assets/host-3d-v1/archive-doctor/rig-changes.json"),
        rootProject.file("tools/prepare-android-host.cjs"))
    outputs.files(file("src/main/assets/host/doctor.glb"), file("src/main/assets/host/source.json"))
}
tasks.named("preBuild").configure { dependsOn(prepareHostModel) }
