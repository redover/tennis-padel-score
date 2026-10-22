plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Screenshot delle schermate senza emulatore: ./gradlew :wear:recordPaparazziDebug
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.redover.punteggio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.redover.punteggio"
        // Wear OS 3+ (il OnePlus Watch 2R ha Wear OS 4).
        minSdk = 30
        targetSdk = 34
        versionCode = 9
        versionName = "0.9.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Firmato con la chiave di debug: comodo per installarlo sul proprio orologio.
            signingConfig = signingConfigs.getByName("debug")
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
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear)
    implementation(libs.wear.ongoing)
    implementation(libs.androidx.core.ktx)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
