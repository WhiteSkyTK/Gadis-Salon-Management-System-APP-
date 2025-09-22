import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
    alias(libs.plugins.google.gms.google.services)
    id("com.google.firebase.crashlytics")
}

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(FileInputStream(localPropertiesFile))
}
val mapsApiKey = properties.getProperty("MAPS_API_KEY")
val keyStorePassword = properties.getProperty("KEYSTORE_PASSWORD")
val KeyAlias = properties.getProperty("KEY_ALIAS")
val KeyPassword = properties.getProperty("KEY_PASSWORD")

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
                keyAlias = KeyAlias
                keyPassword = KeyPassword
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

        // This makes the key available to AndroidManifest.xml (for the map)
        manifestPlaceholders["mapsApiKey"] = mapsApiKey ?: "YOUR_DEFAULT_KEY"

        // ADD THIS LINE: This makes the key available to your Kotlin code
        buildConfigField("String", "MAPS_API_KEY", "\"${mapsApiKey}\"")
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
        buildConfig = true
    }
}

dependencies {
    // Firebase Bill of Materials (BoM) - This manages all other Firebase library versions
    implementation(platform(libs.firebase.bom))

    // Firebase Libraries (no versions needed)
    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.2")
    implementation("com.google.firebase:firebase-appcheck-playintegrity:19.0.0")
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.2")
    implementation("com.google.firebase:firebase-functions-ktx:21.2.1")
    implementation("com.google.firebase:firebase-crashlytics-ktx:19.4.4")
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.google.firebase.appcheck.playintegrity)

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

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.coil)
    implementation(libs.places)
    implementation("com.prolificinteractive:material-calendarview:1.4.3")
}