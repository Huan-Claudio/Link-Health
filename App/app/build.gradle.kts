import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

plugins {
    alias(libs.plugins.android.application)
}

/**
 * Localiza o IPv4 da Wi-Fi/Ethernet ativa para o emulador acessar a API local.
 * LINK_HEALTH_BACKEND_HOST permite uma substituicao manual somente se necessario.
 */
fun detectarHostBackend(): String {
    val bloqueadas = listOf("virtual", "vmware", "vbox", "hyper-v", "wsl", "docker", "bluetooth")
    val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        .filter { networkInterface ->
            networkInterface.isUp && !networkInterface.isLoopback && !networkInterface.isVirtual &&
                bloqueadas.none { bloqueada ->
                    "${networkInterface.name} ${networkInterface.displayName}".lowercase().contains(bloqueada)
                }
        }
        .sortedBy { networkInterface ->
            val nome = "${networkInterface.name} ${networkInterface.displayName}".lowercase()
            when {
                "wi-fi" in nome || "wifi" in nome || "wlan" in nome -> 0
                "ethernet" in nome || "eth" in nome -> 1
                else -> 2
            }
        }

    return interfaces.asSequence()
        .flatMap { networkInterface -> Collections.list(networkInterface.inetAddresses).asSequence() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { address -> !address.isLoopbackAddress && !address.isLinkLocalAddress && address.isSiteLocalAddress }
        ?.hostAddress
        ?: throw GradleException("Nao foi encontrado um IPv4 local para acessar o backend. Conecte-se a uma rede ou defina LINK_HEALTH_BACKEND_HOST.")
}

val backendHost = System.getenv("LINK_HEALTH_BACKEND_HOST")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: detectarHostBackend()

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
            // IP identificado automaticamente em cada build de desenvolvimento.
            // A configuração é exclusiva do build debug; a versão publicada usa HTTPS.
            buildConfigField("String", "BACKEND_BASE_URL", "\"http://${backendHost}:8080\"")
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
