plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aula.delta_mobile_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aula.delta_mobile_app"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "CHATBOT_API_URL", "\"https://delta-artificial-intelligence.onrender.com/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildTypes {
        debug {
            buildConfigField("String", "DELTA_API_URL", "\"${providers.gradleProperty("deltaApiUrl").getOrElse("https://delta-api-postgres.onrender.com/")}\"")
            manifestPlaceholders["deltaCleartext"] = "true"
        }
        release {
            val apiUrl = providers.gradleProperty("deltaReleaseApiUrl").getOrElse("https://delta-api-postgres.onrender.com/")
            require(apiUrl.startsWith("https://")) { "Release exige HTTPS" }
            buildConfigField("String", "DELTA_API_URL", "\"$apiUrl\"")
            manifestPlaceholders["deltaCleartext"] = "false"
        }
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.11.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.11.0")
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.mlkit)
    implementation(libs.mlkit.barcode)
    implementation(libs.activity)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.core.splashscreen)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
