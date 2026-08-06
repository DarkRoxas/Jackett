import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Configuration Google Wallet et TMDB : renseignée dans local.properties
// (non versionné) ou via des variables d'environnement sur le poste / la CI.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun localConfig(key: String): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: "")

android {
    namespace = "fr.cinepass.ticket"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.cinepass.ticket"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Identifiant émetteur de la console Google Wallet (ex : 3388000000012345678)
        buildConfigField("String", "WALLET_ISSUER_ID", "\"${localConfig("WALLET_ISSUER_ID")}\"")
        // Suffixe de la classe de billet créée dans la console Wallet
        buildConfigField("String", "WALLET_CLASS_SUFFIX", "\"${localConfig("WALLET_CLASS_SUFFIX").ifEmpty { "cinepass_event_class" }}\"")
        // Nom affiché de l'émetteur sur le pass
        buildConfigField("String", "WALLET_ISSUER_NAME", "\"${localConfig("WALLET_ISSUER_NAME").ifEmpty { "CinePass" }}\"")
        // Endpoint backend qui renvoie le JWT signé (mode recommandé en production)
        buildConfigField("String", "WALLET_JWT_ENDPOINT", "\"${localConfig("WALLET_JWT_ENDPOINT")}\"")

        // Clé d'API TMDB pour la recherche de films (facultative)
        buildConfigField("String", "TMDB_API_KEY", "\"${localConfig("TMDB_API_KEY")}\"")
    }

    signingConfigs {
        // Clé de debug versionnée : sans elle, chaque machine (et chaque run de
        // CI) génère la sienne, et Android refuse d'installer le nouvel APK
        // par-dessus l'ancien pour cause de signature différente.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")

            // Signature locale du JWT : uniquement pour le développement.
            // La clé privée du compte de service ne doit JAMAIS être livrée en production.
            buildConfigField("String", "WALLET_SA_EMAIL", "\"${localConfig("WALLET_SA_EMAIL")}\"")
            buildConfigField(
                "String",
                "WALLET_SA_PRIVATE_KEY",
                "\"${localConfig("WALLET_SA_PRIVATE_KEY").replace("\n", "\\n")}\"",
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Pas de clé privée embarquée en release : le JWT vient du backend.
            buildConfigField("String", "WALLET_SA_EMAIL", "\"\"")
            buildConfigField("String", "WALLET_SA_PRIVATE_KEY", "\"\"")
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.zxing.core)
    implementation(libs.play.services.pay)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    // `org.json` n'est qu'un stub dans l'android.jar mockable : on fournit
    // l'implémentation réelle aux tests JVM du builder de pass Wallet.
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
