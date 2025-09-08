import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
    alias(libs.plugins.google.gms.google.services)
}

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(FileInputStream(localPropertiesFile))
}
val mapsApiKey = properties.getProperty("MAPS_API_KEY")
val keyStorePassword = properties.getProperty("KEYSTORE_PASSWORD")
val keyAlias = properties.getProperty("KEY_ALIAS")
val keyPassword = properties.getProperty("KEY_PASSWORD")

android {
    namespace = "com.rst.gadissalonmanagementsystemapp"
    compileSdk = 36

    signingConfigs {
        create("release") {
            // Get the path to your keystore file
            val keystoreFile = rootProject.file("release-keystore.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = keyStorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.rst.gadissalonmanagementsystemapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["mapsApiKey"] = mapsApiKey ?: "YOUR_DEFAULT_KEY"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        viewBinding = true
    }
}

dependencies {
    // Firebase Bill of Materials (BoM) - This manages all other Firebase library versions
    implementation(libs.firebase.bom)

    // Firebase Libraries - Notice they do NOT have a version number. The BoM handles it.
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.appcheck.playintegrity)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Your existing app libraries
    implementation(libs.lottie)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.google.firebase.appcheck.playintegrity)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}