plugins {
    id("java")
    id("com.gradleup.shadow") version("9.3.1")
}

group = "REPLACE_WITH_YOUR_GROUP_ID"
version = "REPLACE_WITH_YOUR_VERSION"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer-1.0.0.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
    withJavadocJar()
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    javadoc {
        options.encoding = "UTF-8"
        (options as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/java"))
        resources.setSrcDirs(listOf("src/resources"))
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}