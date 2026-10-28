plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.intellij.platform.grammarkit") version "2.19.0"
}

group = "com.x1010data"
version = "1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
    }
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

// The parser (K.bnf) and lexer (K.flex) are generated on every build rather than
// committed, so the grammar files stay the single source of truth.
tasks {
    generateParser {
        sourceFile = file("src/com/x1010data/intellik/K.bnf")
    }
    generateLexer {
        sourceFile = file("src/com/x1010data/intellik/K.flex")
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
