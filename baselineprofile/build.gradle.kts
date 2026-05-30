plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.samidevstudio.pxllauncherneo.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    buildTypes {
        release {
            debuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.ui.test.junit4)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.junit)
}

baselineProfile {
    warnings {
        maxAgpVersion = false
    }
}
