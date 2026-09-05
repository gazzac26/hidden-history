import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.24" // Downgraded to 1.9.24 for KAPT stability
    kotlin("kapt") // Room annotation processing
}

android {
    namespace = "com.hiddenhistory"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hiddenhistory"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject keys from local.properties
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_KEY",
            "\"${localProperties.getProperty("SUPABASE_KEY", "")}\""
        )

        // Inject Google Maps API Key into AndroidManifest.xml
        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY", "")
    }

    // Prevents AAPT2 from compressing large CSV files in assets
    // so they can be read raw line-by-line
    aaptOptions {
        noCompress("csv")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true // Required to generate BuildConfig fields
    }
}

dependencies {

    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    implementation(libs.androidx.material3)

    implementation(libs.androidx.ui)

    implementation(libs.androidx.ui.graphics)

    implementation(libs.androidx.ui.tooling.preview)

    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("androidx.compose.material:material-icons-extended")


    // ============================================================
    // GOOGLE MAPS
    // ============================================================

    implementation("com.google.maps.android:maps-compose:4.3.3")

    implementation("com.google.android.gms:play-services-maps:18.2.0")

    implementation("com.google.android.gms:play-services-location:21.2.0")


    // ============================================================
    // SUPABASE & KTOR
    // ============================================================

    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.1"))

    implementation("io.github.jan-tennert.supabase:postgrest-kt")

    implementation("io.github.jan-tennert.supabase:realtime-kt")

    implementation("io.github.jan-tennert.supabase:functions-kt")

    implementation("io.github.jan-tennert.supabase:auth-kt")

    implementation("io.github.jan-tennert.supabase:storage-kt")


    // ============================================================
    // NETWORK & SERIALIZATION
    // ============================================================

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("io.ktor:ktor-client-android:3.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")


    // ============================================================
    // COIL IMAGE LOADING
    // ============================================================

    implementation("io.coil-kt:coil-compose:2.7.0")


    // ============================================================
    // ROOM DATABASE & KAPT
    // ============================================================

    val roomVersion = "2.6.1"

    implementation("androidx.room:room-runtime:$roomVersion")

    implementation("androidx.room:room-ktx:$roomVersion")

    kapt("androidx.room:room-compiler:$roomVersion")


    // ============================================================
    // GOOGLE PLAY BILLING
    // ============================================================

    implementation("com.android.billingclient:billing-ktx:9.1.0")


    // ============================================================
    // TESTING
    // ============================================================

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))

    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.test.manifest)

    debugImplementation(libs.androidx.ui.tooling)
}