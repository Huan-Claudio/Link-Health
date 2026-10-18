plugins {
    alias(libs.plugins.android.application)
}

// Evita que arquivos temporários do Gradle fiquem bloqueados pela sincronização do OneDrive.
buildDir = File(
    System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir"),
    "LinkHealth-Android-Build/${project.name}"
)

android {
    namespace = "br.edu.pucgoias.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.edu.pucgoias.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // IP local deste computador para o emulador durante o desenvolvimento.
            // A configuração é exclusiva do build debug; a versão publicada usa HTTPS.
            buildConfigField("String", "BACKEND_BASE_URL", "\"http://192.168.1.95:8080\"")
        }
        release {
            optimization {
                enable = false
            }
            // A versão distribuída só deve receber uma URL HTTPS real, definida na implantação.
            buildConfigField("String", "BACKEND_BASE_URL", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // Gera uma classe de binding para cada layout (ex.: activity_login.xml -> ActivityLoginBinding)
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
