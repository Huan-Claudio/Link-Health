plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

// O OneDrive pode bloquear arquivos compilados dentro de build/. Quando o clone
// estiver nessa pasta, os arquivos temporários do Gradle ficam fora da nuvem.
if (project.projectDir.absolutePath.contains("\\OneDrive\\", ignoreCase = true)) {
    layout.buildDirectory.set(file(System.getProperty("java.io.tmpdir") + "link-health-backend-build"))
}

group = "br.edu.pucgoias"
version = "0.0.1-SNAPSHOT"

java {
    // Compila bytecode compatível com Java 17, a versão mínima do servidor.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.flywaydb:flyway-core")

    // Dependências de persistência são ativadas somente no perfil postgres.
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
