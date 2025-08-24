plugins {
    id("com.android.test")
    id("androidx.baselineprofile") version "1.4.0"
    kotlin("android")
}

android {
    namespace = "com.valdo.notasinteligentesvaldo.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Este es el módulo de la app que vas a perfilar
    targetProjectPath = ":app"

    // No definimos 'release' aquí, solo usamos el build type por defecto (debug)
}

baselineProfile {
    // Ejecuta las pruebas en dispositivos conectados para generar el perfil
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.3")
}