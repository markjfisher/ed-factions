plugins {
    groovy
}

val options: String? by project

group = "fish.net"
version = "1.6.0"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("commons-cli:commons-cli:1.4")
    implementation ("org.codehaus.groovy:groovy-all:3.0.5")
    implementation ("org.codehaus.groovy:groovy-cli-commons:3.0.5")
    implementation("org.apache.httpcomponents:httpclient:4.5.12")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-simple:1.7.30")

    // oops, there are no tests anyway...
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.1")
}

tasks {
    getByName<Wrapper>("wrapper") {
        gradleVersion = "6.5"
        distributionType = Wrapper.DistributionType.ALL
    }

    register(name = "stats", type = JavaExec::class) {
        description = "Gets stats for chosen faction"
        main = "fish.ed.FactionStatsOutput"
        classpath = sourceSets["main"].runtimeClasspath
        if (project.hasProperty("args")) {
            val argsString = project.properties["args"] as String
            val splitter = """([^\s"']+)|["']([^'"]*)["']""".toRegex()
            val argsList = splitter.findAll(argsString).toList().map { it.value.replace("'", "").replace("\"", "") }
            args = argsList
        }
    }

    register(name = "within", type = JavaExec::class) {
        description = "Displays systems within given range of given system"
        main = "fish.ed.Within"
        classpath = sourceSets["main"].runtimeClasspath
        if (project.hasProperty("args")) {
            val argsString = project.properties["args"] as String
            val splitter = """([^\s"']+)|["']([^'"]*)["']""".toRegex()
            val argsList = splitter.findAll(argsString).toList().map { it.value.replace("'", "").replace("\"", "") }
            args = argsList
        }
    }
}
