plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.espzera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.espzera"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    // Dependências do catálogo de versões (libs)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Dependência direta para a biblioteca de exportação CSV
    implementation("com.opencsv:opencsv:5.9")

    // Dependências de Teste
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    // A linha abaixo foi corrigida (o ponto final no fim foi removido)
    androidTestImplementation(libs.espresso.core)

    /*
     As linhas abaixo foram removidas pois eram duplicatas das dependências
     que já estavam sendo chamadas pelo catálogo 'libs' no topo.
     implementation (androidx.appcompat:appcompat:1.6.1)
     implementation (com.google.android.material:material:1.11.0)
    */
}