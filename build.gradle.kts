import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.intellij.platform.grammarkit") version "2.19.0"
}

group = "com.tangentcode"
version = "1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    // The IntelliJ test framework needs opentest4j on the classpath even for JUnit 4 tests.
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

// The parser (K.bnf) and lexer (K.flex) are generated on every build rather than
// committed, so the grammar files stay the single source of truth.
tasks {
    generateParser {
        sourceFile = file("src/com/tangentcode/intellik/K.bnf")
    }
    generateLexer {
        sourceFile = file("src/com/tangentcode/intellik/K.flex")
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        java.srcDir(tasks.generateParser)
        java.srcDir(tasks.generateLexer)
        resources.setSrcDirs(listOf("resources", "src"))
        resources.exclude("**/*.java", "**/*.bnf", "**/*.flex")
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(emptyList<String>())
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("253")
            untilBuild.set(provider { null })
        }
    }
    buildSearchableOptions.set(false)
    instrumentCode.set(false)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
}
