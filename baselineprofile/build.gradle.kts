plugins {
    id("com.android.test")
    // (Desactivado temporalmente) id("androidx.baselineprofile") version "1.4.1" -> incompatible con AGP 9.0
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

// (Desactivado temporalmente)
// baselineProfile {
//     // Ejecuta las pruebas en dispositivos conectados para generar el perfil
//     useConnectedDevices = true
// }

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.benchmark.macro.junit4)
}