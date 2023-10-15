plugins {
    jacoco
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
}

group = "Pupsen&Vupsen"
version = "1.1.0.14-alpha"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-devtools")

    implementation("io.springfox:springfox-boot-starter:3.0.0")
    implementation("io.springfox:springfox-swagger2:3.0.0")
    implementation("io.springfox:springfox-swagger-ui:3.0.0")

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

//    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    implementation("io.micrometer:micrometer-core:1.6.6")
//    implementation("io.micrometer:micrometer-registry-prometheus:1.6.6")

    runtimeOnly("mysql:mysql-connector-java")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.beust:klaxon:5.6")

    implementation("com.squareup.okhttp3:okhttp:4.2.0")

    implementation("com.h2database:h2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation(platform("org.junit:junit-bom:5.9.0"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

val jacocoExclude = listOf(
    "**/configuration/**",
    "**/entities/**",
    "**/enums/**",
    "**/repositories/**",
    "**/services/**",
    "**/*Application*"
)

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    useJUnitPlatform()
    maxHeapSize = "2G"
    extensions.configure<JacocoTaskExtension> {
        excludes = jacocoExclude
    }
    // Uncomment to run concurrent tests on your own PC
    /*jvmArgs(
        "-Xmx4096m",
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.util=ALL-UNNAMED"
    )*/
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(false)
        csv.required.set(true)
        csv.outputLocation.set(file("${buildDir}/jacoco/report.csv"))
        html.outputLocation.set(file("${buildDir}/reports/jacoco"))
    }
    classDirectories.setFrom(classDirectories.files.map {
        fileTree(it).matching {
            exclude(jacocoExclude)
        }
    })
}