import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

plugins {
    `build-scan`
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("com.gradle.plugin-publish") version "0.10.0"
    id("gradle.site") version "0.2"
    id("org.jetbrains.dokka") version "0.9.17"
    kotlin("jvm") version "1.3.10"
}

val junitPlatformVersion = "1.1.0"
val spekVersion = "2.0.0-rc.1"

group = "org.gradle.plugins"
version = "0.5"
description = "Generates documentation in HTML for given project"

val webUrl = "https://gradle-guides.github.io/${project.name}/"
val githubUrl = "https://github.com/gradle-guides/${project.name}.git"

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    if (!System.getenv("CI").isNullOrEmpty()) {
        publishAlways()
        tag("CI")
    }
}

site {
    outputDir.set(file("$rootDir/docs"))
    websiteUrl.set(webUrl)
    vcsUrl.set(githubUrl)
}

// Separate integration tests from "fast" tests
// NOTE: deprecation warnings from the following lines are caused by the Kotlin plugin using a deprecated API when adding its own sourceSet management here
val intTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + configurations.testRuntime.get()
    runtimeClasspath += output + compileClasspath
}

val intTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val intTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs the functional tests"
    group = JavaBasePlugin.VERIFICATION_GROUP

    testClassesDirs = intTest.output.classesDirs
    classpath = intTest.runtimeClasspath
    shouldRunAfter(tasks.test)

    reports {
        html.destination = file("${html.destination}/functional")
        junitXml.destination = file("${junitXml.destination}/functional")
    }

    timeout.set(Duration.ofMinutes(2))
}

val sourcesJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

val dokkaJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    from(tasks.dokka)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        includeEngines("spek2")
    }
}

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    jdkVersion = 8
    reportUndocumented = false
}

tasks.check {
    dependsOn(integrationTest)
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.freemarker:freemarker:2.3.26-incubating")

    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }

    testRuntimeOnly(kotlin("reflect"))
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
        exclude(group = "org.junit.platform")
        exclude(group = "org.jetbrains.kotlin")
    }

    testImplementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")

    intTestImplementation("org.jsoup:jsoup:1.10.2") {
        because("Integration tests parse generated HTML for verification")
    }
    intTestImplementation(gradleTestKit())
}

// Configure java-gradle-plugin
gradlePlugin {
    plugins {
        register("sitePlugin") {
            id = "gradle.site"
            implementationClass = "org.gradle.plugins.site.SitePlugin"
        }
    }
}

// Configure plugin-publish plugin
pluginBundle {
    website = webUrl
    vcsUrl = githubUrl
    description = project.description
    tags = listOf("documentation", "site")

    plugins {
        named("sitePlugin") {
            // ID and implementation class are used from `gradlePlugin` config
            displayName = "Gradle Site Plugin"
        }
    }

    // Prefix group with "gradle.plugin" because plugin portal doesn't allow groups starting with "org.gradle"
    mavenCoordinates {
        groupId = "gradle.plugin.${project.group}"
    }
}

artifacts {
    add(configurations.archives.name, dokkaJar)
    add(configurations.archives.name, sourcesJar)
}

// Configure maven-publish plugin
publishing {
    publications.withType<MavenPublication> {
        artifact(dokkaJar.get())
        artifact(sourcesJar.get())

        pom {
            name.set(project.name)
            description.set(project.description)
            url.set(webUrl)

            scm {
                url.set(githubUrl)
            }

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("eriwen")
                    name.set("Eric Wendelin")
                    email.set("eric@gradle.com")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(configurations.archives.get())
    setRequired(Callable {
        gradle.taskGraph.hasTask("publishPlugins")
    })
}
