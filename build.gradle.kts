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

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    shadowJar {
        relocate("com.zaxxer.hikari", "com.minekartastudio.kartaauctionhouse.lib.hikaricp")
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
