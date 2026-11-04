import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing: credentials live OUTSIDE the repo in %USERPROFILE%\tvbrowser-signing\keystore.properties
// Format:
//   storeFile=C:/Users/you/tvbrowser-signing/tvbrowser-release.jks
//   storePassword=...
//   keyAlias=tvbrowser
//   keyPassword=...
val signingPropsFile = file(System.getProperty("user.home") + "/tvbrowser-signing/keystore.properties")
val signingProps = Properties().apply {
    if (signingPropsFile.exists()) signingPropsFile.inputStream().use { load(it) }
}
val hasReleaseSigning = signingProps.getProperty("storeFile") != null

android {
    namespace = "net.reconburrito.tvbrowser"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.reconburrito.tvbrowser"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Runtime: no androidx UI libraries on purpose. Plain android.* APIs only, so the APK stays small
    // and the main source set compiles against android.jar + Kotlin stdlib alone.
    testImplementation("junit:junit:4.13.2")
    // Real org.json for JVM unit tests; android.jar's copy is a stub that throws at runtime.
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
}
