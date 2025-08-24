plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("com.gradleup.shadow") version "9.0.0-beta9"
}

group = "com.minekartastudio"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.5")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    // Add NOP logger to silence SLF4J warning from HikariCP
    implementation("org.slf4j:slf4j-nop:2.0.13")
    implementation("org.yaml:snakeyaml:2.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    shadowJar {
        relocate("com.zaxxer.hikari", "com.minekartastudio.kartaauctionhouse.lib.hikaricp")
        relocate("com.mysql", "com.minekartastudio.kartaauctionhouse.lib.mysql")
        relocate("com.google.protobuf", "com.minekartastudio.kartaauctionhouse.lib.protobuf")
        relocate("org.slf4j", "com.minekartastudio.kartaauctionhouse.lib.slf4j")
        relocate("org.yaml", "com.minekartastudio.kartaauctionhouse.lib.yaml")
        archiveClassifier.set("")
    }

    reobfJar {
        dependsOn(jar)
    }

    test {
        useJUnitPlatform()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
